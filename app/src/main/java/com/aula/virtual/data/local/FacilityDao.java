package com.aula.virtual.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.aula.virtual.data.entity.Facility;
import java.util.List;

@Dao
public interface FacilityDao {
    @Query("SELECT * FROM facilities")
    List<Facility> getAllFacilities();

    @Query("SELECT * FROM facilities WHERE id = :id LIMIT 1")
    Facility getFacilityById(int id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Facility facility);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Facility> facilities);

    @Query("DELETE FROM facilities WHERE id = :id")
    void deleteById(int id);

    @Query("DELETE FROM facilities")
    void deleteAll();
}
