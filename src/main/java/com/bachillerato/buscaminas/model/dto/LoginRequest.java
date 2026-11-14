package com.bachillerato.buscaminas.model.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "El usuario o correo es obligatorio")
        String identificador,

        @NotBlank(message = "La contraseña es obligatoria")
        String password
) {}