package com.aula.virtual.ui;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.aula.virtual.R;
import com.aula.virtual.data.entity.BlogEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BlogAdapter extends RecyclerView.Adapter<BlogAdapter.ViewHolder> {
    private List<BlogEntry> entries = new ArrayList<>();
    private final boolean isAdmin;
    private final OnBlogActionListener listener;
    private OnStartDragListener dragListener;
    private final Set<Integer> expandedItems = new HashSet<>();

    public interface OnBlogActionListener {
        void onEdit(BlogEntry entry);
        void onDelete(BlogEntry entry);
        void onOrderChanged(List<BlogEntry> entries);
        void onDiscussionClick(BlogEntry entry);
    }

    public interface OnStartDragListener {
        void onStartDrag(RecyclerView.ViewHolder viewHolder);
    }

    public BlogAdapter(boolean isAdmin, OnBlogActionListener listener) {
        this.isAdmin = isAdmin;
        this.listener = listener;
    }

    public void setOnStartDragListener(OnStartDragListener dragListener) {
        this.dragListener = dragListener;
    }

    public void setEntries(List<BlogEntry> newEntries) {
        if (newEntries == null) return;
        if (isSameEntryList(this.entries, newEntries)) {
            return;
        }
        this.entries = new ArrayList<>(newEntries);
        notifyDataSetChanged();
    }

    private boolean isSameEntryList(List<BlogEntry> oldList, List<BlogEntry> newList) {
        if (oldList == newList) return true;
        if (oldList == null || newList == null) return false;
        if (oldList.size() != newList.size()) return false;
        for (int i = 0; i < oldList.size(); i++) {
            BlogEntry o = oldList.get(i);
            BlogEntry n = newList.get(i);
            if (o == null || n == null) return false;
            if (o.id != n.id) return false;
            if (!equalsNullSafe(o.title, n.title)) return false;
            if (!equalsNullSafe(o.content, n.content)) return false;
            if (!equalsNullSafe(o.category, n.category)) return false;
            if (o.position != n.position) return false;
        }
        return true;
    }

    private boolean equalsNullSafe(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    public List<BlogEntry> getEntries() {
        return entries;
    }

    public boolean onItemMove(int fromPosition, int toPosition) {
        Collections.swap(entries, fromPosition, toPosition);
        notifyItemMoved(fromPosition, toPosition);
        return true;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_blog_entry, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BlogEntry entry = entries.get(position);
        holder.tvCategory.setText(entry.category);
        holder.tvTitle.setText(entry.title);
        holder.tvContent.setText(entry.content);

        // Expand/Collapse logic
        boolean isExpanded = expandedItems.contains(entry.id);
        
        if (isExpanded) {
            holder.tvContent.setMaxLines(Integer.MAX_VALUE);
            holder.btnReadMore.setText("Ver menos");
        } else {
            holder.tvContent.setMaxLines(3);
            holder.btnReadMore.setText("Ver más");
        }

        // Medir de manera exacta si el texto es lo suficientemente largo
        holder.tvContent.post(() -> {
            int lineCount = holder.tvContent.getLineCount();
            boolean hasHiddenText = lineCount > 3 || (entry.content != null && entry.content.contains("\n"));
            holder.btnReadMore.setVisibility((isExpanded || hasHiddenText) ? View.VISIBLE : View.GONE);
        });

        holder.btnReadMore.setOnClickListener(v -> {
            int currentPos = holder.getAdapterPosition();
            if (currentPos != RecyclerView.NO_POSITION) {
                if (isExpanded) expandedItems.remove(entry.id);
                else expandedItems.add(entry.id);
                notifyItemChanged(currentPos);
            }
        });

        holder.btnDiscussion.setOnClickListener(v -> {
            if (listener != null) listener.onDiscussionClick(entry);
        });

        // Dynamic category colors
        if ("Aviso".equals(entry.category)) {
            holder.tvCategory.setBackgroundResource(R.drawable.bg_badge_primary);
        } else if ("Parcial".equals(entry.category)) {
            holder.tvCategory.setBackgroundResource(R.drawable.bg_badge_danger);
        } else if ("Tarea".equals(entry.category)) {
            holder.tvCategory.setBackgroundResource(R.drawable.bg_badge_success);
        }

        if (isAdmin) {
            holder.layoutAdminActions.setVisibility(View.VISIBLE);
            holder.ivDragHandle.setVisibility(View.VISIBLE);
            holder.btnEdit.setOnClickListener(v -> listener.onEdit(entry));
            holder.btnDelete.setOnClickListener(v -> listener.onDelete(entry));

            holder.ivDragHandle.setOnTouchListener((v, event) -> {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN && dragListener != null) {
                    dragListener.onStartDrag(holder);
                }
                return false;
            });
        } else {
            holder.layoutAdminActions.setVisibility(View.GONE);
            holder.ivDragHandle.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategory, tvTitle, tvContent, btnReadMore;
        View layoutAdminActions;
        Button btnEdit, btnDelete;
        ImageView ivDragHandle;
        Button btnDiscussion;

        ViewHolder(View itemView) {
            super(itemView);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvContent = itemView.findViewById(R.id.tvContent);
            btnReadMore = itemView.findViewById(R.id.btnReadMore);
            layoutAdminActions = itemView.findViewById(R.id.layoutAdminActions);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            ivDragHandle = itemView.findViewById(R.id.ivDragHandle);
            btnDiscussion = itemView.findViewById(R.id.btnDiscussion);
        }
    }
}
