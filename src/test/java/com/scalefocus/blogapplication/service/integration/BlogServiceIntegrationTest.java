package com.scalefocus.blogapplication.service.integration;

import com.scalefocus.blogapplication.dto.*;
import com.scalefocus.blogapplication.model.MediaType;
import com.scalefocus.blogapplication.repository.BlogPostRepository;
import com.scalefocus.blogapplication.repository.UserRepository;
import com.scalefocus.blogapplication.service.BlogService;
import com.scalefocus.blogapplication.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.data.domain.Page;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

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

    @Test
    void testAddMediaFiles() throws IOException {
        // Create a blog post
        BlogPostDto newBlog = new BlogPostDto();
        newBlog.setTitle("Blog with Media " + System.currentTimeMillis());
        newBlog.setContent("Content for blog with media.");
        BlogPostDto createdBlog = blogService.createBlog(newBlog);

        BufferedImage image = new BufferedImage(100, 200, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setPaint(Color.BLUE);
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        baos.flush();
        byte[] imageBytes = baos.toByteArray();
        baos.close();

        // Add media files to the blog post
        MultipartFile multipartFile = new MockMultipartFile(
                "file",
                "image.jpg",
                "image/jpeg",
                imageBytes
        );

        MediaFileDto mediaFileDto = MediaFileDto.builder()
                .content(imageBytes)
                .mediaType(MediaType.IMAGE)
                .width(100)
                .height(200)
                .size(multipartFile.getSize())
                .build();

        BlogPostDto updatedBlog = blogService.addMediaFiles(createdBlog.getId(), List.of(multipartFile), List.of(mediaFileDto));

        assertNotNull(updatedBlog);
        assertEquals(1, updatedBlog.getMediaFiles().size());
        MediaFileDto addedMediaFile = updatedBlog.getMediaFiles().get(0);
        assertEquals(MediaType.IMAGE, addedMediaFile.getMediaType());
        assertEquals(multipartFile.getSize(), addedMediaFile.getSize());
        assertEquals(100, addedMediaFile.getWidth());
        assertEquals(200, addedMediaFile.getHeight());
    }

    @Test
    void testRemoveMediaFile() throws IOException {
        // Create a blog post
        BlogPostDto newBlog = new BlogPostDto();
        newBlog.setTitle("Blog with Media " + System.currentTimeMillis());
        newBlog.setContent("Content for blog with media.");
        BlogPostDto createdBlog = blogService.createBlog(newBlog);

        BufferedImage image = new BufferedImage(100, 200, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setPaint(Color.BLUE);
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        baos.flush();
        byte[] imageBytes = baos.toByteArray();
        baos.close();

        // Add media files to the blog post
        MultipartFile multipartFile = new MockMultipartFile(
                "file",
                "image.jpg",
                "image/jpeg",
                imageBytes
        );

        MediaFileDto mediaFileDto = MediaFileDto.builder()
                .content(imageBytes)
                .mediaType(MediaType.IMAGE)
                .width(100)
                .height(200)
                .size(multipartFile.getSize())
                .build();

        BlogPostDto updatedBlog = blogService.addMediaFiles(createdBlog.getId(), List.of(multipartFile), List.of(mediaFileDto));

        // Remove the media file
        Long mediaFileId = updatedBlog.getMediaFiles().get(0).getId();
        BlogPostDto blogAfterRemoval = blogService.removeMediaFile(createdBlog.getId(), mediaFileId);

        assertNotNull(blogAfterRemoval);
        assertTrue(blogAfterRemoval.getMediaFiles().isEmpty());
    }
}
