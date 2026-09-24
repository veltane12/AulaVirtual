package com.aula.virtual.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "blog_entries")
public class BlogEntry {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public int subjectId;
    public String category; // Parcial 1-5, Tarea, Info
    public String title;
    public String content;
    public int position;

    public BlogEntry() {}

    public BlogEntry(int subjectId, String category, String title, String content) {
        this.subjectId = subjectId;
        this.category = category;
        this.title = title;
        this.content = content;
        this.position = 0;
    }
}
