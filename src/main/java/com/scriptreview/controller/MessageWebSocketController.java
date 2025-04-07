package com.scriptreview.controller;

import com.scriptreview.dto.MessageDto;
import com.scriptreview.model.User;
import com.scriptreview.service.ChatService;
import com.scriptreview.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;

/**
 * Contrôleur WebSocket pour gérer les messages entre auteurs et reviewers
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class MessageWebSocketController {

    private final ChatService chatService;
    private final UserService userService;

    /**
     * Gère l'envoi des messages pour un script spécifique.
     * L'ID du script est inclus dans le chemin pour un meilleur routage et sécurité.
     */
    @MessageMapping("/script/{scriptId}/message.send")
    @Transactional
    public void sendMessage(@DestinationVariable Long scriptId, @Payload MessageDto message, Principal principal) {
        log.info("Receiving message for script {}: '{}' from user: {}", 
                 scriptId,
                 message.getContent() != null ? message.getContent().substring(0, Math.min(message.getContent().length(), 50)) : null,
                 principal != null ? principal.getName() : "unknown");

        try {
            // Validation de l'utilisateur
            if (principal == null) {
                log.error("Unauthorized: No authenticated user found");
                return;
            }

            // Récupérer l'utilisateur
            User sender = userService.getUserByEmail(principal.getName());
            
            // Déléguer au service pour le traitement et l'envoi
            chatService.sendMessage(message, scriptId, sender);
            
        } catch (Exception e) {
            log.error("Error processing message for script {}", scriptId, e);
        }
    }
} 