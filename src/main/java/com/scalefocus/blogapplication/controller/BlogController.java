package com.scalefocus.blogapplication.controller;

import com.scalefocus.blogapplication.dto.BlogPostDto;
import com.scalefocus.blogapplication.dto.BlogPostSummaryDto;
import com.scalefocus.blogapplication.dto.TagDto;
import com.scalefocus.blogapplication.service.BlogService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/blogs")
public class BlogController {

    private final BlogService blogService;


    public BlogController(BlogService blogService) {
        this.blogService = blogService;
    }

    @PostMapping
    public ResponseEntity<?> createBlog(@RequestBody @Valid BlogPostDto blogDto) {
        return ResponseEntity.ok(blogService.createBlog(blogDto));
    }

    @GetMapping
    public ResponseEntity<?> getBlogs() {
        List<BlogPostDto> blogs = blogService.getBlogs();
        if (blogs.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(blogs);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBlog(@PathVariable Long id) {
        blogService.deleteBlog(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getBlog(@PathVariable Long id) {
        BlogPostDto blog = blogService.getBlog(id);
        if (blog == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(blog);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateBlog(@PathVariable Long id, @RequestBody @Valid BlogPostDto blogDto) {
        BlogPostDto updatedBlog = blogService.updateBlog(id, blogDto);
        if (updatedBlog == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updatedBlog);
    }

    @PostMapping("/{id}/tag")
    public ResponseEntity<?> addTag(@PathVariable Long id, @RequestBody @Valid TagDto tag) {
        BlogPostDto blogPostDto = blogService.addTag(id, tag);
        if (blogPostDto == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(blogPostDto);
    }

    @DeleteMapping("/{id}/tag/{tagName}")
    public ResponseEntity<?> removeTag(@PathVariable Long id, @PathVariable String tagName) {
        BlogPostDto blogPostDto = blogService.removeTag(id, tagName);
        if (blogPostDto == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(blogPostDto);
    }

    @PostMapping("/{id}/tag/{tagName}")
    public ResponseEntity<?> addTagByName(@PathVariable Long id, @PathVariable String tagName) {
        BlogPostDto blogPostDto = blogService.addTagByName(id, tagName);
        if (blogPostDto == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(blogPostDto);
    }

    @GetMapping("/tags/{tagName}/blogs")
    public ResponseEntity<?> getBlogsByTag(@PathVariable String tagName) {
        List<BlogPostDto> blogs = blogService.getBlogsByTag(tagName);
        if (blogs.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(blogs);
    }

    @GetMapping("/summarized")
    public ResponseEntity<?> getSummarizedBlogs() {
        List< BlogPostSummaryDto > blogs = blogService.getSummarizedBlogs();
        if (blogs.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(blogs);
    }
}
