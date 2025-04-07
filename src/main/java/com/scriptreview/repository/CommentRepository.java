package com.scriptreview.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.scriptreview.model.Comment;
import com.scriptreview.model.Script;


@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
	List<Comment> findByScript(Script script);

	List<Comment> findByScriptOrderByCreatedAtDesc(Script script);

	List<Comment> findByScriptAndLineNumber(Script script, Integer lineNumber);

	List<Comment> findByScriptIdOrderByCreatedAtDesc(Long scriptId);
	
	/**
	 * Récupère les commentaires faits par l'auteur du script
	 */
	@Query("SELECT c FROM Comment c WHERE c.script.id = :scriptId AND c.user.id = c.script.author.id ORDER BY c.createdAt DESC")
	List<Comment> findAuthorCommentsByScriptId(@Param("scriptId") Long scriptId);
	
	/**
	 * Récupère les commentaires faits par les reviewers du script
	 */
	@Query("SELECT c FROM Comment c WHERE c.script.id = :scriptId AND c.user.id != c.script.author.id ORDER BY c.createdAt DESC")
	List<Comment> findReviewerCommentsByScriptId(@Param("scriptId") Long scriptId);
	
	/**
	 * Récupère les commentaires faits par un utilisateur spécifique sur un script
	 */
	@Query("SELECT c FROM Comment c WHERE c.script.id = :scriptId AND c.user.id = :userId ORDER BY c.createdAt DESC")
	List<Comment> findCommentsByScriptIdAndUserId(@Param("scriptId") Long scriptId, @Param("userId") Long userId);
}