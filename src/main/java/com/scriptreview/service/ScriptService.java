package com.scriptreview.service;

import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scriptreview.dto.NotificationDto;
import com.scriptreview.dto.RevisionHistoryDto;
import com.scriptreview.dto.ScriptDto;
import com.scriptreview.dto.UserDto;
import com.scriptreview.model.RevisionHistory;
import com.scriptreview.model.Script;
import com.scriptreview.model.ScriptStatus;
import com.scriptreview.model.User;
import com.scriptreview.repository.ScriptRepository;
import com.scriptreview.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ScriptService {
	@Autowired
	private ScriptRepository scriptRepository;
	@Autowired
	private UserRepository userRepository;
	@Lazy
	@Autowired
	private SimpMessagingTemplate messagingTemplate;

	@Transactional
	public ScriptDto createScript(Script script, Long authorId) {
		System.out.println("Attempting to create script with authorId: " + authorId);
		System.out.println("Script details - Title: " + script.getTitle() + ", Content: " + script.getContent());

		User author = userRepository.findById(authorId).orElseThrow(() -> new RuntimeException("Author not found"));

		System.out.println("Found author: " + author.getFirstname() + " " + author.getLastname());

		script.setAuthor(author);
		script.setStatus(ScriptStatus.PENDING_REVIEW);
		script.setAssignedReviewers(new ArrayList<>());
		script.setComments(new ArrayList<>());

		System.out.println("Saving script...");
		Script savedScript = scriptRepository.save(script);
		System.out.println("Script saved successfully with ID: " + savedScript.getId());

		return convertToDto(savedScript);
	}

	public ScriptDto getScriptById(Long id) {
		return scriptRepository.findById(id).map(this::convertToDto)
				.orElseThrow(() -> new RuntimeException("Script not found"));
	}

	public List<ScriptDto> getAllScripts() {
		return scriptRepository.findAll().stream().map(this::convertToDto).collect(Collectors.toList());
	}

	public List<ScriptDto> getScriptsByAuthor(Long authorId) {
		User author = userRepository.findById(authorId).orElseThrow(() -> new RuntimeException("Author not found"));
		return scriptRepository.findByAuthor(author).stream().map(this::convertToDto).collect(Collectors.toList());
	}

	public List<ScriptDto> getScriptsByReviewer(Long reviewerId) {
		User reviewer = userRepository.findById(reviewerId)
				.orElseThrow(() -> new RuntimeException("Reviewer not found"));
		return scriptRepository.findByAssignedReviewersContaining(reviewer).stream().map(this::convertToDto)
				.collect(Collectors.toList());
	}

	@Transactional
	public ScriptDto updateScript(Long id, Script scriptDetails) {
		Script script = scriptRepository.findById(id).orElseThrow(() -> new RuntimeException("Script not found"));

		script.setTitle(scriptDetails.getTitle());
		script.setContent(scriptDetails.getContent());
		script.setStatus(scriptDetails.getStatus());
		script.setUpdatedAt(LocalDateTime.now() );
		return convertToDto(scriptRepository.save(script));
	}

	@Transactional
	public ScriptDto assignReviewer(Long scriptId, Long reviewerId) {
		Script script = scriptRepository.findById(scriptId).orElseThrow(() -> new RuntimeException("Script not found"));
		User reviewer = userRepository.findById(reviewerId)
				.orElseThrow(() -> new RuntimeException("Reviewer not found"));

		script.getAssignedReviewers().add(reviewer);
		return convertToDto(scriptRepository.save(script));
	}

	@Transactional
	public ScriptDto assignAuthor(Long scriptId, Long authorId) {
		Script script = scriptRepository.findById(scriptId)
				.orElseThrow(() -> new RuntimeException("Script not found "));
		User author = userRepository.findById(authorId).orElseThrow(() -> new RuntimeException("Author not found"));
		script.setAuthor(author);
		return convertToDto(scriptRepository.save(script));

	}

	@Transactional
	public ScriptDto removeReviewer(Long scriptId, Long reviewerId) {
		Script script = scriptRepository.findById(scriptId).orElseThrow(() -> new RuntimeException("script not found"));
		User reviewer = userRepository.findById(reviewerId)
				.orElseThrow(() -> new RuntimeException("Reviewer not found"));
		script.getAssignedReviewers().remove(reviewer);
		return convertToDto(scriptRepository.save(script));
	}

	@Transactional
	public ScriptDto updateStatus(Long scriptId, ScriptStatus status, Long userId, String comment) throws AccessDeniedException {
	    // Récupérer l'utilisateur actuel
	    User currentUser = userRepository.findById(userId)
	            .orElseThrow(() -> new RuntimeException("User not found"));
	    
	    // Récupérer le script
	    Script script = scriptRepository.findById(scriptId)
	            .orElseThrow(() -> new RuntimeException("Script not found"));
	    
	    // Vérifier que l'utilisateur est soit l'auteur, soit un reviewer assigné
	    boolean isAuthor = script.getAuthor().getId().equals(userId);
	    boolean isAssignedReviewer = script.getAssignedReviewers().stream()
	            .anyMatch(reviewer -> reviewer.getId().equals(userId));
	    
	    if (!isAuthor && !isAssignedReviewer) {
	        throw new AccessDeniedException("Seuls l'auteur et les reviewers assignés peuvent modifier le statut du script");
	    }

	    // Enregistrer l'ancien statut
	    ScriptStatus oldStatus = script.getStatus();

	    // Mettre à jour le statut
	    script.setStatus(status);

	    // Enregistrer la modification dans l'historique
	    RevisionHistory revision = RevisionHistory.builder()
	            .script(script)
	            .user(currentUser)
	            .oldStatus(oldStatus)
	            .newStatus(status)
	            .comment(comment)
	            .build();

	    script.getRevisionHistory().add(revision);
	    Script savedScript = scriptRepository.save(script);
	    
	    // Envoyer directement la notification au lieu d'utiliser StatusNotificationController
	    // Création de la notification
	    NotificationDto notification = NotificationDto.builder()
		    .message("Le statut du script \"" + script.getTitle() + "\" a été modifié de " + 
		             oldStatus.name() + " à " + status.name())
		    .type("STATUS_CHANGE")
		    .timestamp(LocalDateTime.now())
		    .scriptId(scriptId)
		    .isRead(false)
		    .build();
		
	    // Ajouter les commentaires si présents
	    if (comment != null && !comment.isEmpty()) {
		    notification.setMessage(notification.getMessage() + " avec le commentaire: \"" + comment + "\"");
	    }
	    
	    // Notifier l'auteur (sauf s'il est celui qui a fait la modification)
	    if (!script.getAuthor().getId().equals(userId)) {
		    messagingTemplate.convertAndSendToUser(
		            script.getAuthor().getUsername(),
		            "/queue/notifications",
		            notification
		    );
	    }
	    
	    // Notifier les reviewers (sauf celui qui a fait la modification)
	    script.getAssignedReviewers().forEach(reviewer -> {
		    if (!reviewer.getId().equals(userId)) {
		        messagingTemplate.convertAndSendToUser(
		                reviewer.getUsername(),
		                "/queue/notifications",
		                notification
		        );
		    }
	    });
	    
	    // Envoyer également à tous les utilisateurs abonnés au topic du script
	    messagingTemplate.convertAndSend(
		    "/topic/script." + scriptId + ".status",
		    notification
	    );

	    return convertToDto(savedScript);
	}

	/**
	 * Méthode originale pour la rétrocompatibilité
	 * Cette méthode ne vérifie pas les autorisations et devrait être utilisée uniquement 
	 * dans des contextes où les autorisations ont déjà été vérifiées.
	 */
	@Transactional
	public ScriptDto updateTitle(Long scriptId, String title) {
		Script script = scriptRepository.findById(scriptId).orElseThrow(() -> new RuntimeException("Script not found"));
		script.setTitle(title);
		return convertToDto(scriptRepository.save(script));
	}

	/**
	 * Met à jour le titre d'un script après vérification des autorisations
	 * 
	 * @param scriptId ID du script
	 * @param title Nouveau titre
	 * @param userId ID de l'utilisateur qui fait la modification
	 * @return Le script mis à jour
	 * @throws AccessDeniedException si l'utilisateur n'est pas autorisé à modifier le titre
	 */
	@Transactional
	public ScriptDto updateTitle(Long scriptId, String title, Long userId) throws AccessDeniedException {
		Script script = scriptRepository.findById(scriptId).orElseThrow(() -> new RuntimeException("Script not found"));
		
		// Seul l'auteur peut modifier le titre du script
		if (!script.getAuthor().getId().equals(userId)) {
			throw new AccessDeniedException("Seul l'auteur peut modifier le titre du script");
		}

		script.setTitle(title);
		return convertToDto(scriptRepository.save(script));
	}

	public void deleteScript(Long id) {
		scriptRepository.deleteById(id);
	}
	
	
	@Transactional(readOnly = true)
	public List<RevisionHistoryDto> getRevisionHistory(Long scriptId) {
	    Script script = scriptRepository.findById(scriptId)
	            .orElseThrow(() -> new RuntimeException("Script not found"));
	    return script.getRevisionHistory().stream()
	            .map(this::convertToRevisionHistoryDto)
	            .collect(Collectors.toList());
	}

	private RevisionHistoryDto convertToRevisionHistoryDto(RevisionHistory history) {
	    return RevisionHistoryDto.builder()
	            .id(history.getId())
	            .oldStatus(history.getOldStatus().name())
	            .newStatus(history.getNewStatus().name())
	            .comment(history.getComment())
	            .modifiedAt(history.getModifiedAt())
	            .user(convertToUserDto(history.getUser()))
	            .build();
	}

	private ScriptDto convertToDto(Script script) {
		return ScriptDto.builder()
				.id(script.getId())
				.title(script.getTitle())
				.content(script.getContent())
				.author(convertToUserDto(script.getAuthor()))
				.status(script.getStatus().name())
				.createdAt(script.getCreatedAt())
				.updatedAt(script.getUpdatedAt())
				.assignedReviewers(script.getAssignedReviewers().stream()
						.map(this::convertToUserDto)
						.collect(Collectors.toList()))
				.revisionHistory(script.getRevisionHistory().stream()
						.map(this::convertToRevisionHistoryDto)
						.collect(Collectors.toList()))
				.build();
	}

	private UserDto convertToUserDto(User user) {
		return UserDto.builder()
				.id(user.getId())
				.firstname(user.getFirstname())
				.lastname(user.getLastname())
				.email(user.getEmail())
				.role(user.getRole().name())
				.build();
	}

	@Transactional(readOnly = true)
	public Script getScriptEntityById(Long id) {
		return scriptRepository.findById(id)
			.orElseThrow(() -> new RuntimeException("Script not found"));
	}

	@Transactional(readOnly = true)
	public Script getScriptWithReviewersById(Long id) {
		return scriptRepository.findByIdWithReviewers(id)
			.orElseThrow(() -> new RuntimeException("Script not found with ID: " + id));
	}
}