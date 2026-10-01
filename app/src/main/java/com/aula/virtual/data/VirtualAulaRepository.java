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
import com.aula.virtual.ui.ThemeHelper;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VirtualAulaRepository {
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

    private Context context;

    public VirtualAulaRepository() {
        this(null);
    }

    public VirtualAulaRepository(Context context) {
        this.context = context != null ? context.getApplicationContext() : null;
        if (this.context != null) {
            try {
                this.db = AppDatabase.getInstance(this.context);
            } catch (Exception e) {
                e.printStackTrace();
                this.db = null;
            }
        }
    }

    private ApiService getApiService() {
        return RetrofitClient.getApiService(this.context);
    }

    private int consecutiveFailures = 0;
    private static final int FAILURE_THRESHOLD = 3;

    private boolean isLocalMode() {
        return context != null && ThemeHelper.isLocalMode(context);
    }

    public static List<User> deduplicateUsers(List<User> list) {
        if (list == null) return new ArrayList<>();
        Map<String, User> map = new LinkedHashMap<>();
        for (User u : list) {
            if (u != null && u.carnet != null && !u.carnet.trim().isEmpty()) {
                map.put(u.carnet.trim().toUpperCase(), u);
            }
        }
        return new ArrayList<>(map.values());
    }

    public static List<Subject> deduplicateSubjects(List<Subject> list) {
        if (list == null) return new ArrayList<>();
        Map<String, Subject> map = new LinkedHashMap<>();
        for (Subject s : list) {
            if (s != null && s.name != null) {
                String key = (s.id > 0 ? ("ID_" + s.id) : (s.name.trim() + "_" + (s.section != null ? s.section.trim() : ""))).toUpperCase();
                map.put(key, s);
            }
        }
        return new ArrayList<>(map.values());
    }

    public static List<Faculty> deduplicateFaculties(List<Faculty> list) {
        if (list == null) return new ArrayList<>();
        Map<String, Faculty> map = new LinkedHashMap<>();
        for (Faculty f : list) {
            if (f != null && f.name != null) {
                map.put(f.name.trim().toUpperCase(), f);
            }
        }
        return new ArrayList<>(map.values());
    }

    public static List<Facility> deduplicateFacilities(List<Facility> list) {
        if (list == null) return new ArrayList<>();
        Map<String, Facility> map = new LinkedHashMap<>();
        for (Facility f : list) {
            if (f != null && f.name != null) {
                map.put(f.name.trim().toUpperCase(), f);
            }
        }
        return new ArrayList<>(map.values());
    }

    private <T> void performCall(Call<T> call, Callback<T> callback) {
        if (call == null) return;
        call.enqueue(new Callback<T>() {
            @Override
            public void onResponse(Call<T> call, Response<T> response) {
                boolean isSuccess = response != null && (response.isSuccessful() || response.code() < 500);
                if (isSuccess) {
                    consecutiveFailures = 0;
                    if (connectionStatusListener != null && !isLocalMode()) {
                        connectionStatusListener.onStatusChanged(true);
                    }
                } else {
                    consecutiveFailures++;
                    if (consecutiveFailures >= FAILURE_THRESHOLD && connectionStatusListener != null && !isLocalMode()) {
                        connectionStatusListener.onStatusChanged(false);
                    }
                }
                if (callback != null) {
                    callback.onResponse(call, response);
                }
            }

            @Override
            public void onFailure(Call<T> call, Throwable t) {
                if (call.isCanceled()) {
                    if (callback != null) callback.onFailure(call, t);
                    return;
                }
                consecutiveFailures++;
                if (consecutiveFailures >= FAILURE_THRESHOLD && connectionStatusListener != null && !isLocalMode()) {
                    connectionStatusListener.onStatusChanged(false);
                }
                if (callback != null) {
                    callback.onFailure(call, t);
                }
            }
        });
    }

    public void login(String carnet, String password, Callback<User> callback) {
        if (isLocalMode()) {
            if (db != null) {
                executor.execute(() -> {
                    try {
                        User localUser = db.userDao().login(carnet, password);
                        if (localUser != null) {
                            mainHandler.post(() -> callback.onResponse(null, Response.success(localUser)));
                        } else {
                            mainHandler.post(() -> callback.onFailure(null, new Throwable("Credenciales inválidas en almacenamiento local.")));
                        }
                    } catch (Exception e) {
                        mainHandler.post(() -> callback.onFailure(null, e));
                    }
                });
            } else {
                callback.onFailure(null, new Throwable("Base de datos local no disponible."));
            }
            return;
        }

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
        performCall(getApiService().login(carnet, password), new Callback<User>() {
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
        if (isLocalMode()) {
            if (db != null) {
                executor.execute(() -> {
                    try {
                        List<User> cached = db.userDao().getStudents();
                        mainHandler.post(() -> callback.onResponse(null, Response.success(deduplicateUsers(cached))));
                    } catch (Exception e) {
                        mainHandler.post(() -> callback.onFailure(null, e));
                    }
                });
            } else {
                callback.onResponse(null, Response.success(new ArrayList<>()));
            }
            return;
        }

        performCall(getApiService().getStudents(), new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onResponse(call, Response.success(deduplicateUsers(response.body())));
                } else {
                    callback.onResponse(call, response);
                }
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                callback.onFailure(call, t);
            }
        });
    }

    public void getAllAdmins(Callback<List<User>> callback) {
        if (isLocalMode()) {
            if (db != null) {
                executor.execute(() -> {
                    try {
                        List<User> cached = db.userDao().getAdmins();
                        mainHandler.post(() -> callback.onResponse(null, Response.success(deduplicateUsers(cached))));
                    } catch (Exception e) {
                        mainHandler.post(() -> callback.onFailure(null, e));
                    }
                });
            } else {
                callback.onResponse(null, Response.success(new ArrayList<>()));
            }
            return;
        }

        performCall(getApiService().getAdmins(), new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onResponse(call, Response.success(deduplicateUsers(response.body())));
                } else {
                    callback.onResponse(call, response);
                }
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                callback.onFailure(call, t);
            }
        });
    }

    public void getAllProfessors(Callback<List<User>> callback) {
        if (isLocalMode()) {
            if (db != null) {
                executor.execute(() -> {
                    try {
                        List<User> cached = db.userDao().getProfessors();
                        mainHandler.post(() -> callback.onResponse(null, Response.success(deduplicateUsers(cached))));
                    } catch (Exception e) {
                        mainHandler.post(() -> callback.onFailure(null, e));
                    }
                });
            } else {
                callback.onResponse(null, Response.success(new ArrayList<>()));
            }
            return;
        }

        performCall(getApiService().getProfessors(), new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onResponse(call, Response.success(deduplicateUsers(response.body())));
                } else {
                    callback.onResponse(call, response);
                }
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                callback.onFailure(call, t);
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
        performCall(getApiService().getStudentSchedules(studentId), new Callback<List<ScheduleInfo>>() {
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
        performCall(getApiService().getProfessorSchedules(professorId), new Callback<List<ScheduleInfo>>() {
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
        if (isLocalMode()) {
            if (db != null) {
                executor.execute(() -> {
                    try {
                        List<Subject> cached = db.subjectDao().getAllSubjects();
                        mainHandler.post(() -> callback.onResponse(null, Response.success(deduplicateSubjects(cached))));
                    } catch (Exception e) {
                        mainHandler.post(() -> callback.onFailure(null, e));
                    }
                });
            } else {
                callback.onResponse(null, Response.success(new ArrayList<>()));
            }
            return;
        }

        performCall(getApiService().getSubjects(), new Callback<List<Subject>>() {
            @Override
            public void onResponse(Call<List<Subject>> call, Response<List<Subject>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onResponse(call, Response.success(deduplicateSubjects(response.body())));
                } else {
                    callback.onResponse(call, response);
                }
            }

            @Override
            public void onFailure(Call<List<Subject>> call, Throwable t) {
                callback.onFailure(call, t);
            }
        });
    }

    public void getAllFaculties(Callback<List<Faculty>> callback) {
        if (isLocalMode()) {
            if (db != null) {
                executor.execute(() -> {
                    try {
                        List<Faculty> cached = db.facultyDao().getAllFaculties();
                        mainHandler.post(() -> callback.onResponse(null, Response.success(deduplicateFaculties(cached))));
                    } catch (Exception e) {
                        mainHandler.post(() -> callback.onFailure(null, e));
                    }
                });
            } else {
                callback.onResponse(null, Response.success(new ArrayList<>()));
            }
            return;
        }

        performCall(getApiService().getFaculties(), new Callback<List<Faculty>>() {
            @Override
            public void onResponse(Call<List<Faculty>> call, Response<List<Faculty>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onResponse(call, Response.success(deduplicateFaculties(response.body())));
                } else {
                    callback.onResponse(call, response);
                }
            }

            @Override
            public void onFailure(Call<List<Faculty>> call, Throwable t) {
                callback.onFailure(call, t);
            }
        });
    }

    public void insertUser(User user, Callback<User> callback) {
        if (isLocalMode()) {
            if (db != null) {
                executor.execute(() -> {
                    try {
                        if (user.id == 0) user.id = (int) (System.currentTimeMillis() & 0x7fffffff);
                        db.userDao().insert(user);
                        mainHandler.post(() -> {
                            if (callback != null) callback.onResponse(null, Response.success(user));
                        });
                    } catch (Exception e) {
                        mainHandler.post(() -> {
                            if (callback != null) callback.onFailure(null, e);
                        });
                    }
                });
            } else {
                if (callback != null) callback.onFailure(null, new Throwable("Base de datos local no disponible."));
            }
            return;
        }

        performCall(getApiService().createUser(user), callback);
    }

    public void updateUser(User user, Callback<User> callback) {
        if (isLocalMode()) {
            if (db != null) {
                executor.execute(() -> {
                    try {
                        db.userDao().insert(user);
                        mainHandler.post(() -> {
                            if (callback != null) callback.onResponse(null, Response.success(user));
                        });
                    } catch (Exception e) {
                        mainHandler.post(() -> {
                            if (callback != null) callback.onFailure(null, e);
                        });
                    }
                });
            } else {
                if (callback != null) callback.onFailure(null, new Throwable("Base de datos local no disponible."));
            }
            return;
        }

        performCall(getApiService().updateUser(user.id, user), callback);
    }

    public void deleteUser(int userId, Callback<Void> callback) {
        if (isLocalMode()) {
            if (db != null) {
                executor.execute(() -> {
                    try {
                        db.userDao().deleteById(userId);
                        mainHandler.post(() -> {
                            if (callback != null) callback.onResponse(null, Response.success(null));
                        });
                    } catch (Exception e) {
                        mainHandler.post(() -> {
                            if (callback != null) callback.onFailure(null, e);
                        });
                    }
                });
            } else {
                if (callback != null) callback.onFailure(null, new Throwable("Base de datos local no disponible."));
            }
            return;
        }

        performCall(getApiService().deleteUser(userId), callback);
    }

    public void insertSubject(Subject subject, Callback<Subject> callback) {
        if (isLocalMode()) {
            if (db != null) {
                executor.execute(() -> {
                    try {
                        if (subject.id == 0) subject.id = (int) (System.currentTimeMillis() & 0x7fffffff);
                        db.subjectDao().insert(subject);
                        mainHandler.post(() -> {
                            if (callback != null) callback.onResponse(null, Response.success(subject));
                        });
                    } catch (Exception e) {
                        mainHandler.post(() -> {
                            if (callback != null) callback.onFailure(null, e);
                        });
                    }
                });
            } else {
                if (callback != null) callback.onFailure(null, new Throwable("Base de datos local no disponible."));
            }
            return;
        }

        performCall(getApiService().createSubject(subject), callback);
    }

    public void updateSubject(Subject subject, Callback<Subject> callback) {
        if (isLocalMode()) {
            if (db != null) {
                executor.execute(() -> {
                    try {
                        db.subjectDao().insert(subject);
                        mainHandler.post(() -> {
                            if (callback != null) callback.onResponse(null, Response.success(subject));
                        });
                    } catch (Exception e) {
                        mainHandler.post(() -> {
                            if (callback != null) callback.onFailure(null, e);
                        });
                    }
                });
            } else {
                if (callback != null) callback.onFailure(null, new Throwable("Base de datos local no disponible."));
            }
            return;
        }

        performCall(getApiService().updateSubject(subject.id, subject), callback);
    }

    public void deleteSubject(int subId, Callback<Void> callback) {
        if (isLocalMode()) {
            if (db != null) {
                executor.execute(() -> {
                    try {
                        db.subjectDao().deleteById(subId);
                        mainHandler.post(() -> {
                            if (callback != null) callback.onResponse(null, Response.success(null));
                        });
                    } catch (Exception e) {
                        mainHandler.post(() -> {
                            if (callback != null) callback.onFailure(null, e);
                        });
                    }
                });
            } else {
                if (callback != null) callback.onFailure(null, new Throwable("Base de datos local no disponible."));
            }
            return;
        }

        performCall(getApiService().deleteSubject(subId), callback);
    }

    public void insertFaculty(Faculty faculty, Callback<Faculty> callback) {
        if (isLocalMode()) {
            if (db != null) {
                executor.execute(() -> {
                    try {
                        if (faculty.id == 0) faculty.id = (int) (System.currentTimeMillis() & 0x7fffffff);
                        db.facultyDao().insert(faculty);
                        mainHandler.post(() -> {
                            if (callback != null) callback.onResponse(null, Response.success(faculty));
                        });
                    } catch (Exception e) {
                        mainHandler.post(() -> {
                            if (callback != null) callback.onFailure(null, e);
                        });
                    }
                });
            } else {
                if (callback != null) callback.onFailure(null, new Throwable("Base de datos local no disponible."));
            }
            return;
        }

        performCall(getApiService().createFaculty(faculty), callback);
    }

    public void updateFaculty(Faculty faculty, Callback<Faculty> callback) {
        if (isLocalMode()) {
            if (db != null) {
                executor.execute(() -> {
                    try {
                        db.facultyDao().insert(faculty);
                        mainHandler.post(() -> {
                            if (callback != null) callback.onResponse(null, Response.success(faculty));
                        });
                    } catch (Exception e) {
                        mainHandler.post(() -> {
                            if (callback != null) callback.onFailure(null, e);
                        });
                    }
                });
            } else {
                if (callback != null) callback.onFailure(null, new Throwable("Base de datos local no disponible."));
            }
            return;
        }

        performCall(getApiService().updateFaculty(faculty.id, faculty), callback);
    }

    public void deleteFaculty(int facId, Callback<Void> callback) {
        if (isLocalMode()) {
            if (db != null) {
                executor.execute(() -> {
                    try {
                        db.facultyDao().deleteById(facId);
                        mainHandler.post(() -> {
                            if (callback != null) callback.onResponse(null, Response.success(null));
                        });
                    } catch (Exception e) {
                        mainHandler.post(() -> {
                            if (callback != null) callback.onFailure(null, e);
                        });
                    }
                });
            } else {
                if (callback != null) callback.onFailure(null, new Throwable("Base de datos local no disponible."));
            }
            return;
        }

        performCall(getApiService().deleteFaculty(facId), callback);
    }

    public void enrollStudent(Enrollment enrollment, Callback<Enrollment> callback) {
        if (isLocalMode()) {
            if (db != null) {
                executor.execute(() -> {
                    try {
                        if (enrollment.id == 0) enrollment.id = (int) (System.currentTimeMillis() & 0x7fffffff);
                        db.enrollmentDao().insert(enrollment);
                        mainHandler.post(() -> {
                            if (callback != null) callback.onResponse(null, Response.success(enrollment));
                        });
                    } catch (Exception e) {
                        mainHandler.post(() -> {
                            if (callback != null) callback.onFailure(null, e);
                        });
                    }
                });
            } else {
                if (callback != null) callback.onFailure(null, new Throwable("Base de datos local no disponible."));
            }
            return;
        }

        performCall(getApiService().enrollStudent(enrollment), callback);
    }

    public void updateEnrollment(Enrollment enrollment, Callback<Enrollment> callback) {
        if (isLocalMode()) {
            if (db != null) {
                executor.execute(() -> {
                    try {
                        db.enrollmentDao().insert(enrollment);
                        mainHandler.post(() -> {
                            if (callback != null) callback.onResponse(null, Response.success(enrollment));
                        });
                    } catch (Exception e) {
                        mainHandler.post(() -> {
                            if (callback != null) callback.onFailure(null, e);
                        });
                    }
                });
            } else {
                if (callback != null) callback.onFailure(null, new Throwable("Base de datos local no disponible."));
            }
            return;
        }

        performCall(getApiService().updateEnrollment(enrollment.id, enrollment), callback);
    }

    public void deleteEnrollment(int enId, Callback<Void> callback) {
        if (isLocalMode()) {
            if (db != null) {
                executor.execute(() -> {
                    try {
                        db.enrollmentDao().deleteById(enId);
                        mainHandler.post(() -> {
                            if (callback != null) callback.onResponse(null, Response.success(null));
                        });
                    } catch (Exception e) {
                        mainHandler.post(() -> {
                            if (callback != null) callback.onFailure(null, e);
                        });
                    }
                });
            } else {
                if (callback != null) callback.onFailure(null, new Throwable("Base de datos local no disponible."));
            }
            return;
        }

        performCall(getApiService().deleteEnrollment(enId), callback);
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
        performCall(getApiService().getGradeInfoForStudent(studentId), new Callback<List<StudentGradeInfo>>() {
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
        performCall(getApiService().getUserById(id), callback);
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
        performCall(getApiService().getSubjectById(id), callback);
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
        performCall(getApiService().getFacultyById(id), callback);
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
        performCall(getApiService().getFacilityById(id), callback);
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
        performCall(getApiService().getEnrollmentById(id), callback);
    }

    public void getSubjectsByProfessor(int professorId, Callback<List<Subject>> callback) {
        if (isLocalMode()) {
            if (db != null) {
                executor.execute(() -> {
                    try {
                        List<Subject> cached = db.subjectDao().getSubjectsByProfessor(professorId);
                        mainHandler.post(() -> callback.onResponse(null, Response.success(deduplicateSubjects(cached))));
                    } catch (Exception e) {
                        mainHandler.post(() -> callback.onFailure(null, e));
                    }
                });
            } else {
                callback.onResponse(null, Response.success(new ArrayList<>()));
            }
            return;
        }

        performCall(getApiService().getSubjectsByProfessor(professorId), callback);
    }

    public void getSubjectEnrollments(int subjectId, Callback<List<StudentGradeInfo>> callback) {
        if (isLocalMode()) {
            if (db != null) {
                executor.execute(() -> {
                    try {
                        List<Enrollment> enrollments = db.enrollmentDao().getBySubjectId(subjectId);
                        List<StudentGradeInfo> infos = new ArrayList<>();
                        if (enrollments != null) {
                            for (Enrollment en : enrollments) {
                                Subject sub = db.subjectDao().getSubjectById(en.subjectId);
                                User stu = db.userDao().getUserById(en.studentId);
                                StudentGradeInfo info = new StudentGradeInfo();
                                info.enrollment = en;
                                info.subject = sub;
                                info.student = stu;
                                infos.add(info);
                            }
                        }
                        mainHandler.post(() -> callback.onResponse(null, Response.success(infos)));
                    } catch (Exception e) {
                        mainHandler.post(() -> callback.onFailure(null, e));
                    }
                });
            } else {
                callback.onResponse(null, Response.success(new ArrayList<>()));
            }
            return;
        }

        performCall(getApiService().getSubjectEnrollments(subjectId), callback);
    }

    public void getSubjectParticipants(int subjectId, Callback<List<User>> callback) {
        if (isLocalMode()) {
            if (db != null) {
                executor.execute(() -> {
                    try {
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
                        mainHandler.post(() -> callback.onResponse(null, Response.success(deduplicateUsers(participants))));
                    } catch (Exception e) {
                        mainHandler.post(() -> callback.onFailure(null, e));
                    }
                });
            } else {
                callback.onResponse(null, Response.success(new ArrayList<>()));
            }
            return;
        }

        performCall(getApiService().getSubjectParticipants(subjectId), new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onResponse(call, Response.success(deduplicateUsers(response.body())));
                } else {
                    callback.onResponse(call, response);
                }
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                callback.onFailure(call, t);
            }
        });
    }

    // --- Facilities ---
    public void getAllFacilities(Callback<List<Facility>> callback) {
        if (isLocalMode()) {
            if (db != null) {
                executor.execute(() -> {
                    try {
                        List<Facility> cached = db.facilityDao().getAllFacilities();
                        mainHandler.post(() -> callback.onResponse(null, Response.success(deduplicateFacilities(cached))));
                    } catch (Exception e) {
                        mainHandler.post(() -> callback.onFailure(null, e));
                    }
                });
            } else {
                callback.onResponse(null, Response.success(new ArrayList<>()));
            }
            return;
        }

        performCall(getApiService().getFacilities(), new Callback<List<Facility>>() {
            @Override
            public void onResponse(Call<List<Facility>> call, Response<List<Facility>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onResponse(call, Response.success(deduplicateFacilities(response.body())));
                } else {
                    callback.onResponse(call, response);
                }
            }

            @Override
            public void onFailure(Call<List<Facility>> call, Throwable t) {
                callback.onFailure(call, t);
            }
        });
    }

    public void insertFacility(Facility facility, Callback<Facility> callback) {
        if (isLocalMode()) {
            if (db != null) {
                executor.execute(() -> {
                    try {
                        if (facility.id == 0) facility.id = (int) (System.currentTimeMillis() & 0x7fffffff);
                        db.facilityDao().insert(facility);
                        mainHandler.post(() -> {
                            if (callback != null) callback.onResponse(null, Response.success(facility));
                        });
                    } catch (Exception e) {
                        mainHandler.post(() -> {
                            if (callback != null) callback.onFailure(null, e);
                        });
                    }
                });
            } else {
                if (callback != null) callback.onFailure(null, new Throwable("Base de datos local no disponible."));
            }
            return;
        }

        performCall(getApiService().createFacility(facility), callback);
    }

    public void updateFacility(Facility facility, Callback<Facility> callback) {
        if (isLocalMode()) {
            if (db != null) {
                executor.execute(() -> {
                    try {
                        db.facilityDao().insert(facility);
                        mainHandler.post(() -> {
                            if (callback != null) callback.onResponse(null, Response.success(facility));
                        });
                    } catch (Exception e) {
                        mainHandler.post(() -> {
                            if (callback != null) callback.onFailure(null, e);
                        });
                    }
                });
            } else {
                if (callback != null) callback.onFailure(null, new Throwable("Base de datos local no disponible."));
            }
            return;
        }

        performCall(getApiService().updateFacility(facility.id, facility), callback);
    }

    public void deleteFacility(int facId, Callback<Void> callback) {
        if (isLocalMode()) {
            if (db != null) {
                executor.execute(() -> {
                    try {
                        db.facilityDao().deleteById(facId);
                        mainHandler.post(() -> {
                            if (callback != null) callback.onResponse(null, Response.success(null));
                        });
                    } catch (Exception e) {
                        mainHandler.post(() -> {
                            if (callback != null) callback.onFailure(null, e);
                        });
                    }
                });
            } else {
                if (callback != null) callback.onFailure(null, new Throwable("Base de datos local no disponible."));
            }
            return;
        }

        performCall(getApiService().deleteFacility(facId), callback);
    }

    // --- Schedules ---
    public void getFacilitySchedules(int facId, Callback<List<ScheduleInfo>> callback) {
        if (isLocalMode()) {
            if (db != null) {
                executor.execute(() -> {
                    try {
                        List<FacilitySchedule> schedules = db.facilityScheduleDao().getByFacilityId(facId);
                        List<ScheduleInfo> infos = new ArrayList<>();
                        if (schedules != null) {
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
                        }
                        mainHandler.post(() -> callback.onResponse(null, Response.success(infos)));
                    } catch (Exception e) {
                        mainHandler.post(() -> callback.onFailure(null, e));
                    }
                });
            } else {
                callback.onResponse(null, Response.success(new ArrayList<>()));
            }
            return;
        }

        if (db != null) {
            executor.execute(() -> {
                try {
                    List<FacilitySchedule> schedules = db.facilityScheduleDao().getByFacilityId(facId);
                    List<ScheduleInfo> infos = new ArrayList<>();
                    if (schedules != null) {
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
                    }
                    mainHandler.post(() -> callback.onResponse(null, Response.success(infos)));
                } catch (Exception e) { e.printStackTrace(); }
            });
        }
        performCall(getApiService().getFacilitySchedules(facId), new Callback<List<ScheduleInfo>>() {
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
                        if (schedules != null) {
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
        if (isLocalMode()) {
            if (db != null) {
                executor.execute(() -> {
                    try {
                        if (schedule.id == 0) schedule.id = (int) (System.currentTimeMillis() & 0x7fffffff);
                        db.facilityScheduleDao().insert(schedule);
                        mainHandler.post(() -> {
                            if (callback != null) callback.onResponse(null, Response.success(schedule));
                        });
                    } catch (Exception e) {
                        mainHandler.post(() -> {
                            if (callback != null) callback.onFailure(null, e);
                        });
                    }
                });
            } else {
                if (callback != null) callback.onFailure(null, new Throwable("Base de datos local no disponible."));
            }
            return;
        }

        performCall(getApiService().createSchedule(schedule), callback);
    }

    public void updateSchedule(FacilitySchedule schedule, Callback<FacilitySchedule> callback) {
        if (isLocalMode()) {
            if (db != null) {
                executor.execute(() -> {
                    try {
                        db.facilityScheduleDao().insert(schedule);
                        mainHandler.post(() -> {
                            if (callback != null) callback.onResponse(null, Response.success(schedule));
                        });
                    } catch (Exception e) {
                        mainHandler.post(() -> {
                            if (callback != null) callback.onFailure(null, e);
                        });
                    }
                });
            } else {
                if (callback != null) callback.onFailure(null, new Throwable("Base de datos local no disponible."));
            }
            return;
        }

        performCall(getApiService().updateSchedule(schedule.id, schedule), callback);
    }

    public void deleteSchedule(int schId, Callback<Void> callback) {
        if (isLocalMode()) {
            if (db != null) {
                executor.execute(() -> {
                    try {
                        db.facilityScheduleDao().deleteById(schId);
                        mainHandler.post(() -> {
                            if (callback != null) callback.onResponse(null, Response.success(null));
                        });
                    } catch (Exception e) {
                        mainHandler.post(() -> {
                            if (callback != null) callback.onFailure(null, e);
                        });
                    }
                });
            } else {
                if (callback != null) callback.onFailure(null, new Throwable("Base de datos local no disponible."));
            }
            return;
        }

        performCall(getApiService().deleteSchedule(schId), callback);
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
        performCall(getApiService().updateUserSubjectColor(userId, subjectId, color), callback);
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
            performCall(getApiService().getUserSubjectColor(userId, subjectId), new Callback<Map<String, String>>() {
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
        if (isLocalMode()) {
            if (db != null) {
                executor.execute(() -> {
                    try {
                        List<BlogEntry> cached = db.blogDao().getEntriesBySubject(subjectId);
                        mainHandler.post(() -> callback.onResponse(null, Response.success(cached != null ? cached : new ArrayList<>())));
                    } catch (Exception e) {
                        mainHandler.post(() -> callback.onFailure(null, e));
                    }
                });
            } else {
                callback.onResponse(null, Response.success(new ArrayList<>()));
            }
            return;
        }

        performCall(getApiService().getSubjectBlog(subjectId), callback);
    }

    public void insertBlogEntry(BlogEntry entry, Callback<BlogEntry> callback) {
        if (isLocalMode()) {
            if (db != null) {
                executor.execute(() -> {
                    try {
                        if (entry.id == 0) entry.id = (int) (System.currentTimeMillis() & 0x7fffffff);
                        db.blogDao().insertEntry(entry);
                        mainHandler.post(() -> {
                            if (callback != null) callback.onResponse(null, Response.success(entry));
                        });
                    } catch (Exception e) {
                        mainHandler.post(() -> {
                            if (callback != null) callback.onFailure(null, e);
                        });
                    }
                });
            } else {
                if (callback != null) callback.onFailure(null, new Throwable("Base de datos local no disponible."));
            }
            return;
        }

        performCall(getApiService().createBlogEntry(entry), callback);
    }

    public void updateBlogEntry(BlogEntry entry, Callback<BlogEntry> callback) {
        if (isLocalMode()) {
            if (db != null) {
                executor.execute(() -> {
                    try {
                        db.blogDao().insertEntry(entry);
                        mainHandler.post(() -> {
                            if (callback != null) callback.onResponse(null, Response.success(entry));
                        });
                    } catch (Exception e) {
                        mainHandler.post(() -> {
                            if (callback != null) callback.onFailure(null, e);
                        });
                    }
                });
            } else {
                if (callback != null) callback.onFailure(null, new Throwable("Base de datos local no disponible."));
            }
            return;
        }

        performCall(getApiService().updateBlogEntry(entry.id, entry), callback);
    }

    public void reorderBlog(List<BlogEntry> entries, Callback<Void> callback) {
        if (isLocalMode()) {
            if (db != null) {
                executor.execute(() -> {
                    try {
                        for (int i = 0; i < entries.size(); i++) {
                            BlogEntry e = entries.get(i);
                            e.position = i;
                            db.blogDao().insertEntry(e);
                        }
                        mainHandler.post(() -> {
                            if (callback != null) callback.onResponse(null, Response.success(null));
                        });
                    } catch (Exception e) {
                        mainHandler.post(() -> {
                            if (callback != null) callback.onFailure(null, e);
                        });
                    }
                });
            } else {
                if (callback != null) callback.onFailure(null, new Throwable("Base de datos local no disponible."));
            }
            return;
        }

        performCall(getApiService().reorderBlog(entries), callback);
    }

    public void deleteBlogEntry(int entryId, Callback<Void> callback) {
        if (isLocalMode()) {
            if (db != null) {
                executor.execute(() -> {
                    try {
                        db.blogDao().deleteEntryById(entryId);
                        mainHandler.post(() -> {
                            if (callback != null) callback.onResponse(null, Response.success(null));
                        });
                    } catch (Exception e) {
                        mainHandler.post(() -> {
                            if (callback != null) callback.onFailure(null, e);
                        });
                    }
                });
            } else {
                if (callback != null) callback.onFailure(null, new Throwable("Base de datos local no disponible."));
            }
            return;
        }

        performCall(getApiService().deleteBlogEntry(entryId), callback);
    }

    // --- Blog Comments ---
    public void getBlogComments(int entryId, Callback<List<CommentInfo>> callback) {
        if (isLocalMode()) {
            if (db != null) {
                executor.execute(() -> {
                    try {
                        List<BlogComment> comments = db.blogDao().getCommentsByEntry(entryId);
                        List<CommentInfo> infos = new ArrayList<>();
                        if (comments != null) {
                            for (BlogComment c : comments) {
                                User u = db.userDao().getUserById(c.userId);
                                CommentInfo info = new CommentInfo();
                                info.comment = c;
                                info.userName = u != null ? u.name : "Usuario";
                                info.userRole = u != null ? u.role : "STUDENT";
                                info.userPhoto = u != null ? u.profile_image : null;
                                infos.add(info);
                            }
                        }
                        mainHandler.post(() -> callback.onResponse(null, Response.success(infos)));
                    } catch (Exception e) {
                        mainHandler.post(() -> callback.onFailure(null, e));
                    }
                });
            } else {
                callback.onResponse(null, Response.success(new ArrayList<>()));
            }
            return;
        }

        performCall(getApiService().getBlogComments(entryId), callback);
    }

    public void insertBlogComment(BlogComment comment, Callback<BlogComment> callback) {
        if (isLocalMode()) {
            if (db != null) {
                executor.execute(() -> {
                    try {
                        if (comment.id == 0) comment.id = (int) (System.currentTimeMillis() & 0x7fffffff);
                        db.blogDao().insertComment(comment);
                        mainHandler.post(() -> {
                            if (callback != null) callback.onResponse(null, Response.success(comment));
                        });
                    } catch (Exception e) {
                        mainHandler.post(() -> {
                            if (callback != null) callback.onFailure(null, e);
                        });
                    }
                });
            } else {
                if (callback != null) callback.onFailure(null, new Throwable("Base de datos local no disponible."));
            }
            return;
        }

        performCall(getApiService().createBlogComment(comment), callback);
    }

    public void deleteBlogComment(int commentId, Callback<Void> callback) {
        if (isLocalMode()) {
            if (db != null) {
                executor.execute(() -> {
                    try {
                        db.blogDao().deleteCommentById(commentId);
                        mainHandler.post(() -> {
                            if (callback != null) callback.onResponse(null, Response.success(null));
                        });
                    } catch (Exception e) {
                        mainHandler.post(() -> {
                            if (callback != null) callback.onFailure(null, e);
                        });
                    }
                });
            } else {
                if (callback != null) callback.onFailure(null, new Throwable("Base de datos local no disponible."));
            }
            return;
        }

        performCall(getApiService().deleteBlogComment(commentId), callback);
    }

    public void clearBlogDiscussion(int entryId, Callback<Void> callback) {
        if (isLocalMode()) {
            if (db != null) {
                executor.execute(() -> {
                    try {
                        db.blogDao().deleteCommentsByEntry(entryId);
                        mainHandler.post(() -> {
                            if (callback != null) callback.onResponse(null, Response.success(null));
                        });
                    } catch (Exception e) {
                        mainHandler.post(() -> {
                            if (callback != null) callback.onFailure(null, e);
                        });
                    }
                });
            } else {
                if (callback != null) callback.onFailure(null, new Throwable("Base de datos local no disponible."));
            }
            return;
        }

        performCall(getApiService().clearBlogDiscussion(entryId), callback);
    }

    // --- Notifications ---
    public void getNotifications(Callback<List<Notification>> callback) {
        if (isLocalMode()) {
            if (db != null) {
                executor.execute(() -> {
                    try {
                        List<Notification> cached = db.notificationDao().getAllNotifications();
                        mainHandler.post(() -> callback.onResponse(null, Response.success(cached != null ? cached : new ArrayList<>())));
                    } catch (Exception e) {
                        mainHandler.post(() -> callback.onFailure(null, e));
                    }
                });
            } else {
                callback.onResponse(null, Response.success(new ArrayList<>()));
            }
            return;
        }

        performCall(getApiService().getNotifications(), callback);
    }

    public void insertNotification(Notification notification, Callback<Void> callback) {
        if (isLocalMode()) {
            if (db != null) {
                executor.execute(() -> {
                    try {
                        if (notification.id == 0) notification.id = (int) (System.currentTimeMillis() & 0x7fffffff);
                        db.notificationDao().insertNotification(notification);
                        mainHandler.post(() -> {
                            if (callback != null) callback.onResponse(null, Response.success(null));
                        });
                    } catch (Exception e) {
                        mainHandler.post(() -> {
                            if (callback != null) callback.onFailure(null, e);
                        });
                    }
                });
            } else {
                if (callback != null) callback.onFailure(null, new Throwable("Base de datos local no disponible."));
            }
            return;
        }

        performCall(getApiService().createNotification(notification), new Callback<Notification>() {
            @Override
            public void onResponse(Call<Notification> call, Response<Notification> response) {
                if (response.isSuccessful()) {
                    if (callback != null) callback.onResponse(null, Response.success(null));
                } else {
                    String errMessage = "Error al procesar la notificación.";
                    try {
                        if (response.errorBody() != null) {
                            String errJson = response.errorBody().string();
                            JSONObject obj = new JSONObject(errJson);
                            if (obj.has("detail")) {
                                errMessage = obj.getString("detail");
                            }
                        }
                    } catch (Exception ignored) {}

                    if (callback != null) callback.onFailure(null, new Exception(errMessage));
                }
            }

            @Override
            public void onFailure(Call<Notification> call, Throwable t) {
                if (callback != null) callback.onFailure(null, t);
            }
        });
    }

    public void updateNotification(Notification notification, Callback<Void> callback) {
        if (isLocalMode()) {
            if (db != null) {
                executor.execute(() -> {
                    try {
                        db.notificationDao().updateNotification(notification);
                        mainHandler.post(() -> {
                            if (callback != null) callback.onResponse(null, Response.success(null));
                        });
                    } catch (Exception e) {
                        mainHandler.post(() -> {
                            if (callback != null) callback.onFailure(null, e);
                        });
                    }
                });
            } else {
                if (callback != null) callback.onFailure(null, new Throwable("Base de datos local no disponible."));
            }
            return;
        }

        performCall(getApiService().updateNotification(notification.id, notification), new Callback<Notification>() {
            @Override
            public void onResponse(Call<Notification> call, Response<Notification> response) {
                if (callback != null) callback.onResponse(null, Response.success(null));
            }

            @Override
            public void onFailure(Call<Notification> call, Throwable t) {
                if (callback != null) callback.onFailure(null, t);
            }
        });
    }

    public void deleteNotification(int id, Callback<Void> callback) {
        if (isLocalMode()) {
            if (db != null) {
                executor.execute(() -> {
                    try {
                        db.notificationDao().deleteNotificationById(id);
                        mainHandler.post(() -> {
                            if (callback != null) callback.onResponse(null, Response.success(null));
                        });
                    } catch (Exception e) {
                        mainHandler.post(() -> {
                            if (callback != null) callback.onFailure(null, e);
                        });
                    }
                });
            } else {
                if (callback != null) callback.onFailure(null, new Throwable("Base de datos local no disponible."));
            }
            return;
        }

        performCall(getApiService().deleteNotification(id), new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (callback != null) callback.onResponse(null, Response.success(null));
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                if (callback != null) callback.onFailure(null, t);
            }
        });
    }

    // --- Synchronize all data from SQL Server for Offline use ---
    public void downloadAllDataForOffline(SyncCallback callback) {
        if (db == null) {
            callback.onError("Base de datos local no disponible");
            return;
        }

        performCall(getApiService().getStudents(), new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> respStudents) {
                if (respStudents.isSuccessful() && respStudents.body() != null) {
                    executor.execute(() -> db.userDao().insertAll(respStudents.body()));
                }
                performCall(getApiService().getAdmins(), new Callback<List<User>>() {
                    @Override
                    public void onResponse(Call<List<User>> call, Response<List<User>> respAdmins) {
                        if (respAdmins.isSuccessful() && respAdmins.body() != null) {
                            executor.execute(() -> db.userDao().insertAll(respAdmins.body()));
                        }
                        performCall(getApiService().getProfessors(), new Callback<List<User>>() {
                            @Override
                            public void onResponse(Call<List<User>> call, Response<List<User>> respProfs) {
                                if (respProfs.isSuccessful() && respProfs.body() != null) {
                                    executor.execute(() -> db.userDao().insertAll(respProfs.body()));
                                }
                                performCall(getApiService().getFaculties(), new Callback<List<Faculty>>() {
                                    @Override
                                    public void onResponse(Call<List<Faculty>> call, Response<List<Faculty>> respFacs) {
                                        if (respFacs.isSuccessful() && respFacs.body() != null) {
                                            executor.execute(() -> db.facultyDao().insertAll(respFacs.body()));
                                        }
                                        performCall(getApiService().getSubjects(), new Callback<List<Subject>>() {
                                            @Override
                                            public void onResponse(Call<List<Subject>> call, Response<List<Subject>> respSubs) {
                                                if (respSubs.isSuccessful() && respSubs.body() != null) {
                                                    List<Subject> subs = respSubs.body();
                                                    executor.execute(() -> db.subjectDao().insertAll(subs));

                                                    for (Subject s : subs) {
                                                        performCall(getApiService().getSubjectBlog(s.id), new Callback<List<BlogEntry>>() {
                                                            @Override
                                                            public void onResponse(Call<List<BlogEntry>> call, Response<List<BlogEntry>> respBlog) {
                                                                if (respBlog.isSuccessful() && respBlog.body() != null) {
                                                                    List<BlogEntry> entries = respBlog.body();
                                                                    executor.execute(() -> db.blogDao().insertEntries(entries));

                                                                    for (BlogEntry entry : entries) {
                                                                        performCall(getApiService().getBlogComments(entry.id), new Callback<List<CommentInfo>>() {
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

                                                        performCall(getApiService().getSubjectParticipants(s.id), new Callback<List<User>>() {
                                                            @Override
                                                            public void onResponse(Call<List<User>> call, Response<List<User>> respPart) {
                                                                if (respPart.isSuccessful() && respPart.body() != null) {
                                                                    executor.execute(() -> db.userDao().insertAll(respPart.body()));
                                                                }
                                                            }
                                                            @Override public void onFailure(Call<List<User>> call, Throwable t) {}
                                                        });

                                                        performCall(getApiService().getSubjectEnrollments(s.id), new Callback<List<StudentGradeInfo>>() {
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
                                                performCall(getApiService().getFacilities(), new Callback<List<Facility>>() {
                                                    @Override
                                                    public void onResponse(Call<List<Facility>> call, Response<List<Facility>> respFacilities) {
                                                        if (respFacilities.isSuccessful() && respFacilities.body() != null) {
                                                            List<Facility> facs = respFacilities.body();
                                                            executor.execute(() -> db.facilityDao().insertAll(facs));

                                                            for (Facility f : facs) {
                                                                performCall(getApiService().getFacilitySchedules(f.id), new Callback<List<ScheduleInfo>>() {
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

    public void checkServerHealth(Callback<Void> callback) {
        if (isLocalMode()) {
            consecutiveFailures = 0;
            if (connectionStatusListener != null) connectionStatusListener.onStatusChanged(true);
            if (callback != null) callback.onResponse(null, Response.success(null));
            return;
        }
        performCall(getApiService().healthCheck(), new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (callback != null) callback.onResponse(null, Response.success(null));
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                if (callback != null) callback.onFailure(null, t);
            }
        });
    }

    public void clearAllOfflineData(SyncCallback callback) {
        if (db == null) {
            if (callback != null) callback.onError("Base de datos local no disponible");
            return;
        }
        executor.execute(() -> {
            try {
                db.clearAllTables();

                // Re-seed Master Admin & Mandatory Faculties
                db.userDao().insert(new User("ADMIN12345", "Admin Maestro", "ASD###", "ADMIN", "Administrativa"));
                
                Faculty f1 = new Faculty("Docencia", "Facultad obligatoria asignada automáticamente a todos los Profesores.");
                f1.id = 998;
                db.facultyDao().insert(f1);

                Faculty f2 = new Faculty("Administrativa", "Facultad obligatoria asignada automáticamente a todos los Administradores.");
                f2.id = 999;
                db.facultyDao().insert(f2);

                mainHandler.post(() -> {
                    if (callback != null) callback.onSuccess("Se han eliminado todos los datos del servidor almacenados en el dispositivo.");
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (callback != null) callback.onError("Error al eliminar los datos locales: " + e.getMessage());
                });
            }
        });
    }

    public void uploadAllOfflineDataToOnline(SyncCallback callback) {
        if (db == null) {
            callback.onError("Base de datos local no disponible.");
            return;
        }

        executor.execute(() -> {
            try {
                List<Faculty> faculties = db.facultyDao().getAllFaculties();
                List<Subject> subjects = db.subjectDao().getAllSubjects();
                List<Facility> facilitiesList = db.facilityDao().getAllFacilities();
                List<User> users = db.userDao().getAllUsers();
                List<BlogEntry> blogEntries = db.blogDao().getAllEntries();
                List<Notification> notifications = db.notificationDao().getAllNotifications();

                int totalToUpload = 0;
                if (faculties != null) {
                    for (Faculty f : faculties) {
                        if (f.id != 998 && f.id != 999) totalToUpload++;
                    }
                }
                if (subjects != null) totalToUpload += subjects.size();
                if (facilitiesList != null) totalToUpload += facilitiesList.size();
                if (users != null) {
                    for (User u : users) {
                        if (!"ADMIN12345".equalsIgnoreCase(u.carnet)) totalToUpload++;
                    }
                }
                if (blogEntries != null) totalToUpload += blogEntries.size();
                if (notifications != null) totalToUpload += notifications.size();

                if (totalToUpload == 0) {
                    mainHandler.post(() -> callback.onSuccess("No existen registros locales nuevos para subir al servidor."));
                    return;
                }

                int[] uploadedCount = {0};

                // 1. Upload Faculties
                if (faculties != null) {
                    for (Faculty f : faculties) {
                        if (f.id != 998 && f.id != 999) {
                            Faculty newF = new Faculty(f.name, f.description);
                            newF.id = 0;
                            try {
                                Response<Faculty> resp = getApiService().createFaculty(newF).execute();
                                if (resp.isSuccessful()) uploadedCount[0]++;
                            } catch (Exception ignored) {}
                        }
                    }
                }

                // 2. Upload Subjects
                if (subjects != null) {
                    for (Subject s : subjects) {
                        Subject newS = new Subject(s.name, s.description, s.faculty, s.professorId);
                        newS.section = s.section;
                        newS.color = s.color;
                        newS.id = 0;
                        try {
                            Response<Subject> resp = getApiService().createSubject(newS).execute();
                            if (resp.isSuccessful()) uploadedCount[0]++;
                        } catch (Exception ignored) {}
                    }
                }

                // 3. Upload Facilities
                if (facilitiesList != null) {
                    for (Facility fac : facilitiesList) {
                        Facility newFac = new Facility(fac.name, fac.type, fac.description);
                        newFac.id = 0;
                        try {
                            Response<Facility> resp = getApiService().createFacility(newFac).execute();
                            if (resp.isSuccessful()) uploadedCount[0]++;
                        } catch (Exception ignored) {}
                    }
                }

                // 4. Upload Users
                if (users != null) {
                    for (User u : users) {
                        if (!"ADMIN12345".equalsIgnoreCase(u.carnet)) {
                            try {
                                Response<User> resp = getApiService().createUser(u).execute();
                                if (resp.isSuccessful()) uploadedCount[0]++;
                            } catch (Exception ignored) {}
                        }
                    }
                }

                // 5. Upload Blog Entries
                if (blogEntries != null) {
                    for (BlogEntry e : blogEntries) {
                        BlogEntry newE = new BlogEntry(e.subjectId, e.category, e.title, e.content);
                        newE.id = 0;
                        try {
                            Response<BlogEntry> resp = getApiService().createBlogEntry(newE).execute();
                            if (resp.isSuccessful()) uploadedCount[0]++;
                        } catch (Exception ignored) {}
                    }
                }

                // 6. Upload Notifications
                if (notifications != null) {
                    for (Notification n : notifications) {
                        Notification newN = new Notification(n.title, n.message, n.targetType, n.targetValue, n.senderName, n.timestamp);
                        newN.id = 0;
                        try {
                            Response<Notification> resp = getApiService().createNotification(newN).execute();
                            if (resp.isSuccessful()) uploadedCount[0]++;
                        } catch (Exception ignored) {}
                    }
                }

                final int count = uploadedCount[0];
                mainHandler.post(() -> callback.onSuccess("¡Proceso finalizado! Se han subido " + count + " registros locales al servidor como nuevos registros."));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Error al subir registros al servidor: " + e.getMessage()));
            }
        });
    }
}
