package com.scriptreview.config;

import com.scriptreview.model.Script;
import com.scriptreview.model.User;
import com.scriptreview.service.ScriptService;
import com.scriptreview.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.security.Principal;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final UserService userService;
    private final ScriptService scriptService;
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;

    // Pattern pour extraire l'ID du script depuis les destinations
    private static final Pattern SCRIPT_PATTERN = Pattern.compile("/topic/script\\.(\\d+)(?:\\..*)?");

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Activer le broker en mémoire pour /topic et /queue
        config.enableSimpleBroker("/topic", "/queue", "/user");
        
        // Préfixe pour les messages destinés aux méthodes @MessageMapping
        config.setApplicationDestinationPrefixes("/app");
        
        // Préfixe pour les destinations spécifiques à l'utilisateur
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Point de terminaison WebSocket avec fallback SockJS
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")  
                .withSockJS()
                .setClientLibraryUrl("https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                SimpMessageHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, SimpMessageHeaderAccessor.class);
                
                if (accessor != null) {
                    // Extraire les informations du message
                    String destination = accessor.getDestination();
                    SimpMessageType messageType = accessor.getMessageType();
                    
                    log.debug("WebSocket message - Type: {}, Destination: {}", messageType, destination);
                    
                    // Vérifier s'il y a un en-tête d'autorisation
                    List<String> authHeaders = accessor.getNativeHeader("Authorization");
                    if (authHeaders != null && !authHeaders.isEmpty()) {
                        String bearerToken = authHeaders.get(0);
                        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
                            try {
                                // Extraire et valider le token
                                String token = bearerToken.substring(7);
                                if (authenticateUser(token, accessor)) {
                                    log.debug("Successfully authenticated WebSocket connection with JWT token");
                                }
                            } catch (Exception e) {
                                log.error("Error authenticating WebSocket connection: {}", e.getMessage());
                            }
                        }
                    }
                    
                    // Pour les messages SUBSCRIBE, vérifier également les autorisations
                    if (messageType == SimpMessageType.SUBSCRIBE && destination != null && destination.startsWith("/topic/script.")) {
                        // Vérifier que l'utilisateur est authentifié 
                        Principal principal = accessor.getUser();
                        if (principal == null || !(principal instanceof Authentication)) {
                            log.warn("Unauthorized WebSocket subscription attempt to {}", destination);
                            return null; // Bloquer la souscription
                        }
                        
                        Authentication auth = (Authentication) principal;
                        if (!(auth instanceof UsernamePasswordAuthenticationToken)) {
                            log.warn("Unsupported authentication type for WebSocket: {}", auth.getClass().getName());
                            return null;
                        }
                        
                        try {
                            // Extraire l'ID du script depuis la destination
                            Matcher matcher = SCRIPT_PATTERN.matcher(destination);
                            if (!matcher.matches()) {
                                log.warn("Invalid script topic format: {}", destination);
                                return null;
                            }
                            
                            Long scriptId = Long.parseLong(matcher.group(1));
                            String username = auth.getName();
                            
                            // Récupérer l'utilisateur et le script avec ses reviewers
                            User user = userService.getUserByEmail(username);
                            Script script = scriptService.getScriptWithReviewersById(scriptId);
                            
                            // Vérifier si l'utilisateur est autorisé (auteur ou reviewer)
                            boolean isAuthorized = script.getAuthor().getId().equals(user.getId()) || 
                                script.getAssignedReviewers().stream()
                                .anyMatch(reviewer -> reviewer.getId().equals(user.getId()));
                            
                            if (!isAuthorized) {
                                log.warn("User {} not authorized to subscribe to script {} topic", username, scriptId);
                                return null; // Bloquer la souscription
                            }
                            
                            log.info("User {} authorized to subscribe to script {} topic", username, scriptId);
                        } catch (Exception e) {
                            log.error("Error during WebSocket subscription authorization", e);
                            return null; // Bloquer par sécurité en cas d'erreur
                        }
                    }
                }
                
                return message;
            }
        });
    }
    
    /**
     * Authentifie un utilisateur à partir d'un token JWT
     * @param token Le token JWT
     * @param accessor L'accesseur d'en-têtes de message
     * @return true si l'authentification réussit, false sinon
     */
    private boolean authenticateUser(String token, SimpMessageHeaderAccessor accessor) {
        try {
            // Extraire le nom d'utilisateur (email) du token
            String userEmail = jwtService.extractUsername(token);
            
            if (userEmail != null) {
                // Vérifier si le token est blacklisté
                if (tokenBlacklistService.isTokenBlacklisted(token)) {
                    log.warn("Blacklisted JWT token was used for WebSocket connection");
                    return false;
                }
                
                // Charger les détails de l'utilisateur et vérifier la validité du token
                UserDetails userDetails = userService.loadUserByUsername(userEmail);
                
                if (jwtService.isTokenValid(token, userDetails)) {
                    // Créer l'authentification
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    
                    // Définir l'authentification dans le contexte de sécurité et l'accesseur de message
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    accessor.setUser(auth);
                    
                    return true;
                }
            }
        } catch (Exception e) {
            log.error("Authentication error in WebSocket connection", e);
        }
        
        return false;
    }
}