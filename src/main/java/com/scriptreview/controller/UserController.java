package com.scriptreview.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import com.scriptreview.dto.UserDto;
import com.scriptreview.model.User;
import com.scriptreview.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@Validated
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class UserController {
	@Autowired
	private UserService userService;

	
	@PostMapping
	public ResponseEntity<UserDto> createUser(@Valid @RequestBody User user) {
		return ResponseEntity.ok(userService.createUser(user));
	}

	
	@GetMapping("/{id}")
	public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
		return ResponseEntity.ok(userService.getUserById(id));
	}

	
	@GetMapping
	public ResponseEntity<List<UserDto>> getAllUsers() {
		return ResponseEntity.ok(userService.getAllUsers());
	}

	
	@PutMapping("/{id}")
	public ResponseEntity<UserDto> updateUser(@PathVariable Long id, @RequestBody User userDetails) {
		return ResponseEntity.ok(userService.updateUser(id, userDetails));
	}
    @PutMapping("/{id}/update/firstname")
	public ResponseEntity<UserDto> updateUserFirstname(@PathVariable Long id ,@RequestBody User userDetails){
	return ResponseEntity.ok(userService.updateUserFirstname(id, userDetails));
			};
			@PutMapping("/{id}/update/lastname")
			public ResponseEntity<UserDto> updateUserLastname(@PathVariable Long id,@RequestBody User userDetails){
				return ResponseEntity.ok(userService.updateUserLastname(id, userDetails));
			}
			@PutMapping("/{id}/update/email")
			public ResponseEntity<UserDto> updateUserEmail(@PathVariable Long id,@RequestBody User userDetails){
				return ResponseEntity.ok(userService.updateUserEmail(id, userDetails));
			}
			@PutMapping("/{id}/update/role")
			public ResponseEntity<UserDto> updateUserRole(@PathVariable Long id,@RequestBody User userDetails){
				return ResponseEntity.ok(userService.updateUserRole(id, userDetails));
			}
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
		userService.deleteUser(id);
		return ResponseEntity.ok().build();
	}
}