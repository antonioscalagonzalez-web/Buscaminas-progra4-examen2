package com.bachillerato.buscaminas.service;

import com.bachillerato.buscaminas.exception.NegocioException;
import com.bachillerato.buscaminas.model.dto.AccionCeldaRequest;
import com.bachillerato.buscaminas.model.dto.NuevaPartidaRequest;
import com.bachillerato.buscaminas.model.dto.PartidaResponse;
import com.bachillerato.buscaminas.model.entity.EstadoPartida;
import com.bachillerato.buscaminas.model.entity.Partida;
import com.bachillerato.buscaminas.model.entity.Usuario;
import com.bachillerato.buscaminas.repository.PartidaRepository;
import com.bachillerato.buscaminas.repository.UsuarioRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

@Service
public class PartidaService {

    private final PartidaRepository partidaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ObjectMapper objectMapper;
    private final SecureRandom random = new SecureRandom();

    public PartidaService(PartidaRepository partidaRepository,
                          UsuarioRepository usuarioRepository,
                          ObjectMapper objectMapper) {
        this.partidaRepository = partidaRepository;
        this.usuarioRepository = usuarioRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public PartidaResponse iniciarPartida(String nombreUsuario, NuevaPartidaRequest request) {
        Usuario base = obtenerUsuario(nombreUsuario);
        Usuario usuario = usuarioRepository.findByIdForUpdate(base.getId())
                .orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        if (partidaRepository.findByUsuarioIdAndEstado(usuario.getId(), EstadoPartida.EN_CURSO).isPresent()) {
            throw new NegocioException(HttpStatus.CONFLICT, "Ya tienes una partida en curso. Termínala o abandónala antes de iniciar otra.");
        }

        String nivelNormalizado = request.nivel().trim().toUpperCase();
        EstadoTablero tablero = crearTableroPorNivel(nivelNormalizado);

        Partida partida = new Partida();
        partida.setUsuario(usuario);
        partida.setNivel(nivelNormalizado);
        partida.setEstado(EstadoPartida.EN_CURSO);
        partida.setMovimientos((short) 0);
        partida.setEstrellas((byte) 0);
        partida.setFechaInicio(LocalDateTime.now());
        partida.setTablero(serializarTablero(tablero));

        Partida guardada = partidaRepository.save(partida);
        return mapearAResponse(guardada, tablero);
    }

    @Transactional(readOnly = true)
    public PartidaResponse obtenerPartidaActiva(String nombreUsuario) {
        Usuario usuario = obtenerUsuario(nombreUsuario);
        Partida partida = partidaRepository.findByUsuarioIdAndEstado(usuario.getId(), EstadoPartida.EN_CURSO)
                .orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND, "No tienes ninguna partida en curso"));

