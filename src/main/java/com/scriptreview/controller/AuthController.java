package com.scriptreview.controller;

import com.scriptreview.dto.AuthenticationRequest;
import com.scriptreview.dto.AuthenticationResponse;
import com.scriptreview.dto.RegisterRequest;
import com.scriptreview.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
@Tag(name = "Authentication", description = "API d'authentification")
public class AuthController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthenticationService service;

    @Operation(
            description = "Endpoint pour enregistrer un nouvel utilisateur",
            summary = "Créer un nouveau compte utilisateur",
            responses = {
                    @ApiResponse(
                            description = "Success",
                            responseCode = "200"
                    ),
                    @ApiResponse(
                            description = "Bad Request",
                            responseCode = "400"
                    )
            }
    )
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

    @Operation(
            description = "Endpoint pour s'authentifier",
            summary = "Se connecter à l'application",
            responses = {
                    @ApiResponse(
                            description = "Success",
                            responseCode = "200"
                    ),
                    @ApiResponse(
                            description = "Identifiants invalides",
                            responseCode = "403"
                    )
            }
    )
    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticationResponse> authenticate(
            @RequestBody AuthenticationRequest request
    ) {
        return ResponseEntity.ok(service.authenticate(request));
    }
} 