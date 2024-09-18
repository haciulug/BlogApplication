package com.scalefocus.blogapplication.util;

import com.scalefocus.blogapplication.dto.AuthenticationRequest;
import com.scalefocus.blogapplication.dto.PasswordChangeRequest;
import com.scalefocus.blogapplication.dto.RegistrationRequest;
import com.scalefocus.blogapplication.dto.TokenRefreshRequest;
import com.scalefocus.blogapplication.model.RefreshToken;
import com.scalefocus.blogapplication.model.User;
import lombok.experimental.UtilityClass;

import java.time.Instant;
import java.time.LocalDateTime;

@UtilityClass
public class TestUtil {

    public static User getTestUser() {
        return User.builder()
                .username("test")
                .password("password")
                .authority("Admin")
                .id(1L)
                .accountNonLocked(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public static PasswordChangeRequest getTestPasswordChangeRequest() {
        return PasswordChangeRequest.builder()
                .newPassword("test")
                .oldPassword("password")
                .build();
    }

    public static AuthenticationRequest getAuthenticationRequest() {
        return AuthenticationRequest.builder()
                .username("test")
                .password("password")
                .build();
    }

    public static RegistrationRequest getRegistrationRequest() {
        return RegistrationRequest.builder()
                .username("test")
                .password("password")
                .build();
    }

    public static RefreshToken getRefreshToken() {
        return RefreshToken.builder()
                .user(getTestUser())
                .token("refreshToken")
                .id(1L)
                .expiresAt(Instant.now().plusSeconds(6000))
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static TokenRefreshRequest getTokenRefreshRequest() {
        return TokenRefreshRequest.builder()
                .token("Test token")
                .build();
    }
}