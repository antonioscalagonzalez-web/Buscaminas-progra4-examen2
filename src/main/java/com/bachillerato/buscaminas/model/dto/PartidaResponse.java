package com.bachillerato.buscaminas.model.dto;

import java.util.List;

public record PartidaResponse(
        Long id,
        String nivel,
        String estado,
        int filas,
        int columnas,
        int totalMinas,
        int banderasRestantes,
        short movimientos,
        Integer tiempoMs,
        byte estrellas,
        List<List<String>> tableroVisible
) {}