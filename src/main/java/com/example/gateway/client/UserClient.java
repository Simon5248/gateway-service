package com.example.gateway.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.http.ResponseEntity;
import com.example.gateway.dto.UserDetailsDTO;

@FeignClient(
    name = "user-service",
    fallback = UserClientFallback.class
)
public interface UserClient {
    
    @GetMapping("/api/users/details/{email}")
    UserDetailsDTO getUserDetailsByEmail(@PathVariable("email") String email);

    @GetMapping("/api/users/validate-token")
    boolean validateToken(@RequestHeader("Authorization") String token);

    @GetMapping("/api/users/token/extract-email")
    ResponseEntity<String> extractUsername(@RequestHeader("Authorization") String token);
    
    @GetMapping("/api/users/token/extract-user-id")
    ResponseEntity<Long> extractUserId(@RequestHeader("Authorization") String token);
}
