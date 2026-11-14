package com.bachillerato.buscaminas.service;

import com.bachillerato.buscaminas.exception.NegocioException;
import com.bachillerato.buscaminas.model.dto.RankingItemResponse;
import com.bachillerato.buscaminas.repository.PartidaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class RankingService {

    private static final Set<String> NIVELES_VALIDOS = Set.of("PRINCIPIANTE", "INTERMEDIO", "EXPERTO");

    private final PartidaRepository partidaRepository;

    public RankingService(PartidaRepository partidaRepository) {
        this.partidaRepository = partidaRepository;
    }

    @Transactional(readOnly = true)
    public List<RankingItemResponse> obtenerRanking(String nivel) {
        String nivelFiltro = null;

        if (nivel != null && !nivel.isBlank()) {
            nivelFiltro = nivel.trim().toUpperCase();
            if (!NIVELES_VALIDOS.contains(nivelFiltro)) {
                throw new NegocioException(HttpStatus.BAD_REQUEST,
                        "Nivel inválido. Valores permitidos: PRINCIPIANTE, INTERMEDIO, EXPERTO");
            }
        }

        List<Object[]> filas = partidaRepository.consultarRankingPorNivel(nivelFiltro);
        return filas.stream()
                .map(this::mapearFila)
                .toList();
    }

    private RankingItemResponse mapearFila(Object[] fila) {
        Long usuarioId = ((Number) fila[0]).longValue();
        String nombreUsuario = fila[1].toString();
        String nivel = fila[2].toString();
        Integer mejorTiempoMs = fila[3] != null ? ((Number) fila[3]).intValue() : null;
        Long partidasTerminadas = fila[4] != null ? ((Number) fila[4]).longValue() : 0L;
        String ultimoJuego = fila[5] != null ? fila[5].toString() : null;
        Double promedioMovimientos = fila[6] != null ? ((Number) fila[6]).doubleValue() : 0.0;

        return new RankingItemResponse(
                usuarioId,
                nombreUsuario,
                nivel,
                mejorTiempoMs,
                partidasTerminadas,
                ultimoJuego,
                promedioMovimientos
        );
    }
}