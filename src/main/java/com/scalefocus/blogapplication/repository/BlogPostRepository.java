package com.scalefocus.blogapplication.repository;

import com.scalefocus.blogapplication.model.BlogPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BlogPostRepository extends JpaRepository<BlogPost, Long> {

    @Query("SELECT b FROM BlogPost b JOIN b.tags t WHERE t.name = :tagName")
    List<BlogPost> findAllByTags_Name(@Param("tagName") String tagName);

    List<BlogPost> findAllByUser_Id(Long userId);
}
