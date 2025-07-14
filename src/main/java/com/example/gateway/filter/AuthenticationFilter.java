package com.example.gateway.filter;

import com.example.gateway.client.UserClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Arrays;

@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationFilter.class);

    private final ApplicationContext applicationContext;
    private UserClient userClient;

    public AuthenticationFilter(ApplicationContext applicationContext) {
        super(Config.class);
        this.applicationContext = applicationContext;
    }

    private UserClient getUserClient() {
        if (userClient == null) {
            userClient = applicationContext.getBean(UserClient.class);
        }
        return userClient;
    }

    public static final List<String> publicApiEndpoints = Arrays.asList(
        "/api/auth/register",
        "/api/auth/login"
    );

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getURI().getPath();
            logger.debug("開始處理請求，路徑: {}", path);

            // 1. 檢查請求的路徑是否為公開 API 或 OPTIONS 請求
            if (publicApiEndpoints.stream().anyMatch(path::startsWith) || 
                exchange.getRequest().getMethod() == HttpMethod.OPTIONS) {
                logger.debug("允許通過: 公開端點或 OPTIONS 請求: {}", path);
                return chain.filter(exchange);
            }

            // 2. 獲取 Authorization Header
            HttpHeaders headers = exchange.getRequest().getHeaders();
            if (!headers.containsKey(HttpHeaders.AUTHORIZATION)) {
                logger.warn("驗證失敗: 缺少 Authorization 標頭");
                return handleUnauthorized(exchange, "Missing Authorization header");
            }

            String authHeader = headers.getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                logger.warn("驗證失敗: Authorization 標頭格式不正確");
                return handleUnauthorized(exchange, "Invalid Authorization header");
            }

            try {
                // 3. 先驗證令牌
                logger.debug("開始驗證令牌");
                String bearerToken = authHeader;  // 使用完整的Bearer token
                if (!getUserClient().validateToken(bearerToken)) {
                    logger.warn(authHeader);
                    logger.warn("JWT 令牌驗證失敗");
                    return handleUnauthorized(exchange, "Invalid JWT token");
                }
                logger.debug("JWT 令牌驗證成功");

                // 4. 提取用戶信箱
                logger.debug("嘗試提取使用者信箱");
                ResponseEntity<String> emailResponse = getUserClient().extractUsername(bearerToken);
                if (emailResponse == null || emailResponse.getBody() == null) {
                    logger.warn("無法提取使用者信箱，response: {}", emailResponse);
                    return handleUnauthorized(exchange, "Invalid JWT: user email not found");
                }
                String userEmail = emailResponse.getBody();
                logger.debug("成功提取使用者信箱: {}", userEmail);

                // 5. 提取用戶ID
                ResponseEntity<Long> userIdResponse = getUserClient().extractUserId(bearerToken);
                if (userIdResponse == null || userIdResponse.getBody() == null) {
                    logger.warn("無法提取使用者ID，response: {}", userIdResponse);
                    return handleUnauthorized(exchange, "Invalid JWT: user ID not found");
                }
                Long userId = userIdResponse.getBody();
                logger.debug("成功提取使用者ID: {}", userId);

                // 6. 在請求中添加用戶信息
                ServerWebExchange modifiedExchange = exchange.mutate()
                    .request(exchange.getRequest().mutate()
                        .header("X-User-ID", String.valueOf(userId))
                        .header("X-User-Email", userEmail)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .build())
                    .build();

                logger.debug("認證成功，轉發請求到: {}", path);
                return chain.filter(modifiedExchange);

            } catch (Exception e) {
                logger.error("JWT 驗證過程中發生錯誤: {}", e.getMessage());
                return handleUnauthorized(exchange, "Authentication error: " + e.getMessage());
            }
        };
    }

    private Mono<Void> handleUnauthorized(ServerWebExchange exchange, String message) {
        logger.warn("Unauthorized request: {} for path: {}", message, exchange.getRequest().getURI().getPath());
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    public static class Config {
    }
}
