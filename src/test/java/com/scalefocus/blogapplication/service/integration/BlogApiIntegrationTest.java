package com.scalefocus.blogapplication.service.integration;

import com.scalefocus.blogapplication.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class BlogApiIntegrationTest {

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
    void testCreateBlogPost() {
        BlogPostDto blogPostDto = BlogPostDto.builder().title("Integration Test Blog").content("Integration test blog content.").build();

        HttpEntity<BlogPostDto> request = new HttpEntity<>(blogPostDto, headers);
        ResponseEntity<BlogPostDto> response = restTemplate.postForEntity(getRootUrl() + "/api/blogs", request, BlogPostDto.class);

        assertNotNull(response.getBody());
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("Integration Test Blog", response.getBody().getTitle());
    }

    @Test
    void testGetAllBlogPosts() {
        String url = getRootUrl() + "/api/blogs?page=0&size=10";

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<PageResponse<BlogPostDto>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                requestEntity,
                new ParameterizedTypeReference<PageResponse<BlogPostDto>>() {}
        );

        assertNotNull(response.getBody());
        assertFalse(response.getBody().getContent().isEmpty());
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testUpdateBlogPost() {
        // Create a blog post
        BlogPostDto blogPostDto = BlogPostDto.builder().title("Blog to Update").content("Initial content.").build();

        HttpEntity<BlogPostDto> request = new HttpEntity<>(blogPostDto, headers);
        ResponseEntity<BlogPostDto> createResponse = restTemplate.postForEntity(getRootUrl() + "/api/blogs", request, BlogPostDto.class);
        Long blogId = createResponse.getBody().getId();

        // Update the blog post
        BlogPostDto updatedBlog = BlogPostDto.builder().title("Updated Title").content("Updated content.").build();

        HttpEntity<BlogPostDto> updateRequest = new HttpEntity<>(updatedBlog, headers);
        restTemplate.exchange(getRootUrl() + "/api/blogs/" + blogId, HttpMethod.PUT, updateRequest, BlogPostDto.class);

        // Retrieve the updated blog post
        HttpEntity<Void> getRequest = new HttpEntity<>(headers);
        ResponseEntity<BlogPostDto> getResponse = restTemplate.exchange(
                getRootUrl() + "/api/blogs/" + blogId,
                HttpMethod.GET,
                getRequest,
                BlogPostDto.class
        );

        assertEquals("Updated Title", getResponse.getBody().getTitle());
        assertEquals("Updated content.", getResponse.getBody().getContent());
    }

    @Test
    void testDeleteBlogPost() {
        // Create a blog post
        BlogPostDto blogPostDto = BlogPostDto.builder().title("Blog to Delete").content("Content to delete.").build();

        HttpEntity<BlogPostDto> request = new HttpEntity<>(blogPostDto, headers);
        ResponseEntity<BlogPostDto> createResponse = restTemplate.postForEntity(getRootUrl() + "/api/blogs", request, BlogPostDto.class);
        Long blogId = createResponse.getBody().getId();

        // Delete the blog post
        HttpEntity<Void> deleteRequest = new HttpEntity<>(headers);
        restTemplate.exchange(getRootUrl() + "/api/blogs/" + blogId, HttpMethod.DELETE, deleteRequest, Void.class);

        // Attempt to retrieve the deleted blog
        HttpEntity<Void> getRequest = new HttpEntity<>(headers);
        ResponseEntity<BlogPostDto> getResponse = restTemplate.exchange(
                getRootUrl() + "/api/blogs/" + blogId,
                HttpMethod.GET,
                getRequest,
                BlogPostDto.class
        );

        assertEquals(HttpStatus.NOT_FOUND, getResponse.getStatusCode());
    }

    @Test
    void testGetBlogPost() {
        // Create a blog post
        BlogPostDto newBlog = new BlogPostDto();
        newBlog.setTitle("Specific Blog");
        newBlog.setContent("Specific content.");
        HttpEntity<BlogPostDto> request = new HttpEntity<>(newBlog, headers);
        ResponseEntity<BlogPostDto> createResponse = restTemplate.postForEntity(getRootUrl() + "/api/blogs", request, BlogPostDto.class);

        Long id = createResponse.getBody().getId();

        // Retrieve the blog post
        HttpEntity<Void> getRequest = new HttpEntity<>(headers);
        ResponseEntity<BlogPostDto> response = restTemplate.exchange(
                getRootUrl() + "/api/blogs/" + id,
                HttpMethod.GET,
                getRequest,
                BlogPostDto.class
        );

        assertNotNull(response.getBody());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Specific Blog", response.getBody().getTitle());
    }

    @Test
    void testAddTag() {
        // Create a blog post
        BlogPostDto newBlog = new BlogPostDto();
        newBlog.setTitle("Blog for Tagging");
        newBlog.setContent("Content before tagging.");
        HttpEntity<BlogPostDto> request = new HttpEntity<>(newBlog, headers);
        ResponseEntity<BlogPostDto> createResponse = restTemplate.postForEntity(getRootUrl() + "/api/blogs", request, BlogPostDto.class);

        Long id = createResponse.getBody().getId();
        TagDto newTag = TagDto.builder().name("New Tag").build();

        HttpEntity<TagDto> tagRequest = new HttpEntity<>(newTag, headers);
        ResponseEntity<BlogPostDto> response = restTemplate.postForEntity(getRootUrl() + "/api/blogs/" + id + "/tag", tagRequest, BlogPostDto.class);

        assertNotNull(response.getBody());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().getTags().stream().anyMatch(tag -> tag.getName().equals("New Tag")));
    }

    @Test
    void testRemoveTag() {
        // Create a blog post
        BlogPostDto newBlog = new BlogPostDto();
        newBlog.setTitle("Blog to Remove Tag");
        newBlog.setContent("Content with tag.");
        HttpEntity<BlogPostDto> request = new HttpEntity<>(newBlog, headers);
        ResponseEntity<BlogPostDto> createResponse = restTemplate.postForEntity(getRootUrl() + "/api/blogs", request, BlogPostDto.class);
        Long id = createResponse.getBody().getId();

        // Add a tag to the blog post
        TagDto tagDto = TagDto.builder().name("Initial Tag").build();
        HttpEntity<TagDto> tagRequest = new HttpEntity<>(tagDto, headers);
        restTemplate.postForEntity(getRootUrl() + "/api/blogs/" + id + "/tag", tagRequest, BlogPostDto.class);

        // Remove the tag
        HttpEntity<Void> deleteRequest = new HttpEntity<>(headers);
        restTemplate.exchange(getRootUrl() + "/api/blogs/" + id + "/tag/Initial Tag", HttpMethod.DELETE, deleteRequest, Void.class);

        // Verify the tag is removed
        HttpEntity<Void> getRequest = new HttpEntity<>(headers);
        ResponseEntity<BlogPostDto> response = restTemplate.exchange(
                getRootUrl() + "/api/blogs/" + id,
                HttpMethod.GET,
                getRequest,
                BlogPostDto.class
        );

        assertTrue(response.getBody().getTags().isEmpty());
    }

    @Test
    void testGetBlogsByTag() {
        // Create blog posts with the tag
        TagDto tag = TagDto.builder().name("Tech").build();
        BlogPostDto blog1 = BlogPostDto.builder().title("Tagged Blog 1").content("Content 1").tags(Set.of(tag)).build();
        BlogPostDto blog2 = BlogPostDto.builder().title("Tagged Blog 2").content("Content 2").tags(Set.of(tag)).build();

        HttpEntity<BlogPostDto> request1 = new HttpEntity<>(blog1, headers);
        restTemplate.postForEntity(getRootUrl() + "/api/blogs", request1, BlogPostDto.class);

        HttpEntity<BlogPostDto> request2 = new HttpEntity<>(blog2, headers);
        restTemplate.postForEntity(getRootUrl() + "/api/blogs", request2, BlogPostDto.class);

        // Retrieve blogs by tag
        String url = getRootUrl() + "/api/blogs/tags/Tech/blogs?page=0&size=10";

        HttpEntity<Void> getRequest = new HttpEntity<>(headers);
        ResponseEntity<PageResponse<BlogPostDto>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                getRequest,
                new ParameterizedTypeReference<PageResponse<BlogPostDto>>() {}
        );

        assertNotNull(response.getBody());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().getContent().size());
    }

    @Test
    void testGetSummarizedBlogs() {
        // Create a blog post
        BlogPostDto blogPostDto = BlogPostDto.builder().title("Summarized Blog").content("Summarized content.").build();
        HttpEntity<BlogPostDto> request = new HttpEntity<>(blogPostDto, headers);
        restTemplate.postForEntity(getRootUrl() + "/api/blogs", request, BlogPostDto.class);

        // Retrieve summarized blogs
        String url = getRootUrl() + "/api/blogs/summarized?page=0&size=10";

        HttpEntity<Void> getRequest = new HttpEntity<>(headers);
        ResponseEntity<PageResponse<BlogPostSummaryDto>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                getRequest,
                new ParameterizedTypeReference<PageResponse<BlogPostSummaryDto>>() {}
        );

        assertNotNull(response.getBody());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertFalse(response.getBody().getContent().isEmpty());
    }

    @Test
    void testSearchBlogs() {
        // Create blog posts
        BlogPostDto blog1 = BlogPostDto.builder().title("Searchable Blog 1").content("Content about Java.").build();
        BlogPostDto blog2 = BlogPostDto.builder().title("Searchable Blog 2").content("Content about Spring.").build();

        HttpEntity<BlogPostDto> request1 = new HttpEntity<>(blog1, headers);
        restTemplate.postForEntity(getRootUrl() + "/api/blogs", request1, BlogPostDto.class);

        HttpEntity<BlogPostDto> request2 = new HttpEntity<>(blog2, headers);
        restTemplate.postForEntity(getRootUrl() + "/api/blogs", request2, BlogPostDto.class);

        // Search blogs
        String url = getRootUrl() + "/api/blogs/search?query=Java&page=0&size=10";

        HttpEntity<Void> getRequest = new HttpEntity<>(headers);
        ResponseEntity<PageResponse<BlogPostDto>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                getRequest,
                new ParameterizedTypeReference<PageResponse<BlogPostDto>>() {}
        );

        assertNotNull(response.getBody());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().getContent().size());
        assertEquals("Searchable Blog 1", response.getBody().getContent().get(0).getTitle());
    }

    @Test
    void testAddMediaFiles() {
        // Create a blog post
        BlogPostDto blogPostDto = BlogPostDto.builder().title("Blog with Media").content("Content with media.").build();
        HttpEntity<BlogPostDto> request = new HttpEntity<>(blogPostDto, headers);
        ResponseEntity<BlogPostDto> createResponse = restTemplate.postForEntity(getRootUrl() + "/api/blogs", request, BlogPostDto.class);
        Long blogId = createResponse.getBody().getId();

        // Add media files
        MediaFileDto mediaFileDto = new MediaFileDto();
        mediaFileDto.setUrl("http://example.com/image.jpg");
        mediaFileDto.setMediaType(com.scalefocus.blogapplication.model.MediaType.IMAGE);

        HttpEntity<List<MediaFileDto>> mediaRequest = new HttpEntity<>(List.of(mediaFileDto), headers);
        ResponseEntity<BlogPostDto> response = restTemplate.postForEntity(getRootUrl() + "/api/blogs/" + blogId + "/media", mediaRequest, BlogPostDto.class);

        assertNotNull(response.getBody());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().getMediaFiles().size());
    }

    @Test
    void testRemoveMediaFile() {
        // Create a blog post
        String uniqueTitle = "Blog with Media " + UUID.randomUUID().toString();
        BlogPostDto blogPostDto = BlogPostDto.builder().title(uniqueTitle).content("Content with media.").build();
        HttpEntity<BlogPostDto> request = new HttpEntity<>(blogPostDto, headers);
        ResponseEntity<BlogPostDto> createResponse = restTemplate.postForEntity(getRootUrl() + "/api/blogs", request, BlogPostDto.class);
        Long blogId = createResponse.getBody().getId();

        // Add media files
        MediaFileDto mediaFileDto = new MediaFileDto();
        mediaFileDto.setUrl("http://example.com/image.jpg");
        mediaFileDto.setMediaType(com.scalefocus.blogapplication.model.MediaType.IMAGE);

        HttpEntity<List<MediaFileDto>> mediaRequest = new HttpEntity<>(List.of(mediaFileDto), headers);
        ResponseEntity<BlogPostDto> addMediaResponse = restTemplate.postForEntity(getRootUrl() + "/api/blogs/" + blogId + "/media", mediaRequest, BlogPostDto.class);
        List<MediaFileDto> mediaFiles = addMediaResponse.getBody().getMediaFiles();
        assertFalse(mediaFiles.isEmpty(), "Media files list should not be empty");
        Long mediaFileId = mediaFiles.get(0).getId();

        // Remove media file
        HttpEntity<Void> deleteRequest = new HttpEntity<>(headers);
        restTemplate.exchange(getRootUrl() + "/api/blogs/" + blogId + "/media/" + mediaFileId, HttpMethod.DELETE, deleteRequest, BlogPostDto.class);

        // Verify media file is removed
        HttpEntity<Void> getRequest = new HttpEntity<>(headers);
        ResponseEntity<BlogPostDto> getResponse = restTemplate.exchange(
                getRootUrl() + "/api/blogs/" + blogId,
                HttpMethod.GET,
                getRequest,
                BlogPostDto.class
        );

        assertTrue(getResponse.getBody().getMediaFiles().isEmpty());
    }

    private static class PageResponse<T> {
        private List<T> content;
        private int pageNumber;
        private int pageSize;
        private long totalElements;
        private int totalPages;


        public List<T> getContent() {
            return content;
        }

        public void setContent(List<T> content) {
            this.content = content;
        }

    }
}
