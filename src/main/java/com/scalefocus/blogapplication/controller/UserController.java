package com.scalefocus.blogapplication.controller;

import com.scalefocus.blogapplication.dto.*;
import com.scalefocus.blogapplication.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("api/users")
@Tag(name = "User API", description = "Operations related to user management")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "Register a new user", description = "Registers a new user with the provided details.")
    @ApiResponse(responseCode = "201", description = "User registered successfully")
    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(
            @Parameter(description = "Registration details", required = true)
            @Valid @RequestBody RegistrationRequest request) {
        AuthenticationResponse response = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "User login", description = "Authenticates a user and returns JWT tokens.")
    @ApiResponse(responseCode = "200", description = "User authenticated successfully")
    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> authenticate(
            @Parameter(description = "Authentication details", required = true)
            @Valid @RequestBody AuthenticationRequest request) {
        return ResponseEntity.ok(this.userService.authenticate(request));
    }

    @Operation(summary = "Refresh JWT token", description = "Refreshes the JWT access token using a refresh token.")
    @ApiResponse(responseCode = "200", description = "Token refreshed successfully")
    @PostMapping("token/refresh")
    public ResponseEntity<RefreshResponse> refresh(
            @Parameter(description = "Refresh token", required = true)
            @RequestBody TokenRefreshRequest request) {
        return ResponseEntity.ok(this.userService.refreshToken(request));
    }

    @Operation(summary = "Change user password", description = "Allows the authenticated user to change their password.")
    @ApiResponse(responseCode = "200", description = "Password changed successfully")
    @PreAuthorize("hasAuthority('Write')")
    @PutMapping("/password/change")
    public ResponseEntity<String> changePassword(
            @Parameter(description = "Password change request", required = true)
            @Valid @RequestBody PasswordChangeRequest request) {
        return ResponseEntity.ok(this.userService.changePassword(request));
    }

    @Operation(summary = "Change user authority", description = "Changes the authority of a user. Admin only.")
    @ApiResponse(responseCode = "200", description = "User authority changed successfully")
    @ApiResponse(responseCode = "403", description = "Access denied")
    @PreAuthorize("hasAuthority('Admin')")
    @PatchMapping("/{userId}/authority")
    public ResponseEntity<UserResponse> changeUserAuthority(
            @Parameter(description = "ID of the user to update", required = true)
            @PathVariable Long userId,
            @Parameter(description = "New authority level", required = true)
            @RequestParam String authority) {
        return ResponseEntity.ok(this.userService.changeUserAuthorityById(userId, authority));
    }

    @Operation(summary = "Delete a user", description = "Deletes the specified user. Admin only.")
    @ApiResponse(responseCode = "200", description = "User deleted successfully")
    @ApiResponse(responseCode = "403", description = "Access denied")
    @PreAuthorize("hasAuthority('Admin')")
    @DeleteMapping("/{userId}")
    public ResponseEntity<UserResponse> deleteUserById(
            @Parameter(description = "ID of the user to delete", required = true)
            @PathVariable Long userId) {
        return ResponseEntity.ok(this.userService.deleteUserById(userId));
    }

    @Operation(summary = "User logout", description = "Logs out the user by invalidating the refresh token.")
    @ApiResponse(responseCode = "200", description = "User logged out successfully")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @Parameter(description = "Refresh token to invalidate", required = true)
            @RequestBody TokenRefreshRequest request) {
        userService.logout(request);
        return ResponseEntity.ok().build();
    }

}
