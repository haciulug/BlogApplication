package com.scalefocus.blogapplication.dto;

import com.scalefocus.blogapplication.model.MediaType;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaFileDto {
    private Long id;
    private String url;
    private MediaType mediaType;
    private int width;
    private int height;
}
