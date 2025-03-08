package com.scriptreview.repository;

import com.scriptreview.model.Script;
import com.scriptreview.model.ScriptVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScriptVersionRepository extends JpaRepository<ScriptVersion, Long> {
    List<ScriptVersion> findByScriptOrderByCreatedAtDesc(Script script);
    ScriptVersion findFirstByScriptOrderByCreatedAtDesc(Script script);
} 