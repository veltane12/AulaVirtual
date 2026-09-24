package com.aula.virtual.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.aula.virtual.R;
import java.util.ArrayList;
import java.util.List;

public class SearchableAdapter extends RecyclerView.Adapter<SearchableAdapter.ViewHolder> {
    private List<SearchableItem> items = new ArrayList<>();
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(SearchableItem item);
    }

    public static class SearchableItem {
        public int id;
        public String text;
        public String subtext;
        public SearchableItem(int id, String text, String subtext) { 
            this.id = id; 
            this.text = text; 
            this.subtext = subtext;
        }
    }

    public SearchableAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<SearchableItem> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SearchableItem item = items.get(position);
        holder.tvText.setText(item.text);
        holder.tvSubtext.setText(item.subtext != null ? item.subtext : "");
        holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvText, tvSubtext;
        ViewHolder(View itemView) { 
            super(itemView);
            tvText = itemView.findViewById(android.R.id.text1);
            tvSubtext = itemView.findViewById(android.R.id.text2);
        }
    }
}
