package com.scalefocus.blogapplication.controller;

import com.scalefocus.blogapplication.dto.BlogPostDto;
import com.scalefocus.blogapplication.dto.BlogPostSummaryDto;
import com.scalefocus.blogapplication.dto.TagDto;
import com.scalefocus.blogapplication.service.BlogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/blogs")
@Tag(name = "Blog API", description = "Operations related to blog posts")
@RequiredArgsConstructor
public class BlogController {

    private final BlogService blogService;

    @Operation(summary = "Create a new blog post", description = "Creates a new blog post with the given details.")
    @ApiResponse(responseCode = "201", description = "Blog post created successfully")
    @PostMapping
    public ResponseEntity<?> createBlog(
            @Parameter(description = "Blog post details", required = true)
            @Valid @RequestBody BlogPostDto blogDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(blogService.createBlog(blogDto));
    }

    @Operation(summary = "Get all blog posts", description = "Retrieves a list of all blog posts.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved list of blog posts")
    @ApiResponse(responseCode = "404", description = "No blog posts found")
    @GetMapping
    public ResponseEntity<?> getBlogs() {
        List<BlogPostDto> blogs = blogService.getBlogs();
        if (blogs.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(blogs);
    }

    @Operation(summary = "Delete a blog post", description = "Deletes the blog post with the specified ID.")
    @ApiResponse(responseCode = "204", description = "Blog post deleted successfully")
    @ApiResponse(responseCode = "404", description = "Blog post not found")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBlog(
            @Parameter(description = "ID of the blog post to delete", required = true)
            @PathVariable Long id) {
        blogService.deleteBlog(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get a blog post by ID", description = "Retrieves the blog post with the specified ID.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved the blog post")
    @ApiResponse(responseCode = "404", description = "Blog post not found")
    @GetMapping("/{id}")
    public ResponseEntity<?> getBlog(
            @Parameter(description = "ID of the blog post to retrieve", required = true)
            @PathVariable Long id) {
        BlogPostDto blog = blogService.getBlog(id);
        if (blog == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(blog);
    }

    @Operation(summary = "Update a blog post", description = "Updates the blog post with the specified ID.")
    @ApiResponse(responseCode = "200", description = "Blog post updated successfully")
    @ApiResponse(responseCode = "404", description = "Blog post not found")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateBlog(
            @Parameter(description = "ID of the blog post to update", required = true)
            @PathVariable Long id,
            @Parameter(description = "Updated blog post details", required = true)
            @Valid @RequestBody BlogPostDto blogDto) {
        BlogPostDto updatedBlog = blogService.updateBlog(id, blogDto);
        if (updatedBlog == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updatedBlog);
    }

    @Operation(summary = "Add a tag to a blog post", description = "Adds a new tag to the specified blog post.")
    @ApiResponse(responseCode = "200", description = "Tag added successfully")
    @ApiResponse(responseCode = "404", description = "Blog post not found")
    @PostMapping("/{id}/tag")
    public ResponseEntity<?> addTag(
            @Parameter(description = "ID of the blog post to add a tag to", required = true)
            @PathVariable Long id,
            @Parameter(description = "Tag details", required = true)
            @Valid @RequestBody TagDto tag) {
        BlogPostDto blogPostDto = blogService.addTag(id, tag);
        if (blogPostDto == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(blogPostDto);
    }

    @Operation(summary = "Remove a tag from a blog post", description = "Removes the specified tag from the blog post.")
    @ApiResponse(responseCode = "200", description = "Tag removed successfully")
    @ApiResponse(responseCode = "404", description = "Blog post or tag not found")
    @DeleteMapping("/{id}/tag/{tagName}")
    public ResponseEntity<?> removeTag(
            @Parameter(description = "ID of the blog post", required = true)
            @PathVariable Long id,
            @Parameter(description = "Name of the tag to remove", required = true)
            @PathVariable String tagName) {
        BlogPostDto blogPostDto = blogService.removeTag(id, tagName);
        if (blogPostDto == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(blogPostDto);
    }

    @Operation(summary = "Add a tag to a blog post by tag name", description = "Adds an existing tag to the blog post by tag name.")
    @ApiResponse(responseCode = "200", description = "Tag added successfully")
    @ApiResponse(responseCode = "404", description = "Blog post or tag not found")
    @PostMapping("/{id}/tag/{tagName}")
    public ResponseEntity<?> addTagByName(
            @Parameter(description = "ID of the blog post", required = true)
            @PathVariable Long id,
            @Parameter(description = "Name of the tag to add", required = true)
            @PathVariable String tagName) {
        BlogPostDto blogPostDto = blogService.addTagByName(id, tagName);
        if (blogPostDto == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(blogPostDto);
    }

    @GetMapping("/tags/{tagName}/blogs")
    public ResponseEntity<?> getBlogsByTag(
            @Parameter(description = "Name of the tag", required = true)
            @PathVariable String tagName) {
        List<BlogPostDto> blogs = blogService.getBlogsByTag(tagName);
        if (blogs.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(blogs);
    }

    @Operation(summary = "Get summarized blog posts", description = "Retrieves a list of blog posts with summaries.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved summarized blog posts")
    @GetMapping("/summarized")
    public ResponseEntity<?> getSummarizedBlogs() {
        List< BlogPostSummaryDto > blogs = blogService.getSummarizedBlogs();
        if (blogs.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(blogs);
    }

    @Operation(summary = "Get blogs by user ID", description = "Retrieves all blog posts authored by the specified user.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved user's blog posts")
    @ApiResponse(responseCode = "404", description = "No blog posts found for the given user")
    @GetMapping("/user/{userId}/blogs")
    public ResponseEntity<?> getBlogsByUser(
            @Parameter(description = "ID of the user", required = true)
            @PathVariable Long userId) {
        List<BlogPostDto> blogs = blogService.getBlogsByUser(userId);
        if (blogs.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(blogs);
    }

}