        return mapearAResponse(partida, deserializarTablero(partida.getTablero()));
    }

    @Transactional(readOnly = true)
    public PartidaResponse obtenerPartidaPorId(String nombreUsuario, Long partidaId) {
        Partida partida = obtenerYValidarPropiedad(nombreUsuario, partidaId);
        return mapearAResponse(partida, deserializarTablero(partida.getTablero()));
    }

    @Transactional
    public PartidaResponse revelarCelda(String nombreUsuario, Long partidaId, AccionCeldaRequest request) {
        Partida partida = obtenerYValidarPropiedad(nombreUsuario, partidaId);
        validarPartidaEnCurso(partida);

        EstadoTablero tablero = deserializarTablero(partida.getTablero());
        int fila = request.fila();
        int columna = request.columna();
        validarCoordenadas(tablero, fila, columna);

        if (tablero.getBanderas()[fila][columna]) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "No puedes revelar una celda marcada con bandera");
        }

        if (tablero.getReveladas()[fila][columna]) {
            return mapearAResponse(partida, tablero);
        }

        if (!tablero.isMinasColocadas()) {
            colocarMinas(tablero, fila, columna);
            tablero.setMinasColocadas(true);
        }

        partida.setMovimientos((short) (partida.getMovimientos() + 1));

        if (tablero.getMinas()[fila][columna]) {
            revelarTodasLasMinas(tablero);
            LocalDateTime fin = LocalDateTime.now();
            partida.setEstado(EstadoPartida.TERMINADA);
            partida.setFechaFin(fin);
            partida.setTiempoMs(calcularTiempoMs(partida.getFechaInicio(), fin));
            partida.setEstrellas((byte) 0);
            partida.setTablero(serializarTablero(tablero));
            return mapearAResponse(partidaRepository.save(partida), tablero);
        }

        revelarEnCascada(tablero, fila, columna);

        if (verificarVictoria(tablero)) {
            LocalDateTime fin = LocalDateTime.now();
            int tiempoMs = calcularTiempoMs(partida.getFechaInicio(), fin);
            partida.setEstado(EstadoPartida.TERMINADA);
            partida.setFechaFin(fin);
            partida.setTiempoMs(tiempoMs);
            partida.setEstrellas(calcularEstrellas(partida.getNivel(), tiempoMs));
        }

        partida.setTablero(serializarTablero(tablero));
        return mapearAResponse(partidaRepository.save(partida), tablero);
    }

    @Transactional
    public PartidaResponse alternarBandera(String nombreUsuario, Long partidaId, AccionCeldaRequest request) {
        Partida partida = obtenerYValidarPropiedad(nombreUsuario, partidaId);
        validarPartidaEnCurso(partida);

        EstadoTablero tablero = deserializarTablero(partida.getTablero());
        int fila = request.fila();
        int columna = request.columna();
        validarCoordenadas(tablero, fila, columna);

        if (tablero.getReveladas()[fila][columna]) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "No puedes colocar una bandera en una celda ya revelada");
        }

        boolean estadoActual = tablero.getBanderas()[fila][columna];
        tablero.getBanderas()[fila][columna] = !estadoActual;

        partida.setTablero(serializarTablero(tablero));
        return mapearAResponse(partidaRepository.save(partida), tablero);
    }

    @Transactional
    public PartidaResponse abandonarPartida(String nombreUsuario, Long partidaId) {
        Partida partida = obtenerYValidarPropiedad(nombreUsuario, partidaId);
        validarPartidaEnCurso(partida);

        EstadoTablero tablero = deserializarTablero(partida.getTablero());
        revelarTodasLasMinas(tablero);

        LocalDateTime fin = LocalDateTime.now();
        partida.setEstado(EstadoPartida.ABANDONADA);
        partida.setFechaFin(fin);
        partida.setTiempoMs(calcularTiempoMs(partida.getFechaInicio(), fin));
        partida.setEstrellas((byte) 0);
        partida.setTablero(serializarTablero(tablero));

        return mapearAResponse(partidaRepository.save(partida), tablero);
    }

    @Transactional(readOnly = true)
    public List<PartidaResponse> obtenerHistorial(String nombreUsuario) {
        Usuario usuario = obtenerUsuario(nombreUsuario);
        return partidaRepository.findHistorialPorUsuario(usuario.getId()).stream()
                .map(p -> mapearAResponse(p, deserializarTablero(p.getTablero())))
                .toList();
    }

    private Usuario obtenerUsuario(String nombreUsuario) {
        return usuarioRepository.findByNombreUsuario(nombreUsuario)
                .orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    private Partida obtenerYValidarPropiedad(String nombreUsuario, Long partidaId) {
        Partida partida = partidaRepository.findById(partidaId)
                .orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND, "Partida no encontrada"));

        if (!partida.getUsuario().getNombreUsuario().equals(nombreUsuario)) {
            throw new NegocioException(HttpStatus.FORBIDDEN, "No tienes permiso para acceder a esta partida");
        }
        return partida;
    }

    private void validarPartidaEnCurso(Partida partida) {
        if (partida.getEstado() != EstadoPartida.EN_CURSO) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "La partida ya finalizó y no admite más acciones");
        }
    }

    private void validarCoordenadas(EstadoTablero tablero, int fila, int columna) {
        if (fila < 0 || fila >= tablero.getFilas() || columna < 0 || columna >= tablero.getColumnas()) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "Coordenadas fuera de los límites del tablero");
        }
    }

    private EstadoTablero crearTableroPorNivel(String nivel) {
        int filas;
        int columnas;
        int minas;

        switch (nivel) {
            case "PRINCIPIANTE" -> {
                filas = 9;
                columnas = 9;
                minas = 10;
            }
            case "INTERMEDIO" -> {
                filas = 16;
                columnas = 16;
                minas = 40;
            }
            case "EXPERTO" -> {
                filas = 16;
                columnas = 30;
                minas = 99;
            }
            default -> throw new NegocioException(HttpStatus.BAD_REQUEST,
                    "Nivel inválido. Valores permitidos: PRINCIPIANTE, INTERMEDIO, EXPERTO");
        }

        EstadoTablero tablero = new EstadoTablero();
        tablero.setFilas(filas);
        tablero.setColumnas(columnas);
        tablero.setTotalMinas(minas);
        tablero.setMinasColocadas(false);
        tablero.setMinas(new boolean[filas][columnas]);
        tablero.setReveladas(new boolean[filas][columnas]);
        tablero.setBanderas(new boolean[filas][columnas]);
        tablero.setAdyacentes(new int[filas][columnas]);
        return tablero;
    }

    private void colocarMinas(EstadoTablero tablero, int filaSegura, int colSegura) {
        int filas = tablero.getFilas();
        int columnas = tablero.getColumnas();
        int colocadas = 0;

        while (colocadas < tablero.getTotalMinas()) {
            int r = random.nextInt(filas);
            int c = random.nextInt(columnas);

            boolean zonaSegura = Math.abs(r - filaSegura) <= 1 && Math.abs(c - colSegura) <= 1;
            if (!zonaSegura && !tablero.getMinas()[r][c]) {
                tablero.getMinas()[r][c] = true;
                colocadas++;
            }
        }

        int[][] adyacentes = new int[filas][columnas];
        for (int r = 0; r < filas; r++) {
            for (int c = 0; c < columnas; c++) {
                if (!tablero.getMinas()[r][c]) {
                    adyacentes[r][c] = contarMinasVecinas(tablero, r, c);
                }
            }
        }
        tablero.setAdyacentes(adyacentes);
    }

    private int contarMinasVecinas(EstadoTablero tablero, int fila, int col) {
        int contador = 0;
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) {
                    continue;
                }
                int nr = fila + dr;
                int nc = col + dc;
                if (nr >= 0 && nr < tablero.getFilas() && nc >= 0 && nc < tablero.getColumnas()) {
                    if (tablero.getMinas()[nr][nc]) {
                        contador++;
                    }
                }
            }
        }
        return contador;
    }

    private void revelarEnCascada(EstadoTablero tablero, int filaInicial, int colInicial) {
        Queue<int[]> cola = new ArrayDeque<>();
        cola.add(new int[]{filaInicial, colInicial});
        tablero.getReveladas()[filaInicial][colInicial] = true;

        while (!cola.isEmpty()) {
            int[] actual = cola.poll();
            int r = actual[0];
            int c = actual[1];

            if (tablero.getAdyacentes()[r][c] == 0) {
                for (int dr = -1; dr <= 1; dr++) {
                    for (int dc = -1; dc <= 1; dc++) {
                        int nr = r + dr;
                        int nc = c + dc;
                        if (nr >= 0 && nr < tablero.getFilas() && nc >= 0 && nc < tablero.getColumnas()) {
                            if (!tablero.getReveladas()[nr][nc]
                                    && !tablero.getBanderas()[nr][nc]
                                    && !tablero.getMinas()[nr][nc]) {
                                tablero.getReveladas()[nr][nc] = true;
                                cola.add(new int[]{nr, nc});
                            }
                        }
                    }
                }
            }
        }
    }

    private void revelarTodasLasMinas(EstadoTablero tablero) {
        for (int r = 0; r < tablero.getFilas(); r++) {
            for (int c = 0; c < tablero.getColumnas(); c++) {
                if (tablero.getMinas()[r][c]) {
                    tablero.getReveladas()[r][c] = true;
                }
            }
        }
    }

    private boolean verificarVictoria(EstadoTablero tablero) {
        int celdasSeguras = (tablero.getFilas() * tablero.getColumnas()) - tablero.getTotalMinas();
        int reveladasCount = 0;

        for (int r = 0; r < tablero.getFilas(); r++) {
            for (int c = 0; c < tablero.getColumnas(); c++) {
                if (tablero.getReveladas()[r][c] && !tablero.getMinas()[r][c]) {
                    reveladasCount++;
                }
            }
        }
        return reveladasCount == celdasSeguras;
    }

    private int calcularTiempoMs(LocalDateTime inicio, LocalDateTime fin) {
        long ms = Math.max(1L, Duration.between(inicio, fin).toMillis());
        return (int) Math.min(Integer.MAX_VALUE, ms);
    }

    private byte calcularEstrellas(String nivel, int tiempoMs) {
        return switch (nivel) {
            case "PRINCIPIANTE" -> tiempoMs <= 60_000 ? (byte) 3 : (tiempoMs <= 120_000 ? (byte) 2 : (byte) 1);
            case "INTERMEDIO" -> tiempoMs <= 180_000 ? (byte) 3 : (tiempoMs <= 360_000 ? (byte) 2 : (byte) 1);
            case "EXPERTO" -> tiempoMs <= 420_000 ? (byte) 3 : (tiempoMs <= 720_000 ? (byte) 2 : (byte) 1);
            default -> (byte) 1;
        };
    }

    private PartidaResponse mapearAResponse(Partida partida, EstadoTablero tablero) {
        int banderasColocadas = 0;
        List<List<String>> visible = new ArrayList<>();

        for (int r = 0; r < tablero.getFilas(); r++) {
            List<String> filaVisible = new ArrayList<>();
            for (int c = 0; c < tablero.getColumnas(); c++) {
                if (tablero.getBanderas()[r][c]) {
                    banderasColocadas++;
                }

                if (tablero.getReveladas()[r][c]) {
                    if (tablero.getMinas()[r][c]) {
                        filaVisible.add("M");
                    } else {
                        filaVisible.add(String.valueOf(tablero.getAdyacentes()[r][c]));
                    }
                } else if (tablero.getBanderas()[r][c]) {
                    filaVisible.add("F");
                } else {
                    filaVisible.add("H");
                }
            }
            visible.add(filaVisible);
        }

        int banderasRestantes = tablero.getTotalMinas() - banderasColocadas;

        return new PartidaResponse(
                partida.getId(),
                partida.getNivel(),
                partida.getEstado().name(),
                tablero.getFilas(),
                tablero.getColumnas(),
                tablero.getTotalMinas(),
                banderasRestantes,
                partida.getMovimientos(),
                partida.getTiempoMs(),
                partida.getEstrellas(),
                visible
        );
    }

    private String serializarTablero(EstadoTablero tablero) {
        try {
            return objectMapper.writeValueAsString(tablero);
        } catch (JsonProcessingException e) {
            throw new NegocioException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al serializar el estado del tablero");
        }
    }

    private EstadoTablero deserializarTablero(String json) {
        try {
            return objectMapper.readValue(json, EstadoTablero.class);
        } catch (JsonProcessingException e) {
            throw new NegocioException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al leer el estado del tablero");
        }
    }

    public static class EstadoTablero {
        private int filas;
        private int columnas;
        private int totalMinas;
        private boolean minasColocadas;
        private boolean[][] minas;
        private boolean[][] reveladas;
        private boolean[][] banderas;
        private int[][] adyacentes;

        public EstadoTablero() {}

        public int getFilas() { return filas; }
        public void setFilas(int filas) { this.filas = filas; }

        public int getColumnas() { return columnas; }
        public void setColumnas(int columnas) { this.columnas = columnas; }

        public int getTotalMinas() { return totalMinas; }
        public void setTotalMinas(int totalMinas) { this.totalMinas = totalMinas; }

        public boolean isMinasColocadas() { return minasColocadas; }
        public void setMinasColocadas(boolean minasColocadas) { this.minasColocadas = minasColocadas; }

        public boolean[][] getMinas() { return minas; }
        public void setMinas(boolean[][] minas) { this.minas = minas; }

        public boolean[][] getReveladas() { return reveladas; }
        public void setReveladas(boolean[][] reveladas) { this.reveladas = reveladas; }

        public boolean[][] getBanderas() { return banderas; }
        public void setBanderas(boolean[][] banderas) { this.banderas = banderas; }

        public int[][] getAdyacentes() { return adyacentes; }
        public void setAdyacentes(int[][] adyacentes) { this.adyacentes = adyacentes; }
    }
}