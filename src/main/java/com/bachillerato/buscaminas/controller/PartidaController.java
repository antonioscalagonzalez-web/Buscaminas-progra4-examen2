package com.bachillerato.buscaminas.controller;

import com.bachillerato.buscaminas.model.dto.AccionCeldaRequest;
import com.bachillerato.buscaminas.model.dto.NuevaPartidaRequest;
import com.bachillerato.buscaminas.model.dto.PartidaResponse;
import com.bachillerato.buscaminas.service.PartidaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/partidas")
public class PartidaController {

    private final PartidaService partidaService;

    public PartidaController(PartidaService partidaService) {
        this.partidaService = partidaService;
    }

    @PostMapping
    public ResponseEntity<PartidaResponse> iniciarPartida(@Valid @RequestBody NuevaPartidaRequest request,
                                                          Authentication authentication) {
        PartidaResponse partida = partidaService.iniciarPartida(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(partida);
    }

    @GetMapping("/activa")
    public ResponseEntity<PartidaResponse> obtenerActiva(Authentication authentication) {
        PartidaResponse partida = partidaService.obtenerPartidaActiva(authentication.getName());
        return ResponseEntity.ok(partida);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PartidaResponse> obtenerPorId(@PathVariable Long id,
                                                        Authentication authentication) {
        PartidaResponse partida = partidaService.obtenerPartidaPorId(authentication.getName(), id);
        return ResponseEntity.ok(partida);
    }

    @PostMapping("/{id}/revelar")
    public ResponseEntity<PartidaResponse> revelarCelda(@PathVariable Long id,
                                                        @Valid @RequestBody AccionCeldaRequest request,
                                                        Authentication authentication) {
        PartidaResponse partida = partidaService.revelarCelda(authentication.getName(), id, request);
        return ResponseEntity.ok(partida);
    }

    @PostMapping("/{id}/bandera")
    public ResponseEntity<PartidaResponse> alternarBandera(@PathVariable Long id,
                                                           @Valid @RequestBody AccionCeldaRequest request,
                                                           Authentication authentication) {
        PartidaResponse partida = partidaService.alternarBandera(authentication.getName(), id, request);
        return ResponseEntity.ok(partida);
    }

    @PostMapping("/{id}/abandonar")
    public ResponseEntity<PartidaResponse> abandonar(@PathVariable Long id,
                                                     Authentication authentication) {
        PartidaResponse partida = partidaService.abandonarPartida(authentication.getName(), id);
        return ResponseEntity.ok(partida);
    }

    @GetMapping("/historial")
    public ResponseEntity<List<PartidaResponse>> historial(Authentication authentication) {
        return ResponseEntity.ok(partidaService.obtenerHistorial(authentication.getName()));
    }
}