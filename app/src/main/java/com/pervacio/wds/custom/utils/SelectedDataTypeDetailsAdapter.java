package com.pervacio.wds.custom.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.pervacio.wds.CustomTextView;
import com.pervacio.wds.R;

import java.util.ArrayList;


/**
 * Created by Pervacio on 02/21/2018.
 */

public class SelectedDataTypeDetailsAdapter extends RecyclerView.Adapter<SelectedDataTypeDetailsAdapter.MyViewHolder> {


    private ArrayList<String> datatypeDetailsList;
    private Context mContext;


    public SelectedDataTypeDetailsAdapter(Context context, ArrayList<String> datatypeDetailsList) {
        this.datatypeDetailsList = datatypeDetailsList;
        this.mContext = context;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.childview_contenttype, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, final int position) {
        holder.content_image.setVisibility(View.GONE);
        holder.content_select.setVisibility(View.GONE);
        holder.content_name.setText(datatypeDetailsList.get(position));
        holder.content_count.setVisibility(View.GONE);

    }

    @Override
    public int getItemCount() {
        return datatypeDetailsList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public CustomTextView content_name, content_count;
        public ImageView content_image, content_result;
        public CheckBox content_select;
        public LinearLayout mainLayout;

        public MyViewHolder(View view) {
            super(view);
            content_name = (CustomTextView) view.findViewById(R.id.content_name);
            content_count = (CustomTextView) view.findViewById(R.id.content_progresscount);
            content_select = (CheckBox) view.findViewById(R.id.content_select);
            content_image = (ImageView) view.findViewById(R.id.content_image);
            content_result = (ImageView) view.findViewById(R.id.content_result);
            mainLayout = (LinearLayout) view.findViewById(R.id.mainLayout);
        }
    }
}

