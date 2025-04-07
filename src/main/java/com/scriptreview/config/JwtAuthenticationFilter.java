package com.scriptreview.config;

import java.io.IOException;
import java.sql.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
	private final JwtService jwtService;
	private final UserDetailsService userDetailsService;
	private final TokenBlacklistService tokenBlacklistService;

	@Override
	protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain) throws ServletException, IOException {
		// Skip filter for WebSocket handshake requests - WebSocket security is handled separately
		if (isWebSocketHandshake(request)) {
			filterChain.doFilter(request, response);
			return;
		}

		final String authHeader = request.getHeader("Authorization");
		final String jwt;
		final String userEmail;

		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			filterChain.doFilter(request, response);
			return;
		}

		jwt = authHeader.substring(7);

		// Check if token is blacklisted
		if (tokenBlacklistService.isTokenBlacklisted(jwt)) {
			log.warn("Blacklisted JWT token was used: {}", jwt);
			filterChain.doFilter(request, response);
			return;
		}

		try {
			userEmail = jwtService.extractUsername(jwt);
			if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
				UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
				if (jwtService.isTokenValid(jwt, userDetails)) {
					UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
							userDetails,
							null,
							userDetails.getAuthorities()
					);
					authToken.setDetails(
							new WebAuthenticationDetailsSource().buildDetails(request)
					);
					SecurityContextHolder.getContext().setAuthentication(authToken);
				}
			}
		} catch (Exception e) {
			log.error("JWT validation error", e);
			// Don't throw exception, just continue with unauthenticated request
		}

		filterChain.doFilter(request, response);
	}

	/**
	 * Déterminer si la requête est pour une handshake WebSocket
	 */
	private boolean isWebSocketHandshake(HttpServletRequest request) {
		return "GET".equals(request.getMethod()) && 
			   request.getRequestURI().contains("/ws") &&
			   "Upgrade".equalsIgnoreCase(request.getHeader("Connection")) &&
			   "websocket".equalsIgnoreCase(request.getHeader("Upgrade"));
	}

	public void blacklistToken(String token) {
		// Extraire la date d'expiration du token
		Date expirationDate = (Date) jwtService.extractExpiration(token);

		// Ajouter le token à la blacklist avec sa date d'expiration
		tokenBlacklistService.blacklistToken(token, expirationDate);
	}
}