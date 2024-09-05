package com.scalefocus.blogapplication.service;

import com.scalefocus.blogapplication.dto.BlogPostDto;
import com.scalefocus.blogapplication.dto.BlogPostSummaryDto;
import com.scalefocus.blogapplication.dto.TagDto;
import com.scalefocus.blogapplication.mapper.BlogPostMapper;
import com.scalefocus.blogapplication.model.BlogPost;
import com.scalefocus.blogapplication.repository.BlogPostRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BlogServiceImpl implements BlogService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlogServiceImpl.class);

    private final BlogPostRepository blogPostRepository;
    private final BlogPostMapper blogPostMapper;
    private final TagService tagService;

    public BlogServiceImpl(BlogPostRepository blogPostRepository, BlogPostMapper blogPostMapper, TagService tagService) {
        this.blogPostRepository = blogPostRepository;
        this.blogPostMapper = blogPostMapper;
        this.tagService = tagService;
    }

    @Override
    @Transactional
    public BlogPostDto createBlog(BlogPostDto blogDto) {
        BlogPost blogPost = blogPostMapper.toEntity(blogDto);
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
        blogPostRepository.deleteById(id);
        LOGGER.info("Blog with id {} deleted", id);
    }

    @Override
    public BlogPostDto getBlog(Long id) {
        return blogPostMapper.toDto(blogPostRepository.findById(id).orElse(null));
    }

    @Override
    public BlogPostDto updateBlog(Long id, BlogPostDto blogDto) {
        Optional<BlogPost> blogPost = blogPostRepository.findById(id);
        if (blogPost.isPresent()) {
            BlogPost updatedBlog = blogPostMapper.toEntity(blogDto);
            updatedBlog.setId(id);
            updatedBlog.setTags(blogPost.get().getTags());
            BlogPostDto updatedBlogDto = blogPostMapper.toDto(blogPostRepository.save(updatedBlog));
            LOGGER.info("Blog with id {} updated", id);
            return updatedBlogDto;
        }
        LOGGER.error("Blog with id {} not found", id);
        return null;
    }

    @Override
    public BlogPostDto addTag(Long id, TagDto tag) {
        Optional<BlogPost> blogPost = blogPostRepository.findById(id);
        if (blogPost.isPresent()) {
            TagDto createdTag = tagService.createTag(tag);
            blogPost.get().getTags().add(tagService.toEntity(createdTag));
            BlogPostDto updatedBlog = blogPostMapper.toDto(blogPostRepository.save(blogPost.get()));
            LOGGER.info("Tag {} added to blog with id {}", tag.getName(), id);
            return updatedBlog;
        }
        LOGGER.error("Blog with id {} not found", id);
        return null;
    }

    @Override
    public BlogPostDto addTagByName(Long id, String tagName) {
        Optional<BlogPost> blogPost = blogPostRepository.findById(id);
        if (blogPost.isPresent()) {
            TagDto tag = tagService.getTagByName(tagName);
            if (tag == null) {
                tag = tagService.createTag(TagDto.builder().name(tagName).build());
            }
            blogPost.get().getTags().add(tagService.toEntity(tag));
            BlogPostDto updatedBlog = blogPostMapper.toDto(blogPostRepository.save(blogPost.get()));
            LOGGER.info("Tag {} added to blog with id {}", tagName, id);
            return updatedBlog;
        }
        LOGGER.error("Blog with id {} not found", id);
        return null;
    }

    @Override
    public BlogPostDto removeTag(Long id, String tagName) {
        Optional<BlogPost> blogPost = blogPostRepository.findById(id);
        if (blogPost.isPresent()) {
            TagDto tag = tagService.getTagByName(tagName);
            blogPost.get().getTags().remove(tagService.toEntity(tag));
            BlogPostDto updatedBlog = blogPostMapper.toDto(blogPostRepository.save(blogPost.get()));
            LOGGER.info("Tag {} removed from blog with id {}", tagName, id);
            return updatedBlog;
        }
        LOGGER.error("Blog with id {} not found", id);
        return null;
    }

    @Override
    public List<BlogPostDto> getBlogsByTag(String tagName) {
        return blogPostMapper.toDtoList(blogPostRepository.findAllByTagsContains(tagName));
    }

    @Override
    public List<BlogPostSummaryDto> getSummarizedBlogs() {
        List<BlogPost> blogPosts = blogPostRepository.findAll();
        return blogPosts.stream()
                .map(blogPostMapper::toSummaryDto)
                .toList();
    }
}
