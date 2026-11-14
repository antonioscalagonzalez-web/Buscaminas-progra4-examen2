package com.bachillerato.buscaminas.model.dto;

import jakarta.validation.constraints.NotBlank;

public record NuevaPartidaRequest(
        @NotBlank(message = "El nivel es obligatorio (PRINCIPIANTE, INTERMEDIO, EXPERTO)")
        String nivel
) {}