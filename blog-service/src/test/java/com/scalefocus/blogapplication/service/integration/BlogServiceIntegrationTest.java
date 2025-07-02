package com.scalefocus.blogapplication.service.integration;

import com.scalefocus.blogapplication.dto.*;
import com.scalefocus.blogapplication.repository.BlogPostRepository;
import com.scalefocus.blogapplication.security.JwtUserDetails;
import com.scalefocus.blogapplication.service.BlogService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.data.domain.Page;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.scalefocus.blogapplication.mapper.BlogPostMapper.SUMMARY_LENGTH;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@Sql(
        scripts = "/test-data.sql",
        config = @SqlConfig(encoding = "UTF-8")
)
class BlogServiceIntegrationTest {

    @Autowired
    private BlogService blogService;

    @Autowired
    private BlogPostRepository blogPostRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // Mock authentication
        JwtUserDetails userDetails = new JwtUserDetails(1L, "admin", List.of(() -> "ROLE_ADMIN"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
        );

        blogPostRepository.deleteAll();

    }

    @Test
    void testCreateAndRetrieveBlog() {
        // Create a blog post with a unique title
        BlogPostDto newBlog = new BlogPostDto();
        newBlog.setTitle("Integration Test Blog " + System.currentTimeMillis());
        newBlog.setContent("Content for integration testing.");
        BlogPostDto createdBlog = blogService.createBlog(newBlog);

        assertNotNull(createdBlog);
        assertNotNull(createdBlog.getId());

        // Retrieve the blog post
        BlogPostDto retrievedBlog = blogService.getBlog(createdBlog.getId());
        assertEquals(newBlog.getTitle(), retrievedBlog.getTitle());  // Use dynamically generated title
        assertEquals("Content for integration testing.", retrievedBlog.getContent());
    }

    @Test
    void testUpdateBlog() {
        // Create a blog post
        BlogPostDto newBlog = new BlogPostDto();
        newBlog.setTitle("Original Title");
        newBlog.setContent("Original Content");
        BlogPostDto createdBlog = blogService.createBlog(newBlog);

        // Update the blog post
        BlogPostDto updatedDto = new BlogPostDto();
        updatedDto.setTitle("Updated Title");
        updatedDto.setContent("Updated Content");
        BlogPostDto updatedBlog = blogService.updateBlog(createdBlog.getId(), updatedDto);

        assertNotNull(updatedBlog);
        assertEquals("Updated Title", updatedBlog.getTitle());
        assertEquals("Updated Content", updatedBlog.getContent());

        // Verify the update
        BlogPostDto checkBlog = blogService.getBlog(createdBlog.getId());
        assertEquals("Updated Title", checkBlog.getTitle());
        assertEquals("Updated Content", checkBlog.getContent());
    }

    @Test
    void testDeleteBlog() {
        // Create a blog post
        BlogPostDto newBlog = new BlogPostDto();
        newBlog.setTitle("To Be Deleted");
        newBlog.setContent("Content that will be deleted.");
        BlogPostDto createdBlog = blogService.createBlog(newBlog);

        // Delete the blog post
        blogService.deleteBlog(createdBlog.getId());

        // Attempt to retrieve the deleted blog
        assertThrows(EntityNotFoundException.class, () -> blogService.getBlog(createdBlog.getId()));
    }

    @Test
    void testAddTag() {
        // Create a blog post
        BlogPostDto newBlog = new BlogPostDto();
        newBlog.setTitle("Blog with Tag");
        newBlog.setContent("Content for blog with tag.");
        BlogPostDto createdBlog = blogService.createBlog(newBlog);

        // Add a tag to the blog post
        BlogPostDto updatedBlog = blogService.addTagByName(createdBlog.getId(), "Test Tag");

        assertNotNull(updatedBlog);
        assertEquals(1, updatedBlog.getTags().size());
        assertEquals("Test Tag", updatedBlog.getTags().iterator().next().getName());
    }

    @Test
    void testRemoveTag() {
        // Create a blog post
        BlogPostDto newBlog = new BlogPostDto();
        newBlog.setTitle("Blog with Tag");
        newBlog.setContent("Content for blog with tag.");
        BlogPostDto createdBlog = blogService.createBlog(newBlog);

        // Add a tag to the blog post
        blogService.addTagByName(createdBlog.getId(), "Test Tag");

        // Remove the tag from the blog post
        BlogPostDto removedTagBlog = blogService.removeTag(createdBlog.getId(), "Test Tag");

        assertNotNull(removedTagBlog);
        assertTrue(removedTagBlog.getTags().isEmpty());
    }

    @Test
    void testGetBlogsByTag() {
        // Create a blog post
        BlogPostDto newBlog = new BlogPostDto();
        newBlog.setTitle("Blog with Tag");
        newBlog.setContent("Content for blog with tag.");
        BlogPostDto createdBlog = blogService.createBlog(newBlog);

        // Add a tag to the blog post
        blogService.addTagByName(createdBlog.getId(), "Test Tag");

        // Retrieve the blog posts by tag
        Page<BlogPostDto> result = blogService.getBlogsByTag("Test Tag", 0, 10);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals("Blog with Tag", result.getContent().get(0).getTitle());
    }

    @Test
    void testGetSummarizedBlogs() {
        // Create a blog post with a unique title
        BlogPostDto newBlog = new BlogPostDto();
        newBlog.setTitle("Summarized Blog " + System.currentTimeMillis());
        newBlog.setContent("Content for summarized blog.");
        blogService.createBlog(newBlog);

        // Retrieve summarized blogs
        Page<BlogPostSummaryDto> result = blogService.getSummarizedBlogs(0, 10);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(newBlog.getTitle(), result.getContent().get(0).getTitle());
        assertEquals("Content for summarized blog.".substring(0, SUMMARY_LENGTH), result.getContent().get(0).getSummary());
    }
}
