package com.dm.assist.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

import com.dm.assist.common.CharacterDialog;
import com.dm.assist.common.OnItemClickListener;
import com.dm.assist.R;

public class DialogAdapter extends RecyclerView.Adapter<DialogAdapter.ViewHolder> {

    private CharacterDialog dialog;

    public DialogAdapter(CharacterDialog dialog) {
        this.dialog = dialog;
    }

    public OnItemClickListener onItemClickListener;

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.dialog_item, parent, false);
        return new ViewHolder(view, onItemClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String msg = dialog.conversation.get(position);
        holder.messageTextView.setText(msg);
    }

    @Override
    public int getItemCount() {
        return dialog.conversation.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView messageTextView;

        public ViewHolder(@NonNull View itemView, final OnItemClickListener listener) {
            super(itemView);
            messageTextView = itemView.findViewById(R.id.messageTextView);

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