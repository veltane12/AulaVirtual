package com.aula.virtual.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.aula.virtual.R;
import com.aula.virtual.data.StudentGradeInfo;
import com.aula.virtual.data.entity.Enrollment;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GradeAdapter extends RecyclerView.Adapter<GradeAdapter.ViewHolder> {
    private List<StudentGradeInfo> gradeInfos = new ArrayList<>();
    private final boolean isAdmin;
    private boolean isStudentList = false;
    private OnGradeActionListener listener;

    public interface OnGradeActionListener {
        void onEditGrades(StudentGradeInfo info);
        void onViewBlog(StudentGradeInfo info);
        default void onDeleteEnrollment(StudentGradeInfo info) {}
        default void onStudentClick(StudentGradeInfo info) {}
    }

    private Map<Integer, String> facilityMap = new HashMap<>();

    public GradeAdapter(boolean isAdmin) {
        this(isAdmin, false);
    }

    public GradeAdapter(boolean isAdmin, boolean isStudentList) {
        this.isAdmin = isAdmin;
        this.isStudentList = isStudentList;
    }

    public void setOnGradeActionListener(OnGradeActionListener listener) {
        this.listener = listener;
    }

    public void setGradeInfos(List<StudentGradeInfo> gradeInfos) {
        this.gradeInfos = gradeInfos;
        notifyDataSetChanged();
    }

    public void setFacilityMap(Map<Integer, String> map) {
        if (map != null) {
            this.facilityMap = map;
            notifyDataSetChanged();
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_grade, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StudentGradeInfo info = gradeInfos.get(position);
        if (info == null) return;
        
        if (isStudentList) {
            // Professor view in ProfessorStudentListFragment: Each row is a Student
            holder.tvName.setText(info.student != null ? info.student.name : "Alumno");
            if (holder.tvSection != null) {
                holder.tvSection.setVisibility(View.GONE);
            }
            holder.btnBlog.setVisibility(View.GONE);
            holder.btnProfile.setVisibility(View.VISIBLE);
            holder.btnProfile.setOnClickListener(v -> {
                if (listener != null) listener.onStudentClick(info);
            });
            holder.btnEdit.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
            holder.btnEdit.setOnClickListener(v -> {
                if (listener != null) listener.onEditGrades(info);
            });
            holder.btnDelete.setVisibility(View.GONE);
        } else {
            // Subject view in AdminEnrollmentListFragment & Student views: Each row is a Subject
            holder.tvName.setText(info.subject != null ? info.subject.name : "Materia Desconocida");
            if (holder.tvSection != null) {
                if (info.subject != null) {
                    String sec = (info.subject.section != null && !info.subject.section.isEmpty()) ? info.subject.section : "01";
                    holder.tvSection.setText("Sección " + sec);
                    holder.tvSection.setVisibility(View.VISIBLE);
                } else {
                    holder.tvSection.setVisibility(View.GONE);
                }
            }
            holder.btnProfile.setVisibility(View.GONE); // No Profile button for subject cards
            holder.btnBlog.setText("Gestionar");
            holder.btnBlog.setVisibility(View.VISIBLE);
            holder.btnBlog.setOnClickListener(v -> {
                if (listener != null) {
                    if (isAdmin) listener.onViewBlog(info);
                    else listener.onStudentClick(info);
                }
            });
            holder.btnEdit.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
            holder.btnEdit.setOnClickListener(v -> {
                if (listener != null) listener.onEditGrades(info);
            });
            holder.btnDelete.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
            holder.btnDelete.setOnClickListener(v -> {
                if (listener != null) listener.onDeleteEnrollment(info);
            });
        }

        if (holder.tvFacility != null && info.subject != null) {
            String facName = facilityMap.get(info.subject.id);
            if (facName != null && !facName.isEmpty()) {
                holder.tvFacility.setText("🏢 Instalación: " + facName);
                holder.tvFacility.setVisibility(View.VISIBLE);
            } else {
                holder.tvFacility.setVisibility(View.GONE);
            }
        } else if (holder.tvFacility != null) {
            holder.tvFacility.setVisibility(View.GONE);
        }
        
        Enrollment e = info.enrollment;
        if (e != null) {
            holder.tvP1.setText(formatGrade(e.grade1));
            setGradeColor(holder.tvP1, e.grade1);
            
            holder.tvP2.setText(formatGrade(e.grade2));
            setGradeColor(holder.tvP2, e.grade2);
            
            holder.tvP3.setText(formatGrade(e.grade3));
            setGradeColor(holder.tvP3, e.grade3);
            
            holder.tvP4.setText(formatGrade(e.grade4));
            setGradeColor(holder.tvP4, e.grade4);
            
            holder.tvP5.setText(formatGrade(e.grade5));
            setGradeColor(holder.tvP5, e.grade5);
            
            double sum = 0;
            int count = 0;
            if (e.grade1 != null) { sum += e.grade1; count++; }
            if (e.grade2 != null) { sum += e.grade2; count++; }
            if (e.grade3 != null) { sum += e.grade3; count++; }
            if (e.grade4 != null) { sum += e.grade4; count++; }
            if (e.grade5 != null) { sum += e.grade5; count++; }
            
            if (count > 0) {
                double avg = sum / count;
                holder.tvAverage.setText(String.format(Locale.getDefault(), "%.1f", avg));
                setGradeColor(holder.tvAverage, avg);
            } else {
                holder.tvAverage.setText("N/A");
                holder.tvAverage.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.bs_secondary));
            }
        } else {
            holder.tvP1.setText("-");
            holder.tvP2.setText("-");
            holder.tvP3.setText("-");
            holder.tvP4.setText("-");
            holder.tvP5.setText("-");
            holder.tvAverage.setText("N/A");
            int secondary = ContextCompat.getColor(holder.itemView.getContext(), R.color.bs_secondary);
            holder.tvP1.setTextColor(secondary);
            holder.tvP2.setTextColor(secondary);
            holder.tvP3.setTextColor(secondary);
            holder.tvP4.setTextColor(secondary);
            holder.tvP5.setTextColor(secondary);
            holder.tvAverage.setTextColor(secondary);
        }




    }

    private void setGradeColor(TextView tv, Double grade) {
        if (grade == null) {
            tv.setTextColor(ContextCompat.getColor(tv.getContext(), R.color.bs_secondary));
            return;
        }
        int colorRes;
        if (grade <= 3.0) {
            colorRes = R.color.bs_danger;
        } else if (grade <= 6.0) {
            colorRes = R.color.bs_warning;
        } else {
            colorRes = R.color.bs_success;
        }
        tv.setTextColor(ContextCompat.getColor(tv.getContext(), colorRes));
    }

    private String formatGrade(Double grade) {
        if (grade == null) return "-";
        return String.format(Locale.getDefault(), "%.1f", grade);
    }

    @Override
    public int getItemCount() {
        return gradeInfos.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvSection, tvFacility, tvP1, tvP2, tvP3, tvP4, tvP5, tvAverage;
        Button btnBlog, btnEdit, btnDelete, btnProfile;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvSubjectName);
            tvSection = itemView.findViewById(R.id.tvSection);
            tvFacility = itemView.findViewById(R.id.tvFacility);
            tvP1 = itemView.findViewById(R.id.tvP1);
            tvP2 = itemView.findViewById(R.id.tvP2);
            tvP3 = itemView.findViewById(R.id.tvP3);
            tvP4 = itemView.findViewById(R.id.tvP4);
            tvP5 = itemView.findViewById(R.id.tvP5);
            tvAverage = itemView.findViewById(R.id.tvAverage);
            btnBlog = itemView.findViewById(R.id.btnGoToBlog);
            btnEdit = itemView.findViewById(R.id.btnEditGrades);
            btnDelete = itemView.findViewById(R.id.btnDeleteEnrollment);
            btnProfile = itemView.findViewById(R.id.btnProfile);
        }
    }
}
