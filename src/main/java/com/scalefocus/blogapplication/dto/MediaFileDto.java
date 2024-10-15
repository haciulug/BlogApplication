package com.scalefocus.blogapplication.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.scalefocus.blogapplication.model.MediaType;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaFileDto {
    private Long id;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private byte[] content;
    private MediaType mediaType;
    private int width;
    private int height;
    private long size;
}
