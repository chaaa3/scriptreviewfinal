package com.scriptreview.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "revision_history")
public class RevisionHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "script_id", nullable = false)
    private Script script;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    private ScriptStatus oldStatus;

    @Enumerated(EnumType.STRING)
    private ScriptStatus newStatus;

    private String comment;

    private LocalDateTime modifiedAt;

    @PrePersist
    protected void onCreate() {
        modifiedAt = LocalDateTime.now();
    }
}