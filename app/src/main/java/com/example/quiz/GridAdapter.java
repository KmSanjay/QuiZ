package com.example.quiz;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdValue;
import com.google.android.gms.ads.OnPaidEventListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class GridAdapter extends BaseAdapter {

    private List<String> sets;
    private String category;

    public GridAdapter(List<String> sets, String category) {
        this.sets = sets;
        this.category=category;

    }

    @Override
    public int getCount() {
        return sets.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view ;
        if (convertView==null){
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.set_items,parent,false);
        }else{
            view=convertView;
        }
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent questionIntent= new Intent(parent.getContext(),QuestionActivity.class);
                questionIntent.putExtra("category",category);
                questionIntent.putExtra("setid", sets.get(position));
                parent.getContext().startActivity(questionIntent);
            }
        });
        ((TextView)view.findViewById(R.id.textView)).setText(String.valueOf(position+1));
        return view;
    }
}
