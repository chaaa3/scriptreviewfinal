package com.scriptreview.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.scriptreview.dto.CommentDto;
import com.scriptreview.model.Comment;
import com.scriptreview.service.CommentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class CommentController {
	@Autowired
	private CommentService commentService;

	@PostMapping
	public ResponseEntity<CommentDto> createComment(@RequestBody Comment comment, @RequestParam Long scriptId,
			@RequestParam Long userId) {
		return ResponseEntity.ok(commentService.createComment(comment, scriptId, userId));
	}

	@GetMapping("/script/{scriptId}")
	public ResponseEntity<List<CommentDto>> getCommentsByScript(@PathVariable Long scriptId) {
		return ResponseEntity.ok(commentService.getCommentsByScript(scriptId));
	}

	@GetMapping("/script/{scriptId}/line/{lineNumber}")
	public ResponseEntity<List<CommentDto>> getCommentsByScriptAndLine(@PathVariable Long scriptId,
			@PathVariable Integer lineNumber) {
		return ResponseEntity.ok(commentService.getCommentsByScriptAndLine(scriptId, lineNumber));
	}

	@PutMapping("/{id}")
	public ResponseEntity<CommentDto> updateComment(@PathVariable Long id, @RequestParam String content) {
		return ResponseEntity.ok(commentService.updateComment(id, content));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteComment(@PathVariable Long id) {
		commentService.deleteComment(id);
		return ResponseEntity.ok().build();
	}
}