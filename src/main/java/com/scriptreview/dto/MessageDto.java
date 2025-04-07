package com.scriptreview.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageDto {
	private Long scriptId;
	private Long senderId;
	private String senderName;
	private String content;
	private LocalDateTime timestamp;
	private String type;
	private Object payload;
	private List<String> userMentions;
	private List<String> hashtags;
}