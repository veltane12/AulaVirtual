package com.aula.virtual.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.aula.virtual.R;
import com.aula.virtual.data.entity.Subject;
import java.util.ArrayList;
import java.util.List;

public class SubjectAdapter extends RecyclerView.Adapter<SubjectAdapter.ViewHolder> {
    private List<Subject> subjects = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Subject subject);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setSubjects(List<Subject> subjects) {
        this.subjects = subjects;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_subject, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Subject subject = subjects.get(position);
        if (subject == null) return;
        
        holder.tvName.setText(subject.name != null ? subject.name : "Sin nombre");
        holder.tvFaculty.setText(subject.faculty != null ? subject.faculty : "Sin Facultad");
        holder.tvSection.setText(subject.section != null ? "Sección: " + subject.section : "Sección: 01");
        holder.tvDescription.setText(subject.description != null ? subject.description : "");
        
        holder.btnManage.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(subject);
        });
    }

    @Override
    public int getItemCount() {
        return subjects.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvFaculty, tvDescription, tvSection;
        Button btnManage;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvSubjectName);
            tvFaculty = itemView.findViewById(R.id.tvFaculty);
            tvSection = itemView.findViewById(R.id.tvSection);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            btnManage = itemView.findViewById(R.id.btnManage);
        }
    }
}
