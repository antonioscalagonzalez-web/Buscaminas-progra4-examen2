package com.bachillerato.buscaminas.service;

import com.bachillerato.buscaminas.model.dto.AccionCeldaRequest;
import com.bachillerato.buscaminas.model.dto.PartidaResponse;
import com.bachillerato.buscaminas.model.entity.EstadoPartida;
import com.bachillerato.buscaminas.model.entity.Partida;
import com.bachillerato.buscaminas.model.entity.Usuario;
import com.bachillerato.buscaminas.repository.PartidaRepository;
import com.bachillerato.buscaminas.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartidaServiceTest {

    @Mock
    private PartidaRepository partidaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    private ObjectMapper objectMapper;
    private PartidaService partidaService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        partidaService = new PartidaService(partidaRepository, usuarioRepository, objectMapper);
    }

    @Test
    void revelarCeldaVacia_debeAbrirEnCadenaCasillasContiguasYFronteraNumerica() throws Exception {
        // Tablero 4x4 con una única mina en (3, 3):
        //  0  0  0  0
        //  0  0  0  0
        //  0  0  1  1
        //  0  0  1  M
        PartidaService.EstadoTablero tablero = new PartidaService.EstadoTablero();
        tablero.setFilas(4);
        tablero.setColumnas(4);
        tablero.setTotalMinas(1);
        tablero.setMinasColocadas(true);

        boolean[][] minas = new boolean[4][4];
        minas[3][3] = true;
        tablero.setMinas(minas);
        tablero.setReveladas(new boolean[4][4]);
        tablero.setBanderas(new boolean[4][4]);

        int[][] adyacentes = {
                {0, 0, 0, 0},
                {0, 0, 0, 0},
                {0, 0, 1, 1},
                {0, 0, 1, 0}
        };
        tablero.setAdyacentes(adyacentes);

        Usuario jugador = new Usuario();
        jugador.setNombreUsuario("jugador1");

        Partida partida = new Partida();
        partida.setUsuario(jugador);
        partida.setNivel("PRINCIPIANTE");
        partida.setEstado(EstadoPartida.EN_CURSO);
        partida.setMovimientos((short) 0);
        partida.setEstrellas((byte) 0);
        partida.setFechaInicio(LocalDateTime.now().minusSeconds(5));
        partida.setTablero(objectMapper.writeValueAsString(tablero));

        when(partidaRepository.findById(1L)).thenReturn(Optional.of(partida));
        when(partidaRepository.save(any(Partida.class))).thenAnswer(inv -> inv.getArgument(0));

        PartidaResponse response = partidaService.revelarCelda("jugador1", 1L, new AccionCeldaRequest(0, 0));
        List<List<String>> visible = response.tablero();

        assertEquals("EN_CURSO", response.estado());
        assertEquals("0", visible.get(0).get(0));
        assertEquals("0", visible.get(1).get(1));
        assertEquals("1", visible.get(2).get(2));
        assertEquals("1", visible.get(2).get(3));
        assertEquals("1", visible.get(3).get(2));
        assertEquals("H", visible.get(3).get(3), "La celda con mina debe permanecer oculta");
    }
}