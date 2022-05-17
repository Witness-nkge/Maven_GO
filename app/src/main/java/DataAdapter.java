package com.maven.maven;


import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class DataAdapter extends RecyclerView.Adapter<DataAdapter.MyHolder> {
    ArrayList<Data> data;
    Context context;

    public DataAdapter(Context context,ArrayList<Data> data){
        this.context = context;
        this.data = data;
    }

    @NonNull
    @Override
    public DataAdapter.MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item,null,true);
        MyHolder myHolder = new MyHolder(view);
        return myHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull DataAdapter.MyHolder holder, int position) {

        holder.title.setText(data.get(position).getTitle());
        holder.link.setText(data.get(position).getLink());
        holder.description.setText(data.get(position).getDescription());
        holder.web.setText(data.get(position).getWeb());

        holder.card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String web = holder.web.getText().toString();

                Intent intent = new Intent(context,WebActivity.class);
                intent.putExtra("web", web);
                context.startActivity(intent);

            }
        });

    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public static class MyHolder extends RecyclerView.ViewHolder{

        TextView title,link,description,web;
        CardView card;

        public MyHolder(View view){
            super(view);

            title = view.findViewById(R.id.title);
            link = view.findViewById(R.id.link);
            description = view.findViewById(R.id.description);
            web = view.findViewById(R.id.web);
            card = view.findViewById(R.id.card);


        }
    }
}
