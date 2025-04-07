package com.scriptreview.controller;

import com.scriptreview.dto.NotificationDto;
import com.scriptreview.model.Script;
import com.scriptreview.model.ScriptStatus;
import com.scriptreview.repository.ScriptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

/**
 * Cette classe est maintenant dédiée à la diffusion de notifications
 * Elle n'est plus utilisée directement par ScriptService pour éviter une dépendance circulaire
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class StatusNotificationController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ScriptRepository scriptRepository;
    
    // La méthode notifyStatusChange a été déplacée dans ScriptService pour éviter la dépendance circulaire
} 