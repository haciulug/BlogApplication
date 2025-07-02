package com.scalefocus.blogapplication.service;

import com.scalefocus.blogapplication.dto.BlogEventDto;
import com.scalefocus.blogapplication.dto.BlogPostDto;
import com.scalefocus.blogapplication.dto.BlogPostSummaryDto;
import com.scalefocus.blogapplication.dto.TagDto;
import com.scalefocus.blogapplication.kafka.KafkaProducerService;
import com.scalefocus.blogapplication.mapper.BlogPostMapper;
import com.scalefocus.blogapplication.mapper.MediaFileMapper;
import com.scalefocus.blogapplication.model.BlogPost;
import com.scalefocus.blogapplication.model.Tag;
import com.scalefocus.blogapplication.repository.BlogPostRepository;
import com.scalefocus.blogapplication.security.JwtUserDetails;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.hibernate.search.engine.search.query.SearchResult;
import org.springframework.data.domain.Page;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class BlogServiceImpl implements BlogService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlogServiceImpl.class);

    private final BlogPostRepository blogPostRepository;
    private final BlogPostMapper blogPostMapper;
    private final TagService tagService;
    private final MediaFileMapper mediaFileMapper;
    private final KafkaProducerService kafkaProducerService;

    private final EntityManager entityManager;

    public BlogServiceImpl(BlogPostRepository blogPostRepository, BlogPostMapper blogPostMapper, TagService tagService, MediaFileMapper mediaFileMapper, KafkaProducerService kafkaProducerService, EntityManager entityManager) {
        this.blogPostRepository = blogPostRepository;
        this.blogPostMapper = blogPostMapper;
        this.tagService = tagService;
        this.mediaFileMapper = mediaFileMapper;
        this.kafkaProducerService = kafkaProducerService;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public BlogPostDto createBlog(BlogPostDto blogDto) {
        System.out.println(blogDto);
        if (blogDto == null) {
            throw new IllegalArgumentException("Blog cannot be null");
        }
        BlogPost blogPost = blogPostMapper.toEntity(blogDto);

        // Process tags
        Set<Tag> tags = new HashSet<>();
        if (blogPost.getTags() == null) {
            blogPost.setTags(tags);
        }
        for (Tag tag : blogPost.getTags()) {
            Tag managedTag = tagService.findOrCreateTag(tag);
            tags.add(managedTag);
        }
        blogPost.setTags(tags);

        // Set user information from JWT token
        Long userId = getCurrentUserId();
        System.out.println("userId" + userId);
        blogPost.setUserId(userId);

        BlogPost savedBlogPost = blogPostRepository.save(blogPost);
        BlogPostDto createdBlog = blogPostMapper.toDto(savedBlogPost);
        LOGGER.info("Blog with id {} created", createdBlog.getId());

        // Publish event to Kafka
        BlogEventDto blogEvent = new BlogEventDto(createdBlog.getId(), "CREATED", createdBlog);
        kafkaProducerService.sendBlogEvent(blogEvent);

        return createdBlog;
    }

    @Override
    public Page<BlogPostDto> getBlogs(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return blogPostRepository.findAll(pageable)
                .map(blogPostMapper::toDto);
    }

    @Override
    public void deleteBlog(Long id) {
        BlogPost blogPost = blogPostRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Blog not found"));
        Long userId = getCurrentUserId();
        if (!blogPost.getUserId().equals(userId)) {
            throw new AccessDeniedException("You can only delete your own posts");
        }
        blogPostRepository.delete(blogPost);
        LOGGER.info("Blog with id {} deleted by user {}", id, userId);

        // Publish event to Kafka
        BlogPostDto deletedBlog = blogPostMapper.toDto(blogPost);
        BlogEventDto blogEvent = new BlogEventDto(id, "DELETED", deletedBlog);
        kafkaProducerService.sendBlogEvent(blogEvent);
    }

    @Override
    public BlogPostDto getBlog(Long id) {
        BlogPost blogPost = blogPostRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Blog not found with id " + id));
        return blogPostMapper.toDto(blogPost);
    }

    @Override
    public BlogPostDto updateBlog(Long id, BlogPostDto blogDto) {
        BlogPost blogPost = blogPostRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Blog not found with id " + id));
        Long userId = getCurrentUserId();
        if (!blogPost.getUserId().equals(userId)) {
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

        // Publish event to Kafka
        BlogPostDto updatedBlog = blogPostMapper.toDto(blogPost);
        BlogEventDto blogEvent = new BlogEventDto(id, "UPDATED", updatedBlog);
        kafkaProducerService.sendBlogEvent(blogEvent);

        return updatedBlog;
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
    public Page<BlogPostDto> getBlogsByTag(String tagName, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return blogPostRepository.findAllByTags_Name(tagName, pageable)
                .map(blogPostMapper::toDto);
    }

    @Override
    public Page<BlogPostSummaryDto> getSummarizedBlogs(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return blogPostRepository.findAll(pageable)
                .map(blogPostMapper::toSummaryDto);
    }

    @Override
    public Page<BlogPostDto> getBlogsByUser(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<BlogPost> blogPosts = blogPostRepository.findAllByUserId(userId, pageable);
        if (blogPosts.isEmpty()) {
            LOGGER.error("No blogs found for user with id {}", userId);
            throw new EntityNotFoundException("No blogs found for user with id " + userId);
        }
        return blogPosts.map(blogPostMapper::toDto);
    }

    @Override
    public Page<BlogPostDto> searchBlogs(String query, int page, int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            SearchSession searchSession = Search.session(entityManager);

            SearchResult<BlogPost> result = searchSession.search(BlogPost.class)
                    .where(f -> f.simpleQueryString()
                            .fields("title", "content", "tags.name")
                            .matching(query))
                    .fetch((int) pageable.getOffset(), pageable.getPageSize());

            List<BlogPost> blogPosts = result.hits();
            long totalHits = result.total().hitCount();

            List<BlogPostDto> dtos = blogPosts.stream()
                    .map(blogPostMapper::toDto)
                    .collect(Collectors.toList());

            return new PageImpl<>(dtos, pageable, totalHits);
        }
        catch (Exception e) {
            LOGGER.error("Error occurred while searching for blogs: {}", e.getMessage());
            throw new RuntimeException("Error occurred while searching for blogs", e);
        }
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof JwtUserDetails) {
                System.out.println("JwtUserDetails");
                System.out.println(principal);
                return ((JwtUserDetails) principal).getUserId();
            } else {
                throw new AccessDeniedException("Invalid principal type: " + principal.getClass());
            }
        }
        throw new AccessDeniedException("User is not authenticated");
    }
}
