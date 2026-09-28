package com.aula.virtual.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.aula.virtual.data.entity.Notification;
import java.util.List;

@Dao
public interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY id DESC")
    List<Notification> getAllNotifications();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertNotification(Notification notification);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Notification> notifications);
}
