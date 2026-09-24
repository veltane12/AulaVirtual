package com.aula.virtual.data.local;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.aula.virtual.data.entity.BlogComment;
import com.aula.virtual.data.entity.BlogEntry;
import com.aula.virtual.data.entity.Enrollment;
import com.aula.virtual.data.entity.Facility;
import com.aula.virtual.data.entity.FacilitySchedule;
import com.aula.virtual.data.entity.Faculty;
import com.aula.virtual.data.entity.Subject;
import com.aula.virtual.data.entity.User;

import com.aula.virtual.data.entity.UserSubjectColor;

@Database(
    entities = {
        User.class,
        Faculty.class,
        Subject.class,
        Facility.class,
        FacilitySchedule.class,
        Enrollment.class,
        BlogEntry.class,
        BlogComment.class,
        UserSubjectColor.class
    },
    version = 5,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase INSTANCE;

    public abstract UserDao userDao();
    public abstract FacultyDao facultyDao();
    public abstract SubjectDao subjectDao();
    public abstract FacilityDao facilityDao();
    public abstract FacilityScheduleDao facilityScheduleDao();
    public abstract EnrollmentDao enrollmentDao();
    public abstract BlogDao blogDao();
    public abstract UserSubjectColorDao userSubjectColorDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "aula_virtual_db"
                    )
                    .fallbackToDestructiveMigration()
                    .build();
                }
            }
        }
        return INSTANCE;
    }
}
