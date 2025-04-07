package com.scriptreview.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import com.scriptreview.model.Script;
import com.scriptreview.model.ScriptStatus;
import com.scriptreview.model.User;

@Repository
public interface ScriptRepository extends JpaRepository<Script, Long> {
	List<Script> findByAuthor(User author);

	List<Script> findByStatus(ScriptStatus status);

	@EntityGraph(attributePaths = {"revisionHistory"})
    Optional<Script> findWithRevisionHistoryById(Long id);

	List<Script> findByAssignedReviewersContaining(User reviewer);

	@Query("SELECT s FROM Script s WHERE s.status = ?1 AND ?2 MEMBER OF s.assignedReviewers")
	List<Script> findByStatusAndReviewer(ScriptStatus status, User reviewer);

	@Query("SELECT s FROM Script s LEFT JOIN FETCH s.assignedReviewers LEFT JOIN FETCH s.author WHERE s.id = :id")
	Optional<Script> findByIdWithReviewers(@Param("id") Long id);
}