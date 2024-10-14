package com.scalefocus.blogapplication.service;

import com.scalefocus.blogapplication.dto.BlogPostDto;
import com.scalefocus.blogapplication.dto.BlogPostSummaryDto;
import com.scalefocus.blogapplication.dto.MediaFileDto;
import com.scalefocus.blogapplication.dto.TagDto;
import com.scalefocus.blogapplication.mapper.BlogPostMapper;
import com.scalefocus.blogapplication.mapper.MediaFileMapper;
import com.scalefocus.blogapplication.model.BlogPost;
import com.scalefocus.blogapplication.model.MediaFile;
import com.scalefocus.blogapplication.model.Tag;
import com.scalefocus.blogapplication.repository.BlogPostRepository;
import com.scalefocus.blogapplication.repository.UserRepository;
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
    private final MediaFileMapper mediaFileMapper;

    private final EntityManager entityManager;

    public BlogServiceImpl(BlogPostRepository blogPostRepository, BlogPostMapper blogPostMapper, TagService tagService, UserRepository userRepository, MediaFileMapper mediaFileMapper, EntityManager entityManager) {
        this.blogPostRepository = blogPostRepository;
        this.blogPostMapper = blogPostMapper;
        this.tagService = tagService;
        this.userRepository = userRepository;
        this.mediaFileMapper = mediaFileMapper;
        this.entityManager = entityManager;
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
    public Page<BlogPostDto> getBlogs(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return blogPostRepository.findAll(pageable)
                .map(blogPostMapper::toDto);
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
        if (!userRepository.existsById(userId)) {
            LOGGER.error("User with id {} not found", userId);
            throw new EntityNotFoundException("User with id " + userId + " not found");
        }
        Pageable pageable = PageRequest.of(page, size);
        Page<BlogPost> blogPosts = blogPostRepository.findAllByUser_Id(userId, pageable);
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


    @Override
    @Transactional
    public BlogPostDto addMediaFiles(Long postId, List<MediaFileDto> mediaFilesDto) {
        BlogPost blogPost = blogPostRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("Blog post not found"));

        List<MediaFile> mediaFiles = mediaFilesDto.stream()
                .map(dto -> {
                    MediaFile mediaFile = mediaFileMapper.toEntity(dto);
                    mediaFile.setBlogPost(blogPost);
                    return mediaFile;
                })
                .collect(Collectors.toList());

        blogPost.getMediaFiles().addAll(mediaFiles);
        blogPostRepository.save(blogPost);

        LOGGER.info("Media files added to blog post with id {}", postId);
        return blogPostMapper.toDto(blogPost);
    }

    @Override
    @Transactional
    public BlogPostDto removeMediaFile(Long postId, Long mediaFileId) {
        BlogPost blogPost = blogPostRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("Blog post not found"));

        boolean removed = blogPost.getMediaFiles().removeIf(media -> media.getId().equals(mediaFileId));
        if (!removed) {
            throw new EntityNotFoundException("Media file not found in the blog post");
        }

        blogPostRepository.save(blogPost);

        LOGGER.info("Media file with id {} removed from blog post with id {}", mediaFileId, postId);
        return blogPostMapper.toDto(blogPost);
    }
}
