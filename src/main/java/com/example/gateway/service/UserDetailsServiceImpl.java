package com.example.gateway.service;

import com.example.gateway.client.UserClient;
import com.example.gateway.dto.UserDetailsDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.util.stream.Collectors;

@Service
@Lazy
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserClient userClient;

    @Autowired
    public UserDetailsServiceImpl(UserClient userClient) {
        this.userClient = userClient;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        try {
            UserDetailsDTO userDTO = userClient.getUserDetailsByEmail(email);
            return new User(
                userDTO.getEmail(),
                userDTO.getPassword(),
                userDTO.isEnabled(),
                true,
                true,
                true,
                userDTO.getAuthorities().stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList())
            );
        } catch (Exception e) {
            throw new UsernameNotFoundException("User not found with email: " + email);
        }
    }
}
