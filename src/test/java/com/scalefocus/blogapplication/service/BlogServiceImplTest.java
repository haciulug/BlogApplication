package com.scalefocus.blogapplication.service;

import com.scalefocus.blogapplication.dto.BlogPostDto;
import com.scalefocus.blogapplication.dto.MediaFileDto;
import com.scalefocus.blogapplication.dto.TagDto;
import com.scalefocus.blogapplication.mapper.BlogPostMapper;
import com.scalefocus.blogapplication.mapper.MediaFileMapper;
import com.scalefocus.blogapplication.model.*;
import com.scalefocus.blogapplication.repository.BlogPostRepository;
import com.scalefocus.blogapplication.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StreamUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BlogServiceImplTest {

    @Mock
    private BlogPostRepository blogPostRepository;

    @Mock
    private BlogPostMapper blogPostMapper;

    @Mock
    private TagService tagService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MediaFileMapper mediaFileMapper;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private BlogServiceImpl blogService;

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

        BlogPost blogPost = new BlogPost();
        blogPost.setId(1L);
        blogPost.setTitle("Test Blog");
        blogPost.setUser(testUser);
        blogPost.setTags(new HashSet<>());
        when(blogPostRepository.findById(1L)).thenReturn(Optional.of(blogPost));
    }

    @Test
    void createBlog_ShouldCreateBlog_WhenGivenValidBlog() {
        // Arrange
        BlogPostDto dto = new BlogPostDto();
        dto.setTitle("Test Blog");
        dto.setContent("Test Content");

        BlogPostDto returnedDto = new BlogPostDto();
        returnedDto.setId(1L);
        returnedDto.setTitle(dto.getTitle());
        returnedDto.setContent(dto.getContent());

        // Override default username for this test
        when(authentication.getName()).thenReturn("testUser");

        BlogPost blogEntity = new BlogPost();
        blogEntity.setTitle(dto.getTitle());
        blogEntity.setContent(dto.getContent());
        blogEntity.setTags(new HashSet<>()); // Initialize tags

        when(blogPostMapper.toEntity(any(BlogPostDto.class))).thenReturn(blogEntity);
        when(blogPostRepository.save(any(BlogPost.class))).thenReturn(blogEntity);
        when(blogPostMapper.toDto(any(BlogPost.class))).thenReturn(returnedDto);
        when(userRepository.findByUsername("testUser")).thenReturn(Optional.of(new User()));

        // Act
        BlogPostDto createdBlog = blogService.createBlog(dto);

        // Assert
        assertNotNull(createdBlog);
        assertEquals("Test Blog", createdBlog.getTitle());
        verify(blogPostRepository).save(any(BlogPost.class));
    }

    @Test
    void getBlogs_ShouldReturnAllBlogs_WhenBlogsExist() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        List<BlogPost> blogPosts = List.of(new BlogPost());
        Page<BlogPost> blogPostPage = new PageImpl<>(blogPosts, pageable, blogPosts.size());
        when(blogPostRepository.findAll(any(Pageable.class))).thenReturn(blogPostPage);

        BlogPostDto blogPostDto = new BlogPostDto();
        when(blogPostMapper.toDto(any(BlogPost.class))).thenReturn(blogPostDto);

        // Act
        Page<BlogPostDto> result = blogService.getBlogs(0, 10);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertFalse(result.isEmpty(), "Result should not be empty");
        verify(blogPostRepository, times(1)).findAll(any(Pageable.class));
        verify(blogPostMapper, times(blogPosts.size())).toDto(any(BlogPost.class));
    }

    @Test
    void deleteBlog_ShouldRemoveBlog_WhenBlogExists() {
        // Arrange
        Long blogId = 1L;
        BlogPost blogPost = new BlogPost();
        blogPost.setId(blogId);
        User user = new User();
        user.setUsername("testUser");
        blogPost.setUser(user);

        when(blogPostRepository.findById(blogId)).thenReturn(Optional.of(blogPost));
        when(authentication.getName()).thenReturn("testUser");

        // Act
        blogService.deleteBlog(blogId);

        // Assert
        verify(blogPostRepository).delete(blogPost);
    }

    @Test
    void getBlog_ShouldReturnBlog_WhenBlogExists() {
        // Arrange
        Long blogId = 1L;
        BlogPost blogPost = new BlogPost();
        blogPost.setId(blogId);
        when(blogPostRepository.findById(blogId)).thenReturn(Optional.of(blogPost));
        when(blogPostMapper.toDto(blogPost)).thenReturn(new BlogPostDto());

        // Act
        BlogPostDto result = blogService.getBlog(blogId);

        // Assert
        assertNotNull(result);
        verify(blogPostRepository).findById(blogId);
        verify(blogPostMapper).toDto(blogPost);
    }

    @Test
    void updateBlog_ShouldUpdateBlog_WhenBlogExists() {
        // Arrange
        Long blogId = 1L;
        String username = "testUser";

        User user = new User();
        user.setUsername(username);

        BlogPost existingBlog = new BlogPost();
        existingBlog.setId(blogId);
        existingBlog.setUser(user); // Associate the user

        BlogPostDto dto = new BlogPostDto();
        dto.setId(blogId);
        dto.setTitle("Updated Title");
        dto.setContent("Updated Content");

        when(blogPostRepository.findById(blogId)).thenReturn(Optional.of(existingBlog));
        when(blogPostRepository.save(existingBlog)).thenReturn(existingBlog);
        when(blogPostMapper.toDto(existingBlog)).thenReturn(dto);

        when(authentication.getName()).thenReturn(username);

        // Act
        BlogPostDto updatedBlog = blogService.updateBlog(blogId, dto);

        // Assert
        assertNotNull(updatedBlog, "Updated blog should not be null");
        assertEquals(blogId, updatedBlog.getId(), "Blog ID should match");
        assertEquals("Updated Title", updatedBlog.getTitle(), "Blog title should be updated");
        assertEquals("Updated Content", updatedBlog.getContent(), "Blog content should be updated");

        // Verify interactions
        verify(blogPostRepository).findById(blogId);
        verify(blogPostRepository).save(existingBlog);
        verify(blogPostMapper).toDto(existingBlog);
    }

    @Test
    void addTag_ShouldAddTagToBlog_WhenBlogExists() {
        // Arrange
        Long blogId = 1L;
        TagDto tagDto = TagDto.builder().name("Test Tag").build();
        BlogPost blogPost = new BlogPost();
        blogPost.setId(blogId);
        blogPost.setTags(new HashSet<>()); // Initialize tags

        Tag tagEntity = new Tag();
        tagEntity.setName("Test Tag");

        when(blogPostRepository.findById(blogId)).thenReturn(Optional.of(blogPost));
        when(tagService.toEntity(tagDto)).thenReturn(tagEntity);
        when(tagService.findOrCreateTag(tagEntity)).thenReturn(tagEntity);
        when(blogPostRepository.save(blogPost)).thenReturn(blogPost);
        when(blogPostMapper.toDto(blogPost)).thenReturn(new BlogPostDto());

        // Act
        BlogPostDto result = blogService.addTag(blogId, tagDto);

        // Assert
        assertNotNull(result);
        verify(blogPostRepository).save(blogPost);
        verify(blogPostRepository).findById(blogId);
        verify(tagService).findOrCreateTag(tagEntity);
    }

    @Test
    void addTagByName_ShouldAddTagToBlog_WhenBlogExists() {
        // Arrange
        Long blogId = 1L;
        String tagName = "Test Tag";
        BlogPost blogPost = new BlogPost();
        blogPost.setId(blogId);

        TagDto tagDto = TagDto.builder().name(tagName).build();
        Tag tagEntity = new Tag();
        tagEntity.setName(tagName);

        when(blogPostRepository.findById(blogId)).thenReturn(Optional.of(blogPost));
        when(tagService.getTagByName(tagName)).thenReturn(null); // Simulate tag not existing
        when(tagService.createTag(any(TagDto.class))).thenReturn(tagDto);
        when(tagService.toEntity(tagDto)).thenReturn(tagEntity);
        when(blogPostRepository.save(blogPost)).thenReturn(blogPost);
        when(blogPostMapper.toDto(blogPost)).thenReturn(new BlogPostDto());

        // Act
        BlogPostDto result = blogService.addTagByName(blogId, tagName);

        // Assert
        assertNotNull(result);
        verify(blogPostRepository).save(blogPost);
        verify(blogPostRepository).findById(blogId);
        verify(tagService).createTag(any(TagDto.class));
        verify(tagService).toEntity(tagDto);
    }

    @Test
    void removeTag_ShouldRemoveTagFromBlog_WhenBlogExists() {
        // Arrange
        Long blogId = 1L;
        String tagName = "Test Tag";
        BlogPost blogPost = new BlogPost();
        blogPost.setId(blogId);
        Tag tag = new Tag();
        tag.setName(tagName);
        blogPost.getTags().add(tag);

        when(blogPostRepository.findById(blogId)).thenReturn(Optional.of(blogPost));
        when(blogPostRepository.save(blogPost)).thenReturn(blogPost);
        when(blogPostMapper.toDto(blogPost)).thenReturn(new BlogPostDto());

        // Act
        BlogPostDto result = blogService.removeTag(blogId, tagName);

        // Assert
        assertNotNull(result);
        verify(blogPostRepository).save(blogPost);
        verify(blogPostRepository).findById(blogId);
    }

    @Test
    void getBlogsByTag_ShouldReturnBlogsByTag_WhenTagExists() {
        // Arrange
        String tagName = "Test Tag";
        Pageable pageable = PageRequest.of(0, 10);
        List<BlogPost> blogPosts = List.of(new BlogPost());
        Page<BlogPost> blogPostPage = new PageImpl<>(blogPosts, pageable, blogPosts.size());

        when(blogPostRepository.findAllByTags_Name(eq(tagName), any(Pageable.class))).thenReturn(blogPostPage);
        when(blogPostMapper.toDto(any(BlogPost.class))).thenReturn(new BlogPostDto());

        // Act
        Page<BlogPostDto> result = blogService.getBlogsByTag(tagName, 0, 10);

        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
        verify(blogPostRepository).findAllByTags_Name(eq(tagName), any(Pageable.class));
        verify(blogPostMapper, times(blogPosts.size())).toDto(any(BlogPost.class));
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

        when(blogPostRepository.findById(blogId)).thenReturn(Optional.of(blogPost));
        when(mediaFileMapper.toDto(any(MediaFile.class))).thenReturn(new MediaFileDto());

        BlogPostDto blogPostDto = new BlogPostDto();
        MediaFileDto mediaFileDto = new MediaFileDto();
        blogPostDto.setMediaFiles(List.of(mediaFileDto));
        when(blogPostMapper.toDto(any(BlogPost.class))).thenReturn(blogPostDto);

        // Act
        BlogPostDto result = blogService.addMediaFiles(blogId, List.of(multipartFile));

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
        BlogPostDto result = blogService.removeMediaFile(blogId, mediaFileId);

        // Assert
        assertNotNull(result);
        verify(blogPostRepository).save(blogPost);
        verify(blogPostRepository).findById(blogId);
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void updateBlog_ShouldThrowException_WhenBlogDoesNotExist() {
        // Arrange
        Long blogId = 1L;
        BlogPostDto dto = new BlogPostDto();
        dto.setId(blogId);

        when(blogPostRepository.findById(blogId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> blogService.updateBlog(blogId, dto));
    }

    @Test
    void createBlog_ShouldThrowException_WhenGivenNullBlog() {
        assertThrows(IllegalArgumentException.class, () -> blogService.createBlog(null));
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void getBlog_ShouldThrowException_WhenBlogDoesNotExist() {
        Long blogId = 1L;

        when(blogPostRepository.findById(blogId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> blogService.getBlog(blogId));
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void deleteBlog_ShouldThrowException_WhenBlogDoesNotExist() {
        Long nonExistentBlogId = 999L;

        when(blogPostRepository.findById(nonExistentBlogId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> blogService.deleteBlog(nonExistentBlogId));

        verify(blogPostRepository, never()).delete(any(BlogPost.class));
    }
}
