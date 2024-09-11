package com.scalefocus.blogapplication;

import com.scalefocus.blogapplication.dto.BlogPostDto;
import com.scalefocus.blogapplication.dto.TagDto;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
@ActiveProfiles("test")
class BlogApiIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    private String getRootUrl() {
        return "http://localhost:" + port;
    }

    @Test
    void testCreateBlogPost() {
        BlogPostDto blogPostDto = BlogPostDto.builder().title("Integration Test Blog").content("Integration test blog content.").build();

        ResponseEntity<BlogPostDto> response = restTemplate.postForEntity(getRootUrl() + "/api/blogs", blogPostDto, BlogPostDto.class);

        assertNotNull(response.getBody());
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("Integration Test Blog", response.getBody().getTitle());
    }

    @Test
    void testGetAllBlogPosts() {
        ResponseEntity<List> response = restTemplate.getForEntity(getRootUrl() + "/api/blogs", List.class);

        assertNotNull(response.getBody());
        assertFalse(response.getBody().isEmpty());
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testUpdateBlogPost() {
        BlogPostDto blogPostDto = BlogPostDto.builder().title("Blog to Update").content("Initial content.").build();

        ResponseEntity<BlogPostDto> createResponse = restTemplate.postForEntity(getRootUrl() + "/api/blogs", blogPostDto, BlogPostDto.class);
        Long blogId = createResponse.getBody().getId();

        BlogPostDto updatedBlog = BlogPostDto.builder().title("Updated Title").content("Updated content.").build();

        restTemplate.put(getRootUrl() + "/api/blogs/" + blogId, updatedBlog);

        BlogPostDto retrievedBlog = restTemplate.getForObject(getRootUrl() + "/api/blogs/" + blogId, BlogPostDto.class);
        assertEquals("Updated Title", retrievedBlog.getTitle());
        assertEquals("Updated content.", retrievedBlog.getContent());
    }

    @Test
    void testDeleteBlogPost() {
        BlogPostDto blogPostDto = BlogPostDto.builder().title("Blog to Delete").content("Content of the blog to delete.").build();
        blogPostDto.setTitle("Blog to Delete");
        blogPostDto.setContent("Content of the blog to delete.");

        ResponseEntity<BlogPostDto> createResponse = restTemplate.postForEntity(getRootUrl() + "/api/blogs", blogPostDto, BlogPostDto.class);
        Long blogId = createResponse.getBody().getId();

        restTemplate.delete(getRootUrl() + "/api/blogs/" + blogId);

        try {
            ResponseEntity<BlogPostDto> getResponse = restTemplate.getForEntity(getRootUrl() + "/api/blogs/" + blogId, BlogPostDto.class);
            if (getResponse.getBody() != null) {
                fail("Blog was not deleted");
            }
        } catch (HttpClientErrorException ex) {
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }
    }

    @Test
    void testGetBlogPost() {
        BlogPostDto newBlog = new BlogPostDto();
        newBlog.setTitle("Specific Blog");
        newBlog.setContent("Specific content.");
        ResponseEntity<BlogPostDto> createResponse = restTemplate.postForEntity(getRootUrl() + "/api/blogs", newBlog, BlogPostDto.class);

        Long id = createResponse.getBody().getId();
        ResponseEntity<BlogPostDto> response = restTemplate.getForEntity(getRootUrl() + "/api/blogs/" + id, BlogPostDto.class);

        assertNotNull(response.getBody());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Specific Blog", response.getBody().getTitle());
    }

    @Test
    void testAddTag() {
        BlogPostDto newBlog = new BlogPostDto();
        newBlog.setTitle("Blog for Tagging");
        newBlog.setContent("Content before tagging.");
        ResponseEntity<BlogPostDto> createResponse = restTemplate.postForEntity(getRootUrl() + "/api/blogs", newBlog, BlogPostDto.class);

        Long id = createResponse.getBody().getId();
        TagDto newTag = TagDto.builder().name("New Tag").build();

        ResponseEntity<BlogPostDto> response = restTemplate.postForEntity(getRootUrl() + "/api/blogs/" + id + "/tag", newTag, BlogPostDto.class);

        assertNotNull(response.getBody());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().getTags().stream().anyMatch(tag -> tag.getName().equals("New Tag")));
    }

    @Test
    void testRemoveTag() {
        BlogPostDto newBlog = new BlogPostDto();
        newBlog.setTitle("Blog to Remove Tag");
        newBlog.setContent("Content with tag.");
        ResponseEntity<BlogPostDto> createResponse = restTemplate.postForEntity(getRootUrl() + "/api/blogs", newBlog, BlogPostDto.class);
        Long id = createResponse.getBody().getId();

        restTemplate.postForEntity(getRootUrl() + "/api/blogs/" + id + "/tag", TagDto.builder().name("Initial Tag").build(), BlogPostDto.class);

        restTemplate.delete(getRootUrl() + "/api/blogs/" + id + "/tag/Initial Tag");

        ResponseEntity<BlogPostDto> response = restTemplate.getForEntity(getRootUrl() + "/api/blogs/" + id, BlogPostDto.class);
        assertTrue(response.getBody().getTags().isEmpty());
    }

    @Test
    void testGetBlogsByTag() {
        TagDto tag = TagDto.builder().name("Tech").build();
        BlogPostDto blog1 = BlogPostDto.builder().title("Tagged Blog 1").content("Content 1").tags(Set.of(tag)).build();
        BlogPostDto blog2 = BlogPostDto.builder().title("Tagged Blog 2").content("Content 2").tags(Set.of(tag)).build();
        restTemplate.postForEntity(getRootUrl() + "/api/blogs", blog1, BlogPostDto.class);
        restTemplate.postForEntity(getRootUrl() + "/api/blogs", blog2, BlogPostDto.class);

        ResponseEntity<List> response = restTemplate.getForEntity(getRootUrl() + "/api/blogs/tags/Tech/blogs", List.class);

        assertNotNull(response.getBody());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
    }

    @Test
    void testGetSummarizedBlogs() {
        restTemplate.postForEntity(getRootUrl() + "/api/blogs", BlogPostDto.builder().title("Summarized Blog").content("Summarized content.").build(), BlogPostDto.class);

        ResponseEntity<List> response = restTemplate.getForEntity(getRootUrl() + "/api/blogs/summarized", List.class);

        assertNotNull(response.getBody());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertFalse(response.getBody().isEmpty());
    }
}
