package com.scriptreview.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "scripts")
public class Script {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotNull
    private String title;
    @NotNull
    @Column(columnDefinition = "TEXT")
    private String content;
    
    @ManyToOne
    @JoinColumn(name = "author_id")
    private User author;
    
    @Enumerated(EnumType.STRING)
    private ScriptStatus status;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    


	@ManyToMany
    @JoinTable(
        name = "script_reviewers",
        joinColumns = @JoinColumn(name = "script_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> assignedReviewers = new ArrayList<>();
    
    @OneToMany(mappedBy = "script", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();
    @OneToMany(mappedBy = "script", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties("script") // Ignorer la sérialisation de cette propriété
    private List<RevisionHistory> revisionHistory = new ArrayList<>();
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public User getAuthor() {
		return author;
	}

	public void setAuthor(User author) {
		this.author = author;
	}

	public ScriptStatus getStatus() {
		return status;
	}

	public void setStatus(ScriptStatus status) {
		this.status = status;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}

	public List<User> getAssignedReviewers() {
		return assignedReviewers;
	}

	public void setAssignedReviewers(List<User> assignedReviewers) {
		this.assignedReviewers = assignedReviewers;
	}

	public List<Comment> getComments() {
		return comments;
	}
	
	
	public void setComments(List<Comment> comments) {
		this.comments = comments;
	}
} 