package com.aula.virtual.data.local;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.aula.virtual.data.entity.BlogComment;
import com.aula.virtual.data.entity.BlogEntry;
import com.aula.virtual.data.entity.Enrollment;
import com.aula.virtual.data.entity.Facility;
import com.aula.virtual.data.entity.FacilitySchedule;
import com.aula.virtual.data.entity.Faculty;
import com.aula.virtual.data.entity.Notification;
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
        UserSubjectColor.class,
        Notification.class
    },
    version = 6,
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
    public abstract NotificationDao notificationDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "aula_virtual_db"
                    )
                    .addCallback(new Callback() {
                        @Override
                        public void onCreate(@NonNull SupportSQLiteDatabase db) {
                            super.onCreate(db);
                            seedInitialData(db);
                        }

                        @Override
                        public void onOpen(@NonNull SupportSQLiteDatabase db) {
                            super.onOpen(db);
                            seedInitialData(db);
                        }
                    })
                    .fallbackToDestructiveMigration()
                    .build();
                }
            }
        }
        return INSTANCE;
    }

    private static void seedInitialData(SupportSQLiteDatabase db) {
        db.execSQL("INSERT INTO faculties (id, name, description) " +
                "SELECT 998, 'Docencia', 'Facultad obligatoria asignada automáticamente a todos los Profesores.' " +
                "WHERE NOT EXISTS (SELECT 1 FROM faculties WHERE id = 998);");

        db.execSQL("INSERT INTO faculties (id, name, description) " +
                "SELECT 999, 'Administrativa', 'Facultad obligatoria asignada automáticamente a todos los Administradores.' " +
                "WHERE NOT EXISTS (SELECT 1 FROM faculties WHERE id = 999);");

        db.execSQL("INSERT INTO users (carnet, name, password, role, faculty, can_change_photo) " +
                "SELECT 'ADMIN12345', 'Admin Maestro', 'ASD###', 'ADMIN', 'Administrativa', 1 " +
                "WHERE NOT EXISTS (SELECT 1 FROM users WHERE carnet = 'ADMIN12345');");
    }
}
