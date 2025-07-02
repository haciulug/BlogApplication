package com.scalefocus.blogapplication.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BlogEventDto {
    private Long blogId;
    private String eventType;
    private BlogPostDto blogPostDto;
}
