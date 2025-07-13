package com.example.gateway.filter;

import com.example.gateway.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Arrays;

@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationFilter.class);

    @Autowired
    private JwtUtil jwtUtil;

    private final ObjectProvider<UserDetailsService> userDetailsServiceProvider;

    public AuthenticationFilter(ObjectProvider<UserDetailsService> userDetailsServiceProvider) {
        super(Config.class);
        this.userDetailsServiceProvider = userDetailsServiceProvider;
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
            logger.debug("請求方法: {}", exchange.getRequest().getMethod());

            // 1. 檢查請求的路徑是否為公開 API 或 OPTIONS 請求
            logger.debug("步驟1: 檢查是否為公開 API 或 OPTIONS 請求");
            logger.debug("公開 API 列表: {}", publicApiEndpoints);
            if (publicApiEndpoints.stream().anyMatch(path::startsWith) || 
                exchange.getRequest().getMethod() == HttpMethod.OPTIONS) {
                logger.debug("允許通過: 公開端點或 OPTIONS 請求: {}", path);
                return chain.filter(exchange);
            }

            // 2. 獲取 Authorization Header
            logger.debug("步驟2: 檢查 Authorization Header");
            HttpHeaders headers = exchange.getRequest().getHeaders();
            logger.debug("請求標頭: {}", headers.toSingleValueMap());
            
            if (!headers.containsKey(HttpHeaders.AUTHORIZATION)) {
                logger.warn("驗證失敗: 缺少 Authorization 標頭");
                return handleUnauthorized(exchange, "Missing Authorization header");
            }

            String authHeader = headers.getFirst(HttpHeaders.AUTHORIZATION);
            logger.debug("Authorization 標頭格式檢查");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                logger.warn("驗證失敗: Authorization 標頭格式不正確");
                return handleUnauthorized(exchange, "Invalid Authorization header");
            }

            // 3. 提取並驗證 JWT
            logger.debug("步驟3: 開始 JWT 令牌驗證");
            String jwt = authHeader.substring(7);
            try {
                logger.debug("收到的完整JWT: {}", jwt);
                logger.debug("JWT 長度: {} 字元", jwt.length());
                logger.debug("JWT 結構分析:");
                String[] jwtParts = jwt.split("\\.");
                if (jwtParts.length == 3) {
                    logger.debug("JWT 格式正確 (包含 header.payload.signature)");
                    logger.debug("Header 長度: {}", jwtParts[0].length());
                    logger.debug("Payload 長度: {}", jwtParts[1].length());
                    logger.debug("Signature 長度: {}", jwtParts[2].length());
                } else {
                    logger.warn("JWT 格式不正確，預期3個部分，實際: {}", jwtParts.length);
                }
                
                logger.debug("嘗試提取使用者信箱");
                String userEmail = jwtUtil.extractUsername(jwt);
                logger.debug("提取的 email: {}", userEmail);
                
                if (userEmail != null) {
                    logger.debug("成功提取使用者信箱: {}", userEmail);
                    logger.debug("開始驗證令牌格式和簽名");
                    
                    if (!jwtUtil.validateTokenFormat(jwt)) {
                        logger.warn("JWT 令牌格式或簽名驗證失敗");
                        return handleUnauthorized(exchange, "Invalid JWT token format or signature");
                    }
                    logger.debug("JWT 令牌格式和簽名驗證成功");

                    // 4. 提取用戶 ID 並添加到請求標頭
                    logger.debug("步驟4: 開始提取用戶 ID");
                    Long userId = null;
                    try {
                        userId = jwtUtil.extractUserId(jwt);
                        logger.debug("成功提取到用戶 ID: {}", userId);
                        if (userId == null) {
                            logger.warn("用戶 ID 為 null");
                            return handleUnauthorized(exchange, "User ID not found in token");
                        }
                    } catch (Exception e) {
                        logger.error("提取用戶 ID 時發生錯誤", e);
                        return handleUnauthorized(exchange, "Error extracting user ID: " + e.getMessage());
                    }
                    
                    // 在修改請求之前記錄原始請求資訊
                    logger.debug("原始請求標頭資訊:");
                    exchange.getRequest().getHeaders().forEach((key, value) -> {
                        logger.debug("原始標頭 - {}: {}", key, value);
                    });

                    ServerWebExchange modifiedExchange = exchange.mutate()
                        .request(exchange.getRequest().mutate()
                            .header("X-User-ID", String.valueOf(userId))
                            .header(HttpHeaders.AUTHORIZATION, authHeader)  // 轉發原始的 Authorization 標頭
                            .build())
                        .build();
                    
                    // 在修改請求之後記錄修改後的請求資訊
                    logger.debug("修改後的請求標頭資訊:");
                    modifiedExchange.getRequest().getHeaders().forEach((key, value) -> {
                        logger.debug("修改後標頭 - {}: {}", key, value);
                    });
                    
                    logger.debug("步驟5: 所有驗證通過，放行請求到: {}", path);
                    logger.debug("請求目標URI: {}", modifiedExchange.getRequest().getURI());
                    return chain.filter(modifiedExchange);
                } else {
                    logger.warn("JWT 令牌中未找到使用者信箱");
                    return handleUnauthorized(exchange, "Invalid JWT: user email not found");
                }
            } catch (Exception e) {
                logger.error("JWT 驗證過程中發生錯誤", e);
                return handleUnauthorized(exchange, "Invalid JWT: " + e.getMessage());
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
