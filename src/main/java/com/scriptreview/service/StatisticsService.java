package com.scriptreview.service;

import com.scriptreview.model.Script;
import com.scriptreview.model.ScriptStatus;
import com.scriptreview.model.User;
import com.scriptreview.repository.CommentRepository;
import com.scriptreview.repository.ScriptRepository;
import com.scriptreview.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final ScriptRepository scriptRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    public Map<String, Object> generateScriptStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        
        // Nombre total de scripts par statut
        Map<ScriptStatus, Long> scriptsByStatus = scriptRepository.findAll().stream()
                .collect(Collectors.groupingBy(Script::getStatus, Collectors.counting()));
        statistics.put("scriptsByStatus", scriptsByStatus);

        // Temps moyen de revue
        double averageReviewTime = scriptRepository.findAll().stream()
                .filter(s -> s.getStatus() == ScriptStatus.REVIEWED)
                .mapToLong(s -> ChronoUnit.HOURS.between(s.getCreatedAt(), s.getUpdatedAt()))
                .average()
                .orElse(0.0);
        statistics.put("averageReviewTimeHours", averageReviewTime);

        // Scripts en attente de revue depuis plus de 7 jours
        long oldPendingScripts = scriptRepository.findAll().stream()
                .filter(s -> s.getStatus() == ScriptStatus.PENDING_REVIEW)
                .filter(s -> ChronoUnit.DAYS.between(s.getCreatedAt(), LocalDateTime.now()) > 7)
                .count();
        statistics.put("oldPendingScripts", oldPendingScripts);

        return statistics;
    }

    public Map<String, Object> generateUserStatistics() {
        Map<String, Object> statistics = new HashMap<>();

        // Nombre de scripts par auteur
        Map<String, Long> scriptsByAuthor = scriptRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        s -> s.getAuthor().getEmail(),
                        Collectors.counting()
                ));
        statistics.put("scriptsByAuthor", scriptsByAuthor);

        // Nombre de commentaires par reviewer
        Map<String, Long> commentsByReviewer = commentRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        c -> c.getUser().getEmail(),
                        Collectors.counting()
                ));
        statistics.put("commentsByReviewer", commentsByReviewer);

        return statistics;
    }

    public Map<String, Object> generateUserPerformanceMetrics(User user) {
        Map<String, Object> metrics = new HashMap<>();

        // Temps moyen de revue pour les scripts assignés
        double averageReviewTime = scriptRepository.findAll().stream()
                .filter(s -> s.getAssignedReviewers().contains(user))
                .filter(s -> s.getStatus() == ScriptStatus.REVIEWED)
                .mapToLong(s -> ChronoUnit.HOURS.between(s.getCreatedAt(), s.getUpdatedAt()))
                .average()
                .orElse(0.0);
        metrics.put("averageReviewTimeHours", averageReviewTime);

        // Nombre de scripts revus
        long reviewedScripts = scriptRepository.findAll().stream()
                .filter(s -> s.getAssignedReviewers().contains(user))
                .filter(s -> s.getStatus() == ScriptStatus.REVIEWED)
                .count();
        metrics.put("reviewedScripts", reviewedScripts);

        return metrics;
    }
} 