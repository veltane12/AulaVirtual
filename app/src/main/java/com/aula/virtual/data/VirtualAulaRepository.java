package com.aula.virtual.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.aula.virtual.data.entity.BlogComment;
import com.aula.virtual.data.entity.BlogEntry;
import com.aula.virtual.data.entity.Facility;
import com.aula.virtual.data.entity.FacilitySchedule;
import com.aula.virtual.data.entity.Enrollment;
import com.aula.virtual.data.entity.Faculty;
import com.aula.virtual.data.entity.Notification;
import com.aula.virtual.data.entity.Subject;
import com.aula.virtual.data.entity.User;
import com.aula.virtual.data.entity.UserSubjectColor;
import com.aula.virtual.data.local.AppDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VirtualAulaRepository {
    private final ApiService apiService;
    private AppDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface ConnectionStatusListener {
        void onStatusChanged(boolean connected);
    }

    public interface SyncCallback {
        void onSuccess(String message);
        void onError(String error);
    }

    private ConnectionStatusListener connectionStatusListener;

    public void setConnectionStatusListener(ConnectionStatusListener listener) {
        this.connectionStatusListener = listener;
    }

    public VirtualAulaRepository() {
        this(null);
    }

    public VirtualAulaRepository(Context context) {
        this.apiService = RetrofitClient.getApiService();
        if (context != null) {
            try {
                this.db = AppDatabase.getInstance(context);
            } catch (Exception e) {
                e.printStackTrace();
                this.db = null;
            }
        }
    }

    private <T> void performCall(Call<T> call, Callback<T> callback) {
        if (call == null) return;
        call.enqueue(new Callback<T>() {
            @Override
            public void onResponse(Call<T> call, Response<T> response) {
                if (connectionStatusListener != null) {
                    connectionStatusListener.onStatusChanged(response != null && (response.isSuccessful() || response.code() < 500));
                }
                if (callback != null) {
                    callback.onResponse(call, response);
                }
            }

            @Override
            public void onFailure(Call<T> call, Throwable t) {
                if (connectionStatusListener != null) connectionStatusListener.onStatusChanged(false);
                if (callback != null) {
                    callback.onFailure(call, t);
                }
            }
        });
    }

    public void login(String carnet, String password, Callback<User> callback) {
        if (db != null) {
            executor.execute(() -> {
                try {
                    User localUser = db.userDao().login(carnet, password);
                    if (localUser != null) {
                        mainHandler.post(() -> callback.onResponse(null, Response.success(localUser)));
                    }
                } catch (Exception e) { e.printStackTrace(); }
            });
        }
        performCall(apiService.login(carnet, password), new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null && db != null) {
                    User user = response.body();
                    executor.execute(() -> {
                        try { db.userDao().insert(user); } catch (Exception e) { e.printStackTrace(); }
                    });
                }
                callback.onResponse(call, response);
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                if (db != null) {
                    executor.execute(() -> {
                        try {
                            User localUser = db.userDao().login(carnet, password);
                            if (localUser != null) {
                                mainHandler.post(() -> callback.onResponse(call, Response.success(localUser)));
                            } else {
                                mainHandler.post(() -> callback.onFailure(call, t));
                            }
                        } catch (Exception e) {
                            mainHandler.post(() -> callback.onFailure(call, t));
                        }
                    });
                } else {
                    callback.onFailure(call, t);
                }
            }
        });
    }

    public void getAllStudents(Callback<List<User>> callback) {
        if (db != null) {
            executor.execute(() -> {
                try {
                    List<User> cached = db.userDao().getStudents();
                    if (cached != null && !cached.isEmpty()) {
                        mainHandler.post(() -> callback.onResponse(null, Response.success(cached)));
                    }
                } catch (Exception e) { e.printStackTrace(); }
            });
        }
        performCall(apiService.getStudents(), new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null && db != null) {
                    List<User> users = response.body();
                    executor.execute(() -> {
                        try { db.userDao().insertAll(users); } catch (Exception e) { e.printStackTrace(); }
                    });
                }
                callback.onResponse(call, response);
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                if (db != null) {
                    executor.execute(() -> {
                        try {
                            List<User> cached = db.userDao().getStudents();
                            if (cached != null && !cached.isEmpty()) {
                                mainHandler.post(() -> callback.onResponse(call, Response.success(cached)));
                            } else {
                                mainHandler.post(() -> callback.onFailure(call, t));
                            }
                        } catch (Exception e) {
                            mainHandler.post(() -> callback.onFailure(call, t));
                        }
                    });
                } else {
                    callback.onFailure(call, t);
                }
            }
        });
    }

    public void getAllAdmins(Callback<List<User>> callback) {
        if (db != null) {
            executor.execute(() -> {
                try {
                    List<User> cached = db.userDao().getAdmins();
                    if (cached != null && !cached.isEmpty()) {
                        mainHandler.post(() -> callback.onResponse(null, Response.success(cached)));
                    }
                } catch (Exception e) { e.printStackTrace(); }
            });
        }
        performCall(apiService.getAdmins(), new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null && db != null) {
                    List<User> users = response.body();
                    executor.execute(() -> {
                        try { db.userDao().insertAll(users); } catch (Exception e) { e.printStackTrace(); }
                    });
                }
                callback.onResponse(call, response);
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                if (db != null) {
                    executor.execute(() -> {
                        try {
                            List<User> cached = db.userDao().getAdmins();
                            if (cached != null && !cached.isEmpty()) {
                                mainHandler.post(() -> callback.onResponse(call, Response.success(cached)));
                            } else {
                                mainHandler.post(() -> callback.onFailure(call, t));
                            }
                        } catch (Exception e) {
                            mainHandler.post(() -> callback.onFailure(call, t));
                        }
                    });
                } else {
                    callback.onFailure(call, t);
                }
            }
        });
    }

    public void getAllProfessors(Callback<List<User>> callback) {
        if (db != null) {
            executor.execute(() -> {
                try {
                    List<User> cached = db.userDao().getProfessors();
                    if (cached != null && !cached.isEmpty()) {
                        mainHandler.post(() -> callback.onResponse(null, Response.success(cached)));
                    }
                } catch (Exception e) { e.printStackTrace(); }
            });
        }
        performCall(apiService.getProfessors(), new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null && db != null) {
                    List<User> users = response.body();
                    executor.execute(() -> {
                        try { db.userDao().insertAll(users); } catch (Exception e) { e.printStackTrace(); }
                    });
                }
                callback.onResponse(call, response);
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                if (db != null) {
                    executor.execute(() -> {
                        try {
                            List<User> cached = db.userDao().getProfessors();
                            if (cached != null && !cached.isEmpty()) {
                                mainHandler.post(() -> callback.onResponse(call, Response.success(cached)));
                            } else {
                                mainHandler.post(() -> callback.onFailure(call, t));
                            }
                        } catch (Exception e) {
                            mainHandler.post(() -> callback.onFailure(call, t));
                        }
                    });
                } else {
                    callback.onFailure(call, t);
                }
            }
        });
    }

    public void getStudentSchedules(int studentId, Callback<List<ScheduleInfo>> callback) {
        if (db != null) {
            executor.execute(() -> {
                try {
                    List<Enrollment> enrollments = db.enrollmentDao().getByStudentId(studentId);
                    List<ScheduleInfo> infos = new ArrayList<>();
                    for (Enrollment en : enrollments) {
                        List<FacilitySchedule> schedules = db.facilityScheduleDao().getBySubjectId(en.subjectId);
                        Subject sub = db.subjectDao().getSubjectById(en.subjectId);
                        UserSubjectColor usc = db.userSubjectColorDao().get(studentId, en.subjectId);
                        String chosenColor = (usc != null && usc.color != null) ? usc.color : ((en.color != null && !en.color.isEmpty()) ? en.color : (sub != null ? sub.color : "BLUE"));
                        for (FacilitySchedule sch : schedules) {
                            User prof = db.userDao().getUserById(sch.professorId);
                            ScheduleInfo info = new ScheduleInfo();
                            info.schedule = sch;
                            info.subjectName = sub != null ? sub.name : "";
                            info.subjectColor = chosenColor;
                            info.professorName = prof != null ? prof.name : "";
                            infos.add(info);
                        }
                    }
                    if (!infos.isEmpty()) {
                        mainHandler.post(() -> callback.onResponse(null, Response.success(infos)));
                    }
                } catch (Exception e) { e.printStackTrace(); }
            });
        }
        performCall(apiService.getStudentSchedules(studentId), new Callback<List<ScheduleInfo>>() {
            @Override
            public void onResponse(Call<List<ScheduleInfo>> call, Response<List<ScheduleInfo>> response) {
                if (response.isSuccessful() && response.body() != null && db != null) {
                    List<ScheduleInfo> serverInfos = response.body();
                    executor.execute(() -> {
                        try {
                            for (ScheduleInfo info : serverInfos) {
                                if (info.schedule != null) {
                                    UserSubjectColor usc = db.userSubjectColorDao().get(studentId, info.schedule.subjectId);
                                    if (usc != null && usc.color != null) {
                                        info.subjectColor = usc.color;
                                    }
                                }
                            }
                            mainHandler.post(() -> callback.onResponse(call, response));
                        } catch (Exception e) {
                            e.printStackTrace();
                            mainHandler.post(() -> callback.onResponse(call, response));
                        }
                    });
                } else {
                    callback.onResponse(call, response);
                }
            }

            @Override
            public void onFailure(Call<List<ScheduleInfo>> call, Throwable t) {
                if (db != null) {
                    executor.execute(() -> {
                        try {
                            List<Enrollment> enrollments = db.enrollmentDao().getByStudentId(studentId);
                            List<ScheduleInfo> infos = new ArrayList<>();
                            for (Enrollment en : enrollments) {
                                List<FacilitySchedule> schedules = db.facilityScheduleDao().getBySubjectId(en.subjectId);
                                Subject sub = db.subjectDao().getSubjectById(en.subjectId);
                                UserSubjectColor usc = db.userSubjectColorDao().get(studentId, en.subjectId);
                                String chosenColor = (usc != null && usc.color != null) ? usc.color : ((en.color != null && !en.color.isEmpty()) ? en.color : (sub != null ? sub.color : "BLUE"));
                                for (FacilitySchedule sch : schedules) {
                                    User prof = db.userDao().getUserById(sch.professorId);
                                    ScheduleInfo info = new ScheduleInfo();
                                    info.schedule = sch;
                                    info.subjectName = sub != null ? sub.name : "";
                                    info.subjectColor = chosenColor;
                                    info.professorName = prof != null ? prof.name : "";
                                    infos.add(info);
                                }
                            }
                            mainHandler.post(() -> callback.onResponse(call, Response.success(infos)));
                        } catch (Exception e) {
                            mainHandler.post(() -> callback.onFailure(call, t));
                        }
                    });
                } else {
                    callback.onFailure(call, t);
                }
            }
        });
    }

    public void getProfessorSchedules(int professorId, Callback<List<ScheduleInfo>> callback) {
        if (db != null) {
            executor.execute(() -> {
                try {
                    List<FacilitySchedule> schedules = db.facilityScheduleDao().getByProfessorId(professorId);
                    List<ScheduleInfo> infos = new ArrayList<>();
                    for (FacilitySchedule sch : schedules) {
                        Subject sub = db.subjectDao().getSubjectById(sch.subjectId);
                        User prof = db.userDao().getUserById(sch.professorId);
                        UserSubjectColor usc = db.userSubjectColorDao().get(professorId, sch.subjectId);
                        String chosenColor = (usc != null && usc.color != null) ? usc.color : (sub != null ? sub.color : "BLUE");
                        ScheduleInfo info = new ScheduleInfo();
                        info.schedule = sch;
                        info.subjectName = sub != null ? sub.name : "";
                        info.subjectColor = chosenColor;
                        info.professorName = prof != null ? prof.name : "";
                        infos.add(info);
                    }
                    if (!infos.isEmpty()) {
                        mainHandler.post(() -> callback.onResponse(null, Response.success(infos)));
                    }
                } catch (Exception e) { e.printStackTrace(); }
            });
        }
        performCall(apiService.getProfessorSchedules(professorId), new Callback<List<ScheduleInfo>>() {
            @Override
            public void onResponse(Call<List<ScheduleInfo>> call, Response<List<ScheduleInfo>> response) {
                if (response.isSuccessful() && response.body() != null && db != null) {
                    List<ScheduleInfo> serverInfos = response.body();
                    executor.execute(() -> {
                        try {
                            for (ScheduleInfo info : serverInfos) {
                                if (info.schedule != null) {
                                    UserSubjectColor usc = db.userSubjectColorDao().get(professorId, info.schedule.subjectId);
                                    if (usc != null && usc.color != null) {
                                        info.subjectColor = usc.color;
                                    }
                                }
                            }
                            mainHandler.post(() -> callback.onResponse(call, response));
                        } catch (Exception e) {
                            e.printStackTrace();
                            mainHandler.post(() -> callback.onResponse(call, response));
                        }
                    });
                } else {
                    callback.onResponse(call, response);
                }
            }

            @Override
            public void onFailure(Call<List<ScheduleInfo>> call, Throwable t) {
                if (db != null) {
                    executor.execute(() -> {
                        try {
                            List<FacilitySchedule> schedules = db.facilityScheduleDao().getByProfessorId(professorId);
                            List<ScheduleInfo> infos = new ArrayList<>();
                            for (FacilitySchedule sch : schedules) {
                                Subject sub = db.subjectDao().getSubjectById(sch.subjectId);
                                User prof = db.userDao().getUserById(sch.professorId);
                                UserSubjectColor usc = db.userSubjectColorDao().get(professorId, sch.subjectId);
                                String chosenColor = (usc != null && usc.color != null) ? usc.color : (sub != null ? sub.color : "BLUE");
                                ScheduleInfo info = new ScheduleInfo();
                                info.schedule = sch;
                                info.subjectName = sub != null ? sub.name : "";
                                info.subjectColor = chosenColor;
                                info.professorName = prof != null ? prof.name : "";
                                infos.add(info);
                            }
                            mainHandler.post(() -> callback.onResponse(call, Response.success(infos)));
                        } catch (Exception e) {
                            mainHandler.post(() -> callback.onFailure(call, t));
                        }
                    });
                } else {
                    callback.onFailure(call, t);
                }
            }
        });
    }

    public void getAllSubjects(Callback<List<Subject>> callback) {
        if (db != null) {
            executor.execute(() -> {
                try {
                    List<Subject> cached = db.subjectDao().getAllSubjects();
                    if (cached != null && !cached.isEmpty()) {
                        mainHandler.post(() -> callback.onResponse(null, Response.success(cached)));
                    }
                } catch (Exception e) { e.printStackTrace(); }
            });
        }
        performCall(apiService.getSubjects(), new Callback<List<Subject>>() {
            @Override
            public void onResponse(Call<List<Subject>> call, Response<List<Subject>> response) {
                if (response.isSuccessful() && response.body() != null && db != null) {
                    List<Subject> subjects = response.body();
                    executor.execute(() -> {
                        try { db.subjectDao().insertAll(subjects); } catch (Exception e) { e.printStackTrace(); }
                    });
                }
                callback.onResponse(call, response);
            }

            @Override
            public void onFailure(Call<List<Subject>> call, Throwable t) {
                if (db != null) {
                    executor.execute(() -> {
                        try {
                            List<Subject> cached = db.subjectDao().getAllSubjects();
                            if (cached != null && !cached.isEmpty()) {
                                mainHandler.post(() -> callback.onResponse(call, Response.success(cached)));
                            } else {
                                mainHandler.post(() -> callback.onFailure(call, t));
                            }
                        } catch (Exception e) {
                            mainHandler.post(() -> callback.onFailure(call, t));
                        }
                    });
                } else {
                    callback.onFailure(call, t);
                }
            }
        });
    }

    public void getAllFaculties(Callback<List<Faculty>> callback) {
        if (db != null) {
            executor.execute(() -> {
                try {
                    List<Faculty> cached = db.facultyDao().getAllFaculties();
                    if (cached != null && !cached.isEmpty()) {
                        mainHandler.post(() -> callback.onResponse(null, Response.success(cached)));
                    }
                } catch (Exception e) { e.printStackTrace(); }
            });
        }
        performCall(apiService.getFaculties(), new Callback<List<Faculty>>() {
            @Override
            public void onResponse(Call<List<Faculty>> call, Response<List<Faculty>> response) {
                if (response.isSuccessful() && response.body() != null && db != null) {
                    List<Faculty> faculties = response.body();
                    executor.execute(() -> {
                        try { db.facultyDao().insertAll(faculties); } catch (Exception e) { e.printStackTrace(); }
                    });
                }
                callback.onResponse(call, response);
            }

            @Override
            public void onFailure(Call<List<Faculty>> call, Throwable t) {
                if (db != null) {
                    executor.execute(() -> {
                        try {
                            List<Faculty> cached = db.facultyDao().getAllFaculties();
                            if (cached != null && !cached.isEmpty()) {
                                mainHandler.post(() -> callback.onResponse(call, Response.success(cached)));
                            } else {
                                mainHandler.post(() -> callback.onFailure(call, t));
                            }
                        } catch (Exception e) {
                            mainHandler.post(() -> callback.onFailure(call, t));
                        }
                    });
                } else {
                    callback.onFailure(call, t);
                }
            }
        });
    }

    public void insertUser(User user, Callback<User> callback) {
        performCall(apiService.createUser(user), new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null && db != null) {
                    User saved = response.body();
                    executor.execute(() -> db.userDao().insert(saved));
                }
                callback.onResponse(call, response);
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                callback.onFailure(call, t);
            }
        });
    }

    public void updateUser(User user, Callback<User> callback) {
        performCall(apiService.updateUser(user.id, user), new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null && db != null) {
                    User updated = response.body();
                    executor.execute(() -> db.userDao().insert(updated));
                }
                callback.onResponse(call, response);
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                callback.onFailure(call, t);
            }
        });
    }

    public void deleteUser(int userId, Callback<Void> callback) {
        performCall(apiService.deleteUser(userId), new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful() && db != null) {
                    executor.execute(() -> db.userDao().deleteById(userId));
                }
                callback.onResponse(call, response);
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                callback.onFailure(call, t);
            }
        });
    }

    public void insertSubject(Subject subject, Callback<Subject> callback) {
        performCall(apiService.createSubject(subject), new Callback<Subject>() {
            @Override
            public void onResponse(Call<Subject> call, Response<Subject> response) {
                if (response.isSuccessful() && response.body() != null && db != null) {
                    Subject saved = response.body();
                    executor.execute(() -> db.subjectDao().insert(saved));
                }
                callback.onResponse(call, response);
            }

            @Override
            public void onFailure(Call<Subject> call, Throwable t) {
                callback.onFailure(call, t);
            }
        });
    }

    public void updateSubject(Subject subject, Callback<Subject> callback) {
        performCall(apiService.updateSubject(subject.id, subject), new Callback<Subject>() {
            @Override
            public void onResponse(Call<Subject> call, Response<Subject> response) {
                if (response.isSuccessful() && response.body() != null && db != null) {
                    Subject updated = response.body();
                    executor.execute(() -> db.subjectDao().insert(updated));
                }
                callback.onResponse(call, response);
            }

            @Override
            public void onFailure(Call<Subject> call, Throwable t) {
                callback.onFailure(call, t);
            }
        });
    }

    public void deleteSubject(int subId, Callback<Void> callback) {
        performCall(apiService.deleteSubject(subId), new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful() && db != null) {
                    executor.execute(() -> db.subjectDao().deleteById(subId));
                }
                callback.onResponse(call, response);
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                callback.onFailure(call, t);
            }
        });
    }

    public void insertFaculty(Faculty faculty, Callback<Faculty> callback) {
        performCall(apiService.createFaculty(faculty), new Callback<Faculty>() {
            @Override
            public void onResponse(Call<Faculty> call, Response<Faculty> response) {
                if (response.isSuccessful() && response.body() != null && db != null) {
                    Faculty saved = response.body();
                    executor.execute(() -> db.facultyDao().insert(saved));
                }
                callback.onResponse(call, response);
            }

            @Override
            public void onFailure(Call<Faculty> call, Throwable t) {
                callback.onFailure(call, t);
            }
        });
    }

    public void updateFaculty(Faculty faculty, Callback<Faculty> callback) {
        performCall(apiService.updateFaculty(faculty.id, faculty), new Callback<Faculty>() {
            @Override
            public void onResponse(Call<Faculty> call, Response<Faculty> response) {
                if (response.isSuccessful() && response.body() != null && db != null) {
                    Faculty updated = response.body();
                    executor.execute(() -> db.facultyDao().insert(updated));
                }
                callback.onResponse(call, response);
            }

            @Override
            public void onFailure(Call<Faculty> call, Throwable t) {
                callback.onFailure(call, t);
            }
        });
    }

    public void deleteFaculty(int facId, Callback<Void> callback) {
        performCall(apiService.deleteFaculty(facId), new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful() && db != null) {
                    executor.execute(() -> db.facultyDao().deleteById(facId));
                }
                callback.onResponse(call, response);
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                callback.onFailure(call, t);
            }
        });
    }

    public void enrollStudent(Enrollment enrollment, Callback<Enrollment> callback) {
        performCall(apiService.enrollStudent(enrollment), callback);
    }

    public void updateEnrollment(Enrollment enrollment, Callback<Enrollment> callback) {
        performCall(apiService.updateEnrollment(enrollment.id, enrollment), callback);
    }

    public void deleteEnrollment(int enId, Callback<Void> callback) {
        performCall(apiService.deleteEnrollment(enId), callback);
    }

    public void getGradeInfoForStudent(int studentId, Callback<List<StudentGradeInfo>> callback) {
        if (db != null) {
            executor.execute(() -> {
                List<Enrollment> enrollments = db.enrollmentDao().getByStudentId(studentId);
                List<StudentGradeInfo> infos = new ArrayList<>();
                for (Enrollment en : enrollments) {
                    Subject sub = db.subjectDao().getSubjectById(en.subjectId);
                    User stu = db.userDao().getUserById(en.studentId);
                    StudentGradeInfo info = new StudentGradeInfo();
                    info.enrollment = en;
                    info.subject = sub;
                    info.student = stu;
                    infos.add(info);
                }
                if (!infos.isEmpty()) {
                    mainHandler.post(() -> callback.onResponse(null, Response.success(infos)));
                }
            });
        }
        performCall(apiService.getGradeInfoForStudent(studentId), new Callback<List<StudentGradeInfo>>() {
            @Override
            public void onResponse(Call<List<StudentGradeInfo>> call, Response<List<StudentGradeInfo>> response) {
                if (response.isSuccessful() && response.body() != null && db != null) {
                    List<StudentGradeInfo> infos = response.body();
                    executor.execute(() -> {
                        for (StudentGradeInfo info : infos) {
                            if (info.enrollment != null) db.enrollmentDao().insert(info.enrollment);
                            if (info.subject != null) db.subjectDao().insert(info.subject);
                            if (info.student != null) db.userDao().insert(info.student);
                        }
                    });
                }
                callback.onResponse(call, response);
            }

            @Override
            public void onFailure(Call<List<StudentGradeInfo>> call, Throwable t) {
                if (db != null) {
                    executor.execute(() -> {
                        List<Enrollment> enrollments = db.enrollmentDao().getByStudentId(studentId);
                        List<StudentGradeInfo> infos = new ArrayList<>();
                        for (Enrollment en : enrollments) {
                            Subject sub = db.subjectDao().getSubjectById(en.subjectId);
                            User stu = db.userDao().getUserById(en.studentId);
                            StudentGradeInfo info = new StudentGradeInfo();
                            info.enrollment = en;
                            info.subject = sub;
                            info.student = stu;
                            infos.add(info);
                        }
                        mainHandler.post(() -> callback.onResponse(call, Response.success(infos)));
                    });
                } else {
                    callback.onFailure(call, t);
                }
            }
        });
    }

    public void getUserById(int id, Callback<User> callback) {
        if (db != null) {
            executor.execute(() -> {
                User cached = db.userDao().getUserById(id);
                if (cached != null) {
                    mainHandler.post(() -> callback.onResponse(null, Response.success(cached)));
                }
            });
        }
        performCall(apiService.getUserById(id), callback);
    }

    public void getSubjectById(int id, Callback<Subject> callback) {
        if (db != null) {
            executor.execute(() -> {
                Subject cached = db.subjectDao().getSubjectById(id);
                if (cached != null) {
                    mainHandler.post(() -> callback.onResponse(null, Response.success(cached)));
                }
            });
        }
        performCall(apiService.getSubjectById(id), callback);
    }

    public void getFacultyById(int id, Callback<Faculty> callback) {
        if (db != null) {
            executor.execute(() -> {
                Faculty cached = db.facultyDao().getFacultyById(id);
                if (cached != null) {
                    mainHandler.post(() -> callback.onResponse(null, Response.success(cached)));
                }
            });
        }
        performCall(apiService.getFacultyById(id), callback);
    }

    public void getFacilityById(int id, Callback<Facility> callback) {
        if (db != null) {
            executor.execute(() -> {
                Facility cached = db.facilityDao().getFacilityById(id);
                if (cached != null) {
                    mainHandler.post(() -> callback.onResponse(null, Response.success(cached)));
                }
            });
        }
        performCall(apiService.getFacilityById(id), callback);
    }

    public void getEnrollmentById(int id, Callback<Enrollment> callback) {
        if (db != null) {
            executor.execute(() -> {
                Enrollment cached = db.enrollmentDao().getById(id);
                if (cached != null) {
                    mainHandler.post(() -> callback.onResponse(null, Response.success(cached)));
                }
            });
        }
        performCall(apiService.getEnrollmentById(id), callback);
    }

    public void getSubjectsByProfessor(int professorId, Callback<List<Subject>> callback) {
        if (db != null) {
            executor.execute(() -> {
                List<Subject> cached = db.subjectDao().getSubjectsByProfessor(professorId);
                if (cached != null && !cached.isEmpty()) {
                    mainHandler.post(() -> callback.onResponse(null, Response.success(cached)));
                }
            });
        }
        performCall(apiService.getSubjectsByProfessor(professorId), new Callback<List<Subject>>() {
            @Override
            public void onResponse(Call<List<Subject>> call, Response<List<Subject>> response) {
                if (response.isSuccessful() && response.body() != null && db != null) {
                    List<Subject> subjects = response.body();
                    executor.execute(() -> db.subjectDao().insertAll(subjects));
                }
                callback.onResponse(call, response);
            }

            @Override
            public void onFailure(Call<List<Subject>> call, Throwable t) {
                if (db != null) {
                    executor.execute(() -> {
                        List<Subject> cached = db.subjectDao().getSubjectsByProfessor(professorId);
                        mainHandler.post(() -> callback.onResponse(call, Response.success(cached)));
                    });
                } else {
                    callback.onFailure(call, t);
                }
            }
        });
    }

    public void getSubjectEnrollments(int subjectId, Callback<List<StudentGradeInfo>> callback) {
        if (db != null) {
            executor.execute(() -> {
                List<Enrollment> enrollments = db.enrollmentDao().getBySubjectId(subjectId);
                List<StudentGradeInfo> infos = new ArrayList<>();
                for (Enrollment en : enrollments) {
                    Subject sub = db.subjectDao().getSubjectById(en.subjectId);
                    User stu = db.userDao().getUserById(en.studentId);
                    StudentGradeInfo info = new StudentGradeInfo();
                    info.enrollment = en;
                    info.subject = sub;
                    info.student = stu;
                    infos.add(info);
                }
                if (!infos.isEmpty()) {
                    mainHandler.post(() -> callback.onResponse(null, Response.success(infos)));
                }
            });
        }
        performCall(apiService.getSubjectEnrollments(subjectId), new Callback<List<StudentGradeInfo>>() {
            @Override
            public void onResponse(Call<List<StudentGradeInfo>> call, Response<List<StudentGradeInfo>> response) {
                if (response.isSuccessful() && response.body() != null && db != null) {
                    List<StudentGradeInfo> infos = response.body();
                    executor.execute(() -> {
                        for (StudentGradeInfo info : infos) {
                            if (info.enrollment != null) db.enrollmentDao().insert(info.enrollment);
                            if (info.subject != null) db.subjectDao().insert(info.subject);
                            if (info.student != null) db.userDao().insert(info.student);
                        }
                    });
                }
                callback.onResponse(call, response);
            }

            @Override
            public void onFailure(Call<List<StudentGradeInfo>> call, Throwable t) {
                if (db != null) {
                    executor.execute(() -> {
                        List<Enrollment> enrollments = db.enrollmentDao().getBySubjectId(subjectId);
                        List<StudentGradeInfo> infos = new ArrayList<>();
                        for (Enrollment en : enrollments) {
                            Subject sub = db.subjectDao().getSubjectById(en.subjectId);
                            User stu = db.userDao().getUserById(en.studentId);
                            StudentGradeInfo info = new StudentGradeInfo();
                            info.enrollment = en;
                            info.subject = sub;
                            info.student = stu;
                            infos.add(info);
                        }
                        mainHandler.post(() -> callback.onResponse(call, Response.success(infos)));
                    });
                } else {
                    callback.onFailure(call, t);
                }
            }
        });
    }

    public void getSubjectParticipants(int subjectId, Callback<List<User>> callback) {
        if (db != null) {
            executor.execute(() -> {
                List<Enrollment> enrollments = db.enrollmentDao().getBySubjectId(subjectId);
                List<FacilitySchedule> schedules = db.facilityScheduleDao().getBySubjectId(subjectId);
                List<User> participants = new ArrayList<>();
                List<Integer> addedUserIds = new ArrayList<>();

                for (FacilitySchedule sch : schedules) {
                    if (!addedUserIds.contains(sch.professorId)) {
                        User prof = db.userDao().getUserById(sch.professorId);
                        if (prof != null) {
                            participants.add(prof);
                            addedUserIds.add(prof.id);
                        }
                    }
                }
                for (Enrollment en : enrollments) {
                    if (!addedUserIds.contains(en.studentId)) {
                        User stu = db.userDao().getUserById(en.studentId);
                        if (stu != null) {
                            participants.add(stu);
                            addedUserIds.add(stu.id);
                        }
                    }
                }
                if (!participants.isEmpty()) {
                    mainHandler.post(() -> callback.onResponse(null, Response.success(participants)));
                }
            });
        }
        performCall(apiService.getSubjectParticipants(subjectId), new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null && db != null) {
                    List<User> users = response.body();
                    executor.execute(() -> db.userDao().insertAll(users));
                }
                callback.onResponse(call, response);
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                if (db != null) {
                    executor.execute(() -> {
                        List<Enrollment> enrollments = db.enrollmentDao().getBySubjectId(subjectId);
                        List<FacilitySchedule> schedules = db.facilityScheduleDao().getBySubjectId(subjectId);
                        List<User> participants = new ArrayList<>();
                        List<Integer> addedUserIds = new ArrayList<>();

                        for (FacilitySchedule sch : schedules) {
                            if (!addedUserIds.contains(sch.professorId)) {
                                User prof = db.userDao().getUserById(sch.professorId);
                                if (prof != null) {
                                    participants.add(prof);
                                    addedUserIds.add(prof.id);
                                }
                            }
                        }
                        for (Enrollment en : enrollments) {
                            if (!addedUserIds.contains(en.studentId)) {
                                User stu = db.userDao().getUserById(en.studentId);
                                if (stu != null) {
                                    participants.add(stu);
                                    addedUserIds.add(stu.id);
                                }
                            }
                        }
                        mainHandler.post(() -> callback.onResponse(call, Response.success(participants)));
                    });
                } else {
                    callback.onFailure(call, t);
                }
            }
        });
    }

    // --- Facilities ---
    public void getAllFacilities(Callback<List<Facility>> callback) {
        if (db != null) {
            executor.execute(() -> {
                List<Facility> cached = db.facilityDao().getAllFacilities();
                if (cached != null && !cached.isEmpty()) {
                    mainHandler.post(() -> callback.onResponse(null, Response.success(cached)));
                }
            });
        }
        performCall(apiService.getFacilities(), new Callback<List<Facility>>() {
            @Override
            public void onResponse(Call<List<Facility>> call, Response<List<Facility>> response) {
                if (response.isSuccessful() && response.body() != null && db != null) {
                    List<Facility> facilities = response.body();
                    executor.execute(() -> db.facilityDao().insertAll(facilities));
                }
                callback.onResponse(call, response);
            }

            @Override
            public void onFailure(Call<List<Facility>> call, Throwable t) {
                if (db != null) {
                    executor.execute(() -> {
                        List<Facility> cached = db.facilityDao().getAllFacilities();
                        if (cached != null && !cached.isEmpty()) {
                            mainHandler.post(() -> callback.onResponse(call, Response.success(cached)));
                        } else {
                            mainHandler.post(() -> callback.onFailure(call, t));
                        }
                    });
                } else {
                    callback.onFailure(call, t);
                }
            }
        });
    }

    public void insertFacility(Facility facility, Callback<Facility> callback) {
        performCall(apiService.createFacility(facility), new Callback<Facility>() {
            @Override
            public void onResponse(Call<Facility> call, Response<Facility> response) {
                if (response.isSuccessful() && response.body() != null && db != null) {
                    Facility saved = response.body();
                    executor.execute(() -> db.facilityDao().insert(saved));
                }
                callback.onResponse(call, response);
            }

            @Override
            public void onFailure(Call<Facility> call, Throwable t) {
                callback.onFailure(call, t);
            }
        });
    }

    public void updateFacility(Facility facility, Callback<Facility> callback) {
        performCall(apiService.updateFacility(facility.id, facility), new Callback<Facility>() {
            @Override
            public void onResponse(Call<Facility> call, Response<Facility> response) {
                if (response.isSuccessful() && response.body() != null && db != null) {
                    Facility updated = response.body();
                    executor.execute(() -> db.facilityDao().insert(updated));
                }
                callback.onResponse(call, response);
            }

            @Override
            public void onFailure(Call<Facility> call, Throwable t) {
                callback.onFailure(call, t);
            }
        });
    }

    public void deleteFacility(int facId, Callback<Void> callback) {
        performCall(apiService.deleteFacility(facId), new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful() && db != null) {
                    executor.execute(() -> db.facilityDao().deleteById(facId));
                }
                callback.onResponse(call, response);
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                callback.onFailure(call, t);
            }
        });
    }

    // --- Schedules ---
    public void getFacilitySchedules(int facId, Callback<List<ScheduleInfo>> callback) {
        if (db != null) {
            executor.execute(() -> {
                List<FacilitySchedule> schedules = db.facilityScheduleDao().getByFacilityId(facId);
                List<ScheduleInfo> infos = new ArrayList<>();
                for (FacilitySchedule sch : schedules) {
                    Subject sub = db.subjectDao().getSubjectById(sch.subjectId);
                    User prof = db.userDao().getUserById(sch.professorId);
                    ScheduleInfo info = new ScheduleInfo();
                    info.schedule = sch;
                    info.subjectName = sub != null ? sub.name : "";
                    info.subjectColor = (sch.color != null && !sch.color.isEmpty()) ? sch.color : (sub != null ? sub.color : "BLUE");
                    info.professorName = prof != null ? prof.name : "";
                    infos.add(info);
                }
                if (!infos.isEmpty()) {
                    mainHandler.post(() -> callback.onResponse(null, Response.success(infos)));
                }
            });
        }
        performCall(apiService.getFacilitySchedules(facId), new Callback<List<ScheduleInfo>>() {
            @Override
            public void onResponse(Call<List<ScheduleInfo>> call, Response<List<ScheduleInfo>> response) {
                callback.onResponse(call, response);
            }

            @Override
            public void onFailure(Call<List<ScheduleInfo>> call, Throwable t) {
                if (db != null) {
                    executor.execute(() -> {
                        List<FacilitySchedule> schedules = db.facilityScheduleDao().getByFacilityId(facId);
                        List<ScheduleInfo> infos = new ArrayList<>();
                        for (FacilitySchedule sch : schedules) {
                            Subject sub = db.subjectDao().getSubjectById(sch.subjectId);
                            User prof = db.userDao().getUserById(sch.professorId);
                            ScheduleInfo info = new ScheduleInfo();
                            info.schedule = sch;
                            info.subjectName = sub != null ? sub.name : "";
                            info.subjectColor = (sch.color != null && !sch.color.isEmpty()) ? sch.color : (sub != null ? sub.color : "BLUE");
                            info.professorName = prof != null ? prof.name : "";
                            infos.add(info);
                        }
                        mainHandler.post(() -> callback.onResponse(call, Response.success(infos)));
                    });
                } else {
                    callback.onFailure(call, t);
                }
            }
        });
    }

    public void insertSchedule(FacilitySchedule schedule, Callback<FacilitySchedule> callback) {
        performCall(apiService.createSchedule(schedule), callback);
    }

    public void updateSchedule(FacilitySchedule schedule, Callback<FacilitySchedule> callback) {
        performCall(apiService.updateSchedule(schedule.id, schedule), callback);
    }

    public void deleteSchedule(int schId, Callback<Void> callback) {
        performCall(apiService.deleteSchedule(schId), callback);
    }

    public void updateUserSubjectColor(int userId, int subjectId, String color, Callback<Void> callback) {
        if (db != null) {
            executor.execute(() -> {
                UserSubjectColor existing = db.userSubjectColorDao().get(userId, subjectId);
                if (existing != null) {
                    existing.color = color;
                    db.userSubjectColorDao().insert(existing);
                } else {
                    db.userSubjectColorDao().insert(new UserSubjectColor(userId, subjectId, color));
                }
                List<Enrollment> enrollments = db.enrollmentDao().getByStudentId(userId);
                if (enrollments != null) {
                    for (Enrollment en : enrollments) {
                        if (en.subjectId == subjectId) {
                            en.color = color;
                            db.enrollmentDao().insert(en);
                        }
                    }
                }
                mainHandler.post(() -> {
                    if (callback != null) callback.onResponse(null, Response.success(null));
                });
            });
        }
        performCall(apiService.updateUserSubjectColor(userId, subjectId, color), callback);
    }

    public void getUserSubjectColor(int userId, int subjectId, Callback<String> callback) {
        if (db != null) {
            executor.execute(() -> {
                UserSubjectColor usc = db.userSubjectColorDao().get(userId, subjectId);
                String color = usc != null ? usc.color : null;
                if (color == null) {
                    List<Enrollment> enrollments = db.enrollmentDao().getByStudentId(userId);
                    if (enrollments != null) {
                        for (Enrollment en : enrollments) {
                            if (en.subjectId == subjectId && en.color != null) {
                                color = en.color;
                                break;
                            }
                        }
                    }
                }
                if (color == null) {
                    Subject sub = db.subjectDao().getSubjectById(subjectId);
                    color = sub != null ? sub.color : "BLUE";
                }
                final String finalColor = color != null ? color : "BLUE";
                mainHandler.post(() -> callback.onResponse(null, Response.success(finalColor)));
            });
        } else {
            performCall(apiService.getUserSubjectColor(userId, subjectId), new Callback<Map<String, String>>() {
                @Override
                public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        callback.onResponse(null, Response.success(response.body().get("color")));
                    } else {
                        callback.onResponse(null, Response.success("BLUE"));
                    }
                }

                @Override
                public void onFailure(Call<Map<String, String>> call, Throwable t) {
                    callback.onResponse(null, Response.success("BLUE"));
                }
            });
        }
    }

    // --- Blog ---
    public void getSubjectBlog(int subjectId, Callback<List<BlogEntry>> callback) {
        if (db != null) {
            executor.execute(() -> {
                List<BlogEntry> cached = db.blogDao().getEntriesBySubject(subjectId);
                if (cached != null && !cached.isEmpty()) {
                    mainHandler.post(() -> callback.onResponse(null, Response.success(cached)));
                }
            });
        }
        performCall(apiService.getSubjectBlog(subjectId), new Callback<List<BlogEntry>>() {
            @Override
            public void onResponse(Call<List<BlogEntry>> call, Response<List<BlogEntry>> response) {
                if (response.isSuccessful() && response.body() != null && db != null) {
                    List<BlogEntry> entries = response.body();
                    executor.execute(() -> db.blogDao().insertEntries(entries));
                }
                callback.onResponse(call, response);
            }

            @Override
            public void onFailure(Call<List<BlogEntry>> call, Throwable t) {
                if (db != null) {
                    executor.execute(() -> {
                        List<BlogEntry> cached = db.blogDao().getEntriesBySubject(subjectId);
                        if (cached != null && !cached.isEmpty()) {
                            mainHandler.post(() -> callback.onResponse(call, Response.success(cached)));
                        } else {
                            mainHandler.post(() -> callback.onFailure(call, t));
                        }
                    });
                } else {
                    callback.onFailure(call, t);
                }
            }
        });
    }

    public void insertBlogEntry(BlogEntry entry, Callback<BlogEntry> callback) {
        performCall(apiService.createBlogEntry(entry), new Callback<BlogEntry>() {
            @Override
            public void onResponse(Call<BlogEntry> call, Response<BlogEntry> response) {
                if (response.isSuccessful() && response.body() != null && db != null) {
                    BlogEntry saved = response.body();
                    executor.execute(() -> db.blogDao().insertEntry(saved));
                }
                callback.onResponse(call, response);
            }

            @Override
            public void onFailure(Call<BlogEntry> call, Throwable t) {
                callback.onFailure(call, t);
            }
        });
    }

    public void updateBlogEntry(BlogEntry entry, Callback<BlogEntry> callback) {
        performCall(apiService.updateBlogEntry(entry.id, entry), new Callback<BlogEntry>() {
            @Override
            public void onResponse(Call<BlogEntry> call, Response<BlogEntry> response) {
                if (response.isSuccessful() && response.body() != null && db != null) {
                    BlogEntry updated = response.body();
                    executor.execute(() -> db.blogDao().insertEntry(updated));
                }
                callback.onResponse(call, response);
            }

            @Override
            public void onFailure(Call<BlogEntry> call, Throwable t) {
                callback.onFailure(call, t);
            }
        });
    }

    public void reorderBlog(List<BlogEntry> entries, Callback<Void> callback) {
        performCall(apiService.reorderBlog(entries), callback);
    }

    public void deleteBlogEntry(int entryId, Callback<Void> callback) {
        performCall(apiService.deleteBlogEntry(entryId), new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful() && db != null) {
                    executor.execute(() -> db.blogDao().deleteEntryById(entryId));
                }
                callback.onResponse(call, response);
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                callback.onFailure(call, t);
            }
        });
    }

    // --- Blog Comments ---
    public void getBlogComments(int entryId, Callback<List<CommentInfo>> callback) {
        if (db != null) {
            executor.execute(() -> {
                List<BlogComment> comments = db.blogDao().getCommentsByEntry(entryId);
                List<CommentInfo> infos = new ArrayList<>();
                for (BlogComment c : comments) {
                    User u = db.userDao().getUserById(c.userId);
                    CommentInfo info = new CommentInfo();
                    info.comment = c;
                    info.userName = u != null ? u.name : "Usuario";
                    info.userRole = u != null ? u.role : "STUDENT";
                    info.userPhoto = u != null ? u.profile_image : null;
                    infos.add(info);
                }
                if (!infos.isEmpty()) {
                    mainHandler.post(() -> callback.onResponse(null, Response.success(infos)));
                }
            });
        }
        performCall(apiService.getBlogComments(entryId), new Callback<List<CommentInfo>>() {
            @Override
            public void onResponse(Call<List<CommentInfo>> call, Response<List<CommentInfo>> response) {
                if (response.isSuccessful() && response.body() != null && db != null) {
                    List<CommentInfo> infos = response.body();
                    executor.execute(() -> {
                        db.blogDao().deleteCommentsByEntry(entryId);
                        for (CommentInfo info : infos) {
                            if (info.comment != null) db.blogDao().insertComment(info.comment);
                        }
                    });
                }
                callback.onResponse(call, response);
            }

            @Override
            public void onFailure(Call<List<CommentInfo>> call, Throwable t) {
                if (db != null) {
                    executor.execute(() -> {
                        List<BlogComment> comments = db.blogDao().getCommentsByEntry(entryId);
                        List<CommentInfo> infos = new ArrayList<>();
                        for (BlogComment c : comments) {
                            User u = db.userDao().getUserById(c.userId);
                            CommentInfo info = new CommentInfo();
                            info.comment = c;
                            info.userName = u != null ? u.name : "Usuario";
                            info.userRole = u != null ? u.role : "STUDENT";
                            info.userPhoto = u != null ? u.profile_image : null;
                            infos.add(info);
                        }
                        mainHandler.post(() -> callback.onResponse(call, Response.success(infos)));
                    });
                } else {
                    callback.onFailure(call, t);
                }
            }
        });
    }

    public void insertBlogComment(BlogComment comment, Callback<BlogComment> callback) {
        performCall(apiService.createBlogComment(comment), new Callback<BlogComment>() {
            @Override
            public void onResponse(Call<BlogComment> call, Response<BlogComment> response) {
                if (response.isSuccessful() && response.body() != null && db != null) {
                    BlogComment created = response.body();
                    executor.execute(() -> db.blogDao().insertComment(created));
                }
                callback.onResponse(call, response);
            }

            @Override
            public void onFailure(Call<BlogComment> call, Throwable t) {
                if (db != null) {
                    executor.execute(() -> db.blogDao().insertComment(comment));
                }
                callback.onFailure(call, t);
            }
        });
    }

    public void deleteBlogComment(int commentId, Callback<Void> callback) {
        if (db != null) {
            executor.execute(() -> db.blogDao().deleteCommentById(commentId));
        }
        performCall(apiService.deleteBlogComment(commentId), callback);
    }

    public void clearBlogDiscussion(int entryId, Callback<Void> callback) {
        if (db != null) {
            executor.execute(() -> db.blogDao().deleteCommentsByEntry(entryId));
        }
        performCall(apiService.clearBlogDiscussion(entryId), callback);
    }

    // --- Notifications ---
    public void getNotifications(Callback<List<Notification>> callback) {
        if (db != null) {
            executor.execute(() -> {
                try {
                    List<Notification> cached = db.notificationDao().getAllNotifications();
                    mainHandler.post(() -> callback.onResponse(null, Response.success(cached != null ? cached : new ArrayList<>())));
                } catch (Exception e) {
                    mainHandler.post(() -> callback.onResponse(null, Response.success(new ArrayList<>())));
                }
            });
        } else {
            callback.onResponse(null, Response.success(new ArrayList<>()));
        }
    }

    public void insertNotification(Notification notification, Callback<Void> callback) {
        if (db != null) {
            executor.execute(() -> {
                try {
                    db.notificationDao().insertNotification(notification);
                    mainHandler.post(() -> {
                        if (callback != null) callback.onResponse(null, Response.success(null));
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    mainHandler.post(() -> {
                        if (callback != null) callback.onFailure(null, e);
                    });
                }
            });
        } else {
            if (callback != null) callback.onResponse(null, Response.success(null));
        }
    }

    // --- Synchronize all data from SQL Server for Offline use ---
    public void downloadAllDataForOffline(SyncCallback callback) {
        if (db == null) {
            callback.onError("Base de datos local no disponible");
            return;
        }

        performCall(apiService.getStudents(), new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> respStudents) {
                if (respStudents.isSuccessful() && respStudents.body() != null) {
                    executor.execute(() -> db.userDao().insertAll(respStudents.body()));
                }
                performCall(apiService.getAdmins(), new Callback<List<User>>() {
                    @Override
                    public void onResponse(Call<List<User>> call, Response<List<User>> respAdmins) {
                        if (respAdmins.isSuccessful() && respAdmins.body() != null) {
                            executor.execute(() -> db.userDao().insertAll(respAdmins.body()));
                        }
                        performCall(apiService.getProfessors(), new Callback<List<User>>() {
                            @Override
                            public void onResponse(Call<List<User>> call, Response<List<User>> respProfs) {
                                if (respProfs.isSuccessful() && respProfs.body() != null) {
                                    executor.execute(() -> db.userDao().insertAll(respProfs.body()));
                                }
                                performCall(apiService.getFaculties(), new Callback<List<Faculty>>() {
                                    @Override
                                    public void onResponse(Call<List<Faculty>> call, Response<List<Faculty>> respFacs) {
                                        if (respFacs.isSuccessful() && respFacs.body() != null) {
                                            executor.execute(() -> db.facultyDao().insertAll(respFacs.body()));
                                        }
                                        performCall(apiService.getSubjects(), new Callback<List<Subject>>() {
                                            @Override
                                            public void onResponse(Call<List<Subject>> call, Response<List<Subject>> respSubs) {
                                                if (respSubs.isSuccessful() && respSubs.body() != null) {
                                                    List<Subject> subs = respSubs.body();
                                                    executor.execute(() -> db.subjectDao().insertAll(subs));

                                                    for (Subject s : subs) {
                                                        performCall(apiService.getSubjectBlog(s.id), new Callback<List<BlogEntry>>() {
                                                            @Override
                                                            public void onResponse(Call<List<BlogEntry>> call, Response<List<BlogEntry>> respBlog) {
                                                                if (respBlog.isSuccessful() && respBlog.body() != null) {
                                                                    List<BlogEntry> entries = respBlog.body();
                                                                    executor.execute(() -> db.blogDao().insertEntries(entries));

                                                                    for (BlogEntry entry : entries) {
                                                                        performCall(apiService.getBlogComments(entry.id), new Callback<List<CommentInfo>>() {
                                                                            @Override
                                                                            public void onResponse(Call<List<CommentInfo>> call, Response<List<CommentInfo>> respComments) {
                                                                                if (respComments.isSuccessful() && respComments.body() != null) {
                                                                                    executor.execute(() -> {
                                                                                        for (CommentInfo ci : respComments.body()) {
                                                                                            if (ci.comment != null) db.blogDao().insertComment(ci.comment);
                                                                                        }
                                                                                    });
                                                                                }
                                                                            }
                                                                            @Override public void onFailure(Call<List<CommentInfo>> call, Throwable t) {}
                                                                        });
                                                                    }
                                                                }
                                                            }
                                                            @Override public void onFailure(Call<List<BlogEntry>> call, Throwable t) {}
                                                        });

                                                        performCall(apiService.getSubjectParticipants(s.id), new Callback<List<User>>() {
                                                            @Override
                                                            public void onResponse(Call<List<User>> call, Response<List<User>> respPart) {
                                                                if (respPart.isSuccessful() && respPart.body() != null) {
                                                                    executor.execute(() -> db.userDao().insertAll(respPart.body()));
                                                                }
                                                            }
                                                            @Override public void onFailure(Call<List<User>> call, Throwable t) {}
                                                        });

                                                        performCall(apiService.getSubjectEnrollments(s.id), new Callback<List<StudentGradeInfo>>() {
                                                            @Override
                                                            public void onResponse(Call<List<StudentGradeInfo>> call, Response<List<StudentGradeInfo>> respGrades) {
                                                                if (respGrades.isSuccessful() && respGrades.body() != null) {
                                                                    executor.execute(() -> {
                                                                        for (StudentGradeInfo info : respGrades.body()) {
                                                                            if (info.enrollment != null) db.enrollmentDao().insert(info.enrollment);
                                                                            if (info.student != null) db.userDao().insert(info.student);
                                                                            if (info.subject != null) db.subjectDao().insert(info.subject);
                                                                        }
                                                                    });
                                                                }
                                                            }
                                                            @Override public void onFailure(Call<List<StudentGradeInfo>> call, Throwable t) {}
                                                        });
                                                    }
                                                }
                                                performCall(apiService.getFacilities(), new Callback<List<Facility>>() {
                                                    @Override
                                                    public void onResponse(Call<List<Facility>> call, Response<List<Facility>> respFacilities) {
                                                        if (respFacilities.isSuccessful() && respFacilities.body() != null) {
                                                            List<Facility> facs = respFacilities.body();
                                                            executor.execute(() -> db.facilityDao().insertAll(facs));

                                                            for (Facility f : facs) {
                                                                performCall(apiService.getFacilitySchedules(f.id), new Callback<List<ScheduleInfo>>() {
                                                                    @Override
                                                                    public void onResponse(Call<List<ScheduleInfo>> call, Response<List<ScheduleInfo>> respSch) {
                                                                        if (respSch.isSuccessful() && respSch.body() != null) {
                                                                            executor.execute(() -> {
                                                                                for (ScheduleInfo info : respSch.body()) {
                                                                                    if (info.schedule != null) {
                                                                                        db.facilityScheduleDao().insert(info.schedule);
                                                                                    }
                                                                                }
                                                                            });
                                                                        }
                                                                    }
                                                                    @Override public void onFailure(Call<List<ScheduleInfo>> call, Throwable t) {}
                                                                });
                                                            }
                                                        }

                                                        callback.onSuccess("¡Todos los datos del servidor han sido descargados para uso offline!");
                                                    }

                                                    @Override
                                                    public void onFailure(Call<List<Facility>> call, Throwable t) {
                                                        callback.onSuccess("Datos guardados parcialmente.");
                                                    }
                                                });
                                            }

                                            @Override
                                            public void onFailure(Call<List<Subject>> call, Throwable t) {
                                                callback.onError("Error al descargar materias.");
                                            }
                                        });
                                    }

                                    @Override
                                    public void onFailure(Call<List<Faculty>> call, Throwable t) {
                                        callback.onError("Error al descargar facultades.");
                                    }
                                });
                            }

                            @Override
                            public void onFailure(Call<List<User>> call, Throwable t) {
                                callback.onError("Error al descargar profesores.");
                            }
                        });
                    }

                    @Override
                    public void onFailure(Call<List<User>> call, Throwable t) {
                        callback.onError("Error al descargar administradores.");
                    }
                });
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                callback.onError("Error de conexión: No se pudo contactar con el servidor SQL.");
            }
        });
    }
}
