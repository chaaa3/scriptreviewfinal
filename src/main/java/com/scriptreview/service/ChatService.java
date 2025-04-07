package com.scriptreview.service;

import com.scriptreview.dto.MessageDto;
import com.scriptreview.model.Script;
import com.scriptreview.model.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service pour gérer les messages de chat entre auteurs et reviewers
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final SimpMessagingTemplate messagingTemplate;
    
    private final TagService tagService;
    private final ScriptService scriptService;

    /**
     * Envoie un message aux participants d'un script (auteur et reviewers)
     *
     * @param message  Message à envoyer
     * @param scriptId ID du script
     * @param sender   Utilisateur qui envoie le message
     */
    @Transactional(readOnly = true)
    public void sendMessage(MessageDto message, Long scriptId, User sender) {
        log.debug("Processing message from user {} for script {}", sender.getEmail(), scriptId);

        // Récupérer le script avec ses reviewers
        Script script = scriptService.getScriptWithReviewersById(scriptId);

        // Vérifier si l'expéditeur est l'auteur ou un reviewer
        boolean isAuthor = script.getAuthor().getId().equals(sender.getId());
        boolean isReviewer = script.getAssignedReviewers().stream()
                .anyMatch(reviewer -> reviewer.getId().equals(sender.getId()));

        if (!isAuthor && !isReviewer) {
            log.warn("User {} attempted to send message to script {} without permission", sender.getEmail(), scriptId);
            throw new SecurityException("Unauthorized: User is neither author nor reviewer of the script");
        }

        // Enrichir le message avec les métadonnées
        enrichMessage(message, scriptId, sender, isAuthor);

        // Envoyer le message à tous les participants
        sendToParticipants(message, script);
        
        log.info("Message sent successfully to script {} participants", scriptId);
    }

    /**
     * Enrichit le message avec les métadonnées nécessaires
     */
    private void enrichMessage(MessageDto message, Long scriptId, User sender, boolean isAuthor) {
        message.setScriptId(scriptId);
        message.setSenderId(sender.getId());
        message.setSenderName(sender.getFirstname() + " " + sender.getLastname());
        message.setType(isAuthor ? "AUTHOR" : "REVIEWER");
        message.setTimestamp(LocalDateTime.now());

        // Extraire les mentions et hashtags si le contenu existe
        if (message.getContent() != null) {
            List<String> mentions = tagService.extractMentions(message.getContent());
            List<String> hashtags = tagService.extractHashtags(message.getContent());
            message.setUserMentions(mentions);
            message.setHashtags(hashtags);
        }
    }

    /**
     * Envoie le message à tous les participants du script
     */
    private void sendToParticipants(MessageDto message, Script script) {
        // Topic pour la conversation du script
        String destination = "/topic/script." + script.getId() + ".messages";
        
        // Diffuser le message à tous les participants autorisés
        messagingTemplate.convertAndSend(destination, message);
        
        log.debug("Message broadcast to topic: {}", destination);
    }
    
    /**
     * Envoie un message système dans la conversation du script
     */
    public void sendSystemMessage(Long scriptId, String content) {
        MessageDto systemMessage = MessageDto.builder()
                .scriptId(scriptId)
                .senderName("System")
                .content(content)
                .type("SYSTEM")
                .timestamp(LocalDateTime.now())
                .build();
                
        String destination = "/topic/script." + scriptId + ".messages";
        messagingTemplate.convertAndSend(destination, systemMessage);
    }
} 