-- Create users table
CREATE TABLE IF NOT EXISTS users (
                                     id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                     username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    display_name VARCHAR(50) NOT NULL,
    authority VARCHAR(50) NOT NULL,
    account_non_locked BOOLEAN NOT NULL DEFAULT TRUE,
    login_attempts INT NOT NULL DEFAULT 0,
    auto_locked_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    );

-- Create blog_posts table
CREATE TABLE IF NOT EXISTS blog_posts (
                                          id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                          title VARCHAR(255) NOT NULL UNIQUE,
    content TEXT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_blog_posts_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );

-- Create tags table
CREATE TABLE IF NOT EXISTS tags (
                                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                    name VARCHAR(100) NOT NULL UNIQUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    );

-- Create blog_post_tags junction table
CREATE TABLE IF NOT EXISTS blog_post_tags (
                                              blog_post_id BIGINT NOT NULL,
                                              tag_id BIGINT NOT NULL,
                                              PRIMARY KEY (blog_post_id, tag_id),
    CONSTRAINT fk_blog_post_tags_blog_post_id FOREIGN KEY (blog_post_id) REFERENCES blog_posts(id) ON DELETE CASCADE,
    CONSTRAINT fk_blog_post_tags_tag_id FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
    );

-- Create refresh_tokens table
CREATE TABLE IF NOT EXISTS refresh_tokens (
                                              id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                              token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_id BIGINT NOT NULL UNIQUE,
    CONSTRAINT fk_refresh_tokens_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );
