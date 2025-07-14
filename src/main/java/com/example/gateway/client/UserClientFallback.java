package com.example.gateway.client;

import com.example.gateway.dto.UserDetailsDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.http.ResponseEntity;

@Component
public class UserClientFallback implements UserClient {
    
    private static final Logger logger = LoggerFactory.getLogger(UserClientFallback.class);
    
    @Override
    public UserDetailsDTO getUserDetailsByEmail(String email) {
        logger.error("Failed to get user details for email: {}", email);
        throw new RuntimeException("User service is not available");
    }

    @Override
    public boolean validateToken(String token) {
        // 如果無法訪問 user-service，預設返回 false
        return false;
    }

    @Override
    public ResponseEntity<String> extractUsername(String token) {
        logger.error("Failed to extract username from token");
        return ResponseEntity.badRequest().body(null);
    }

    @Override
    public ResponseEntity<Long> extractUserId(String token) {
        logger.error("Failed to extract user ID from token");
        return ResponseEntity.badRequest().body(null);
    }
}
