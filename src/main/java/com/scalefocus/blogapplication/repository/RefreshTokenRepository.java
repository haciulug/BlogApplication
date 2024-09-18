package com.scalefocus.blogapplication.repository;

import com.scalefocus.blogapplication.model.RefreshToken;
import com.scalefocus.blogapplication.model.User;
import org.springframework.data.jpa.repository.JpaRepository;


public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    RefreshToken findByToken(String token);
    RefreshToken findByUser(User user);
}
