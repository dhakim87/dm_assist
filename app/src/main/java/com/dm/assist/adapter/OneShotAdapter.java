package com.dm.assist.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.dm.assist.R;
import com.dm.assist.common.OnItemClickListener;
import com.dm.assist.model.OneShot;

import java.util.List;

public class OneShotAdapter extends RecyclerView.Adapter<OneShotAdapter.ViewHolder> {

    private List<OneShot> oneShots;

    public OneShotAdapter(List<OneShot> oneShots) {
        this.oneShots = oneShots;
    }

    public OnItemClickListener onItemClickListener;

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.one_shot_item, parent, false);
        return new ViewHolder(view, onItemClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        OneShot oneShot = oneShots.get(position);
        holder.oneShotNameTextView.setText(oneShot.name);
    }

    @Override
    public int getItemCount() {
        return oneShots.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView oneShotNameTextView;

        public ViewHolder(@NonNull View itemView, final OnItemClickListener listener) {
            super(itemView);
            oneShotNameTextView = itemView.findViewById(R.id.oneShotNameTextView);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (listener != null) {
                        int position = getBindingAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            listener.onItemClick(position);
                        }
                    }
                }
            });
        }
    }
}