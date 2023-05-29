package com.pervacio.wds.custom.appsinstall;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.pervacio.wds.CustomTextView;
import com.pervacio.wds.R;
import com.pervacio.wds.app.DLog;

import com.pervacio.wds.custom.appmigration.AppInfoModel;

import java.util.List;

public class InstallAppDetailsChildAdapter extends RecyclerView.Adapter<InstallAppDetailsChildAdapter.ChildViewHolder> {

    private Context mContext;
    public List<AppInfoModel> appInfoList;
    private AppInstallSelectionListener appInstallSelectionListener;



    public InstallAppDetailsChildAdapter(Context context, List<AppInfoModel> appInfoList, AppInstallSelectionListener appInstallSelectionListener) {
        this.appInfoList = appInfoList;
        this.mContext = context;
        this.appInstallSelectionListener = appInstallSelectionListener;
    }

    @Override
    public ChildViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.childview_contenttype_appsinstall, parent, false);
        return new ChildViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final ChildViewHolder holder, @SuppressLint("RecyclerView") final int position) {
        final AppInfoModel appInfoModel = appInfoList.get(position);
//        holder.content_image.setVisibility(View.GONE);
//        holder.content_select.setVisibility(View.VISIBLE);
        holder.content_name.setText(appInfoList.get(position).getName());
        if(appInfoModel.getIcon()!=null) {
            holder.content_image.setImageDrawable(appInfoModel.getIcon());
        }
        else{
            holder.content_image.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_android));
        }
        if(appInfoModel.isInstallationDone()){
            //installed case
            holder.id_ll_app_install_result.setEnabled(false);
            holder.id_ll_app_install_result_btn.setVisibility(View.GONE);
            holder.id_ll_app_install_result_iv.setImageResource(R.drawable.pass);
            holder.id_ll_app_install_result_iv.setVisibility(View.VISIBLE);

//            holder.id_ll_app_install_result.setEnabled(false);
        }else{
            //Pending case
            holder.id_ll_app_install_result.setEnabled(true);
            if(appInfoModel.isLocalInstallationAttempted()==false){
//                holder.content_result.setImageResource(R.drawable.logo_small);
//                holder.id_ll_app_install_result_iv.setImageResource(R.drawable.logo_small);

                holder.id_ll_app_install_result_iv.setVisibility(View.GONE);
                holder.id_ll_app_install_result_btn.setVisibility(View.VISIBLE);
//                holder.content_name_subheading.setVisibility(View.GONE);
            }else{
                //faile case
//                holder.content_result.setImageResource(R.drawable.ic_passed);
//                holder.content_result.setImageResource(R.drawable.ic_failed);
//                holder.content_result.setImageResource(R.drawable.ic_fail);
//                holder.content_name_subheading.setVisibility(View.VISIBLE);
                holder.id_ll_app_install_result_iv.setVisibility(View.VISIBLE);
                holder.id_ll_app_install_result_btn.setVisibility(View.VISIBLE);
                holder.id_ll_app_install_result_iv.setImageResource(R.drawable.fail2);
            }
//            holder.content_result.setImageResource(R.drawable.ic_failed);
        }


        holder.id_ll_app_install_result_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                int position =  holder.
                DLog.log("InstallAppDetailsChildAdapter content_image onClick position: "+position);
                if(appInstallSelectionListener!=null) {
                    appInstallSelectionListener.onAppInstallSelected(appInfoModel.getPath(), appInfoModel.getPackageName(), position);
                }
            }
        });
        holder.content_name.setVisibility(View.VISIBLE);
        holder.content_select.setChecked(appInfoList.get(position).isChecked());

    }

    @Override
    public int getItemCount() {
        DLog.log("InstallAppDetailsChildAdapter appInfoList.size() "+appInfoList.size());
        return appInfoList.size();
    }

    class ChildViewHolder extends RecyclerView.ViewHolder {

        public CustomTextView content_name, content_count,content_name_subheading;
        public ImageView content_image, id_ll_app_install_result_iv;
        public CheckBox content_select;
        public LinearLayout mainLayout,id_ll_app_install_result;
        public Button id_ll_app_install_result_btn;
//        public ProgressBar progressBar;

        public ChildViewHolder(View view) {
            super(view);
            content_name = (CustomTextView) view.findViewById(R.id.content_name);
            content_count = (CustomTextView) view.findViewById(R.id.content_progresscount);
            content_name_subheading = (CustomTextView) view.findViewById(R.id.content_name_subheading);
            content_select = (CheckBox) view.findViewById(R.id.content_select);
            content_image = (ImageView) view.findViewById(R.id.content_image);
//            content_result = (ImageView) view.findViewById(R.id.content_result);
            id_ll_app_install_result_iv = (ImageView) view.findViewById(R.id.id_ll_app_install_result_iv);
            id_ll_app_install_result_btn = (Button) view.findViewById(R.id.id_ll_app_install_result_btn);
            id_ll_app_install_result = (LinearLayout)view.findViewById(R.id.id_ll_app_install_result);
//            id_ll_app_install_result = (LinearLayout)view.findViewById(R.id.id_ll_app_install_result);
//            id_ll_app_install_result = (LinearLayout)view.findViewById(R.id.id_ll_app_install_result);
//            progressBar = (ProgressBar) view.findViewById(R.id.progressBar1);
//            mainLayout = (LinearLayout) view.findViewById(R.id.mainLayout);
        }
    }


    public interface AppInstallSelectionListener {
        public void onAppInstallSelected(String filePath, String packageName, int position);
    }


}
