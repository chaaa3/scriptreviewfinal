package com.scriptreview.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevisionHistoryDto {
	private Long id;
	private String oldStatus;
	private String newStatus;
	private String comment;
	private LocalDateTime modifiedAt;
	private UserDto user;
}