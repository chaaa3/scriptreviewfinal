package com.scriptreview.controller;

import java.nio.file.AccessDeniedException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;

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

import com.scriptreview.dto.RevisionHistoryDto;
import com.scriptreview.dto.ScriptDto;
import com.scriptreview.model.Script;
import com.scriptreview.model.ScriptStatus;
import com.scriptreview.service.ScriptService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/scripts")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class ScriptController {
	@Autowired
	private ScriptService scriptService;
	
	@Autowired
	private SimpMessagingTemplate messagingTemplate;

	@PostMapping
	
	public ResponseEntity<ScriptDto> createScript(@RequestBody Script script, @RequestParam Long authorId) {
		return ResponseEntity.ok(scriptService.createScript(script, authorId));
	}

	@GetMapping("/{id}")
	public ResponseEntity<ScriptDto> getScriptById(@PathVariable Long id) {
		return ResponseEntity.ok(scriptService.getScriptById(id));
	}

	@GetMapping
	public ResponseEntity<List<ScriptDto>> getAllScripts() {
		return ResponseEntity.ok(scriptService.getAllScripts());
	}

	@GetMapping("/author/{authorId}")
	public ResponseEntity<List<ScriptDto>> getScriptsByAuthor(@PathVariable Long authorId) {
		return ResponseEntity.ok(scriptService.getScriptsByAuthor(authorId));
	}

	@GetMapping("/reviewer/{reviewerId}")
	public ResponseEntity<List<ScriptDto>> getScriptsByReviewer(@PathVariable Long reviewerId) {
		return ResponseEntity.ok(scriptService.getScriptsByReviewer(reviewerId));
	}

	@PutMapping("/{id}")
	public ResponseEntity<ScriptDto> updateScript(@PathVariable Long id, @RequestBody Script scriptDetails) {
		return ResponseEntity.ok(scriptService.updateScript(id, scriptDetails));
	}

	@PutMapping("/{scriptId}/assign/{reviewerId}")
	public ResponseEntity<ScriptDto> assignReviewer(@PathVariable Long scriptId, @PathVariable Long reviewerId) {
		return ResponseEntity.ok(scriptService.assignReviewer(scriptId, reviewerId));
	}

	@PutMapping("/{scriptId}/remove/{reviewerId}")
	public ResponseEntity<ScriptDto> removeReviewer(@PathVariable Long scriptId, @PathVariable Long reviewerId) {
		return ResponseEntity.ok(scriptService.removeReviewer(scriptId, reviewerId));
	}

	@PutMapping("/{scriptId}/assignauthor/{authorId}")
	public ResponseEntity<ScriptDto> assignAuthor(@PathVariable Long scriptId, @PathVariable Long authorId) {
		return ResponseEntity.ok(scriptService.assignAuthor(scriptId, authorId));
	}

	@GetMapping("/{scriptId}/history")
	public ResponseEntity<List<RevisionHistoryDto>> getRevisionHistory(@PathVariable Long scriptId) {
		return ResponseEntity.ok(scriptService.getRevisionHistory(scriptId));
	}

	@PutMapping("/{scriptId}/status")
	public ResponseEntity<ScriptDto> updateStatus(@PathVariable Long scriptId, @RequestParam ScriptStatus status,
			@RequestParam Long userId, @RequestParam(required = false) String comment) throws AccessDeniedException {
		ScriptDto updatedScript = scriptService.updateStatus(scriptId, status, userId, comment);
		
		// La notification est déjà gérée dans le ScriptService
		// Pas besoin de l'envoyer à nouveau ici
		
		return ResponseEntity.ok(updatedScript);
	}

	@PutMapping("/{scriptId}/title")
	public ResponseEntity<ScriptDto> updateTitle(
		@PathVariable Long scriptId, 
		@RequestParam String title,
		@RequestParam Long userId) throws AccessDeniedException {
		return ResponseEntity.ok(scriptService.updateTitle(scriptId, title, userId));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteScript(@PathVariable Long id) {
		scriptService.deleteScript(id);
		return ResponseEntity.ok().build();
	}
}