// ===================================================================================
// FILE: src/main/java/com/example/taskapp/security/JwtAuthFilter.java
// ===================================================================================
package com.example.gateway.util;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        System.out.println("11111111-------------------------------------gateway-service JwtAuthFilter doFilterInternal");
        String authHeader = request.getHeader("Authorization");
        String jwt = null;
        String userEmail = null;
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("-------------------------------------gateway-service JwtAuthFilter doFilterInternal");
            System.out.println("Authorization header is missing or does not start with Bearer");
            System.out.println(authHeader);
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);
        userEmail = jwtUtil.extractUsername(jwt);

        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
            if (jwtUtil.validateToken(jwt, userDetails)) {
                Long userId = jwtUtil.extractUserId(jwt);
                // 將 userId 設為 Principal，方便在 Controller 中直接取得
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        System.out.println("2222222-------------------------------------gateway-service JwtAuthFilter doFilterInternal");
        System.out.println("Authorization header processed successfully");
           
        filterChain.doFilter(request, response);
    }
}