package com.scalefocus.blogapplication.service.integration;

import com.scalefocus.blogapplication.dto.BlogPostDto;
import com.scalefocus.blogapplication.dto.MediaFileDto;
import com.scalefocus.blogapplication.model.MediaType;
import com.scalefocus.blogapplication.repository.BlogPostRepository;
import com.scalefocus.blogapplication.service.BlogService;
import com.scalefocus.blogapplication.service.MediaService;
import com.scalefocus.blogapplication.support.WithMockJwtUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@Sql(
        scripts = "/test-data.sql",
        config = @SqlConfig(encoding = "UTF-8")
)
class MediaServiceIntegrationTest {

    @Autowired
    private BlogService blogService;

    @Autowired
    private BlogPostRepository blogPostRepository;

    @Autowired
    private MediaService mediaService;

    @BeforeEach
    void setUp() {
        blogPostRepository.deleteAll();
    }

    @Test
    @WithMockJwtUser(userId = 1L, username = "admin", roles = {"ADMIN"})
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

        BlogPostDto updatedBlog = mediaService.addMediaFiles(createdBlog.getId(), List.of(multipartFile), List.of(mediaFileDto));

        assertNotNull(updatedBlog);
        assertEquals(1, updatedBlog.getMediaFiles().size());
        MediaFileDto addedMediaFile = updatedBlog.getMediaFiles().get(0);
        assertEquals(MediaType.IMAGE, addedMediaFile.getMediaType());
        assertEquals(multipartFile.getSize(), addedMediaFile.getSize());
        assertEquals(100, addedMediaFile.getWidth());
        assertEquals(200, addedMediaFile.getHeight());
    }

    @Test
    @WithMockJwtUser(userId = 1L, username = "admin", roles = {"ADMIN"})
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

        BlogPostDto updatedBlog = mediaService.addMediaFiles(createdBlog.getId(), List.of(multipartFile), List.of(mediaFileDto));

        // Remove the media file
        Long mediaFileId = updatedBlog.getMediaFiles().get(0).getId();
        BlogPostDto blogAfterRemoval = mediaService.removeMediaFile(createdBlog.getId(), mediaFileId);

        assertNotNull(blogAfterRemoval);
        assertTrue(blogAfterRemoval.getMediaFiles().isEmpty());
    }
}
