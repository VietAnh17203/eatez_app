package com.thuanht.eatez.Adapter;

import android.content.Context;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.thuanht.eatez.R;
import com.thuanht.eatez.databinding.ItemPostFavouriteBinding;
import com.thuanht.eatez.model.Favourite;
import com.thuanht.eatez.utils.DateUtils;

import java.util.List;

public class PostFavouriteAdapter extends RecyclerView.Adapter<PostFavouriteAdapter.MyViewHolder> {

    private List<Favourite> favouriteList;
    private final Context context;
    private OnclickItemListener listener;

    public PostFavouriteAdapter(List<Favourite> favouriteList, Context context, OnclickItemListener listener) {
        this.favouriteList = favouriteList;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPostFavouriteBinding binding = ItemPostFavouriteBinding.inflate(LayoutInflater.from(context), parent, false);
        return new MyViewHolder(binding);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Favourite fv = favouriteList.get(position);

        Glide.with(context)
                .load(fv.getThumbnailImage())
                .placeholder(R.drawable.placeholder_image)
                .into(holder.binding.imageViewDishesFavourite);
        holder.binding.tvTitleDishFavourite.setText(fv.getTitle());
        holder.binding.tvLocationFavourite.setText(fv.getRestaurant().getResAddress());
        holder.binding.tvDateFavourite.setText("Đã lưu " + DateUtils.convertToRelativeTime(fv.getDate()));

        holder.binding.layoutItemFavourite.setOnClickListener(v -> listener.onClick(fv));
        holder.binding.layoutItemFavourite.setOnLongClickListener(v -> {
            listener.longClick(fv);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return favouriteList.size();
    }

    public void removeItem(int position) {
        favouriteList.remove(position);
        notifyItemRemoved(position);
    }

    public Favourite getItem(int position) {
        return favouriteList.get(position);
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        private final ItemPostFavouriteBinding binding;

        public MyViewHolder(@NonNull ItemPostFavouriteBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    public interface OnclickItemListener {
        void onClick(Favourite favourite);
        void longClick(Favourite favourite);
    }
}
