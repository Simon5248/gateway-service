package com.example.gateway.exception;

import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;

@Component
public class GlobalErrorWebExceptionHandler implements ErrorWebExceptionHandler {

    private ObjectMapper objectMapper;

    public GlobalErrorWebExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        Map<String, Object> errorPropertiesMap = new HashMap<>();
        errorPropertiesMap.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        errorPropertiesMap.put("message", ex.getMessage());

        // 根據異常類型設置不同的狀態碼
        if (ex instanceof IllegalStateException) {
            exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
            errorPropertiesMap.put("status", HttpStatus.BAD_REQUEST.value());
        } else {
            exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // 設置響應類型
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // 將錯誤信息轉換為 JSON
        DataBufferFactory bufferFactory = exchange.getResponse().bufferFactory();
        String errorMessage;
        try {
            errorMessage = objectMapper.writeValueAsString(errorPropertiesMap);
        } catch (JsonProcessingException e) {
            errorMessage = "{\"status\": 500, \"message\": \"Internal Server Error\"}";
        }

        return exchange.getResponse().writeWith(
                Mono.just(bufferFactory.wrap(errorMessage.getBytes()))
        );
    }
}
