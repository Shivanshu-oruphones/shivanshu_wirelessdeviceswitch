package com.pervacio.wds.custom.utils;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.recyclerview.widget.RecyclerView;

import com.pervacio.wds.CustomTextView;
import com.pervacio.wds.R;
import com.pervacio.wds.app.EMMigrateStatus;
import com.pervacio.wds.custom.models.ContentDetails;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 * Created by Surya Polasanapalli on 12/19/2017.
 */

public class ContentProgressDetailsAdapter extends RecyclerView.Adapter<ContentProgressDetailsAdapter.MyViewHolder> {

    private HashMap<Integer, ContentDetails> contentDetailsList;

    public ContentProgressDetailsAdapter(HashMap<Integer, ContentDetails> contentDetailsList) {
        this.contentDetailsList = contentDetailsList;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.childview_content_progress, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        try {
            final ContentDetails contentDetails = contentDetailsList.get(getContentType(position));
            if (contentDetails.isSupported() && contentDetails.isSelected() && contentDetails.getProgressCount() > 0) {
                if (holder.mainLayout.getHeight() == 0)
                    holder.mainLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                try {
                    int progressCount = contentDetails.getProgressCount();
                    if (progressCount > contentDetails.getTotalCount()) {
                        progressCount = contentDetails.getTotalCount();
                    }
                    holder.content_image.setImageResource(contentDetails.getImageDrawableId()[0]);
                    holder.content_name.setText(contentDetails.getContentName());
                    holder.content_count.setText(progressCount + "/" + contentDetails.getTotalCount());

                    //For live updates based on size
                    if (EMMigrateStatus.isLiveUpdateRequired(contentDetails.getContentType())) {
                        holder.content_progress.setProgress(
                                (int) ((EMMigrateStatus.getLiveTransferredSize(
                                        contentDetails.getContentType()) * 100) / contentDetails.getTotalSizeOfEntries()));
                    } else {
                        holder.content_progress.setProgress(((contentDetails.getProgressCount() * 100) / contentDetails.getTotalCount()));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                //if (holder.mainLayout.getHeight() != 0)
                holder.mainLayout.setLayoutParams(new LinearLayout.LayoutParams(0, 0));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return contentDetailsList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public CustomTextView content_name, content_count;
        public ImageView content_image;
        public ProgressBar content_progress;
        public LinearLayout mainLayout;

        public MyViewHolder(View view) {
            super(view);
            content_name = (CustomTextView) view.findViewById(R.id.content_name);
            content_count = (CustomTextView) view.findViewById(R.id.content_progresscount);
            content_image = (ImageView) view.findViewById(R.id.content_image);
            content_progress = (ProgressBar) view.findViewById(R.id.content_progress);
            mainLayout = (LinearLayout) view.findViewById(R.id.mainLayout);
        }
    }

    public int getContentType(int position) {
        List<Integer> nameList = new ArrayList<Integer>(contentDetailsList.keySet());
        return nameList.get(position);
    }
}
