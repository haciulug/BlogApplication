package com.scalefocus.blogapplication.service.integration;

import com.scalefocus.blogapplication.dto.BlogPostDto;
import com.scalefocus.blogapplication.dto.TagDto;
import com.scalefocus.blogapplication.mapper.BlogPostMapper;
import com.scalefocus.blogapplication.model.BlogPost;
import com.scalefocus.blogapplication.model.Tag;
import com.scalefocus.blogapplication.model.User;
import com.scalefocus.blogapplication.repository.BlogPostRepository;
import com.scalefocus.blogapplication.repository.TagRepository;
import com.scalefocus.blogapplication.repository.UserRepository;
import com.scalefocus.blogapplication.service.BlogServiceImpl;
import com.scalefocus.blogapplication.service.TagService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Testcontainers
@Transactional
class BlogServiceIntegrationCTest {

    @Autowired
    private BlogPostRepository blogPostRepository;

    @Autowired
    private BlogPostMapper blogPostMapper;

    @Autowired
    private TagService tagService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BlogServiceImpl blogService;

    @Autowired
    private TagRepository tagRepository;

    @BeforeEach
    void setUp() {
        // Clear repositories to ensure a clean state
        blogPostRepository.deleteAll();
        userRepository.deleteAll();

        // Add test user with all required fields
        User testUser = new User();
        testUser.setUsername("testUser");
        testUser.setPassword("password");
        testUser.setDisplayName("Test User");
        testUser.setAuthority("ROLE_USER"); // Set the appropriate authority
        userRepository.save(testUser);
    }

    @Container
    private static MySQLContainer<?> mysqlContainer;

    static {
        String mysqlVersion = System.getProperty("MYSQL_VERSION", "8.0.26");
        mysqlContainer = new MySQLContainer<>("mysql:" + mysqlVersion)
                .withDatabaseName("integration-tests-db")
                .withUsername("sa")
                .withPassword("sa");
        mysqlContainer.start();
    }

