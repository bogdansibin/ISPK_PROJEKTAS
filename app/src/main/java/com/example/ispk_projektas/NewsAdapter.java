package com.example.ispk_projektas;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.NewsViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(NewsItem item);
    }

    private final Context context;
    private final List<NewsItem> items;
    private final OnItemClickListener listener;

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public NewsAdapter(Context context, List<NewsItem> items, OnItemClickListener listener) {
        this.context = context;
        this.items = items;
        this.listener = listener;
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    public void updateData(List<NewsItem> newItems) {
        items.clear();
        items.addAll(newItems);
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
        holder.bind(item, listener, context, auth, db);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class NewsViewHolder extends RecyclerView.ViewHolder {

        TextView titleText;
        TextView dateText;
        ImageButton favoriteButton;

        ImageButton shareButton;

        public NewsViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.newsTitleTextView);
            dateText = itemView.findViewById(R.id.newsDateTextView);
            favoriteButton = itemView.findViewById(R.id.favoriteButton);
            shareButton = itemView.findViewById(R.id.shareButton);
        }

        public void bind(NewsItem item,
                         OnItemClickListener listener,
                         Context context,
                         FirebaseAuth auth,
                         FirebaseFirestore db) {

            titleText.setText(item.title);

            if (item.pubDateMillis > 0) {
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                dateText.setText(df.format(new Date(item.pubDateMillis)));
            } else {
                dateText.setText("");
            }

            updateFavoriteIcon(item.isFavorite, favoriteButton);

            itemView.setOnClickListener(v -> listener.onItemClick(item));

            shareButton.setOnClickListener(v -> {
                String shareText = item.title + "\n" + item.link;

                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, item.title);
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);

                context.startActivity(
                        Intent.createChooser(shareIntent, "Dalintis straipsniu")
                );
            });

            favoriteButton.setOnClickListener(v -> {
                FirebaseUser user = auth.getCurrentUser();
                if (user == null) {
                    Toast.makeText(context,
                            "Prisijunkite, kad išsaugotumėte straipsnius",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                boolean newState = !item.isFavorite;
                item.isFavorite = newState;
                updateFavoriteIcon(newState, favoriteButton);

                String uid = user.getUid();
                String docId = String.valueOf(item.link.hashCode());

                if (newState) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("title", item.title);
                    data.put("link", item.link);
                    data.put("description", item.description);
                    data.put("pubDateMillis", item.pubDateMillis);
                    data.put("createdAt", FieldValue.serverTimestamp());

                    db.collection("users")
                            .document(uid)
                            .collection("favorites")
                            .document(docId)
                            .set(data)
                            .addOnSuccessListener(aVoid ->
                                    Toast.makeText(context, "Straipsnis išsaugotas",
                                            Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e ->
                                    Toast.makeText(context,
                                            "Nepavyko išsaugoti: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show());
                } else {
                    db.collection("users")
                            .document(uid)
                            .collection("favorites")
                            .document(docId)
                            .delete()
                            .addOnSuccessListener(aVoid ->
                                    Toast.makeText(context,
                                            "Straipsnis pašalintas iš išsaugotų",
                                            Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e ->
                                    Toast.makeText(context,
                                            "Nepavyko pašalinti: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show());
                }
            });
        }

        private void updateFavoriteIcon(boolean isFavorite, ImageButton button) {
            if (isFavorite) {
                button.setImageResource(R.drawable.baseline_favorite_24);
                button.setColorFilter(0xFFFF0000); // raudona
            } else {
                button.setImageResource(R.drawable.baseline_favorite_border_24);
                button.setColorFilter(0xFF777777); // pilka
            }
        }
    }
}
