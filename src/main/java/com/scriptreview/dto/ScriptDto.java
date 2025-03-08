package com.scriptreview.dto;

import com.scriptreview.model.ScriptStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ScriptDto {
    private Long id;
    private String title;
    private String content;
    private UserDto author;
    private ScriptStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<UserDto> assignedReviewers;
    private List<CommentDto> comments;
} 