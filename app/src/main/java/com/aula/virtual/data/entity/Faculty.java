package com.aula.virtual.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "faculties")
public class Faculty {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String name;
    public String description;

    public Faculty() {}

    public Faculty(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
