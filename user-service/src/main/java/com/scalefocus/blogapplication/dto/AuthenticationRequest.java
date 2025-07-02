package com.scalefocus.blogapplication.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationRequest {
    @NotBlank(message = "Username cannot be blank")
    @Size(min = 3, max = 50, message = "Username must be between {min} and {max} symbols long")
    private String username;
    @NotBlank(message = "Password cannot be blank")
    @Size(min = 8, max = 20, message = "Password must be between {min} and {max} symbols long")
    private String password;
}