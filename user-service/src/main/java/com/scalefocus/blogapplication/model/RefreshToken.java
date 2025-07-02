package com.scalefocus.blogapplication.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_tokens")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class RefreshToken {
    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long id;
    private String token;
    private Instant expiresAt;
    @CreationTimestamp
    private LocalDateTime createdAt;
    @OneToOne
    @JoinColumn (name = "user_id", referencedColumnName = "id", nullable = false, unique = true)
    private User user;
}