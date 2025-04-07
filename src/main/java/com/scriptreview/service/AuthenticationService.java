package com.scriptreview.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.scriptreview.config.JwtService;
import com.scriptreview.dto.AuthenticationRequest;
import com.scriptreview.dto.AuthenticationResponse;
import com.scriptreview.dto.ChangePasswordRequest;
import com.scriptreview.dto.RegisterRequest;
import com.scriptreview.model.User;
import com.scriptreview.repository.UserRepository;
import com.scriptreview.model.Role;
import java.util.HashMap;
import java.util.Map;

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
                throw new IllegalArgumentException("Role invalide. Les valeurs possibles sont: AUTEUR, REVIEWER, ADMIN,RESPONSABLE");
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
                
        String jwtToken = generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        
        saveUserToken(user, jwtToken);
        
        return AuthenticationResponse.builder()
                .token(jwtToken)
                .refreshToken(refreshToken)
                .build();
    }
    
    private String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        return jwtService.generateToken(claims, user);
    }

    public AuthenticationResponse changePassword(String token, ChangePasswordRequest request) {
        // Extraire l'email de l'utilisateur du token
        String userEmail = jwtService.extractUsername(token.substring(7)); // Enlever "Bearer "
        
        User user = repository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Vérifier le mot de passe actuel
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }

        // Mettre à jour le mot de passe
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        repository.save(user);

        // Générer de nouveaux tokens
        String newToken = jwtService.generateToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);

        return AuthenticationResponse.builder()
                .token(newToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    private void saveUserToken(User user, String jwtToken) {
        // Exemple d'implémentation pour suivre les tokens
        // Dans une application de production, vous pourriez stocker cela en base de données
        // ou utiliser une autre stratégie de gestion des tokens
        log.info("Token généré pour l'utilisateur {}: {}", user.getEmail(), jwtToken);
    }
} 