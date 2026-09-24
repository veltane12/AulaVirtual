package com.aula.virtual.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "blog_comments")
public class BlogComment {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public int blogEntryId;
    public int userId;
    public String content;
    public String timestamp;

    public BlogComment() {}

    public BlogComment(int blogEntryId, int userId, String content) {
        this.blogEntryId = blogEntryId;
        this.userId = userId;
        this.content = content;
    }
}
