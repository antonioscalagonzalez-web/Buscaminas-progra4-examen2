package com.bachillerato.buscaminas.model.dto;

public record RankingItemResponse(
        Long usuarioId,
        String nombreUsuario,
        String nivel,
        Integer mejorTiempoMs,
        Long partidasTerminadas,
        String ultimoJuego,
        Double promedioMovimientos
) {}