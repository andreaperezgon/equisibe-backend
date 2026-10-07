package com.equisibe.dto;

import com.equisibe.model.Role;

public record UserResponse(
        Long id,
        String name,
        String email,
        Role role
) {
}