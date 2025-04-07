package com.scriptreview.controller;

import com.scriptreview.dto.CommentDto;
import com.scriptreview.model.Comment;
import com.scriptreview.model.Script;
import com.scriptreview.repository.ScriptRepository;
import com.scriptreview.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class CommentWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final CommentService commentService;
    private final ScriptRepository scriptRepository;

    @MessageMapping("/comment.add")
    public void addComment(@Payload CommentDto commentDto) {
        // Créer et sauvegarder le commentaire
        CommentDto savedComment = commentService.createComment(
            Comment.builder()
                .content(commentDto.getContent())
                .build(),
            commentDto.getScript().getId(),
            commentDto.getUser().getId()
        );
        
        // Récupérer le script associé
        Script script = scriptRepository.findById(commentDto.getScript().getId())
            .orElseThrow(() -> new RuntimeException("Script not found"));
        
        // Notifier l'auteur du script si le commentaire vient d'un reviewer
        if (!commentDto.getUser().getId().equals(script.getAuthor().getId())) {
            messagingTemplate.convertAndSendToUser(
                script.getAuthor().getUsername(),
                "/queue/notifications",
                "Nouveau commentaire sur votre script: " + script.getTitle()
            );
        }
        
        // Notifier les reviewers si le commentaire vient de l'auteur
        if (commentDto.getUser().getId().equals(script.getAuthor().getId())) {
            script.getAssignedReviewers().forEach(reviewer -> {
                messagingTemplate.convertAndSendToUser(
                    reviewer.getUsername(),
                    "/queue/notifications",
                    "Nouveau commentaire de l'auteur sur le script: " + script.getTitle()
                );
            });
        }
        
        // Envoyer le commentaire à tous les utilisateurs abonnés au script
        messagingTemplate.convertAndSend(
            "/topic/script." + script.getId() + ".comments",
            savedComment
        );
    }
}