package com.scalefocus.blogapplication.service;

import com.scalefocus.blogapplication.dto.*;
import com.scalefocus.blogapplication.exception.custom.RefreshTknExpireException;
import com.scalefocus.blogapplication.exception.custom.UniqueUsernameException;
import com.scalefocus.blogapplication.mapper.UserMapper;
import com.scalefocus.blogapplication.model.RefreshToken;
import com.scalefocus.blogapplication.model.User;
import com.scalefocus.blogapplication.repository.RefreshTokenRepository;
import com.scalefocus.blogapplication.repository.UserRepository;
import com.scalefocus.blogapplication.util.JwtUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final Logger LOGGER = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserMapper userMapper;

    @Override
    public AuthenticationResponse register(RegistrationRequest request) {
        if (this.userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new UniqueUsernameException();
        }
        User user = User.builder()
                .username(request.getUsername())
                .password(this.passwordEncoder.encode(request.getPassword()))
                .displayName(request.getDisplayName())
                .authority("Write")
                .accountNonLocked(true)
                .build();
        userRepository.saveAndFlush(user);
        LOGGER.info("User registered: {}", user.getUsername());

        return createAuthResponse(user, false);
    }

    @Override
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        try {
            this.authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
            LOGGER.info("User authenticated: {}", request.getUsername());
        } catch (LockedException ex) {
            LOGGER.error("User account locked: {}", request.getUsername());
            if (isAutoAccountLockExpired(request.getUsername())) {
                this.authenticate(request);
            } else {
                throw ex;
            }
        }
        User user = this.userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new EntityNotFoundException("Username does not exist"));
        if (user.getLoginAttempts() > 0) {
            user.setLoginAttempts(0);
            this.userRepository.saveAndFlush(user);
        }
        return createAuthResponse(user, true);
    }

    @Override
    public UserResponse deleteUserById(Long userId) {
        User user = this.userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("User not found"));
        this.userRepository.delete(user);
        LOGGER.info("User deleted: {}", user.getUsername());
        return userMapper.userToUserResponse(user);
    }

    @Override
    public RefreshResponse refreshToken(TokenRefreshRequest request) {
        //future check returns null for sure?
        RefreshToken refreshToken = this.refreshTokenRepository.findByToken(request.getToken());

        if (refreshToken == null) {
            throw new BadCredentialsException("Bad credentials");
        }

        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            this.refreshTokenRepository.delete(refreshToken);
            throw new RefreshTknExpireException();
        }
        LOGGER.info("Token refreshed for user: {}", refreshToken.getUser().getUsername());
        return new RefreshResponse(this.jwtUtil.generateToken(refreshToken.getUser()));
    }

    private AuthenticationResponse createAuthResponse(User user, boolean existingUser) {
        if (existingUser) {
            RefreshToken refreshToken = this.refreshTokenRepository.findByUser(user);
            if (refreshToken != null) {
                this.refreshTokenRepository.delete(refreshToken);
            }
        }
        LOGGER.info("JWT token generated for user: {}", user.getUsername());
        return new AuthenticationResponse(jwtUtil.generateToken(user), this.createRefreshToken(user));
    }

    @Override
    public String changePassword(PasswordChangeRequest request) {
        User user = this.userRepository.findByUsername(
                SecurityContextHolder.getContext().getAuthentication().getName()).orElseThrow();

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BadCredentialsException("Old password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        this.userRepository.saveAndFlush(user);
        LOGGER.info("Password changed for user: {}", user.getUsername());

        return "Password changed";
    }

    @Override
    public UserResponse changeUserAuthorityById(Long userId, String authority) {
        User user = this.userRepository.findById(userId).orElseThrow();
        user.setAuthority(authority);
        this.userRepository.saveAndFlush(user);
        LOGGER.info("Authority changed for user: {}", user.getUsername());
        return userMapper.userToUserResponse(user);
    }

    @Override
    public void checkLoginAttempts(String username) {
        Optional<User> userOptional = this.userRepository.findByUsername(username);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            int logInAttempts = 5;
            if (user.getLoginAttempts() < logInAttempts) {
                user.setLoginAttempts(user.getLoginAttempts() + 1);
            } else {
                user.setAutoLockedAt(LocalDateTime.now());
                user.setAccountNonLocked(false);
                LOGGER.error("User account locked: {}", username);
            }
            this.userRepository.saveAndFlush(user);
        }
    }

    @Override
    public void logout(TokenRefreshRequest request) {
        RefreshToken refreshToken = this.refreshTokenRepository.findByToken(request.getToken());
        if (refreshToken != null) {
            this.refreshTokenRepository.delete(refreshToken);
        }
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        SecurityContextHolder.clearContext();
        LOGGER.info("User logged out: {}", username);
    }

    public boolean isAutoAccountLockExpired(String username) {
        Optional<User> userOptional = this.userRepository.findByUsername(username);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            LocalDateTime timeLocked = user.getAutoLockedAt();
            long lockTime = 10;
            if (timeLocked != null && timeLocked.isBefore(LocalDateTime.now().minusMinutes(lockTime))) {
                user.setAutoLockedAt(null);
                user.setAccountNonLocked(true);
                user.setLoginAttempts(0);
                this.userRepository.saveAndFlush(user);
                LOGGER.info("User account unlocked: {}", username);
                return true;
            }
        }
        return false;
    }

    private String createRefreshToken(User user) {
        String token = UUID.randomUUID().toString();

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .expiresAt(Instant.now().plusMillis(Long.parseLong(jwtUtil.getRefreshExpiration())))
                .token(token)
                .build();

        this.refreshTokenRepository.saveAndFlush(refreshToken);
        LOGGER.info("Refresh token generated for user: {}", user.getUsername());
        return token;
    }
}
