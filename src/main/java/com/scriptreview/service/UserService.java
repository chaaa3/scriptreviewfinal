package com.scriptreview.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.scriptreview.dto.UserDto;
import com.scriptreview.model.User;
import com.scriptreview.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {
	@Autowired
	private  UserRepository userRepository;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		return userRepository.findByEmail(username).orElseThrow(() -> new UsernameNotFoundException("User not found"));
	}

	public UserDto createUser(User user) {
		if (userRepository.existsByEmail(user.getEmail())) {
			throw new RuntimeException("Email already exists");
		}
		return convertToDto(userRepository.save(user));
	}

	public UserDto getUserById(Long id) {
		return userRepository.findById(id).map(this::convertToDto)
				.orElseThrow(() -> new RuntimeException("User not found"));
	}

	public List<UserDto> getAllUsers() {
		return userRepository.findAll().stream().map(this::convertToDto).collect(Collectors.toList());
	}

	public UserDto updateUser(Long id, User userDetails) {
		User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));

		user.setFirstname(userDetails.getFirstname());
		user.setLastname(userDetails.getLastname());
		user.setEmail(userDetails.getEmail());

		return convertToDto(userRepository.save(user));
	}

	public void deleteUser(Long id) {
		userRepository.deleteById(id);
	}

	private UserDto convertToDto(User user) {
		return UserDto.builder().id(user.getId()).firstname(user.getFirstname()).lastname(user.getLastname())
				.email(user.getEmail()).role(user.getRole()).build();
	}
}