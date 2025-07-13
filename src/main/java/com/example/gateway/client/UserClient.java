package com.example.gateway.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.example.gateway.dto.UserDetailsDTO;

@FeignClient(
    name = "user-service",
    fallback = UserClientFallback.class
)
public interface UserClient {
    
    @GetMapping("/api/users/details/{email}")
    UserDetailsDTO getUserDetailsByEmail(@PathVariable("email") String email);
}
