package com.bachillerato.buscaminas.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistroRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 80, message = "El nombre no puede superar 80 caracteres")
        String nombre,

        @NotBlank(message = "El nombre de usuario es obligatorio")
        @Size(min = 3, max = 40, message = "El usuario debe tener entre 3 y 40 caracteres")
        String nombreUsuario,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "Formato de correo inválido")
        @Size(max = 120, message = "El correo no puede superar 120 caracteres")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 6, max = 64, message = "La contraseña debe tener entre 6 y 64 caracteres")
        String password
) {}