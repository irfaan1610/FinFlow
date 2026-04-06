package com.project.finance.dto.response;

import com.project.finance.model.Role;

public class AuthResponse {

    private String token;
    private String email;
    private Role role;

    public AuthResponse(String token, String email, Role role) {
        this.token = token;
        this.email = email;
        this.role = role;
    }

    public String getToken() { return token; }
    public String getEmail() { return email; }
    public Role getRole() { return role; }
}
