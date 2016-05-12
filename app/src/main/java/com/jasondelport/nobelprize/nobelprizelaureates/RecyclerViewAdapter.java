package com.jasondelport.nobelprize.nobelprizelaureates;

/**
 * Created by jasondelport on 12/05/16.
 */

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

public class RecyclerViewAdapter extends RecyclerView.Adapter<NobelPrizeViewHolder> {
    private List<NobelPrize> mPrizes;
    private Context mContext;

    public RecyclerViewAdapter(Context context, List<NobelPrize> prizes) {
        this.mContext = context;
        this.mPrizes = prizes;
    }


    public void addNobelPrize(NobelPrize prize) {
        this.mPrizes.add(prize);
        notifyItemInserted(mPrizes.size()-1);
    }

    public List<NobelPrize> getNobelPrizes() {
        return mPrizes;
    }

    @Override
    public NobelPrizeViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recyclerview, null);
        return new NobelPrizeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(NobelPrizeViewHolder viewHolder, final int position) {
        NobelPrize prize = mPrizes.get(position);
        viewHolder.firstname.setText(prize.getLaureates().get(0).getFirstname());
        viewHolder.surname.setText(prize.getLaureates().get(0).getSurname());
        viewHolder.year.setText(Integer.toString(prize.getYear()));
        viewHolder.category.setText(prize.getCategory());
    }

    @Override
    public int getItemCount() {
        return mPrizes.size();
    }

}