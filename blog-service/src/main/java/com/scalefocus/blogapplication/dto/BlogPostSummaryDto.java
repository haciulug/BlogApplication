package com.scalefocus.blogapplication.dto;

import lombok.Data;

@Data
public class BlogPostSummaryDto {
    private Long id;
    private String title;
    private String summary;
}
