package com.scalefocus.blogapplication.repository;

import com.scalefocus.blogapplication.model.BlogPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BlogPostRepository extends JpaRepository<BlogPost, Long> {

    @Query("SELECT b FROM BlogPost b JOIN b.tags t WHERE t.name = :tagName")
    Page<BlogPost> findAllByTags_Name(@Param("tagName") String tagName, Pageable pageable);

    Page<BlogPost> findAllByUser_Id(Long userId, Pageable pageable);
}
