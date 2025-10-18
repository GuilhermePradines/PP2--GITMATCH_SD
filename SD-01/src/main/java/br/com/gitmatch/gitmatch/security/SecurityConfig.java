package br.com.gitmatch.gitmatch.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // desabilita CSRF para testes
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/email/**").permitAll() // libera tudo em /email
                .anyRequest().authenticated() // outras rotas exigem auth
            );

        return http.build();
    }
}
