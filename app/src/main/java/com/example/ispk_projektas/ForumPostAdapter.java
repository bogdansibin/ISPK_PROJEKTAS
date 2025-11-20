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

public class ForumPostAdapter extends RecyclerView.Adapter<ForumPostAdapter.ForumViewHolder> {

    private final List<ForumPost> items;
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    public ForumPostAdapter(List<ForumPost> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ForumViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_forum_post, parent, false);
        return new ForumViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ForumViewHolder holder, int position) {
        ForumPost post = items.get(position);

        holder.textTitle.setText(
                post.getPavadinimas() != null ? post.getPavadinimas() : "(be pavadinimo)"
        );

        String meta = "";
        if (post.getKategorija() != null && !post.getKategorija().isEmpty()) {
            meta += post.getKategorija();
        }
        if (post.getAutoriusVardas() != null && !post.getAutoriusVardas().isEmpty()) {
            if (!meta.isEmpty()) meta += " • ";
            meta += post.getAutoriusVardas();
        }
        if (post.getSukurta() != null) {
            if (!meta.isEmpty()) meta += " • ";
            meta += dateFormat.format(post.getSukurta().toDate());
        }
        holder.textMeta.setText(meta);

        holder.textContent.setText(
                post.getTurinys() != null ? post.getTurinys() : ""
        );
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void replaceData(List<ForumPost> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    static class ForumViewHolder extends RecyclerView.ViewHolder {
        TextView textTitle, textMeta, textContent;

        ForumViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.textTitle);
            textMeta = itemView.findViewById(R.id.textMeta);
            textContent = itemView.findViewById(R.id.textContent);
        }
    }
}
