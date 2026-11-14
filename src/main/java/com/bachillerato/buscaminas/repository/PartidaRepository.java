package com.bachillerato.buscaminas.repository;

import com.bachillerato.buscaminas.model.entity.EstadoPartida;
import com.bachillerato.buscaminas.model.entity.Partida;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface PartidaRepository extends JpaRepository<Partida, Long> {

    Optional<Partida> findByUsuarioIdAndEstado(Long usuarioId, EstadoPartida estado);

    @Query("SELECT p FROM Partida p WHERE p.usuario.id = :usuarioId ORDER BY p.fechaInicio DESC")
    List<Partida> findHistorialPorUsuario(@Param("usuarioId") Long usuarioId);

    @Query(value = """
            SELECT usuario_id, nombre_usuario, nivel, mejor_tiempo_ms,
                   partidas_terminadas, ultimo_juego, promedio_movimientos
            FROM vw_ranking
            WHERE (:nivel IS NULL OR nivel = :nivel)
            ORDER BY mejor_tiempo_ms ASC
            """, nativeQuery = true)
    List<Object[]> consultarRankingPorNivel(@Param("nivel") String nivel);
}