    @DynamicPropertySource
    static void setDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysqlContainer::getJdbcUrl);
        registry.add("spring.datasource.username", mysqlContainer::getUsername);
        registry.add("spring.datasource.password", mysqlContainer::getPassword);
    }

    @Test
    @WithMockUser(username = "testUser", roles = {"USER"})
    void createBlog_ShouldCreateBlog_WhenGivenValidBlog() {
        // Arrange
        BlogPostDto dto = new BlogPostDto();
        dto.setTitle("Test Blog");
        dto.setContent("Test Content");

        // Act
        BlogPostDto createdBlog = blogService.createBlog(dto);

        // Assert
        assertNotNull(createdBlog);
        assertEquals("Test Blog", createdBlog.getTitle());
        assertEquals("Test Content", createdBlog.getContent());
        assertNotNull(createdBlog.getId());

        // Verify that the blog post exists in the repository
        Optional<BlogPost> savedBlogPost = blogPostRepository.findById(createdBlog.getId());
        assertTrue(savedBlogPost.isPresent(), "Blog post should be saved in the repository");
        assertEquals("Test Blog", savedBlogPost.get().getTitle(), "Title should match");
        assertEquals("Test Content", savedBlogPost.get().getContent(), "Content should match");
    }

    @Test
    void getBlogs_ShouldReturnAllBlogs_WhenBlogsExist() {
        // Arrange
        User user = userRepository.findByUsername("testUser").orElseThrow();
        BlogPost blogPost = new BlogPost();
        blogPost.setTitle("Test Blog");
        blogPost.setContent("Test Content");
        blogPost.setUser(user); // Set the user
        blogPostRepository.save(blogPost);

        // Act
        Page<BlogPostDto> result = blogService.getBlogs(0, 10);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertFalse(result.isEmpty(), "Result list should not be empty");
        assertEquals(1, result.getTotalElements(), "Should return one blog post");
        assertEquals("Test Blog", result.getContent().get(0).getTitle(), "Title should match");
    }

    @Test
    @WithMockUser(username = "testUser", roles = {"USER"})
    void deleteBlog_ShouldRemoveBlog_WhenBlogExists() {
        // Arrange
        User user = userRepository.findByUsername("testUser").orElseThrow();
        BlogPost blogPost = new BlogPost();
        blogPost.setTitle("Test Blog");
        blogPost.setContent("Test Content");
        blogPost.setUser(user);
        blogPost = blogPostRepository.save(blogPost);
        Long blogId = blogPost.getId();

        // Act
        blogService.deleteBlog(blogId);

        // Assert
        Optional<BlogPost> deletedBlog = blogPostRepository.findById(blogId);
        assertFalse(deletedBlog.isPresent(), "Blog post should be deleted from the repository");
    }

    @Test
    void getBlog_ShouldReturnBlog_WhenBlogExists() {
        // Arrange
        User user = userRepository.findByUsername("testUser").orElseThrow();
        BlogPost blogPost = new BlogPost();
        blogPost.setTitle("Sample Blog");
        blogPost.setContent("Sample Content");
        blogPost.setUser(user);
        blogPost = blogPostRepository.save(blogPost);
        Long blogId = blogPost.getId();

        // Act
        BlogPostDto result = blogService.getBlog(blogId);

        // Assert
        assertNotNull(result);
        assertEquals("Sample Blog", result.getTitle());
        assertEquals("Sample Content", result.getContent());
    }

    @Test
    @WithMockUser(username = "testUser", roles = {"USER"})
    void updateBlog_ShouldUpdateBlog_WhenBlogExists() {
        // Arrange
        User user = userRepository.findByUsername("testUser").orElseThrow();

        BlogPost existingBlog = new BlogPost();
        existingBlog.setTitle("Original Title");
        existingBlog.setContent("Original Content");
        existingBlog.setUser(user);
        existingBlog = blogPostRepository.save(existingBlog);
        Long blogId = existingBlog.getId();

        BlogPostDto dto = new BlogPostDto();
        dto.setId(blogId);
        dto.setTitle("Updated Title");
        dto.setContent("Updated Content");

        // Act
        BlogPostDto updatedBlog = blogService.updateBlog(blogId, dto);

        // Assert
        assertNotNull(updatedBlog, "Updated blog should not be null");
        assertEquals(blogId, updatedBlog.getId(), "Blog ID should match");
        assertEquals("Updated Title", updatedBlog.getTitle(), "Blog title should be updated");
        assertEquals("Updated Content", updatedBlog.getContent(), "Blog content should be updated");

        // Verify that the blog post in the repository has been updated
        BlogPost savedBlogPost = blogPostRepository.findById(blogId).orElseThrow();
        assertEquals("Updated Title", savedBlogPost.getTitle(), "Blog title should be updated in repository");
        assertEquals("Updated Content", savedBlogPost.getContent(), "Blog content should be updated in repository");
    }

    @Test
    void addTag_ShouldAddTagToBlog_WhenBlogExists() {
        // Arrange
        User user = userRepository.findByUsername("testUser").orElseThrow();
        BlogPost blogPost = new BlogPost();
        blogPost.setTitle("Test Blog");
        blogPost.setContent("Test Content");
        blogPost.setUser(user);
        blogPost = blogPostRepository.save(blogPost);
        Long blogId = blogPost.getId();

        TagDto tagDto = TagDto.builder().name("Test Tag").build();

        // Act
        BlogPostDto result = blogService.addTag(blogId, tagDto);

        // Assert
        assertNotNull(result);
        assertTrue(result.getTags().stream().anyMatch(tag -> "Test Tag".equals(tag.getName())), "Tag should be added to the blog");
    }

    @Test
    void addTagByName_ShouldAddTagToBlog_WhenBlogExists() {
        // Arrange
        User user = userRepository.findByUsername("testUser").orElseThrow();
        BlogPost blogPost = new BlogPost();
        blogPost.setTitle("Test Blog");
        blogPost.setContent("Test Content");
        blogPost.setUser(user);
        blogPost = blogPostRepository.save(blogPost);
        Long blogId = blogPost.getId();

        String tagName = "Test Tag";

        // Act
        BlogPostDto result = blogService.addTagByName(blogId, tagName);

        // Assert
        assertNotNull(result);
        assertTrue(result.getTags().stream().anyMatch(tag -> tagName.equals(tag.getName())), "Tag should be added to the blog");
    }

    @Test
    void removeTag_ShouldRemoveTagFromBlog_WhenBlogExists() {
        // Arrange
        User user = userRepository.findByUsername("testUser").orElseThrow();

        // Create and save the Tag
        Tag tag = new Tag();
        tag.setName("Test Tag");
        tag = tagRepository.save(tag);

        // Create and save the BlogPost
        BlogPost blogPost = new BlogPost();
        blogPost.setTitle("Test Blog");
        blogPost.setContent("Test Content");
        blogPost.setUser(user);
        blogPost.getTags().add(tag);
        blogPost = blogPostRepository.save(blogPost);
        Long blogId = blogPost.getId();

        // Act
        BlogPostDto result = blogService.removeTag(blogId, "Test Tag");

        // Assert
        assertNotNull(result);
        assertFalse(result.getTags().stream().anyMatch(t -> t.getName().equals("Test Tag")), "Tag should be removed from the blog");

        BlogPost updatedBlogPost = blogPostRepository.findById(blogId).orElseThrow();
        assertFalse(updatedBlogPost.getTags().contains(tag), "Tag should be removed from the blog in the repository");
    }

    @Test
    void getBlogsByTag_ShouldReturnBlogsByTag_WhenTagExists() {
        // Arrange
        User user = userRepository.findByUsername("testUser").orElseThrow();

        // Create and save the Tag
        Tag tag = new Tag();
        tag.setName("Test Tag");
        tag = tagRepository.save(tag);

        // Create and save the BlogPost
        BlogPost blogPost = new BlogPost();
        blogPost.setTitle("Blog with Tag");
        blogPost.setContent("Content");
        blogPost.setUser(user);
        blogPost.getTags().add(tag);
        blogPostRepository.save(blogPost);

        // Act
        Page<BlogPostDto> result = blogService.getBlogsByTag("Test Tag", 0, 10);

        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty(), "Should return blogs with the specified tag");
        assertEquals(1, result.getTotalElements(), "Should return one blog post");
        assertEquals("Blog with Tag", result.getContent().get(0).getTitle(), "Title should match");
    }

    @Test
    void updateBlog_ShouldThrowException_WhenBlogDoesNotExist() {
        Long blogId = 999L;
        BlogPostDto dto = new BlogPostDto();
        dto.setId(blogId);
        dto.setTitle("Updated Title");
        dto.setContent("Updated Content");

        blogPostRepository.deleteAll();

        // Act & Assert
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            blogService.updateBlog(blogId, dto);
        });

        assertNotNull(exception);
        assertEquals("Blog not found with id 999", exception.getMessage());

    }

    @Test
    void createBlog_ShouldThrowException_WhenGivenNullBlog() {
        assertThrows(IllegalArgumentException.class, () -> {
            blogService.createBlog(null);
        });
    }

    @Test
    void getBlog_ShouldThrowException_WhenBlogDoesNotExist() {
        Long blogId = 999L;

        blogPostRepository.deleteAll();

        assertThrows(EntityNotFoundException.class, () -> blogService.getBlog(blogId));
    }

    @Test
    void deleteBlog_ShouldThrowException_WhenBlogDoesNotExist() {
        Long nonExistentBlogId = 999L;

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            blogService.deleteBlog(nonExistentBlogId);
        });

        assertEquals("Blog not found", exception.getMessage());
    }
}
