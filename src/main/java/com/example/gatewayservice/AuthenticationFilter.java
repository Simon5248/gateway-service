package com.example.gatewayservice;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.example.gateway.util.JwtUtil;

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Arrays;

@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private final JwtUtil jwtUtil;

    private final List<String> publicApiEndpoints = Arrays.asList(
            "/api/auth/register",
            "/api/auth/login"
    
    );

    public AuthenticationFilter(JwtUtil jwtUtil) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();

            // 1. 如果是 OPTIONS 預檢請求或公開 API，直接放行
            // if (request.getMethod() == HttpMethod.OPTIONS || publicApiEndpoints.stream().anyMatch(path::startsWith)) {
            //     return chain.filter(exchange);
            // }

            if (publicApiEndpoints.stream().anyMatch(path::startsWith)) {
                return chain.filter(exchange);
            }

            // 2. 從 header 獲取 token
            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return handleUnauthorized(exchange, "Missing or invalid Authorization Header");
            }

            String token = authHeader.substring(7);

            // 3. 驗證 token
            if (!jwtUtil.validateToken(token)) {
                return handleUnauthorized(exchange, "Invalid JWT Token");
            }

            // 4. 驗證通過，放行請求
            return chain.filter(exchange);
        };
    }

    private Mono<Void> handleUnauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        // 您可以在此處添加更詳細的錯誤回應內容
        System.out.println("Unauthorized request: " + message); // 在後端日誌中記錄原因
        return response.setComplete();
    }

    public static class Config {
        // 您可以在此處添加過濾器的特定配置
    }
}
