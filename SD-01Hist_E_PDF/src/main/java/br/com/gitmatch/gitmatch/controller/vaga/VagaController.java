package br.com.gitmatch.gitmatch.controller.vaga;
import com.itextpdf.text.pdf.draw.LineSeparator;

import br.com.gitmatch.gitmatch.dto.vaga.*;
import br.com.gitmatch.gitmatch.model.usuario.Usuario;
import br.com.gitmatch.gitmatch.model.vaga.Tecnologia;
import br.com.gitmatch.gitmatch.model.vaga.Vaga;
import br.com.gitmatch.gitmatch.repository.usuario.UsuarioRepository;
import br.com.gitmatch.gitmatch.repository.vaga.VagaRepository;
import br.com.gitmatch.gitmatch.service.GitHubLangStats;
import br.com.gitmatch.gitmatch.service.vaga.VagaService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;



import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.http.*;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.*;



import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.stream.Collectors;


import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.io.File;
import java.io.FileOutputStream;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;


@RestController
@RequestMapping("/vaga")
@CrossOrigin(origins = "*")
public class VagaController {
    @PersistenceContext
private EntityManager entityManager;

    @Autowired
    private VagaService vagaService;

    @Autowired
    private VagaRepository vagaRepo;

    @Autowired
    private UsuarioRepository usuarioRepository;


@GetMapping("/candidaturas/usuario/{idUsuario}")
@SuppressWarnings("unchecked")
public ResponseEntity<?> listarCandidaturasUsuario(@PathVariable Long idUsuario) {
    String sql = """
        SELECT 
            e.nome AS nome_empresa,
            c.data_candidatura,
            c.percentual_compatibilidade,
            v.titulo AS titulo_vaga,
            v.descricao AS descricao_vaga,
            STRING_AGG(t.nome, ', ') AS tecnologias_vaga
        FROM candidaturas c
        JOIN vagas v ON c.id_vaga = v.id_vaga
        JOIN usuarios e ON v.id_empresa = e.id_usuario
        LEFT JOIN vaga_tecnologias vt ON v.id_vaga = vt.id_vaga
        LEFT JOIN tecnologias t ON vt.id_tecnologia = t.id_tecnologia
        WHERE c.id_usuario = :idUsuario
        GROUP BY e.nome, c.data_candidatura, c.percentual_compatibilidade, v.titulo, v.descricao
        ORDER BY c.data_candidatura DESC
    """;

    List<Object[]> resultados = entityManager
            .createNativeQuery(sql)
            .setParameter("idUsuario", idUsuario)
            .getResultList();

    List<Map<String, Object>> candidaturas = new ArrayList<>();

    for (Object[] row : resultados) {
        Map<String, Object> item = new HashMap<>();
        item.put("nome_empresa", row[0]);
        item.put("data_candidatura", row[1]);
        item.put("percentual_compatibilidade", row[2]);
        item.put("titulo_vaga", row[3]);
        item.put("descricao_vaga", row[4]);
        item.put("tecnologias_vaga", row[5]);
        candidaturas.add(item);
    }

    return ResponseEntity.ok(candidaturas);
}

@GetMapping("/candidaturas/usuario/{idUsuario}/pdf")
@SuppressWarnings("unchecked")
public ResponseEntity<byte[]> gerarPdfCandidaturas(@PathVariable Long idUsuario) {
    try {
        String sql = """
            SELECT 
                e.nome AS nome_empresa,
                c.data_candidatura,
                c.percentual_compatibilidade,
                v.titulo AS titulo_vaga,
                v.descricao AS descricao_vaga,
                STRING_AGG(t.nome, ', ') AS tecnologias_vaga,
                c.aprovad_boolean
            FROM candidaturas c
            JOIN vagas v ON c.id_vaga = v.id_vaga
            JOIN usuarios e ON v.id_empresa = e.id_usuario
            LEFT JOIN vaga_tecnologias vt ON v.id_vaga = vt.id_vaga
            LEFT JOIN tecnologias t ON vt.id_tecnologia = t.id_tecnologia
            WHERE c.id_usuario = :idUsuario
            GROUP BY e.nome, c.data_candidatura, c.percentual_compatibilidade, v.titulo, v.descricao, c.aprovad_boolean
            ORDER BY c.data_candidatura DESC
        """;

        List<Object[]> resultados = entityManager
                .createNativeQuery(sql)
                .setParameter("idUsuario", idUsuario)
                .getResultList();

        // === Criar PDF em memória ===
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();

        Font tituloFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
        Paragraph titulo = new Paragraph("Relatório de Candidaturas", tituloFont);
        titulo.setAlignment(Element.ALIGN_CENTER);
        titulo.setSpacingAfter(20);
        document.add(titulo);

        for (Object[] row : resultados) {
            String empresa = String.valueOf(row[0]);
            Object data = row[1];
            Object compat = row[2];
            String tituloVaga = String.valueOf(row[3]);
            String descricao = String.valueOf(row[4]);
            String tecnologias = String.valueOf(row[5]);
            Boolean aprovado = row[6] != null ? (Boolean) row[6] : false;

            Paragraph vagaTitulo = new Paragraph("📌 " + tituloVaga, new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD));
            document.add(vagaTitulo);

            Paragraph empresaP = new Paragraph("🏢 Empresa: " + empresa);
            Paragraph compatP = new Paragraph("🎯 Compatibilidade: " + compat + "%");
            Paragraph statusP = new Paragraph("📋 Status: " + (aprovado ? "APROVADO ✅" : "EM ESPERA ⏳"));
            Paragraph tecnologiasP = new Paragraph("💻 Tecnologias: " + tecnologias);
            Paragraph descricaoP = new Paragraph("📝 Descrição: " + descricao);
            descricaoP.setSpacingAfter(15);

            document.add(empresaP);
            document.add(compatP);
            document.add(statusP);
            document.add(tecnologiasP);
            document.add(descricaoP);

            LineSeparator line = new LineSeparator();
            line.setLineColor(BaseColor.LIGHT_GRAY);
            document.add(line);
            document.add(new Paragraph(" "));
        }

        document.close();

        // === Salvar PDF na pasta arquivosPDF ===
        String pasta = "arquivosPDF";
        File dir = new File(pasta);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String nomeArquivo = "candidaturas_usuario_" + idUsuario + ".pdf";
        String caminhoCompleto = pasta + File.separator + nomeArquivo;

        try (FileOutputStream fos = new FileOutputStream(caminhoCompleto)) {
            fos.write(out.toByteArray());
        }

        System.out.println("PDF salvo em: " + caminhoCompleto);

        // === Retornar PDF para download ===
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + nomeArquivo)
                .body(out.toByteArray());

    } catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}


}
