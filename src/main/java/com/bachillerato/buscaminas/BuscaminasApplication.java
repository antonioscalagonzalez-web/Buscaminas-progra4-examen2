package com.bachillerato.buscaminas;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class BuscaminasApplication {

    public static void main(String[] args) {
        SpringApplication.run(BuscaminasApplication.class, args);
    }

    @Bean
    CommandLineRunner inicializarCredencialesDemo(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        return args -> {
            String hashValido = passwordEncoder.encode("Prueba123$");
            jdbcTemplate.update(
                    "UPDATE usuario SET password_hash = ?, activo = 1 WHERE nombre_usuario IN ('docente', 'jugador1')",
                    hashValido
            );
        };
    }
}