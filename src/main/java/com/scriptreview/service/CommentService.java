package com.scriptreview.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scriptreview.dto.CommentDto;
import com.scriptreview.dto.ScriptCommentsHistoryDto;
import com.scriptreview.dto.ScriptDto;
import com.scriptreview.dto.UserDto;
import com.scriptreview.model.Comment;
import com.scriptreview.model.Script;
import com.scriptreview.model.User;
import com.scriptreview.repository.CommentRepository;
import com.scriptreview.repository.ScriptRepository;
import com.scriptreview.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {
	@Autowired
	private CommentRepository commentRepository;
	@Autowired
	private ScriptRepository scriptRepository;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private SimpMessagingTemplate messagingTemplate;

	@Transactional
	public CommentDto createComment(Comment comment, Long scriptId, Long userId) {
		Script script = scriptRepository.findById(scriptId).orElseThrow(() -> new RuntimeException("Script not found"));
		User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

		log.info("Création d'un commentaire - User ID: {}, Script ID: {}, Auteur du script: {}", 
			userId, scriptId, script.getAuthor().getId());
		
		// Vérifier que l'utilisateur est soit l'auteur, soit un reviewer assigné
		boolean isAuthor = script.getAuthor().getId().equals(userId);
		boolean isAssignedReviewer = script.getAssignedReviewers().stream()
				.anyMatch(reviewer -> reviewer.getId().equals(userId));
		
		if (!isAuthor && !isAssignedReviewer) {
			log.error("Accès refusé - User ID: {} n'est ni l'auteur ni un reviewer assigné du script ID: {}", userId, scriptId);
			throw new RuntimeException("Seuls l'auteur et les reviewers assignés peuvent ajouter des commentaires");
		}
		
		comment.setScript(script);
		comment.setUser(user);
		comment.setCreatedAt(LocalDateTime.now());
		Comment savedComment = commentRepository.save(comment);
		
		log.info("Commentaire sauvegardé avec ID: {}", savedComment.getId());

		// Broadcast le commentaire à tous les abonnés du script
		messagingTemplate.convertAndSend("/topic/comments/" + scriptId, convertToDto(savedComment));
		log.info("Commentaire envoyé à /topic/comments/{}", scriptId);
		
		boolean isReviewer = isUserAReviewerOfScript(user, script);
		log.info("L'utilisateur est-il un reviewer? {}", isReviewer);
		
		// Si le commentaire vient d'un reviewer, notifier l'auteur du script
		if (isReviewer && !user.getId().equals(script.getAuthor().getId())) {
			log.info("Envoi d'une notification à l'auteur car le commentaire vient d'un reviewer");
			notifyAuthorAboutComment(script, user, savedComment);
			return convertToDto(savedComment);
		} 
		// Si le commentaire vient de l'auteur, notifier tous les reviewers
		else if (user.getId().equals(script.getAuthor().getId())) {
			log.info("Envoi d'une notification aux reviewers car le commentaire vient de l'auteur");
			notifyReviewersAboutComment(script, user, savedComment);
		}
		else {
			log.warn("Aucune notification envoyée, l'utilisateur n'est ni l'auteur ni un reviewer reconnu");
		}

		return convertToDto(savedComment);
	}

	public List<CommentDto> getCommentsByScript(Long scriptId) {
		Script script = scriptRepository.findById(scriptId).orElseThrow(() -> new RuntimeException("Script not found"));
		return commentRepository.findByScriptOrderByCreatedAtDesc(script).stream()
				.map(this::convertToDto)
				.collect(Collectors.toList());
	}

	public List<CommentDto> getCommentsByScriptAndLine(Long scriptId, Integer lineNumber) {
		Script script = scriptRepository.findById(scriptId).orElseThrow(() -> new RuntimeException("Script not found"));
		return commentRepository.findByScriptAndLineNumber(script, lineNumber).stream()
				.map(this::convertToDto)
				.collect(Collectors.toList());
	}

	@Transactional
	public CommentDto updateComment(Long id, String content) {
		Comment comment = commentRepository.findById(id).orElseThrow(() -> new RuntimeException("Comment not found"));
		comment.setContent(content);
		return convertToDto(commentRepository.save(comment));
	}

	public void deleteComment(Long id) {
		commentRepository.deleteById(id);
	}
	
	/**
	 * Récupère l'historique complet des commentaires d'un script,
	 * avec une séparation entre commentaires de l'auteur et commentaires des reviewers
	 */
	@Transactional(readOnly = true)
	public ScriptCommentsHistoryDto getScriptCommentsHistory(Long scriptId) {
		Script script = scriptRepository.findById(scriptId)
				.orElseThrow(() -> new RuntimeException("Script not found with ID: " + scriptId));
		
		// Récupérer les commentaires de l'auteur
		List<CommentDto> authorComments = commentRepository.findAuthorCommentsByScriptId(scriptId)
				.stream()
				.map(this::convertToDto)
				.collect(Collectors.toList());
		
		// Récupérer les commentaires des reviewers
		List<Comment> reviewerCommentsEntities = commentRepository.findReviewerCommentsByScriptId(scriptId);
		
		// Convertir les commentaires des reviewers en DTOs
		List<CommentDto> reviewerCommentsDtos = reviewerCommentsEntities.stream()
				.map(this::convertToDto)
				.collect(Collectors.toList());
		
		// Grouper les commentaires des reviewers par user
		Map<UserDto, List<CommentDto>> reviewerCommentsMap = reviewerCommentsDtos.stream()
				.collect(Collectors.groupingBy(CommentDto::getUser));
		
		// Récupérer tous les commentaires pour grouper par ligne
		List<CommentDto> allComments = commentRepository.findByScriptOrderByCreatedAtDesc(script)
				.stream()
				.map(this::convertToDto)
				.collect(Collectors.toList());
		
		// Grouper par numéro de ligne
		Map<Integer, List<CommentDto>> commentsByLine = allComments.stream()
				.filter(comment -> comment.getLineNumber() != null)
				.collect(Collectors.groupingBy(CommentDto::getLineNumber));
		
		// Récupérer la liste des reviewers assignés
		List<UserDto> assignedReviewers = script.getAssignedReviewers().stream()
				.map(this::convertToUserDto)
				.collect(Collectors.toList());
		
		// Construire le DTO final
		return ScriptCommentsHistoryDto.builder()
				.scriptId(script.getId())
				.scriptTitle(script.getTitle())
				.author(convertToUserDto(script.getAuthor()))
				.authorComments(authorComments)
				.reviewerComments(reviewerCommentsMap)
				.assignedReviewers(assignedReviewers)
				.commentsByLine(commentsByLine)
				.totalAuthorComments(authorComments.size())
				.totalReviewerComments(reviewerCommentsDtos.size())
				.totalComments(allComments.size())
				.build();
	}
	
	/**
	 * Vérifie si un utilisateur a le droit de modifier un commentaire.
	 * Un utilisateur peut modifier uniquement ses propres commentaires.
	 * 
	 * @param commentId ID du commentaire
	 * @param userId ID de l'utilisateur
	 * @return true si l'utilisateur peut modifier le commentaire, false sinon
	 */
	public boolean canUserEditComment(Long commentId, Long userId) {
		Comment comment = commentRepository.findById(commentId)
				.orElseThrow(() -> new RuntimeException("Comment not found with ID: " + commentId));
		
		return comment.getUser().getId().equals(userId);
	}
	
	/**
	 * Détermine si un utilisateur est l'auteur du script ou un reviewer.
	 * Utilisé pour vérifier quelle colonne l'utilisateur peut modifier.
	 * 
	 * @param scriptId ID du script
	 * @param userId ID de l'utilisateur
	 * @return "author" si l'utilisateur est l'auteur, "reviewer" s'il est reviewer, null sinon
	 */
	@Transactional(readOnly = true)
	public String getUserRoleForScript(Long scriptId, Long userId) {
		Script script = scriptRepository.findById(scriptId)
				.orElseThrow(() -> new RuntimeException("Script not found with ID: " + scriptId));
		
		// Vérifier si l'utilisateur est l'auteur
		if (script.getAuthor().getId().equals(userId)) {
			return "author";
		}
		
		// Vérifier si l'utilisateur est un reviewer assigné
		for (User reviewer : script.getAssignedReviewers()) {
			if (reviewer.getId().equals(userId)) {
				return "reviewer";
			}
		}
		
		return null; // L'utilisateur n'a pas de rôle pour ce script
	}
	
	/**
	 * Modifie un commentaire si l'utilisateur a les droits appropriés
	 */
	@Transactional
	public CommentDto updateComment(Long id, String content, Long userId) {
		Comment comment = commentRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Comment not found"));
		
		// Vérifier que l'utilisateur est bien le propriétaire du commentaire
		if (!comment.getUser().getId().equals(userId)) {
			throw new RuntimeException("You don't have permission to edit this comment");
		}
		
		comment.setContent(content);
		return convertToDto(commentRepository.save(comment));
	}
	
	/**
	 * Supprime un commentaire si l'utilisateur a les droits appropriés
	 */
	@Transactional
	public void deleteComment(Long id, Long userId) {
		Comment comment = commentRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Comment not found"));
		
		// Vérifier que l'utilisateur est bien le propriétaire du commentaire
		if (!comment.getUser().getId().equals(userId)) {
			throw new RuntimeException("You don't have permission to delete this comment");
		}
		
		commentRepository.deleteById(id);
	}
	
	/**
	 * Récupère les commentaires faits par un utilisateur spécifique sur un script
	 */
	@Transactional(readOnly = true)
	public List<CommentDto> getCommentsByScriptAndUser(Long scriptId, Long userId) {
		return commentRepository.findCommentsByScriptIdAndUserId(scriptId, userId)
				.stream()
				.map(this::convertToDto)
				.collect(Collectors.toList());
	}

	private CommentDto convertToDto(Comment comment) {
		return CommentDto.builder()
				.id(comment.getId())
				.content(comment.getContent())
				.user(convertToUserDto(comment.getUser()))
				.script(convertToScriptDto(comment.getScript()))
				.createdAt(comment.getCreatedAt())
				.lineNumber(comment.getLineNumber())
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

	private ScriptDto convertToScriptDto(Script script) {
		return ScriptDto.builder()
				.id(script.getId())
				.title(script.getTitle())
				.build();
	}

	/**
	 * Vérifie si un utilisateur est un reviewer assigné à un script
	 */
	private boolean isUserAReviewerOfScript(User user, Script script) {
		log.info("Vérification si l'utilisateur {} est un reviewer du script {}", 
			user.getEmail(), script.getTitle());
		
		boolean isReviewer = script.getAssignedReviewers().stream()
			.anyMatch(reviewer -> reviewer.getId().equals(user.getId()));
		
		log.info("Résultat de la vérification: {}", isReviewer);
		log.info("Liste des reviewers assignés: {}", 
			script.getAssignedReviewers().stream()
				.map(r -> r.getId() + "-" + r.getEmail())
				.collect(Collectors.joining(", ")));
		
		return isReviewer;
	}

	/**
	 * Envoie une notification à l'auteur du script lorsqu'un reviewer ajoute un commentaire
	 */
	private void notifyAuthorAboutComment(Script script, User reviewer, Comment comment) {
		User author = script.getAuthor();
		
		// Message de notification
		Map<String, Object> notification = new HashMap<>();
		notification.put("type", "NEW_COMMENT");
		notification.put("scriptId", script.getId());
		notification.put("scriptTitle", script.getTitle());
		notification.put("commentId", comment.getId());
		notification.put("reviewerId", reviewer.getId());
		notification.put("reviewerName", reviewer.getFirstname() + " " + reviewer.getLastname());
		notification.put("commentContent", comment.getContent());
		notification.put("timestamp", LocalDateTime.now());
		notification.put("lineNumber", comment.getLineNumber());
		
		// Envoyer la notification à l'auteur
		messagingTemplate.convertAndSendToUser(
			author.getEmail(),
			"/queue/notifications",
			notification
			
		);
		
		// Log pour debugging
		log.info("Notification envoyée à l'auteur {} pour un commentaire du reviewer {} sur le script {}", 
			author.getEmail(), reviewer.getEmail(), script.getTitle());
		
	}
	

	/**
	 * Envoie une notification aux reviewers lorsque l'auteur du script ajoute un commentaire
	 */
	private void notifyReviewersAboutComment(Script script, User author, Comment comment) {
		// Pour chaque reviewer assigné au script
		for (User reviewer : script.getAssignedReviewers()) {
			// Message de notification
			Map<String, Object> notification = new HashMap<>();
			notification.put("type", "AUTHOR_COMMENT");
			notification.put("scriptId", script.getId());
			notification.put("scriptTitle", script.getTitle());
			notification.put("commentId", comment.getId());
			notification.put("authorId", author.getId());
			notification.put("authorName", author.getFirstname() + " " + author.getLastname());
			notification.put("commentContent", comment.getContent());
			notification.put("timestamp", LocalDateTime.now());
			notification.put("lineNumber", comment.getLineNumber());
			
			// Envoyer la notification au reviewer
			messagingTemplate.convertAndSendToUser(
				reviewer.getEmail(),
				"/queue/notifications",
				notification
			);
			
			// Log pour debugging
			log.info("Notification envoyée au reviewer {} pour un commentaire de l'auteur {} sur le script {}", 
				reviewer.getEmail(), author.getEmail(), script.getTitle());
		}
	}
}