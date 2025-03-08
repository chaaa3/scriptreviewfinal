package com.scriptreview.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scriptreview.dto.CommentDto;
import com.scriptreview.dto.UserDto;
import com.scriptreview.model.Comment;
import com.scriptreview.model.Script;
import com.scriptreview.model.User;
import com.scriptreview.repository.CommentRepository;
import com.scriptreview.repository.ScriptRepository;
import com.scriptreview.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommentService {
	@Autowired
	private CommentRepository commentRepository;
	@Autowired
	private  ScriptRepository scriptRepository;
	@Autowired
	private  UserRepository userRepository;

	@Transactional
	public CommentDto createComment(Comment comment, Long scriptId, Long userId) {
		Script script = scriptRepository.findById(scriptId).orElseThrow(() -> new RuntimeException("Script not found"));
		User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

		comment.setScript(script);
		comment.setUser(user);
		return convertToDto(commentRepository.save(comment));
	}

	public List<CommentDto> getCommentsByScript(Long scriptId) {
		Script script = scriptRepository.findById(scriptId).orElseThrow(() -> new RuntimeException("Script not found"));
		return commentRepository.findByScriptOrderByCreatedAtDesc(script).stream().map(this::convertToDto)
				.collect(Collectors.toList());
	}

	public List<CommentDto> getCommentsByScriptAndLine(Long scriptId, Integer lineNumber) {
		Script script = scriptRepository.findById(scriptId).orElseThrow(() -> new RuntimeException("Script not found"));
		return commentRepository.findByScriptAndLineNumber(script, lineNumber).stream().map(this::convertToDto)
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

	private CommentDto convertToDto(Comment comment) {
		return CommentDto.builder().id(comment.getId()).content(comment.getContent())
				.user(convertToUserDto(comment.getUser())).createdAt(comment.getCreatedAt())
				.lineNumber(comment.getLineNumber()).build();
	}

	private UserDto convertToUserDto(User user) {
		return UserDto.builder().id(user.getId()).firstname(user.getFirstname()).lastname(user.getLastname())
				.email(user.getEmail()).role(user.getRole()).build();
	}
}