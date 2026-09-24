package com.aula.virtual.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "enrollments")
public class Enrollment {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public int studentId;
    public int subjectId;
    public Double grade1;
    public Double grade2;
    public Double grade3;
    public Double grade4;
    public Double grade5;
    public String color = "BLUE";

    public Enrollment() {}

    public Enrollment(int studentId, int subjectId) {
        this.studentId = studentId;
        this.subjectId = subjectId;
    }
}
