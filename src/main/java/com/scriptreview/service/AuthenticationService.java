package com.scriptreview.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.scriptreview.config.JwtService;
import com.scriptreview.dto.AuthenticationRequest;
import com.scriptreview.dto.AuthenticationResponse;
import com.scriptreview.dto.RegisterRequest;
import com.scriptreview.model.User;
import com.scriptreview.repository.UserRepository;
import com.scriptreview.model.Role;

@Service
public class AuthenticationService {
    
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuthenticationService.class);
    
    @Autowired
    private UserRepository repository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private JwtService jwtService;
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    public AuthenticationResponse register(RegisterRequest request) {
        log.info("Début de l'enregistrement pour l'utilisateur: {} {}", request.getFirstname(), request.getLastname());
        log.info("Role reçu: {}", request.getRole());
        
        try {
            if (request.getRole() == null) {
                log.error("Le rôle est null");
                throw new IllegalArgumentException("Le rôle ne peut pas être null");
            }
            
            // Validation du rôle
            try {
                Role roleEnum = Role.valueOf(request.getRole().toString());
                log.info("Role validé: {}", roleEnum);
            } catch (IllegalArgumentException e) {
                log.error("Role invalide: {}", request.getRole());
                throw new IllegalArgumentException("Role invalide. Les valeurs possibles sont: AUTEUR, REVIEWER, ADMIN");
            }
            
            User user = new User();
            user.setFirstname(request.getFirstname());
            user.setLastname(request.getLastname());
            user.setEmail(request.getEmail());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setRole(request.getRole());
            
            log.info("Tentative de sauvegarde de l'utilisateur avec le role: {}", user.getRole());
            User savedUser = repository.save(user);
            log.info("Utilisateur sauvegardé avec succès, ID: {}", savedUser.getId());
            
            String jwtToken = jwtService.generateToken(savedUser);
            String refreshToken = jwtService.generateRefreshToken(savedUser);
            
            return AuthenticationResponse.builder()
                    .token(jwtToken)
                    .refreshToken(refreshToken)
                    .build();
        } catch (Exception e) {
            log.error("Erreur lors de l'enregistrement: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );
        
        User user = repository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));
                
        String jwtToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        
        return AuthenticationResponse.builder()
                .token(jwtToken)
                .refreshToken(refreshToken)
                .build();
    }
} 