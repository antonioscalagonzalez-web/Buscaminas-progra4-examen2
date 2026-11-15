package com.bachillerato.buscaminas.controller;

import com.bachillerato.buscaminas.model.dto.RankingItemResponse;
import com.bachillerato.buscaminas.service.RankingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ranking")
public class RankingController {

    private final RankingService rankingService;

    public RankingController(RankingService rankingService) {
        this.rankingService = rankingService;
    }

    @GetMapping
    public ResponseEntity<List<RankingItemResponse>> obtenerRanking(
            @RequestParam(required = false) String nivel) {
        return ResponseEntity.ok(rankingService.obtenerRanking(nivel));
    }
}