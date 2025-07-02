-- Create media_files table
CREATE TABLE IF NOT EXISTS media_files (
                                           id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                           file_content LONGBLOB NOT NULL,
                                           media_type VARCHAR(50) NOT NULL,
                                           width INT NULL,
                                           height INT NULL,
                                           size BIGINT NOT NULL,
                                           blog_post_id BIGINT NOT NULL,
                                           CONSTRAINT fk_media_files_blog_post_id FOREIGN KEY (blog_post_id) REFERENCES blog_posts(id) ON DELETE CASCADE
);

-- Create index on blog_post_id
CREATE INDEX idx_media_files_blog_post_id ON media_files(blog_post_id);
