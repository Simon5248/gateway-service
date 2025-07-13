package com.example.gateway.client;

import com.example.gateway.dto.UserDetailsDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class UserClientFallback implements UserClient {
    
    private static final Logger logger = LoggerFactory.getLogger(UserClientFallback.class);
    
    @Override
    public UserDetailsDTO getUserDetailsByEmail(String email) {
        logger.error("Failed to get user details for email: {}", email);
        throw new RuntimeException("User service is not available");
    }
}
