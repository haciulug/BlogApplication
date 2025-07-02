package com.scalefocus.blogapplication.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserResponse {
    private Long id;
    private String username;
    private String authority;
    private boolean accountNonLocked;
    private int loginAttempts;
    private LocalDateTime autoLockedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}