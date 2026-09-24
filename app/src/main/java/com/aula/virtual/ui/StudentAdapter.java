package com.aula.virtual.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.aula.virtual.R;
import com.aula.virtual.data.entity.User;
import java.util.ArrayList;
import java.util.List;

public class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.ViewHolder> {
    private List<User> students = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(User user);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setStudents(List<User> students) {
        this.students = students;
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
        User student = students.get(position);
        if (student == null) return;
        
        holder.tvName.setText(student.name != null ? student.name : "Sin nombre");
        holder.tvCarnet.setText(student.carnet != null ? student.carnet : "-");
        holder.tvFaculty.setText(student.faculty != null ? student.faculty : "Sin facultad");
        
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
        Button btnManage;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvCarnet = itemView.findViewById(R.id.tvCarnet);
            tvFaculty = itemView.findViewById(R.id.tvFaculty);
            btnManage = itemView.findViewById(R.id.btnManage);
        }
    }
}
