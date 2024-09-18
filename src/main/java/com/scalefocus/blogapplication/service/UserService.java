package com.scalefocus.blogapplication.service;

import com.scalefocus.blogapplication.dto.*;

public interface UserService {
    AuthenticationResponse register(RegistrationRequest request);
    AuthenticationResponse authenticate(AuthenticationRequest request);
    UserResponse deleteUserById(Long userId);
    RefreshResponse refreshToken(TokenRefreshRequest request);
    String changePassword(PasswordChangeRequest request);
    UserResponse changeUserAuthorityById(Long userId, String authority);
    void checkLoginAttempts(String username);
    void logout(TokenRefreshRequest request);
}
