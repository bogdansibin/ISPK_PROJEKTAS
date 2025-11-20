package com.example.ispk_projektas;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.CommentViewHolder> {

    private final List<PostComment> items;
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    public CommentsAdapter(List<PostComment> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_2, parent, false);
        return new CommentViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        PostComment c = items.get(position);
        String line1 = c.getAutoriusVardas() != null ? c.getAutoriusVardas() : "Anonimas";
        if (c.getSukurta() != null) {
            line1 += " • " + dateFormat.format(c.getSukurta().toDate());
        }
        holder.text1.setText(line1);
        holder.text2.setText(c.getTekstas());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void replaceData(List<PostComment> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView text1, text2;
        CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            text1 = itemView.findViewById(android.R.id.text1);
            text2 = itemView.findViewById(android.R.id.text2);
        }
    }
}
