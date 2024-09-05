package com.scalefocus.blogapplication.dto;

import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
public class BlogPostDto {
    private Long id;
    private String title;
    private String content;
    private Set<TagDto> tags = new HashSet<>();
}
