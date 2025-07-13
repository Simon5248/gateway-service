package com.example.gateway.util;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT 工具類，用於處理 JSON Web Token 的生成、解析和驗證
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration.ms}")
    private long jwtExpirationInMs;
    
    /**
     * 獲取用於簽署 JWT 的密鑰
     * @return SecretKey 用於簽署和驗證 JWT 的密鑰
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 JWT token
     * @param userDetails 用戶詳細信息
     * @param userId 用戶ID
     * @return 生成的 JWT token 字符串
     */
    public String generateToken(UserDetails userDetails, Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", userId);
        claims.put("email", userDetails.getUsername());
        String tokenString = Jwts.builder()
                .setClaims(claims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationInMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
        //tokenString = java.util.Base64.getEncoder().encodeToString(tokenString.getBytes(StandardCharsets.UTF_8));
        System.out.println("Generated JWT Token: " + tokenString);
        return tokenString;
    }
    
    /**
     * 從 token 中提取用戶名
     * @param token JWT token 字符串
     * @return 用戶名
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    
    /**
     * 從 token 中提取用戶ID
     * @param token JWT token 字符串
     * @return 用戶ID
     */
    public Long extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("id", Long.class));
    }

    /**
     * 驗證 token 是否有效
     * @param token JWT token 字符串
     * @param userDetails 用戶詳細信息
     * @return true 如果 token 有效，false 如果 token 無效
     */
    public boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    /**
     * 僅驗證 token 的格式和簽名，不驗證用戶信息
     * @param token JWT token 字符串
     * @return true 如果 token 格式正確且簽名有效，false 如果無效
     */
    public boolean validateTokenFormat(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 檢查 token 是否已過期
     * @param token JWT token 字符串
     * @return true 如果 token 已過期，false 如果 token 仍然有效
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * 從 token 中提取過期時間
     * @param token JWT token 字符串
     * @return token 的過期時間
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * 從 token 中提取指定的 claim
     * @param token JWT token 字符串
     * @param claimsResolver 用於解析 claim 的函數
     * @return claim 的值
     */
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * 從 token 中提取所有的 claims
     * @param token JWT token 字符串
     * @return Claims 對象，包含 token 中的所有信息
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token).getBody();
    }
}
