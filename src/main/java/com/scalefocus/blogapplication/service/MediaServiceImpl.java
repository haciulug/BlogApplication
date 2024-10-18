package com.scalefocus.blogapplication.service;

import com.scalefocus.blogapplication.dto.BlogPostDto;
import com.scalefocus.blogapplication.dto.MediaFileDto;
import com.scalefocus.blogapplication.exception.custom.MediaProcessingException;
import com.scalefocus.blogapplication.mapper.BlogPostMapper;
import com.scalefocus.blogapplication.mapper.MediaFileMapper;
import com.scalefocus.blogapplication.model.BlogPost;
import com.scalefocus.blogapplication.model.MediaFile;
import com.scalefocus.blogapplication.model.MediaType;
import com.scalefocus.blogapplication.repository.BlogPostRepository;
import io.github.techgnious.IVCompressor;
import io.github.techgnious.dto.ImageFormats;
import io.github.techgnious.dto.ResizeResolution;
import io.github.techgnious.dto.VideoFormats;
import io.github.techgnious.exception.ImageException;
import io.github.techgnious.exception.VideoException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
public class MediaServiceImpl implements MediaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MediaServiceImpl.class);

    private final BlogPostRepository blogPostRepository;
    private final BlogPostMapper blogPostMapper;
    private final MediaFileMapper mediaFileMapper;


    public MediaServiceImpl(BlogPostRepository blogPostRepository, BlogPostMapper blogPostMapper, MediaFileMapper mediaFileMapper) {
        this.blogPostRepository = blogPostRepository;
        this.blogPostMapper = blogPostMapper;
        this.mediaFileMapper = mediaFileMapper;
    }

    @Override
    @Transactional
    public BlogPostDto addMediaFiles(Long postId, List<MultipartFile> files, List<MediaFileDto> mediaFileDtos) {
        BlogPost blogPost = getBlogPostById(postId);  // Method for fetching blog post
        List<MediaFile> mediaFiles = new ArrayList<>();
        IVCompressor compressor = new IVCompressor();

        for (int i = 0; i < files.size(); i++) {
            try {
                MediaFile mediaFile = processMediaFile(files.get(i), mediaFileDtos.get(i), compressor);
                mediaFile.setBlogPost(blogPost);
                mediaFiles.add(mediaFile);
            } catch (IOException | ImageException | VideoException e) {
                throw new MediaProcessingException("Failed to process media file", e);
            }
        }

        blogPost.getMediaFiles().addAll(mediaFiles);
        blogPostRepository.save(blogPost);
        LOGGER.info("Media files added to blog post with id {}", postId);
        return blogPostMapper.toDto(blogPost);
    }

    private BlogPost getBlogPostById(Long postId) {
        return blogPostRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("Blog post not found with id " + postId));
    }

    private MediaFile processMediaFile(MultipartFile file, MediaFileDto mediaFileDto, IVCompressor compressor) throws IOException, ImageException, VideoException {
        MediaFile mediaFile = new MediaFile();
        MediaType mediaType = determineMediaType(file);

        if (mediaType == MediaType.IMAGE) {
            processImageFile(file, mediaFileDto, mediaFile, compressor);
        } else if (mediaType == MediaType.VIDEO) {
            processVideoFile(file, mediaFileDto, mediaFile, compressor);
        } else {
            mediaFile.setContent(file.getBytes());
        }

        mediaFile.setSize(file.getSize());
        return mediaFile;
    }

    private void processImageFile(MultipartFile file, MediaFileDto mediaFileDto, MediaFile mediaFile, IVCompressor compressor) throws IOException, ImageException {
        ResizeResolution resolution = calculateResolution(mediaFileDto.getWidth(), mediaFileDto.getHeight());
        byte[] compressedImage = compressor.resizeImage(file.getBytes(), ImageFormats.JPEG, resolution);
        mediaFile.setContent(compressedImage);
        mediaFile.setMediaType(MediaType.IMAGE);
        mediaFile.setWidth(mediaFileDto.getWidth());
        mediaFile.setHeight(mediaFileDto.getHeight());
    }

    private void processVideoFile(MultipartFile file, MediaFileDto mediaFileDto, MediaFile mediaFile, IVCompressor compressor) throws IOException, VideoException {
        ResizeResolution resolution = calculateResolution(mediaFileDto.getWidth(), mediaFileDto.getHeight());
        byte[] compressedVideo = compressor.convertAndResizeVideo(file.getBytes(), VideoFormats.MP4, VideoFormats.MP4, resolution);
        mediaFile.setContent(compressedVideo);
        mediaFile.setMediaType(MediaType.VIDEO);
    }

    private ResizeResolution calculateResolution(int width, int height) {
        if (width > 1920 || height > 1080) {
            return ResizeResolution.R1080P;
        } else if (width > 1280 || height > 720) {
            return ResizeResolution.R720P;
        } else if (width > 640 || height > 480) {
            return ResizeResolution.R480P;
        } else {
            return ResizeResolution.R360P;
        }
    }

    private MediaType determineMediaType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null && contentType.startsWith("image")) {
            return MediaType.IMAGE;
        }
        return MediaType.VIDEO;
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

    @Override
    public MediaFileDto getMediaFile(Long postId, Long mediaFileId) {
        BlogPost blogPost = blogPostRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("Blog post not found"));

        MediaFile mediaFile = blogPost.getMediaFiles().stream()
                .filter(media -> media.getId().equals(mediaFileId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Media file not found in the blog post"));

        return mediaFileMapper.toDto(mediaFile);
    }
}
