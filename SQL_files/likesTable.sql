CREATE TABLE likes (
    video_id INTEGER NOT NULL,
    username VARCHAR(50) NOT NULL,
    PRIMARY KEY (video_id, username),
    CONSTRAINT fk_like_video FOREIGN KEY (video_id) REFERENCES videos(id) ON DELETE CASCADE,
    CONSTRAINT fk_like_user FOREIGN KEY (username) REFERENCES users(username) ON DELETE CASCADE
);
