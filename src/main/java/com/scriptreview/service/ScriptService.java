package com.scriptreview.service;

import com.scriptreview.dto.ScriptDto;
import com.scriptreview.dto.UserDto;
import com.scriptreview.model.Script;
import com.scriptreview.model.ScriptStatus;
import com.scriptreview.model.User;
import com.scriptreview.repository.ScriptRepository;
import com.scriptreview.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ScriptService {
    @Autowired
    private  ScriptRepository scriptRepository;
    @Autowired
    private  UserRepository userRepository;
    
    @Transactional
    public ScriptDto createScript(Script script, Long authorId) {
        System.out.println("Attempting to create script with authorId: " + authorId);
        System.out.println("Script details - Title: " + script.getTitle() + ", Content: " + script.getContent());
        
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new RuntimeException("Author not found"));
        
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
        return scriptRepository.findById(id)
                .map(this::convertToDto)
                .orElseThrow(() -> new RuntimeException("Script not found"));
    }
    
    public List<ScriptDto> getAllScripts() {
        return scriptRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public List<ScriptDto> getScriptsByAuthor(Long authorId) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new RuntimeException("Author not found"));
        return scriptRepository.findByAuthor(author).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public List<ScriptDto> getScriptsByReviewer(Long reviewerId) {
        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new RuntimeException("Reviewer not found"));
        return scriptRepository.findByAssignedReviewersContaining(reviewer).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public ScriptDto updateScript(Long id, Script scriptDetails) {
        Script script = scriptRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Script not found"));
        
        script.setTitle(scriptDetails.getTitle());
        script.setContent(scriptDetails.getContent());
        script.setStatus(scriptDetails.getStatus());
        
        return convertToDto(scriptRepository.save(script));
    }
    
    @Transactional
    public ScriptDto assignReviewer(Long scriptId, Long reviewerId) {
        Script script = scriptRepository.findById(scriptId)
                .orElseThrow(() -> new RuntimeException("Script not found"));
        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new RuntimeException("Reviewer not found"));
        
        script.getAssignedReviewers().add(reviewer);
        return convertToDto(scriptRepository.save(script));
    }
    
    @Transactional
    public ScriptDto updateStatus(Long scriptId, ScriptStatus status) {
        Script script = scriptRepository.findById(scriptId)
                .orElseThrow(() -> new RuntimeException("Script not found"));
        
        script.setStatus(status);
        return convertToDto(scriptRepository.save(script));
    }
    
    public void deleteScript(Long id) {
        scriptRepository.deleteById(id);
    }
    
    private ScriptDto convertToDto(Script script) {
        return ScriptDto.builder()
                .id(script.getId())
                .title(script.getTitle())
                .content(script.getContent())
                .author(convertToUserDto(script.getAuthor()))
                .status(script.getStatus())
                .createdAt(script.getCreatedAt())
                .updatedAt(script.getUpdatedAt())
                .assignedReviewers(script.getAssignedReviewers().stream()
                        .map(this::convertToUserDto)
                        .collect(Collectors.toList()))
                .build();
    }
    
    private UserDto convertToUserDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .firstname(user.getFirstname())
                .lastname(user.getLastname())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
} 