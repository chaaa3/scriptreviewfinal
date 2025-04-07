package com.scriptreview.controller;

import java.util.Date;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scriptreview.config.JwtService;
import com.scriptreview.config.TokenBlacklistService;
import com.scriptreview.dto.AuthenticationRequest;
import com.scriptreview.dto.AuthenticationResponse;
import com.scriptreview.dto.ChangePasswordRequest;
import com.scriptreview.dto.RegisterRequest;
import com.scriptreview.service.AuthenticationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
@Tag(name = "Authentication", description = "API d'authentification")
public class AuthController {
	@Autowired
	private TokenBlacklistService tokenBlacklistService;
	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuthController.class);
@Autowired
private JwtService jwtService; 
	@Autowired
	private AuthenticationService service;

	@Operation(description = "Endpoint pour enregistrer un nouvel utilisateur", summary = "Créer un nouveau compte utilisateur", responses = {
			@ApiResponse(description = "Success", responseCode = "200"),
			@ApiResponse(description = "Bad Request", responseCode = "400") })
	@PostMapping("/register")
	public ResponseEntity<AuthenticationResponse> register(@RequestBody RegisterRequest request) {
		log.info("Tentative d'enregistrement pour l'email: {}", request.getEmail());
		try {
			AuthenticationResponse response = service.register(request);
			log.info("Enregistrement réussi pour l'email: {}", request.getEmail());
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			log.error("Erreur lors de l'enregistrement pour l'email: {}", request.getEmail(), e);
			throw e;
		}
		
	}
	

	@Operation(description = "Endpoint pour s'authentifier", summary = "Se connecter à l'application", responses = {
			@ApiResponse(description = "Success", responseCode = "200"),
			@ApiResponse(description = "Identifiants invalides", responseCode = "403") })
	@PostMapping("/authenticate")
	public ResponseEntity<AuthenticationResponse> authenticate(@RequestBody AuthenticationRequest request) {
		return ResponseEntity.ok(service.authenticate(request));
	}

	@PostMapping("/logout")
	public ResponseEntity<String> logout(@RequestHeader("Authorization") String authHeader) {
	    if (authHeader != null && authHeader.startsWith("Bearer ")) {
	        String jwt = authHeader.substring(7); // Extraire le token JWT

	        // Extraire la date d'expiration du token
	        Date expirationDate = jwtService.extractExpiration(jwt);

	        // Ajouter le token à la blacklist avec sa date d'expiration
	        tokenBlacklistService.blacklistToken(jwt, expirationDate);

	        return ResponseEntity.ok("Déconnexion réussie");
	    }
	    return ResponseEntity.badRequest().body("Token invalide ou manquant");
	}
	@PutMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody ChangePasswordRequest request) {
        try {
            log.info("Password change request received");
            AuthenticationResponse response = service.changePassword(token, request);
            log.info("Password changed successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error changing password: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }

}