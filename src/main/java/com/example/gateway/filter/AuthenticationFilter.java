package com.example.gateway.filter;

import com.example.gateway.util.JwtUtil; // 引入您剛剛複製的 JwtUtil
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
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

            // 1. 檢查請求的路徑是否為公開 API
            if (publicApiEndpoints.stream().anyMatch(path::startsWith)) {
                // 如果是公開 API，直接放行
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
                // 根據您的 JwtUtil 實現，您需要提供 UserDetails 作為第二個參數
                // 這裡假設您有一個方法可以從 JWT 取得 username 並載入 UserDetails
                String username = jwtUtil.extractUsername(jwt);
                // 請根據您的專案實際情況取得 UserDetailsService 實例
                org.springframework.security.core.userdetails.UserDetails userDetails = 
                    org.springframework.security.core.userdetails.User.withUsername(username).password("").authorities(new java.util.ArrayList<>()).build();
                jwtUtil.validateToken(jwt, userDetails); // 使用 JwtUtil 進行驗證
            } catch (Exception e) {
                return handleUnauthorized(exchange, "Invalid JWT: " + e.getMessage());
            }

            // 4. 驗證通過，放行請求
            return chain.filter(exchange);
        };
    }

    // 統一處理未授權的回應
    private Mono<Void> handleUnauthorized(ServerWebExchange exchange, String message) {
        // 在此可以記錄詳細的錯誤日誌
        System.out.println("Unauthorized request: " + message); 
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    // 這是一個內部類，目前我們不需要任何特定的設定
    public static class Config {
    }
}
