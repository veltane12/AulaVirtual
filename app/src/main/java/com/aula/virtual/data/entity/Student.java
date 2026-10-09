package com.aula.virtual.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "students")
public class Student {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public Integer encargadoId;
    public String carnet;
    public String name;
    public String grade;
    public String faculty;
    public String profile_image;

    public Student() {}

    public Student(String carnet, String name, String grade, String faculty, Integer encargadoId) {
        this.carnet = carnet;
        this.name = name;
        this.grade = grade;
        this.faculty = faculty;
        this.encargadoId = encargadoId;
    }
}
