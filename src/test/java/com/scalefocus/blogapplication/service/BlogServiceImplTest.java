package com.scalefocus.blogapplication.service;

import com.scalefocus.blogapplication.dto.BlogPostDto;
import com.scalefocus.blogapplication.dto.TagDto;
import com.scalefocus.blogapplication.mapper.BlogPostMapper;
import com.scalefocus.blogapplication.model.BlogPost;
import com.scalefocus.blogapplication.model.Tag;
import com.scalefocus.blogapplication.repository.BlogPostRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.commons.annotation.Testable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

@Testable
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class BlogServiceImplTest {

    @InjectMocks
    private BlogServiceImpl blogService;

    @Mock
    private BlogPostRepository blogPostRepository;

    @Mock
    private BlogPostMapper blogPostMapper;

    @Mock
    private TagService tagService;

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

        when(blogPostMapper.toEntity(any(BlogPostDto.class))).thenReturn(new BlogPost());
        when(blogPostRepository.save(any(BlogPost.class))).thenReturn(new BlogPost());
        when(blogPostMapper.toDto(any(BlogPost.class))).thenReturn(returnedDto);

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
        assertNotNull(result);
        assertFalse(result.isEmpty());
        verify(blogPostRepository).findAll();
        verify(blogPostMapper).toDtoList(blogPosts);
    }

    @Test
    void deleteBlog_ShouldRemoveBlog_WhenBlogExists() {
        // Arrange
        Long blogId = 1L;

        // Act
        blogService.deleteBlog(blogId);

        // Assert
        verify(blogPostRepository).deleteById(blogId);
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
        BlogPostDto dto = new BlogPostDto();
        dto.setId(blogId);
        BlogPost existingBlog = new BlogPost();
        existingBlog.setId(blogId);
        when(blogPostRepository.findById(blogId)).thenReturn(Optional.of(existingBlog));
        when(blogPostMapper.toEntity(dto)).thenReturn(existingBlog);
        when(blogPostRepository.save(existingBlog)).thenReturn(existingBlog);
        when(blogPostMapper.toDto(existingBlog)).thenReturn(dto);

        // Act
        BlogPostDto updatedBlog = blogService.updateBlog(blogId, dto);

        // Assert
        assertNotNull(updatedBlog);
        assertEquals(blogId, updatedBlog.getId());
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

        when(blogPostRepository.findById(blogId)).thenReturn(Optional.of(blogPost));
        when(tagService.createTag(tagDto)).thenReturn(tagDto);
        when(tagService.toEntity(tagDto)).thenReturn(new Tag());
        when(blogPostRepository.save(blogPost)).thenReturn(blogPost);
        when(blogPostMapper.toDto(blogPost)).thenReturn(new BlogPostDto());

        // Act
        BlogPostDto result = blogService.addTag(blogId, tagDto);

        // Assert
        assertNotNull(result);
        verify(blogPostRepository).save(blogPost);
        verify(blogPostRepository).findById(blogId);
        verify(tagService).createTag(tagDto);
    }

    @Test
    void addTagByName_ShouldAddTagToBlog_WhenBlogExists() {
        // Arrange
        Long blogId = 1L;
        String tagName = "Test Tag";
        BlogPost blogPost = new BlogPost();
        blogPost.setId(blogId);

        when(blogPostRepository.findById(blogId)).thenReturn(Optional.of(blogPost));
        when(tagService.getTagByName(tagName)).thenReturn(null);
        when(tagService.createTag(TagDto.builder().name(tagName).build())).thenReturn(TagDto.builder().name(tagName).build());
        when(tagService.toEntity(any(TagDto.class))).thenReturn(new Tag());
        when(blogPostRepository.save(blogPost)).thenReturn(blogPost);
        when(blogPostMapper.toDto(blogPost)).thenReturn(new BlogPostDto());

        // Act
        BlogPostDto result = blogService.addTagByName(blogId, tagName);

        // Assert
        assertNotNull(result);
        verify(blogPostRepository).save(blogPost);
        verify(blogPostRepository).findById(blogId);
        verify(tagService).createTag(TagDto.builder().name(tagName).build());
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

        when(blogPostRepository.findAllByTagsContains(tagName)).thenReturn(List.of(blogPost));
        when(blogPostMapper.toDtoList(List.of(blogPost))).thenReturn(List.of(new BlogPostDto()));

        // Act
        List<BlogPostDto> result = blogService.getBlogsByTag(tagName);

        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
        verify(blogPostRepository).findAllByTagsContains(tagName);
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

        BlogPostDto result = blogService.updateBlog(blogId, dto);

        assertNull(result);
        verify(blogPostRepository, never()).save(any(BlogPost.class));
    }

    @Test
    void createBlog_ShouldThrowException_WhenGivenNullBlog() {
        assertThrows(IllegalArgumentException.class, () -> {
            blogService.createBlog(null);
        });
    }

    @Test
    void getBlog_ShouldReturnNull_WhenBlogDoesNotExist() {
        Long blogId = 1L;
        when(blogPostRepository.findById(blogId)).thenReturn(Optional.empty());

        BlogPostDto result = blogService.getBlog(blogId);

        assertNull(result);
    }

    @Test
    void deleteBlog_ShouldHandleGracefully_WhenBlogDoesNotExist() {
        Long blogId = 999L;  // Assume this ID does not exist

        doNothing().when(blogPostRepository).deleteById(blogId);

        assertDoesNotThrow(() -> blogService.deleteBlog(blogId));
        verify(blogPostRepository).deleteById(blogId);
    }

    @Test
    void createBlog_ShouldThrowException_WhenDatabaseErrorOccurs() {
        BlogPostDto dto = new BlogPostDto();
        dto.setTitle("New Blog");
        dto.setContent("Content of new blog");

        when(blogPostMapper.toEntity(any(BlogPostDto.class))).thenReturn(new BlogPost());
        when(blogPostRepository.save(any(BlogPost.class))).thenThrow(RuntimeException.class);

        assertThrows(RuntimeException.class, () -> blogService.createBlog(dto));
        verify(blogPostRepository).save(any(BlogPost.class));
    }
}
