package br.com.gitmatch.gitmatch.dto.usuario;


import br.com.gitmatch.gitmatch.enums.TipoUsuario;
import lombok.Data;

@Data
public class UsuarioDTO {
    private String firebaseUid; // Novo campo para vincular ao Firebase
    private String nome;
    private String email;
    private String senha;
    private String fotoPerfil;
    private TipoUsuario tipoUsuario; // "candidato" ou "empresa"
    private String githubUsername; // se candidato
    private String cnpj; // se empres
}