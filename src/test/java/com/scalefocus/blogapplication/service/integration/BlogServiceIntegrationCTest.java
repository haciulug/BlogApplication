package com.scalefocus.blogapplication.service.integration;

import com.scalefocus.blogapplication.dto.BlogPostDto;
import com.scalefocus.blogapplication.dto.TagDto;
import com.scalefocus.blogapplication.mapper.BlogPostMapper;
import com.scalefocus.blogapplication.model.BlogPost;
import com.scalefocus.blogapplication.model.Tag;
import com.scalefocus.blogapplication.model.User;
import com.scalefocus.blogapplication.repository.BlogPostRepository;
import com.scalefocus.blogapplication.repository.UserRepository;
import com.scalefocus.blogapplication.service.BlogService;
import com.scalefocus.blogapplication.service.BlogServiceImpl;
import com.scalefocus.blogapplication.service.TagService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@Testcontainers
class BlogServiceIntegrationCTest {

    @Mock
    private BlogPostRepository blogPostRepository;

    @Mock
    private BlogPostMapper blogPostMapper;

    @Mock
    private TagService tagService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BlogServiceImpl blogService;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        lenient().when(authentication.getName()).thenReturn("testUser");
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
        List<BlogPost> blogPosts = List.of(new BlogPost());
        when(blogPostRepository.findAll()).thenReturn(blogPosts);
        when(blogPostMapper.toDtoList(blogPosts)).thenReturn(List.of(new BlogPostDto()));

        // Act
        List<BlogPostDto> result = blogService.getBlogs();

        // Assert
        assertNotNull(result, "Result should not be null");
        assertFalse(result.isEmpty(), "Result list should not be empty");
        verify(blogPostRepository, times(1)).findAll();
        verify(blogPostMapper, times(1)).toDtoList(blogPosts);
    }

    @Test
    void deleteBlog_ShouldRemoveBlog_WhenBlogExists() {
        // Arrange
        Long blogId = 1L;
        BlogPost blogPost = new BlogPost();
        blogPost.setId(blogId);
        blogPost.setUser(new User());
        blogPost.getUser().setUsername("testUser");

        when(blogPostRepository.findById(blogId)).thenReturn(Optional.of(blogPost));
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("testUser");
        SecurityContextHolder.setContext(securityContext);

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
        Tag tag = new Tag();
        tag.setName(tagName);
        BlogPost blogPost = new BlogPost();
        blogPost.getTags().add(tag);

        when(blogPostRepository.findAllByTags_Name(tagName)).thenReturn(List.of(blogPost));
        when(blogPostMapper.toDtoList(List.of(blogPost))).thenReturn(List.of(new BlogPostDto()));

        // Act
        List<BlogPostDto> result = blogService.getBlogsByTag(tagName);

        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
        verify(blogPostRepository).findAllByTags_Name(tagName);
        verify(blogPostMapper).toDtoList(List.of(blogPost));
    }

    @Test
    void updateBlog_ShouldReturnNull_WhenBlogDoesNotExist() {
        Long blogId = 1L;
        BlogPostDto dto = new BlogPostDto();
        dto.setId(blogId);
        dto.setTitle("Updated Title");
        dto.setContent("Updated Content");

        when(blogPostRepository.findById(blogId)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            blogService.updateBlog(blogId, dto);
        });

        assertNotNull(exception);

        verify(blogPostRepository, never()).save(any(BlogPost.class));
    }

    @Test
    void createBlog_ShouldThrowException_WhenGivenNullBlog() {
        assertThrows(IllegalArgumentException.class, () -> {
            blogService.createBlog(null);
        });
    }

    @Test
    void getBlog_ShouldThrowException_WhenBlogDoesNotExist() {
        Long blogId = 1L;

        when(blogPostRepository.findById(blogId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> blogService.getBlog(blogId));
    }

    @Test
    void deleteBlog_ShouldThrowException_WhenBlogDoesNotExist() {
        Long nonExistentBlogId = 999L;
        doThrow(new EntityNotFoundException("Blog not found"))
                .when(blogPostRepository).findById(nonExistentBlogId);

        assertThrows(EntityNotFoundException.class, () -> blogService.deleteBlog(nonExistentBlogId));
    }
}
