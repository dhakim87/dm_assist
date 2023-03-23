package com.dm.assist.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

import com.dm.assist.common.OnItemClickListener;
import com.dm.assist.model.WorldCharacter;
import com.dm.assist.R;
import com.dm.assist.model.WorldCharacter;

public class CharacterAdapter extends RecyclerView.Adapter<CharacterAdapter.ViewHolder> {

    private List<WorldCharacter> characters;

    public CharacterAdapter(List<WorldCharacter> characters) {
        this.characters = characters;
    }

    public OnItemClickListener onItemClickListener;

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.character_item, parent, false);
        return new ViewHolder(view, onItemClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WorldCharacter character = characters.get(position);
        holder.characterNameTextView.setText(character.name);

        holder.deleteCharacterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int clickedPos = holder.getBindingAdapterPosition();
                // Remove the campaign from your list
                characters.remove(clickedPos);

                // Notify the adapter that the item has been removed
                notifyItemRemoved(clickedPos);
                notifyItemRangeChanged(clickedPos, characters.size());
            }
        });
    }

    @Override
    public int getItemCount() {
        return characters.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView characterNameTextView;
        private ImageButton deleteCharacterButton;


        public ViewHolder(@NonNull View itemView, final OnItemClickListener listener) {
            super(itemView);
            characterNameTextView = itemView.findViewById(R.id.characterNameTextView);
            deleteCharacterButton = itemView.findViewById(R.id.deleteCharacterButton);

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