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
import com.pervacio.wds.app.DLog;
import com.pervacio.wds.custom.appmigration.AppBackupModel;
import com.pervacio.wds.custom.appmigration.AppMigrateUtils;
import com.pervacio.wds.custom.appsinstall.AppSelectionChangedListener;

/**
 * Created by Satyanarayana on 29/04/2020.
 */

public class SelectedAppsDetailsAdapter extends RecyclerView.Adapter<SelectedAppsDetailsAdapter.MyViewHolder> {

    private Context mContext;
    AppSelectionChangedListener appSelectionChangedListener;


    public SelectedAppsDetailsAdapter(Context context) {
        this.mContext = context;
    }

    public SelectedAppsDetailsAdapter(Context context, AppSelectionChangedListener appSelectionChangedListener) {
        this.mContext = context;
        this.appSelectionChangedListener = appSelectionChangedListener;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.childview_contenttype_app_selection, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, final int position) {
        holder.content_image.setVisibility(View.VISIBLE);
        holder.content_select.setVisibility(View.VISIBLE);
        holder.content_name.setText(AppMigrateUtils.backupAppList.get(position).getAppName());
        holder.content_image.setImageDrawable(AppMigrateUtils.backupAppList.get(position).getAppIcon());
//        holder.content_image.setMaxHeight(50);
//        holder.content_image.setMaxWidth(50);

        holder.content_count.setVisibility(View.GONE);
        holder.content_name.setVisibility(View.VISIBLE);
        holder.content_select.setChecked(AppMigrateUtils.backupAppList.get(position).isChecked());

        holder.content_select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DLog.log("Apps Selected");
                CheckBox item = (CheckBox) v;
                AppBackupModel appBackupModel = AppMigrateUtils.backupAppList.get(position);
                appBackupModel.setChecked(item.isChecked());
                AppMigrateUtils.backupAppList.remove(position);
                AppMigrateUtils.backupAppList.add(position,appBackupModel);
                AppMigrateUtils.updateBackupAppsCountSize(position,item.isChecked());
                DLog.log("Apps Selected Apps Count "+AppMigrateUtils.totalAppCount+" AppMigrateUtils.backupAppList size "+AppMigrateUtils.backupAppList.size()+" appSelectionChangedListener "+appSelectionChangedListener);
                if(appSelectionChangedListener!=null){
                    appSelectionChangedListener.onAppSelectionChanged();
                }

            }
        });

    }

    @Override
    public int getItemCount() {
        return AppMigrateUtils.backupAppList.size();
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

