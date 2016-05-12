package com.jasondelport.nobelprize.nobelprizelaureates;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

/**
 * Created by jasondelport on 12/05/16.
 */
public class NobelPrizeViewHolder extends RecyclerView.ViewHolder  {

    public TextView firstname;
    public TextView surname;
    public TextView year;
    public TextView category;

    public NobelPrizeViewHolder(View view) {
        super(view);
        firstname = (TextView) view.findViewById(R.id.item_firstname);
        surname = (TextView) view.findViewById(R.id.item_surname);
        year = (TextView) view.findViewById(R.id.item_year);
        category = (TextView) view.findViewById(R.id.item_category);
    }

}
