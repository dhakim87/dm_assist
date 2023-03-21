package com.dm.assist.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.dm.assist.common.OnItemClickListener;
import com.dm.assist.model.Campaign;
import com.dm.assist.R;

import java.util.List;

public class CampaignAdapter extends RecyclerView.Adapter<CampaignAdapter.ViewHolder> {

    private List<Campaign> campaigns;

    public CampaignAdapter(List<Campaign> campaigns) {
        this.campaigns = campaigns;
    }

    public OnItemClickListener onItemClickListener;

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.campaign_item, parent, false);
        return new ViewHolder(view, onItemClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Campaign campaign = campaigns.get(position);
        holder.campaignNameTextView.setText(campaign.name);
        holder.campaignSettingTextView.setText(campaign.desc);
    }

    @Override
    public int getItemCount() {
        return campaigns.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView campaignNameTextView;
        private TextView campaignSettingTextView;

        public ViewHolder(@NonNull View itemView, final OnItemClickListener listener) {
            super(itemView);
            campaignNameTextView = itemView.findViewById(R.id.campaignNameTextView);
            campaignSettingTextView = itemView.findViewById(R.id.campaignSettingTextView);

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
