package fr.example.androidmapbox;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

public class Hotel {

    public int id;
    public String name;
    public String description;
    public Drawable drawable;

    public Hotel(int id, String name, String description, Drawable drawable) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.drawable = drawable;
    }

    public static class HotelDiff extends DiffUtil.ItemCallback<Hotel> {

        @Override
        public boolean areItemsTheSame(@NonNull Hotel oldItem, @NonNull Hotel newItem) {
            return oldItem.id == newItem.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull Hotel oldItem, @NonNull Hotel newItem) {
            return oldItem == newItem;
        }
    }
}
