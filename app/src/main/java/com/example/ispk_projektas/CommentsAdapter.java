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

    public interface OnCommentLongClickListener {
        void onCommentLongClick(PostComment comment);
    }

    public interface OnReplyClickListener {
        void onReplyClick(PostComment comment);
    }

    private final List<PostComment> items;
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    private boolean isAdmin;
    private OnCommentLongClickListener longClickListener;
    private OnReplyClickListener replyClickListener;

    public CommentsAdapter(List<PostComment> items,
                           boolean isAdmin,
                           OnCommentLongClickListener longClickListener,
                           OnReplyClickListener replyClickListener) {
        this.items = items;
        this.isAdmin = isAdmin;
        this.longClickListener = longClickListener;
        this.replyClickListener = replyClickListener;
    }

    public void setAdmin(boolean isAdmin) {
        this.isAdmin = isAdmin;
        notifyDataSetChanged();
    }

    public void setLongClickListener(OnCommentLongClickListener listener) {
        this.longClickListener = listener;
    }

    public void setReplyClickListener(OnReplyClickListener listener) {
        this.replyClickListener = listener;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        PostComment c = items.get(position);

        // Pirmoji eilutė: autorius + data (+ ⚠️ jei flagged)
        String line1 = c.getAutoriusVardas() != null ? c.getAutoriusVardas() : "Anonimas";
        if (c.getSukurta() != null) {
            line1 += " • " + dateFormat.format(c.getSukurta().toDate());
        }
        if (Boolean.TRUE.equals(c.getFlagged())) {
            line1 = "⚠️ " + line1;
        }
        holder.headerTextView.setText(line1);

        // Komentaro tekstas
        holder.bodyTextView.setText(c.getTekstas());

        // Hierarchija – įtrauka pagal level
        int level = c.getLevel(); // 0 – tėvinis, 1 – atsakymas ir t. t.
        float density = holder.itemView.getResources().getDisplayMetrics().density;
        int basePaddingStart = (int) (16 * density);
        int extraPerLevel = (int) (20 * density);
        int paddingStart = basePaddingStart + (extraPerLevel * level);

        holder.itemView.setPadding(
                paddingStart,
                holder.itemView.getPaddingTop(),
                holder.itemView.getPaddingRight(),
                holder.itemView.getPaddingBottom()
        );

        holder.replyButton.setOnClickListener(v -> {
            if (replyClickListener != null) {
                replyClickListener.onReplyClick(c);
            }
        });

        // Ilgas paspaudimas – perduodam į Activity (admin delete / user report)
        if (longClickListener != null) {
            holder.itemView.setOnLongClickListener(v -> {
                longClickListener.onCommentLongClick(c);
                return true;
            });
        } else {
            holder.itemView.setOnLongClickListener(null);
        }
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
        TextView headerTextView;
        TextView bodyTextView;
        TextView replyButton;

        CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            headerTextView = itemView.findViewById(R.id.commentHeaderTextView);
            bodyTextView = itemView.findViewById(R.id.commentBodyTextView);
            replyButton = itemView.findViewById(R.id.replyButton);
        }
    }
}
