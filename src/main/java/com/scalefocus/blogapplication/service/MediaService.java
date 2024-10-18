package com.scalefocus.blogapplication.service;

import com.scalefocus.blogapplication.dto.BlogPostDto;
import com.scalefocus.blogapplication.dto.MediaFileDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MediaService {

    BlogPostDto addMediaFiles(Long id, List<MultipartFile> mediaFiles, List<MediaFileDto> mediaFileDtos);

    BlogPostDto removeMediaFile(Long id, Long mediaFileId);

    MediaFileDto getMediaFile(Long id, Long mediaFileId);
}
