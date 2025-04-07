package com.scriptreview.dto;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO représentant l'historique des commentaires d'un script
 * avec séparation en deux colonnes: auteur et reviewers
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScriptCommentsHistoryDto {
    private Long scriptId;
    private String scriptTitle;
    
    // Informations sur l'auteur
    private UserDto author;
    
    // Commentaires de l'auteur du script (colonne 1)
    private List<CommentDto> authorComments;
    
    // Commentaires des reviewers (colonne 2) - regroupés par reviewer
    private Map<UserDto, List<CommentDto>> reviewerComments;
    
    // Liste des reviewers assignés au script
    private List<UserDto> assignedReviewers;
    
    // Commentaires par ligne (pour visualiser les commentaires sur des lignes spécifiques)
    private Map<Integer, List<CommentDto>> commentsByLine;
    
    // Statistiques
    private Integer totalAuthorComments;
    private Integer totalReviewerComments;
    private Integer totalComments;
} 