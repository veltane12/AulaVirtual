package com.aula.virtual.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.aula.virtual.data.entity.User;
import java.util.List;

@Dao
public interface UserDao {
    @Query("SELECT * FROM users WHERE role = 'STUDENT'")
    List<User> getStudents();

    @Query("SELECT * FROM users WHERE role = 'ADMIN'")
    List<User> getAdmins();

    @Query("SELECT * FROM users WHERE role = 'PROFESSOR'")
    List<User> getProfessors();

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    User getUserById(int id);

    @Query("SELECT * FROM users WHERE carnet = :carnet AND password = :password LIMIT 1")
    User login(String carnet, String password);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(User user);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<User> users);

    @Query("DELETE FROM users WHERE id = :id")
    void deleteById(int id);

    @Query("DELETE FROM users WHERE role = :role")
    void deleteByRole(String role);

    @Query("DELETE FROM users")
    void deleteAll();
}
