package com.aula.virtual.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.aula.virtual.data.entity.Faculty;
import java.util.List;

@Dao
public interface FacultyDao {
    @Query("SELECT * FROM faculties")
    List<Faculty> getAllFaculties();

    @Query("SELECT * FROM faculties WHERE id = :id LIMIT 1")
    Faculty getFacultyById(int id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Faculty faculty);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Faculty> faculties);

    @Query("DELETE FROM faculties WHERE id = :id")
    void deleteById(int id);

    @Query("DELETE FROM faculties")
    void deleteAll();
}
