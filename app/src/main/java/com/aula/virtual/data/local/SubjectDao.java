package com.aula.virtual.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.aula.virtual.data.entity.Subject;
import java.util.List;

@Dao
public interface SubjectDao {
    @Query("SELECT * FROM subjects")
    List<Subject> getAllSubjects();

    @Query("SELECT * FROM subjects WHERE id = :id LIMIT 1")
    Subject getSubjectById(int id);

    @Query("SELECT DISTINCT * FROM subjects WHERE professorId = :profId OR id IN (SELECT subjectId FROM facility_schedules WHERE professorId = :profId)")
    List<Subject> getSubjectsByProfessor(int profId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Subject subject);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Subject> subjects);

    @Query("DELETE FROM subjects WHERE id = :id")
    void deleteById(int id);

    @Query("DELETE FROM subjects")
    void deleteAll();
}
