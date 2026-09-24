package com.aula.virtual.data.entity;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "user_subject_colors", indices = {@Index(value = {"userId", "subjectId"}, unique = true)})
public class UserSubjectColor {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public int userId;
    public int subjectId;
    public String color;

    public UserSubjectColor() {
        this.color = "BLUE";
    }

    public UserSubjectColor(int userId, int subjectId, String color) {
        this.userId = userId;
        this.subjectId = subjectId;
        this.color = color;
    }
}
