package com.scalefocus.blogapplication.service;

import com.scalefocus.blogapplication.dto.BlogPostDto;
import com.scalefocus.blogapplication.dto.BlogPostSummaryDto;
import com.scalefocus.blogapplication.dto.MediaFileDto;
import com.scalefocus.blogapplication.dto.TagDto;
import org.springframework.data.domain.Page;

import java.util.List;

public interface BlogService {
    BlogPostDto createBlog(BlogPostDto blogDto);

    Page<BlogPostDto> getBlogs(int page, int size);

    BlogPostDto updateBlog(Long id, BlogPostDto blogDto);

    BlogPostDto addTag(Long id, TagDto tag);

    BlogPostDto removeTag(Long id, String tagName);

    Page<BlogPostDto> getBlogsByTag(String tagName, int page, int size);

    Page<BlogPostSummaryDto> getSummarizedBlogs(int page, int size);

    void deleteBlog(Long id);

    BlogPostDto getBlog(Long id);

    BlogPostDto addTagByName(Long id, String tagName);

    Page<BlogPostDto> getBlogsByUser(Long userId, int page, int size);

    Page<BlogPostDto> searchBlogs(String query, int page, int size);

    BlogPostDto addMediaFiles(Long id, List<MediaFileDto> mediaFiles);

    BlogPostDto removeMediaFile(Long id, Long mediaFileId);
}
