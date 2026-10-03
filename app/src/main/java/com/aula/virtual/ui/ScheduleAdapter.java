package com.aula.virtual.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.aula.virtual.R;
import com.aula.virtual.data.ScheduleInfo;
import java.util.ArrayList;
import java.util.List;

public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ViewHolder> {
    private List<ScheduleInfo> schedules = new ArrayList<>();
    private final OnScheduleActionListener listener;

    public interface OnScheduleActionListener {
        void onDelete(ScheduleInfo info);
        void onEdit(ScheduleInfo info);
    }

    public ScheduleAdapter(OnScheduleActionListener listener) {
        this.listener = listener;
    }

    public void setSchedules(List<ScheduleInfo> schedules) {
        this.schedules = schedules;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_schedule, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ScheduleInfo info = schedules.get(position);
        if (info == null) return;
        
        holder.tvSubject.setText(info.subjectName);
        if (holder.tvSection != null) {
            String sec = (info.subjectSection != null && !info.subjectSection.isEmpty()) ? info.subjectSection : "01";
            holder.tvSection.setText("Sección " + sec);
            holder.tvSection.setVisibility(View.VISIBLE);
        }
        holder.tvProfessor.setText(info.professorName);
        holder.tvTime.setText(info.schedule.days + " | " + info.schedule.startTime + " - " + info.schedule.endTime);
        
        holder.btnEdit.setOnClickListener(v -> listener.onEdit(info));
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(info));
    }

    @Override
    public int getItemCount() {
        return schedules.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSubject, tvSection, tvProfessor, tvTime;
        ImageButton btnDelete, btnEdit;

        ViewHolder(View itemView) {
            super(itemView);
            tvSubject = itemView.findViewById(R.id.tvScheduleSubject);
            tvSection = itemView.findViewById(R.id.tvScheduleSection);
            tvProfessor = itemView.findViewById(R.id.tvScheduleProfessor);
            tvTime = itemView.findViewById(R.id.tvScheduleTime);
            btnDelete = itemView.findViewById(R.id.btnDeleteSchedule);
            btnEdit = itemView.findViewById(R.id.btnEditSchedule);
        }
    }
}
