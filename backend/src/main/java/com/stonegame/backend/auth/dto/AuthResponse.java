package com.stonegame.backend.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class AuthResponse {

    private String token;
    private String userId;
    private String username;
    private String email;
    private String role;
}
