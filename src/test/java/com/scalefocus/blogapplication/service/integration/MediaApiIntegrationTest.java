package com.scalefocus.blogapplication.service.integration;

import com.scalefocus.blogapplication.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class MediaApiIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    private HttpHeaders headers;

    private String getRootUrl() {
        return "http://localhost:" + port;
    }

    @BeforeEach
    void setUp() {
        headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + registerAndAuthenticateUser());
        headers.setContentType(MediaType.APPLICATION_JSON);
    }

    private String registerAndAuthenticateUser() {
        // Register the user
        RegistrationRequest registrationRequest = new RegistrationRequest();
        registrationRequest.setUsername("testUser123");
        registrationRequest.setPassword("AdminPassword123");
        registrationRequest.setDisplayName("Admin User");

        restTemplate.postForEntity(
                getRootUrl() + "/api/users/register",
                registrationRequest,
                AuthenticationResponse.class
        );

        // Authenticate the user
        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        authenticationRequest.setUsername("testUser123");
        authenticationRequest.setPassword("AdminPassword123");

        ResponseEntity<AuthenticationResponse> authResponse = restTemplate.postForEntity(
                getRootUrl() + "/api/users/login",
                authenticationRequest,
                AuthenticationResponse.class
        );

        return Objects.requireNonNull(authResponse.getBody()).getJwtToken();
    }

    @Test
    void testAddMediaFiles() {
        // Create a blog post
        BlogPostDto blogPostDto = BlogPostDto.builder()
                .title("Blog with Media")
                .content("Content with media.")
                .build();
        HttpEntity<BlogPostDto> request = new HttpEntity<>(blogPostDto, headers);
        ResponseEntity<BlogPostDto> createResponse = restTemplate.postForEntity(
                getRootUrl() + "/api/blogs", request, BlogPostDto.class);
        Long blogId = createResponse.getBody().getId();

        Resource imageResource = new ClassPathResource("test-image.jpg"); // Ensure this image exists in src/test/resources

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("files", imageResource);

        MediaFileDto mediaFileDto = MediaFileDto.builder()
                .height(100)
                .width(100)
                .build();
        body.add("mediaFiles", List.of(mediaFileDto));

        HttpHeaders fileHeaders = new HttpHeaders();
        fileHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
        fileHeaders.set("Authorization", headers.getFirst("Authorization"));

        HttpEntity<MultiValueMap<String, Object>> mediaRequest = new HttpEntity<>(body, fileHeaders);

        ResponseEntity<BlogPostDto> response = restTemplate.exchange(
                getRootUrl() + "/api/media/" + blogId + "/media",
                HttpMethod.POST,
                mediaRequest,
                BlogPostDto.class);

        assertNotNull(response.getBody());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().getMediaFiles().size());
    }

    @Test
    void testRemoveMediaFile() throws IOException {
        // Create a blog post
        String uniqueTitle = "Blog with Media " + UUID.randomUUID().toString();
        BlogPostDto blogPostDto = BlogPostDto.builder()
                .title(uniqueTitle)
                .content("Content with media.")
                .build();
        HttpEntity<BlogPostDto> request = new HttpEntity<>(blogPostDto, headers);
        ResponseEntity<BlogPostDto> createResponse = restTemplate.postForEntity(
                getRootUrl() + "/api/blogs", request, BlogPostDto.class);
        Long blogId = createResponse.getBody().getId();

        Resource imageResource = new ClassPathResource("test-image.jpg"); // Ensure this image exists

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("files", imageResource);
        MediaFileDto mediaFileDto = MediaFileDto.builder()
                .height(100)
                .width(100)
                .build();
        body.add("mediaFiles", List.of(mediaFileDto));

        HttpHeaders fileHeaders = new HttpHeaders();
        fileHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
        fileHeaders.set("Authorization", headers.getFirst("Authorization"));

        HttpEntity<MultiValueMap<String, Object>> mediaRequest = new HttpEntity<>(body, fileHeaders);

        ResponseEntity<BlogPostDto> addMediaResponse = restTemplate.exchange(
                getRootUrl() + "/api/media/" + blogId + "/media",
                HttpMethod.POST,
                mediaRequest,
                BlogPostDto.class);

        List<MediaFileDto> mediaFiles = addMediaResponse.getBody().getMediaFiles();
        assertFalse(mediaFiles.isEmpty(), "Media files list should not be empty");
        Long mediaFileId = mediaFiles.get(0).getId();

        // Remove media file
        HttpEntity<Void> deleteRequest = new HttpEntity<>(headers);
        restTemplate.exchange(
                getRootUrl() + "/api/media/" + blogId + "/media/" + mediaFileId,
                HttpMethod.DELETE,
                deleteRequest,
                Void.class
        );

        // Verify media file is removed
        HttpEntity<Void> getRequest = new HttpEntity<>(headers);
        ResponseEntity<BlogPostDto> getResponse = restTemplate.exchange(
                getRootUrl() + "/api/media/" + blogId,
                HttpMethod.GET,
                getRequest,
                BlogPostDto.class
        );

        assertTrue(getResponse.getBody().getMediaFiles().isEmpty());
    }
}
