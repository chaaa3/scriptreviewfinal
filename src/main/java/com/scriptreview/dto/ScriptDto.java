package com.scriptreview.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScriptDto {
    private Long id;
    private String title;
    private String content;
    private UserDto author;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<UserDto> assignedReviewers;
    private List<RevisionHistoryDto> revisionHistory;
} 