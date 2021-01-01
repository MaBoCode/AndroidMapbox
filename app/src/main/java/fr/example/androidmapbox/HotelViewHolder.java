package fr.example.androidmapbox;

import androidx.recyclerview.widget.RecyclerView;

import fr.example.androidmapbox.databinding.HotelListItemBinding;

public class HotelViewHolder extends RecyclerView.ViewHolder {

    protected final HotelListItemBinding binding;

    public HotelViewHolder(HotelListItemBinding binding) {
        super(binding.getRoot());
        this.binding = binding;
    }

    public void bind(final Hotel hotel, HotelAdapter.HotelItemClickListener listener) {
        binding.setHotel(hotel);
        binding.setHotelClickListener(listener);
        binding.executePendingBindings();
    }
}
