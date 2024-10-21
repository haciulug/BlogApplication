package com.scalefocus.blogapplication.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "media_files")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    @Column(name = "file_content", nullable = false)
    private byte[] content;

    @Column(name = "media_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private MediaType mediaType;

    private int width;
    private int height;
    private long size;

    @ManyToOne
    @JoinColumn(name = "blog_post_id")
    private BlogPost blogPost;

}
