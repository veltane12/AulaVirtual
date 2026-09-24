package com.aula.virtual.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "facilities")
public class Facility {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String name;
    public String type; // Edificio, Laboratorio, etc.
    public String description;

    public Facility() {}

    public Facility(String name, String type, String description) {
        this.name = name;
        this.type = type;
        this.description = description;
    }
}
