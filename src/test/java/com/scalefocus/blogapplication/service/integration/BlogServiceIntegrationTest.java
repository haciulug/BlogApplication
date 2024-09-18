package com.scalefocus.blogapplication.service.integration;

import com.scalefocus.blogapplication.dto.BlogPostDto;
import com.scalefocus.blogapplication.dto.BlogPostSummaryDto;
import com.scalefocus.blogapplication.dto.RegistrationRequest;
import com.scalefocus.blogapplication.repository.BlogPostRepository;
import com.scalefocus.blogapplication.repository.UserRepository;
import com.scalefocus.blogapplication.service.BlogService;
import com.scalefocus.blogapplication.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static com.scalefocus.blogapplication.mapper.BlogPostMapper.SUMMARY_LENGTH;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class BlogServiceIntegrationTest {

    @Autowired
    private BlogService blogService;

    @Autowired
    private BlogPostRepository blogPostRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        registerAdmin();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("admin", "admin"));
        blogPostRepository.deleteAll();
    }

    private void registerAdmin() {
        // Register the admin user
        userService.register(new RegistrationRequest("admin", "admin", "Admin"));
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
        createdBlog.setTitle("Updated Title");
        createdBlog.setContent("Updated Content");
        BlogPostDto updatedBlog = blogService.updateBlog(createdBlog.getId(), createdBlog);

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
        assertNull(blogPostRepository.findById(createdBlog.getId()).orElse(null));
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
        assertEquals("Test Tag", updatedBlog.getTags().stream().findFirst().isPresent() ? updatedBlog.getTags().stream().findFirst().get().getName() : null);
    }

    @Test
    void testRemoveTag() {
        // Create a blog post
        BlogPostDto newBlog = new BlogPostDto();
        newBlog.setTitle("Blog with Tag");
        newBlog.setContent("Content for blog with tag.");
        BlogPostDto createdBlog = blogService.createBlog(newBlog);

        // Add a tag to the blog post
        BlogPostDto updatedBlog = blogService.addTagByName(createdBlog.getId(), "Test Tag");

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
        BlogPostDto updatedBlog = blogService.addTagByName(createdBlog.getId(), "Test Tag");

        // Retrieve the blog post by tag
        BlogPostDto retrievedBlog = blogService.getBlogsByTag("Test Tag").get(0);

        assertNotNull(retrievedBlog);
        assertEquals("Blog with Tag", retrievedBlog.getTitle());
        assertEquals("Content for blog with tag.", retrievedBlog.getContent());
    }

    @Test
    void testGetSummarizedBlogs() {
        // Create a blog post with a unique title
        BlogPostDto newBlog = new BlogPostDto();
        newBlog.setTitle("Summarized Blog " + System.currentTimeMillis());  // Add a unique suffix
        newBlog.setContent("Content for summarized blog.");
        BlogPostDto createdBlog = blogService.createBlog(newBlog);

        // Retrieve summarized blogs
        BlogPostSummaryDto summarizedBlog = blogService.getSummarizedBlogs().get(0);

        assertNotNull(summarizedBlog);
        assertEquals(newBlog.getTitle(), summarizedBlog.getTitle());  // Match with unique title
        assertEquals("Content for summarized blog.".substring(0, SUMMARY_LENGTH), summarizedBlog.getSummary());
    }
}
