package com.scalefocus.blogapplication.service;

import com.scalefocus.blogapplication.dto.BlogPostDto;
import com.scalefocus.blogapplication.dto.BlogPostSummaryDto;
import com.scalefocus.blogapplication.dto.TagDto;

import java.util.List;

public interface BlogService {
    BlogPostDto createBlog(BlogPostDto blogDto);

    List<BlogPostDto> getBlogs();

    BlogPostDto updateBlog(Long id, BlogPostDto blogDto);

    BlogPostDto addTag(Long id, TagDto tag);

    BlogPostDto removeTag(Long id, String tagName);

    List<BlogPostDto> getBlogsByTag(String tagName);

    List<BlogPostSummaryDto> getSummarizedBlogs();

    void deleteBlog(Long id);

    BlogPostDto getBlog(Long id);

    BlogPostDto addTagByName(Long id, String tagName);

    List<BlogPostDto> getBlogsByUser(Long userId);
}
