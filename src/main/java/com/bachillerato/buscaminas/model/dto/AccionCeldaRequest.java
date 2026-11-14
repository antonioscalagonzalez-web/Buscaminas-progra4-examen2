package com.bachillerato.buscaminas.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AccionCeldaRequest(
        @NotNull(message = "La fila es obligatoria")
        @Min(value = 0, message = "La fila no puede ser negativa")
        Integer fila,

        @NotNull(message = "La columna es obligatoria")
        @Min(value = 0, message = "La columna no puede ser negativa")
        Integer columna
) {}