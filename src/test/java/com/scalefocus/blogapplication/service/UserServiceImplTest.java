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
import com.scalefocus.blogapplication.util.TestUtil;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {UserService.class, UserRepository.class, PasswordEncoder.class,
        RefreshTokenRepository.class, AuthenticationManager.class, JwtUtil.class})
class UserServiceImplTest {

    @InjectMocks
    private UserServiceImpl userService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private UserMapper userMapper;


    private User user;
    private AuthenticationRequest authenticationRequest;
    private RegistrationRequest registrationRequest;
    private RefreshToken refreshToken;
    private TokenRefreshRequest tokenRefreshRequest;
    private final Long id = 1L;
    private PasswordChangeRequest passwordChangeRequest;

    @BeforeEach
    public void setup() {
        this.user = TestUtil.getTestUser();
        this.authenticationRequest = TestUtil.getAuthenticationRequest();
        this.registrationRequest = TestUtil.getRegistrationRequest();
        this.refreshToken = TestUtil.getRefreshToken();
        this.tokenRefreshRequest = TestUtil.getTokenRefreshRequest();
        this.passwordChangeRequest = TestUtil.getTestPasswordChangeRequest();
    }

    @BeforeEach
    public void initSecurityContext() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("test@test.test",
                "password"));
    }

    @Test
    void testRegisterSuccess() {
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(jwtUtil.generateToken(any(User.class))).thenReturn("jwtToken");
        when(userRepository.saveAndFlush(any(User.class))).thenReturn(user);
        when(jwtUtil.getRefreshExpiration()).thenReturn("86400000");

        AuthenticationResponse authenticationResponse = userService.register(registrationRequest);

        assertNotNull(authenticationResponse);
        assertEquals("jwtToken", authenticationResponse.getJwtToken());
        assertNotNull(authenticationResponse.getRefreshToken());
        verify(userRepository).saveAndFlush(any(User.class));
    }

    @Test
    void testRegisterEmailAlreadyExists() {
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user));

        assertThrows(UniqueUsernameException.class, () -> userService.register(registrationRequest));
        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void testAuthenticateSuccess() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(any(User.class))).thenReturn("jwtToken");
        when(jwtUtil.getRefreshExpiration()).thenReturn("86400000");

        AuthenticationResponse response = userService.authenticate(authenticationRequest);

        assertNotNull(response);
        assertEquals("jwtToken", response.getJwtToken());
        assertNotNull(response.getRefreshToken());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void testAuthenticateBadCredentialsException() {
        AuthenticationRequest request = new AuthenticationRequest(authenticationRequest.getUsername(),
                authenticationRequest.getPassword());

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> userService.authenticate(request));

        verify(authenticationManager, times(1))
                .authenticate(argThat(token ->
                        token instanceof UsernamePasswordAuthenticationToken &&
                                token.getPrincipal().equals(authenticationRequest.getUsername()) &&
                                token.getCredentials().equals(authenticationRequest.getPassword())
                ));
    }

    @Test
    void testDeleteUserByIdSuccess() {
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        UserResponse expectedResponse = new UserResponse();
        expectedResponse.setUsername(user.getUsername());
        expectedResponse.setAuthority(user.getAuthority());
        when(userMapper.userToUserResponse(any(User.class))).thenReturn(expectedResponse);

        UserResponse userResponse = this.userService.deleteUserById(id);

        assertNotNull(userResponse);
        assertEquals(user.getUsername(), userResponse.getUsername());
        verify(userRepository, times(1)).delete(user);
    }

    @Test
    void testDeleteUserByIdNoSuchElementException() {
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> userService.deleteUserById(id));
        verify(userRepository, never()).delete(user);
    }

    @Test
    void testRefreshTokenSuccess() {
        when(this.refreshTokenRepository.findByToken(anyString())).thenReturn(refreshToken);
        when(this.jwtUtil.generateToken(any(UserDetails.class))).thenReturn("JWT token");

        RefreshResponse token = this.userService.refreshToken(tokenRefreshRequest);

        assertEquals("JWT token", token.getJwtToken());
    }

    @Test
    void testRefreshTokenBadCredentialException() {
        when(this.refreshTokenRepository.findByToken(anyString())).thenReturn(null);

        assertThrows(BadCredentialsException.class, () -> this.userService.refreshToken(tokenRefreshRequest));
    }

    @Test
    void testRefreshTokenRefreshTknExpiredException() {
        refreshToken.setExpiresAt(Instant.now().minusSeconds(60));

        when(this.refreshTokenRepository.findByToken(any())).thenReturn(refreshToken);

        assertThrows(RefreshTknExpireException.class, () -> this.userService.refreshToken(tokenRefreshRequest));
    }

    @Test
    void testChangePasswordSuccess() {
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        String result = this.userService.changePassword(passwordChangeRequest);

        assertEquals("Password changed", result);
        verify(userRepository, times(1)).saveAndFlush(user);
    }

    @Test
    void testChangePasswordWrongPassword() {
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> this.userService.changePassword(passwordChangeRequest));

        verify(userRepository, never()).saveAndFlush(user);
    }

    @Test
    void testChangePasswordNoSuchElementException() {
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> this.userService.changePassword(passwordChangeRequest));
    }

    @Test
    void testChangeUserAuthorityByIdSuccess() {
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        UserResponse expectedResponse = new UserResponse();
        expectedResponse.setAuthority("test");
        when(userMapper.userToUserResponse(any(User.class))).thenReturn(expectedResponse);

        UserResponse userResponse = this.userService.changeUserAuthorityById(id, "test");

        assertEquals("test", userResponse.getAuthority());
        verify(userRepository, times(1)).saveAndFlush(user);
    }


    @Test
    void testChangeUserAuthorityByIdNoSuchElementException() {
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> this.userService.changeUserAuthorityById(id, "test"));

        verify(userRepository, never()).saveAndFlush(any(User.class));
    }
}