package com.trustingsocial.android;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.Map;

class IDListViewAdapter extends RecyclerView.Adapter<IDListViewAdapter.MyViewHolder> {
    private Map<String, String> dataSet;

    public IDListViewAdapter(Map<String, String> dataSet) {
        this.dataSet = dataSet;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.id_list_item, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        holder.title.setText(dataSet.keySet().toArray()[position].toString());
        holder.textValue.setText(dataSet.values().toArray()[position].toString());

    }

    @Override
    public int getItemCount() {
        return dataSet.size();
    }

    static class MyViewHolder extends RecyclerView.ViewHolder {
        private TextView title;
        private TextView textValue;

        MyViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.ocr_title);
            textValue = itemView.findViewById(R.id.ocr_text);
        }
    }

}