package com.scriptreview.dto;

import com.scriptreview.model.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationDto {
    private Long id;
    private UserDto user;
    private String message;
    private LocalDateTime createdAt;
    private boolean read;
    private ScriptDto script;
    private NotificationType type;
} 