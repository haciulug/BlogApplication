package com.scalefocus.blogapplication.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserEventDto {
    private Long userId;
    private String eventType;
    private String username;
    private String displayName;
}
