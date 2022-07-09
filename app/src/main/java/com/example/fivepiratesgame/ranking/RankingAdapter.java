package com.example.fivepiratesgame.ranking;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fivepiratesgame.R;

import java.util.ArrayList;
import java.util.List;

public class RankingAdapter extends RecyclerView.Adapter<RankingAdapter.RankingViewHolder> {

    private List<RankingData> rankingList;
    private final Context context;

    public RankingAdapter(Context context, List<RankingData> rankingList) {
        this.context = context;
        this.rankingList = rankingList;
    }

    @NonNull
    @Override
    public RankingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_ranking, parent, false);
        RankingViewHolder holder = new RankingViewHolder(view);

        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull RankingViewHolder holder, int position) {
        RankingData rankData = rankingList.get(position);

        holder.tvNickname.setText(rankData.getNickname());
        holder.tvGold.setText(rankData.getGold());
    }

    @Override
    public int getItemCount() {
        return rankingList.size();
    }

    public class RankingViewHolder extends RecyclerView.ViewHolder {
        protected ImageView ivRankProfile;
        protected TextView tvNickname;
        protected TextView tvGold;

        public RankingViewHolder(@NonNull View itemView) {
            super(itemView);
            this.ivRankProfile = (ImageView) itemView.findViewById(R.id.ivRankProfile);
            this.tvNickname = (TextView) itemView.findViewById(R.id.tvNickname);
            this.tvGold = (TextView) itemView.findViewById(R.id.tvGold);
        }
    }
}