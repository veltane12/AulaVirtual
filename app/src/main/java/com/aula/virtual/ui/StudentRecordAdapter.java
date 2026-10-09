package com.aula.virtual.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.aula.virtual.R;
import com.aula.virtual.data.entity.Student;
import java.util.ArrayList;
import java.util.List;

public class StudentRecordAdapter extends RecyclerView.Adapter<StudentRecordAdapter.ViewHolder> {
    private List<Student> students = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Student student);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setStudents(List<Student> students) {
        this.students = students != null ? students : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Student student = students.get(position);
        if (student == null) return;

        holder.tvName.setText(student.name != null ? student.name : "Sin nombre");
        holder.tvCarnet.setText(student.carnet != null ? "Carnet: " + student.carnet : "-");
        String detail = (student.grade != null ? student.grade : "") + 
                         (student.faculty != null ? " • " + student.faculty : "");
        holder.tvFaculty.setText(detail.isEmpty() ? "Sin grado/escuela" : detail);

        holder.btnManage.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(student);
        });
    }

    @Override
    public int getItemCount() {
        return students.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvCarnet;
        TextView tvFaculty;
        View btnManage;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvCarnet = itemView.findViewById(R.id.tvCarnet);
            tvFaculty = itemView.findViewById(R.id.tvFaculty);
            btnManage = itemView.findViewById(R.id.btnManage);
        }
    }
}
