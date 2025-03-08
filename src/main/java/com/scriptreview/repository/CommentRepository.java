package com.scriptreview.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.scriptreview.model.Comment;
import com.scriptreview.model.Script;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
	List<Comment> findByScript(Script script);

	List<Comment> findByScriptOrderByCreatedAtDesc(Script script);

	List<Comment> findByScriptAndLineNumber(Script script, Integer lineNumber);
}