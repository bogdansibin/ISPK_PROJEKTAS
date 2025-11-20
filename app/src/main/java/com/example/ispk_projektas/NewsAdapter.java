package com.example.ispk_projektas;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.NewsViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(NewsItem item);
    }

    private final List<NewsItem> items = new ArrayList<>();
    private final OnItemClickListener listener;

    private final SimpleDateFormat displayFormat =
            new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    public NewsAdapter(List<NewsItem> initial, OnItemClickListener listener) {
        if (initial != null) items.addAll(initial);
        this.listener = listener;
    }

    public void updateData(List<NewsItem> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NewsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_news, parent, false);
        return new NewsViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull NewsViewHolder holder, int position) {
        NewsItem item = items.get(position);
        holder.bind(item, listener, displayFormat);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class NewsViewHolder extends RecyclerView.ViewHolder {

        TextView titleTextView;
        TextView dateTextView;

        public NewsViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.newsTitleTextView);
            dateTextView = itemView.findViewById(R.id.newsDateTextView);
        }

        public void bind(NewsItem item,
                         OnItemClickListener listener,
                         SimpleDateFormat df) {

            titleTextView.setText(item.title != null ? item.title : "");

            if (item.pubDateMillis > 0) {
                dateTextView.setText(df.format(new Date(item.pubDateMillis)));
            } else {
                dateTextView.setText("");
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(item);
            });
        }
    }
}
