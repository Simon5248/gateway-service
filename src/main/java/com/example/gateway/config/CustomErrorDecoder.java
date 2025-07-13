package com.example.gateway.config;

import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public class CustomErrorDecoder implements ErrorDecoder {
    
    @Override
    public Exception decode(String methodKey, Response response) {
        if (response.status() == HttpStatus.NOT_FOUND.value()) {
            return new UsernameNotFoundException("User not found");
        }
        return new RuntimeException("Error occurred when calling user service");
    }
}
