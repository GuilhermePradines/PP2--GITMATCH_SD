
package br.com.gitmatch.gitmatch.controller.email;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/email")
public class EmailController {

    @Autowired
    private JavaMailSender mailSender;

    @PostMapping("/enviar")
    public ResponseEntity<?> enviarEmail(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String mensagem = request.get("mensagem");

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("Mensagem do GitMatch");
            message.setText(mensagem);
            message.setFrom("gitmatch4@gmail.com");
            mailSender.send(message);

            return ResponseEntity.ok("E-mail enviado para: " + email);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erro ao enviar e-mail: " + e.getMessage());
        }
    }
}
