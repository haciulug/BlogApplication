package com.scalefocus.blogapplication.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegistrationRequest {
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 20, message = "Username must be between {min} and {max} symbols long")
    private String username;

    @Pattern(regexp = "^(?!.* )(?=.*\\d)(?=.*[A-Z]).{8,20}$", message = "Password must be 8 - 20 symbols long and must contain uppercase letter")
    private String password;

    @NotBlank(message = "Display name is required")
    @Size(min = 3, max = 50, message = "Display name must be between {min} and {max} symbols long")
    private String displayName;
}
