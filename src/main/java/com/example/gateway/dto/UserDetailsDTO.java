package com.example.gateway.dto;

import lombok.Data;
import java.util.Collection;

@Data
public class UserDetailsDTO {
    private String email;
    private String password;
    private boolean enabled;
    private Collection<String> authorities;
}
