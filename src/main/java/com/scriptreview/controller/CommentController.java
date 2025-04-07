package com.scriptreview.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.scriptreview.dto.CommentDto;
import com.scriptreview.dto.ScriptCommentsHistoryDto;
import com.scriptreview.model.Comment;
import com.scriptreview.service.CommentService;
import com.scriptreview.config.JwtService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class CommentController {
	@Autowired
	private CommentService commentService;

	@Autowired
	private SimpMessagingTemplate messagingTemplate;

	@Autowired
	private JwtService jwtService;

	@PostMapping
	public ResponseEntity<CommentDto> createComment(@RequestBody Comment comment, @RequestParam Long scriptId,
			@RequestHeader("Authorization") String token) {
		Long userId = jwtService.extractUserId(token.replace("Bearer ", ""));
		CommentDto commentDto = commentService.createComment(comment, scriptId, userId);
		
		return ResponseEntity.ok(commentDto);
	}

	@GetMapping("/script/{scriptId}")
	public ResponseEntity<List<CommentDto>> getCommentsByScript(@PathVariable Long scriptId) {
		List<CommentDto> comments = commentService.getCommentsByScript(scriptId);
		return ResponseEntity.ok(comments);
	}

	@GetMapping("/script/{scriptId}/history")
	public ResponseEntity<ScriptCommentsHistoryDto> getScriptCommentsHistory(@PathVariable Long scriptId) {
		ScriptCommentsHistoryDto history = commentService.getScriptCommentsHistory(scriptId);
		return ResponseEntity.ok(history);
	}

	@GetMapping("/script/{scriptId}/user/{userId}")
	public ResponseEntity<List<CommentDto>> getCommentsByScriptAndUser(
			@PathVariable Long scriptId,
			@PathVariable Long userId) {
		List<CommentDto> comments = commentService.getCommentsByScriptAndUser(scriptId, userId);
		return ResponseEntity.ok(comments);
	}

	@GetMapping("/script/{scriptId}/line/{lineNumber}")
	public ResponseEntity<List<CommentDto>> getCommentsByScriptAndLine(@PathVariable Long scriptId,
			@PathVariable Integer lineNumber) {
		List<CommentDto> comments = commentService.getCommentsByScriptAndLine(scriptId, lineNumber);
		return ResponseEntity.ok(comments);
	}

	@PutMapping("/{id}")
	public ResponseEntity<CommentDto> updateComment(
			@PathVariable Long id,
			@RequestBody Comment comment,
			@RequestHeader("Authorization") String token) {
		Long userId = jwtService.extractUserId(token.replace("Bearer ", ""));
		CommentDto commentDto = commentService.updateComment(id, comment.getContent(), userId);
		return ResponseEntity.ok(commentDto);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteComment(
			@PathVariable Long id,
			@RequestHeader("Authorization") String token) {
		Long userId = jwtService.extractUserId(token.replace("Bearer ", ""));
		commentService.deleteComment(id, userId);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/script/{scriptId}/user-role")
	public ResponseEntity<String> getUserRoleForScript(
			@PathVariable Long scriptId,
			@RequestHeader("Authorization") String token) {
		Long userId = jwtService.extractUserId(token.replace("Bearer ", ""));
		String role = commentService.getUserRoleForScript(scriptId, userId);
		if (role == null) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No permission for this script");
		}
		return ResponseEntity.ok(role);
	}

	@GetMapping("/script/{scriptId}/can-edit/{commentId}")
	public ResponseEntity<Boolean> canUserEditComment(
			@PathVariable Long scriptId,
			@PathVariable Long commentId,
			@RequestHeader("Authorization") String token) {
		Long userId = jwtService.extractUserId(token.replace("Bearer ", ""));
		boolean canEdit = commentService.canUserEditComment(commentId, userId);
		return ResponseEntity.ok(canEdit);
	}

	@MessageMapping("/comments")
	public void handleComment(Comment comment) {
		// Process the WebSocket message
		messagingTemplate.convertAndSend("/topic/comments", comment);
	}
}