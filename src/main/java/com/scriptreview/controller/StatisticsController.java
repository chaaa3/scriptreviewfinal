package com.scriptreview.controller;

import com.scriptreview.model.User;
import com.scriptreview.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
@Tag(name = "Statistiques", description = "API pour les statistiques et métriques")
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/scripts")
    @Operation(summary = "Obtenir les statistiques globales des scripts")
    public ResponseEntity<Map<String, Object>> getScriptStatistics() {
        return ResponseEntity.ok(statisticsService.generateScriptStatistics());
    }

    @GetMapping("/users")
    @Operation(summary = "Obtenir les statistiques des utilisateurs")
    public ResponseEntity<Map<String, Object>> getUserStatistics() {
        return ResponseEntity.ok(statisticsService.generateUserStatistics());
    }

    @GetMapping("/performance")
    @Operation(summary = "Obtenir les métriques de performance de l'utilisateur connecté")
    public ResponseEntity<Map<String, Object>> getUserPerformance(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(statisticsService.generateUserPerformanceMetrics(user));
    }
} 