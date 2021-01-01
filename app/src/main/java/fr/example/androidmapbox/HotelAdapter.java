package fr.example.androidmapbox;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import fr.example.androidmapbox.databinding.HotelListItemBinding;

public class HotelAdapter extends ListAdapter<Hotel, HotelViewHolder> {

    protected HotelItemClickListener listener;

    protected HotelAdapter(@NonNull DiffUtil.ItemCallback<Hotel> diffCallback, HotelItemClickListener listener) {
        super(diffCallback);
        this.listener = listener;
    }

    public interface HotelItemClickListener {
        void onItemClick(Hotel hotel);
    }

    @NonNull
    @Override
    public HotelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        HotelListItemBinding binding = HotelListItemBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new HotelViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull HotelViewHolder holder, int position) {
        Hotel hotel = getItem(position);
        holder.bind(hotel, listener);
    }
}
