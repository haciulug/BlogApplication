package com.scalefocus.blogapplication.service;

import com.scalefocus.blogapplication.dto.BlogPostDto;
import com.scalefocus.blogapplication.dto.BlogPostSummaryDto;
import com.scalefocus.blogapplication.dto.TagDto;
import com.scalefocus.blogapplication.mapper.BlogPostMapper;
import com.scalefocus.blogapplication.model.BlogPost;
import com.scalefocus.blogapplication.model.Tag;
import com.scalefocus.blogapplication.repository.BlogPostRepository;
import com.scalefocus.blogapplication.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BlogServiceImpl implements BlogService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlogServiceImpl.class);

    private final BlogPostRepository blogPostRepository;
    private final BlogPostMapper blogPostMapper;
    private final TagService tagService;
    private final UserRepository userRepository;

    public BlogServiceImpl(BlogPostRepository blogPostRepository, BlogPostMapper blogPostMapper, TagService tagService, UserRepository userRepository) {
        this.blogPostRepository = blogPostRepository;
        this.blogPostMapper = blogPostMapper;
        this.tagService = tagService;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public BlogPostDto createBlog(BlogPostDto blogDto) {
        if (blogDto == null) {
            throw new IllegalArgumentException("Blog cannot be null");
        }
        BlogPost blogPost = blogPostMapper.toEntity(blogDto);
        Set<Tag> tags = new HashSet<>();
        if (blogPost.getTags() == null) {
            blogPost.setTags(tags);
        }
        for (Tag tag : blogPost.getTags()) {
            Tag managedTag = tagService.findOrCreateTag(tag);
            tags.add(managedTag);
        }
        blogPost.setTags(tags);
        blogPost.setUser(userRepository.findByUsername(SecurityContextHolder.getContext().getAuthentication().getName())
                .orElseThrow(() -> new EntityNotFoundException("User not found")));

        BlogPostDto createdBlog = blogPostMapper.toDto(blogPostRepository.save(blogPost));
        LOGGER.info("Blog with id {} created", createdBlog.getId());
        return createdBlog;
    }

    @Override
    public List<BlogPostDto> getBlogs() {
        return blogPostMapper.toDtoList(blogPostRepository.findAll());
    }

    @Override
    public void deleteBlog(Long id) {
        BlogPost blogPost = blogPostRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Blog not found"));
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!blogPost.getUser().getUsername().equals(currentUsername)) {
            throw new AccessDeniedException("You can only delete your own posts");
        }
        blogPostRepository.delete(blogPost);
        LOGGER.info("Blog with id {} deleted by user {}", id, currentUsername);
    }

    @Override
    public BlogPostDto getBlog(Long id) {
        BlogPost blogPost = blogPostRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Blog not found with id " + id));
        return blogPostMapper.toDto(blogPost);
    }

    @Override
    public BlogPostDto updateBlog(Long id, BlogPostDto blogDto) {
        BlogPost blogPost = blogPostRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Blog not found with id " + id));
        if (!blogPost.getUser().getUsername().equals(SecurityContextHolder.getContext().getAuthentication().getName())) {
            throw new AccessDeniedException("You can only update your own posts");
        }
        blogPost.setTitle(blogDto.getTitle());
        blogPost.setContent(blogDto.getContent());
        Set<Tag> tags = Optional.ofNullable(blogDto.getTags())
                .map(tagDtos -> tagDtos.stream()
                        .map(tagService::toEntity)
                        .collect(Collectors.toSet()))
                .orElse(Collections.emptySet());
        blogPost.setTags(tags);
        blogPostRepository.save(blogPost);
        LOGGER.info("Blog with id {} updated", id);
        return blogPostMapper.toDto(blogPost);
    }

    @Override
    public BlogPostDto addTag(Long id, TagDto tag) {
        BlogPost blogPost = blogPostRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Blog not found with id " + id));

        Tag managedTag = tagService.findOrCreateTag(tagService.toEntity(tag));
        blogPost.getTags().add(managedTag);
        blogPostRepository.save(blogPost);

        LOGGER.info("Tag {} added to blog with id {}", tag.getName(), id);
        return blogPostMapper.toDto(blogPost);
    }

    @Override
    public BlogPostDto addTagByName(Long id, String tagName) {
        return blogPostRepository.findById(id)
                .map(blogPost -> {
                    TagDto tag = tagService.getTagByName(tagName);
                    if (tag == null) {
                        // Tag doesn't exist, create it
                        tag = tagService.createTag(TagDto.builder().name(tagName).build());
                    }
                    Tag tagEntity = tagService.toEntity(tag);
                    blogPost.getTags().add(tagEntity);
                    blogPostRepository.save(blogPost);
                    LOGGER.info("Tag {} added to blog with id {}", tagName, id);
                    return blogPostMapper.toDto(blogPost);
                })
                .orElseThrow(() -> new EntityNotFoundException("Blog post with ID " + id + " not found"));
    }

    @Override
    public BlogPostDto removeTag(Long id, String tagName) {
        return blogPostRepository.findById(id)
                .map(blogPost -> {
                    blogPost.getTags().removeIf(tag -> tag.getName().equals(tagName));
                    blogPostRepository.save(blogPost);
                    LOGGER.info("Tag {} removed from blog with id {}", tagName, id);
                    return blogPostMapper.toDto(blogPost);
                })
                .orElseThrow(() -> new EntityNotFoundException("Blog post with ID " + id + " not found"));
    }

    @Override
    public List<BlogPostDto> getBlogsByTag(String tagName) {
        return blogPostMapper.toDtoList(blogPostRepository.findAllByTags_Name(tagName));
    }

    @Override
    public List<BlogPostSummaryDto> getSummarizedBlogs() {
        List<BlogPost> blogPosts = blogPostRepository.findAll();
        return blogPosts.stream()
                .map(blogPostMapper::toSummaryDto)
                .toList();
    }

    @Override
    public List<BlogPostDto> getBlogsByUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            LOGGER.error("User with id {} not found", userId);
            throw new EntityNotFoundException("User with id " + userId + " not found");
        }
        List<BlogPost> blogPosts = blogPostRepository.findAllByUser_Id(userId);
        if (blogPosts.isEmpty()) {
            LOGGER.error("No blogs found for user with id {}", userId);
            throw new EntityNotFoundException("No blogs found for user with id " + userId);
        }
        return blogPostMapper.toDtoList(blogPosts);
    }
}
