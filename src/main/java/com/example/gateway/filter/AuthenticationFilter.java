package com.example.gateway.filter;

import com.example.gateway.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Arrays;

@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    @Autowired
    private JwtUtil jwtUtil;

    public static final List<String> publicApiEndpoints = Arrays.asList(
        "/api/auth/register",
        "/api/auth/login"
    );

    public AuthenticationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getURI().getPath();
            
            // 處理 OPTIONS 請求
            if (exchange.getRequest().getMethod() == HttpMethod.OPTIONS) {
                exchange.getResponse().getHeaders().add(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://127.0.0.1:5500");
                exchange.getResponse().getHeaders().add(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET,POST,PUT,DELETE,OPTIONS");
                exchange.getResponse().getHeaders().add(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "*");
                exchange.getResponse().getHeaders().add(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600");
                exchange.getResponse().setStatusCode(HttpStatus.OK);
                return exchange.getResponse().setComplete();
            }

            // 1. 檢查請求的路徑是否為公開 API
            if (publicApiEndpoints.stream().anyMatch(path::startsWith)) {
                exchange.getResponse().getHeaders().add(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://127.0.0.1:5500");
                return chain.filter(exchange);
            }

            // 2. 獲取 Authorization Header
            HttpHeaders headers = exchange.getRequest().getHeaders();
            if (!headers.containsKey(HttpHeaders.AUTHORIZATION)) {
                return handleUnauthorized(exchange, "Missing Authorization header");
            }

            String authHeader = headers.getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return handleUnauthorized(exchange, "Invalid Authorization header");
            }

            // 3. 提取並驗證 JWT
            String jwt = authHeader.substring(7);
            try {
                if (!jwtUtil.validateToken(jwt)) {
                    return handleUnauthorized(exchange, "Invalid JWT token");
                }
            } catch (Exception e) {
                return handleUnauthorized(exchange, "Invalid JWT: " + e.getMessage());
            }

            // 4. 驗證通過，放行請求
            exchange.getResponse().getHeaders().add(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://127.0.0.1:5500");
            return chain.filter(exchange);
        };
    }

    private Mono<Void> handleUnauthorized(ServerWebExchange exchange, String message) {
        System.out.println("Unauthorized request: " + message);
        exchange.getResponse().getHeaders().add(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://127.0.0.1:5500");
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    public static class Config {
    }
}
