package com.aula.virtual.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.aula.virtual.R;
import com.aula.virtual.data.entity.Faculty;
import java.util.ArrayList;
import java.util.List;

public class FacultyAdapter extends RecyclerView.Adapter<FacultyAdapter.ViewHolder> {
    private List<Faculty> faculties = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Faculty faculty);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setFaculties(List<Faculty> faculties) {
        this.faculties = faculties;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_faculty, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Faculty faculty = faculties.get(position);
        if (faculty == null) return;
        
        holder.tvName.setText(faculty.name);
        holder.tvDesc.setText(faculty.description);
        
        holder.btnManage.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(faculty);
        });
    }

    @Override
    public int getItemCount() {
        return faculties.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDesc;
        Button btnManage;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvDesc = itemView.findViewById(R.id.tvDescription);
            btnManage = itemView.findViewById(R.id.btnManage);
        }
    }
}
