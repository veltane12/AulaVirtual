package com.aula.virtual.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "facility_schedules")
public class FacilitySchedule {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public int facilityId;
    public int subjectId;
    public int professorId;
    public String days;
    public String startTime;
    public String endTime;
    public String color = "BLUE";

    public FacilitySchedule() {}

    public FacilitySchedule(int facilityId, int subjectId, int professorId, String days, String startTime, String endTime) {
        this.facilityId = facilityId;
        this.subjectId = subjectId;
        this.professorId = professorId;
        this.days = days;
        this.startTime = startTime;
        this.endTime = endTime;
    }
}
