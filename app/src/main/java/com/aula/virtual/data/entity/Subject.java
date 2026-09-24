package com.aula.virtual.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "subjects")
public class Subject {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String name;
    public String description;
    public String faculty;
    public Integer professorId;
    public String section; // e.g. "01"
    public String color; // hex or name, default "BLUE"

    public Subject() {}

    public Subject(String name, String description, String faculty, Integer professorId) {
        this.name = name;
        this.description = description;
        this.faculty = faculty;
        this.professorId = professorId;
        this.section = "01";
        this.color = "BLUE";
    }
}
