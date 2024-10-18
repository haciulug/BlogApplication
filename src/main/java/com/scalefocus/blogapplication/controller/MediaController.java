package com.scalefocus.blogapplication.controller;

import com.scalefocus.blogapplication.dto.BlogPostDto;
import com.scalefocus.blogapplication.dto.MediaFileDto;
import com.scalefocus.blogapplication.service.MediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/media")
@Tag(name = "Blog API", description = "Operations related to blog posts")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;

    @Operation(summary = "Add media files to a blog post", description = "Adds images and/or videos to the specified blog post.")
    @ApiResponse(responseCode = "200", description = "Media files added successfully")
    @PostMapping("/{id}/media")
    public ResponseEntity<?> addMediaFiles(
            @Parameter(description = "ID of the blog post to add media to", required = true)
            @PathVariable Long id,
            @Parameter(description = "List of media files to add", required = true)
            @RequestPart("files") List<MultipartFile> files,
            @Parameter(description = "Media files DTO for the blog post", required = true)
            @RequestPart("mediaFiles") List<MediaFileDto> mediaFiles) {
        BlogPostDto updatedBlogPost = mediaService.addMediaFiles(id, files, mediaFiles);
        return ResponseEntity.ok(updatedBlogPost);
    }

    @GetMapping("/{blogId}/media/{mediaId}")
    public ResponseEntity<byte[]> getMediaFile(
            @PathVariable Long blogId,
            @PathVariable Long mediaId) {

        MediaFileDto mediaFileDto = mediaService.getMediaFile(blogId, mediaId);

        if (mediaFileDto == null || mediaFileDto.getContent() == null) {
            return ResponseEntity.notFound().build();
        }

        HttpHeaders headers = new HttpHeaders();

        if (mediaFileDto.getMediaType() == com.scalefocus.blogapplication.model.MediaType.IMAGE) {
            headers.setContentType(MediaType.IMAGE_JPEG);
        } else if (mediaFileDto.getMediaType() == com.scalefocus.blogapplication.model.MediaType.VIDEO) {
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        }

        headers.setContentLength(mediaFileDto.getSize());

        return new ResponseEntity<>(mediaFileDto.getContent(), headers, HttpStatus.OK);
    }

    @Operation(summary = "Remove a media file from a blog post", description = "Removes a specific media file from the blog post.")
    @ApiResponse(responseCode = "200", description = "Media file removed successfully")
    @DeleteMapping("/{id}/media/{mediaId}")
    public ResponseEntity<?> removeMediaFile(
            @Parameter(description = "ID of the blog post", required = true)
            @PathVariable Long id,
            @Parameter(description = "ID of the media file to remove", required = true)
            @PathVariable Long mediaId) {
        BlogPostDto updatedBlogPost = mediaService.removeMediaFile(id, mediaId);
        return ResponseEntity.ok(updatedBlogPost);
    }

}
