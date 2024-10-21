package com.scalefocus.blogapplication.service;

import com.scalefocus.blogapplication.dto.BlogPostDto;
import com.scalefocus.blogapplication.dto.MediaFileDto;
import com.scalefocus.blogapplication.mapper.BlogPostMapper;
import com.scalefocus.blogapplication.mapper.MediaFileMapper;
import com.scalefocus.blogapplication.model.*;
import com.scalefocus.blogapplication.repository.BlogPostRepository;
import com.scalefocus.blogapplication.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StreamUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MediaServiceImplTest {

    @Mock
    private BlogPostRepository blogPostRepository;

    @Mock
    private BlogPostMapper blogPostMapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MediaFileMapper mediaFileMapper;

    @InjectMocks
    private MediaServiceImpl mediaService;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @BeforeEach
    void setUp() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(authentication.getName()).thenReturn("testUser");

        User testUser = new User();
        testUser.setUsername("testUser");
        when(userRepository.findByUsername("testUser")).thenReturn(Optional.of(testUser));

    }

    @Test
    void addMediaFiles_ShouldAddMediaFilesToBlog_WhenBlogExists() throws IOException {
        // Arrange
        Long blogId = 1L;
        BlogPost blogPost = new BlogPost();
        blogPost.setId(blogId);

        ClassPathResource imgFile = new ClassPathResource("test-image.jpg");
        byte[] imageBytes = StreamUtils.copyToByteArray(imgFile.getInputStream());

        MultipartFile multipartFile = new MockMultipartFile(
                "file",
                "test-image.jpg",
                "image/jpeg",
                imageBytes
        );

        MediaFileDto ImageDto = MediaFileDto.builder()
                .mediaType(MediaType.IMAGE)
                .height(100)
                .width(100)
                .build();


        when(blogPostRepository.findById(blogId)).thenReturn(Optional.of(blogPost));
        when(mediaFileMapper.toDto(any(MediaFile.class))).thenReturn(new MediaFileDto());

        BlogPostDto blogPostDto = new BlogPostDto();
        MediaFileDto mediaFileDto = new MediaFileDto();
        blogPostDto.setMediaFiles(List.of(mediaFileDto));
        when(blogPostMapper.toDto(any(BlogPost.class))).thenReturn(blogPostDto);

        // Act
        BlogPostDto result = mediaService.addMediaFiles(blogId, List.of(multipartFile), List.of(ImageDto));

        // Assert
        assertNotNull(result);
        verify(blogPostRepository).save(blogPost);
        verify(blogPostRepository).findById(blogId);
        assertEquals(1, result.getMediaFiles().size());
    }

    @Test
    void removeMediaFile_ShouldRemoveMediaFileFromBlog_WhenBlogExists() {
        // Arrange
        Long blogId = 1L;
        Long mediaFileId = 2L;
        BlogPost blogPost = new BlogPost();
        blogPost.setId(blogId);

        MediaFile mediaFile = new MediaFile();
        mediaFile.setId(mediaFileId);
        blogPost.getMediaFiles().add(mediaFile);

        when(blogPostRepository.findById(blogId)).thenReturn(Optional.of(blogPost));
        when(blogPostRepository.save(blogPost)).thenReturn(blogPost);
        when(blogPostMapper.toDto(blogPost)).thenReturn(new BlogPostDto());

        // Act
        BlogPostDto result = mediaService.removeMediaFile(blogId, mediaFileId);

        // Assert
        assertNotNull(result);
        verify(blogPostRepository).save(blogPost);
        verify(blogPostRepository).findById(blogId);
    }
}
