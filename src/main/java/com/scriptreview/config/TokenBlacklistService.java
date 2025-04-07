package com.scriptreview.config;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.scriptreview.model.BlacklistedToken;
import com.scriptreview.repository.BlacklistedTokenRepository;

import jakarta.transaction.Transactional;

@Service
public class TokenBlacklistService {

    @Autowired
    private BlacklistedTokenRepository blacklistedTokenRepository;

    /**
     * Ajoute un token à la liste noire avec sa date d'expiration.
     */
    public void blacklistToken(String token, Date expirationDate) {
        BlacklistedToken blacklistedToken = new BlacklistedToken();
        blacklistedToken.setToken(token);
        blacklistedToken.setExpirationDate(expirationDate);

        blacklistedTokenRepository.save(blacklistedToken);
    }

    /**
     * Vérifie si un token est dans la liste noire.
     */
    public boolean isTokenBlacklisted(String token) {
        return blacklistedTokenRepository.existsByToken(token);
    }

    /**
     * Nettoie périodiquement les tokens expirés de la base de données.
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void cleanupExpiredTokens() {
        blacklistedTokenRepository.deleteByExpirationDateBefore(new Date());
    }
}