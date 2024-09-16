package com.scalefocus.blogapplication.service.integration;

import com.scalefocus.blogapplication.dto.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class UserApiIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    private String getRootUrl() {
        return "http://localhost:" + port;
    }

    private String registerAndAuthenticateUser(String username, String password, String displayName) {
        // Register the user
        RegistrationRequest registrationRequest = new RegistrationRequest();
        registrationRequest.setUsername(username);
        registrationRequest.setPassword(password);
        registrationRequest.setDisplayName(displayName);

        restTemplate.postForEntity(
                getRootUrl() + "/api/users/register",
                registrationRequest,
                Void.class
        );

        // Authenticate the user
        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        authenticationRequest.setUsername(username);
        authenticationRequest.setPassword(password);

        ResponseEntity<AuthenticationResponse> authResponse = restTemplate.postForEntity(
                getRootUrl() + "/api/users/login",
                authenticationRequest,
                AuthenticationResponse.class
        );

        return authResponse.getBody().getJwtToken();
    }

    @Test
    void testRegisterUser() {
        RegistrationRequest registrationRequest = new RegistrationRequest();
        registrationRequest.setUsername("newuser");
        registrationRequest.setPassword("NewUserPassword123");
        registrationRequest.setDisplayName("New User");

        ResponseEntity<Void> response = restTemplate.postForEntity(
                getRootUrl() + "/api/users/register",
                registrationRequest,
                Void.class
        );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    void testAuthenticateUser() {
        // First, register the user
        String username = "authuser";
        String password = "AuthPassword123";
        registerAndAuthenticateUser(username, password, "Auth User");

        // Now, authenticate
        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        authenticationRequest.setUsername(username);
        authenticationRequest.setPassword(password);

        ResponseEntity<AuthenticationResponse> response = restTemplate.postForEntity(
                getRootUrl() + "/api/users/login",
                authenticationRequest,
                AuthenticationResponse.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getJwtToken());
    }

    @Test
    void testChangePassword() {
        String username = "changepassworduser";
        String oldPassword = "OldPassword123";
        String newPassword = "NewPassword123";

        // Register and authenticate the user
        String token = registerAndAuthenticateUser(username, oldPassword, "Change Password User");

        // Create the password change request
        PasswordChangeRequest passwordChangeRequest = new PasswordChangeRequest();
        passwordChangeRequest.setOldPassword(oldPassword);
        passwordChangeRequest.setNewPassword(newPassword);

        // Set the authorization header
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        HttpEntity<PasswordChangeRequest> entity = new HttpEntity<>(passwordChangeRequest, headers);

        // Send the PUT request
        ResponseEntity<String> response = restTemplate.exchange(
                getRootUrl() + "/api/users/password/change",
                HttpMethod.PUT,
                entity,
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());

        // Try to authenticate with the new password
        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        authenticationRequest.setUsername(username);
        authenticationRequest.setPassword(newPassword);

        ResponseEntity<AuthenticationResponse> authResponse = restTemplate.postForEntity(
                getRootUrl() + "/api/users/login",
                authenticationRequest,
                AuthenticationResponse.class
        );

        assertEquals(HttpStatus.OK, authResponse.getStatusCode());
        assertNotNull(authResponse.getBody().getJwtToken());
    }

    @Test
    void testDeleteUser_Unauthorized() {
        String username = "regularuser";
        String password = "UserPassword123";
        String token = registerAndAuthenticateUser(username, password, "Regular User");

        // Try to delete another user without proper authority
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Void> response = restTemplate.exchange(
                getRootUrl() + "/api/users/1",
                HttpMethod.DELETE,
                entity,
                Void.class
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

//    @Test
//    void testDeleteUser_AsAdmin() {
//        String adminUsername = "adminuser";
//        String adminPassword = "AdminPassword123";
//        String adminToken = registerAndAuthenticateUser(adminUsername, adminPassword, "Admin User");
//
//        // Register a regular user to delete
//        String username = "usertodelete";
//        String password = "DeleteMe123";
//        String displayName = "User To Delete";
//        RegistrationRequest registrationRequest = new RegistrationRequest();
//        registrationRequest.setUsername(username);
//        registrationRequest.setPassword(password);
//        registrationRequest.setDisplayName(displayName);
//
//        restTemplate.postForEntity(
//                getRootUrl() + "/api/users/register",
//                registrationRequest,
//                Void.class
//        );
//
//        // Fetch the user ID (assuming you have an endpoint to get user details)
//        Long userIdToDelete = 2L;
//
//        // Delete the user
//        HttpHeaders headers = new HttpHeaders();
//        headers.setBearerAuth(adminToken);
//
//        HttpEntity<Void> entity = new HttpEntity<>(headers);
//
//        ResponseEntity<UserResponse> response = restTemplate.exchange(
//                getRootUrl() + "/api/users/" + userIdToDelete,
//                HttpMethod.DELETE,
//                entity,
//                UserResponse.class
//        );
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//    }

}
