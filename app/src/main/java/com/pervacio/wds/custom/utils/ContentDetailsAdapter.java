package com.pervacio.wds.custom.utils;

import static com.pervacio.wds.custom.utils.Constants.IS_MMDS;

import android.content.Context;
import android.util.Log;
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
import com.pervacio.wds.app.EMDataType;
import com.pervacio.wds.app.EMMigrateStatus;
import com.pervacio.wds.app.EMUtility;
import com.pervacio.wds.app.ui.EasyMigrateActivity;
import com.pervacio.wds.custom.models.ContentDetails;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 * Created by Surya Polasanapalli on 12/19/2017.
 */

public class ContentDetailsAdapter extends RecyclerView.Adapter<ContentDetailsAdapter.MyViewHolder> {


    private HashMap<Integer, ContentDetails> contentDetailsList;
    private int adapterType;
    private Context mContext;
    private ContentSelectionInterface contentSelectionInterface;
    private InstallAppsSelectedInterface installAppsSelectedInterface;

    public static final int PERMISSIONS = 1;
    public static final int CONTENT_SELECTION = 2;
    public static final int SUMMARY = 3;


    public ContentDetailsAdapter(Context context, int adapterType, HashMap<Integer, ContentDetails> contentDetailsList) {
        this.contentDetailsList = contentDetailsList;
        this.adapterType = adapterType;
        this.mContext = context;
        this.contentSelectionInterface = (ContentSelectionInterface) mContext;
        this.installAppsSelectedInterface = (InstallAppsSelectedInterface) mContext;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.childview_contenttype, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, final int position) {
        try {
            final ContentDetails contentDetails = contentDetailsList.get(getContentType(position));
            holder.mainLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            holder.content_image.setImageResource(contentDetails.getImageDrawableId()[0]);
            holder.content_select.setChecked(contentDetails.isSelected());
            holder.content_select.setEnabled(contentDetails.isSupported());
            holder.content_name.setText(contentDetails.getContentName());
            holder.content_count.setText(getCountDetails(contentDetails));
            if (adapterType == CONTENT_SELECTION) {
                if (contentDetails.isSupported() && contentDetails.isPermissionGranted() && contentDetails.getTotalCount() != 0) {
                    holder.content_select.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            CheckBox item = (CheckBox) view;
                            contentSelectionInterface.updateContentSelection(contentDetails.getContentType(), item.isChecked());
                        }
                    });
                } else if (contentDetails.isSupported() && contentDetails.getTotalCount() == 0 && contentDetails.isPermissionGranted()) {
                    holder.content_image.setImageResource(contentDetails.getImageDrawableId()[1]);
                    contentDetailsList.get(getContentType(position)).setSelected(false);
                    holder.content_select.setChecked(false);
                    holder.content_select.setEnabled(false);
                } else if (contentDetails.isSupported() && !contentDetails.isPermissionGranted()) {
                    holder.content_image.setImageResource(contentDetails.getImageDrawableId()[1]);
                    contentDetailsList.get(getContentType(position)).setSelected(false);
                    holder.content_select.setChecked(false);
                    holder.content_select.setEnabled(false);
                } else {
                    holder.mainLayout.setLayoutParams(new LinearLayout.LayoutParams(0, 0));
                    //holder.mainLayout.setVisibility(View.GONE);
                }
                if (contentDetails.getContentType() == EMDataType.EM_DATA_TYPE_SETTINGS)
                    holder.content_view.setVisibility(View.VISIBLE);

                //Enabled OnClick for entire row if content type is Settings
                if (contentDetails.getContentType() == EMDataType.EM_DATA_TYPE_SETTINGS) {
                    holder.mainLayout.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            contentSelectionInterface.selectedDataTypeInfo(contentDetails.getContentType());
                        }
                    });
                }
                if (contentDetails.getContentType() == EMDataType.EM_DATA_TYPE_APP) {

                    holder.mainLayout.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            //contentSelectionInterface.selectedDataTypeInfo(contentDetails.getContentType());
                            DLog.log("Apps Selected");
                            contentSelectionInterface.selectedDataTypeInfo(contentDetails.getContentType());

                            //CheckBox item = (CheckBox) v;
                            //contentSelectionInterface.updateContentSelection(contentDetails.getContentType(), item.isChecked());

                        }
                    });
                }
                if (contentDetails.getContentType() == EMDataType.EM_DATA_TYPE_SMS_MESSAGES && Constants.CUSTOM_SELECTION_ENABLED) {
                    holder.message_setting.setVisibility(View.VISIBLE);
                    holder.message_setting.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (contentDetails.isPermissionGranted()) {
                                contentSelectionInterface.selectedDataTypeInfo(contentDetails.getContentType());
                            } else {
                                DLog.log("Permission denied for " + contentDetails.getContentName());
                            }
                        }
                    });
                } else {
                    holder.message_setting.setVisibility(View.GONE);
                }

            } else if (adapterType == PERMISSIONS) {
                holder.content_select.setEnabled(false);
                holder.content_select.setChecked(false);
                holder.content_select.setVisibility(View.GONE);
                if (contentDetails.isSupported() && !contentDetails.isPermissionGranted()) {
                    holder.content_image.setImageResource(contentDetails.getImageDrawableId()[1]);
                    contentDetailsList.get(getContentType(position)).setSelected(false);
                    holder.content_count.setText("");
                } else {
                    holder.mainLayout.setLayoutParams(new LinearLayout.LayoutParams(0, 0));
                }
            } else {

                if (contentDetails.getContentType() == EMDataType.EM_DATA_TYPE_APP && DashboardLog.getInstance().isThisDest() == true) {
                    DLog.log("ContentDetailsAdapter adapterType "+adapterType+" DashboardLog.getInstance().isThisDest() "+DashboardLog.getInstance().isThisDest()+" contentDetails.getContentType() "+contentDetails.getContentType()+" making visible");
                    holder.ll_content_installapps.setVisibility(View.VISIBLE);
//                    DLog.log(" ContentDetailsAdapter adapterType ");
                    holder.btn_content_installapps.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            installAppsSelectedInterface.onInstallAppsSelectedSummary();
                        }
                    });

                } else {
                    DLog.log("ContentDetailsAdapter adapterType "+adapterType+" DashboardLog.getInstance().isThisDest() "+DashboardLog.getInstance().isThisDest()+" contentDetails.getContentType() "+contentDetails.getContentType()+" making gone");
                    holder.ll_content_installapps.setVisibility(View.GONE);
                }


                holder.content_result.setVisibility(View.VISIBLE);
                holder.content_select.setVisibility(View.GONE);
                if (!contentDetails.isSupported() || !contentDetails.isSelected()) {
                    holder.mainLayout.setLayoutParams(new LinearLayout.LayoutParams(0, 0));
                    //holder.mainLayout.setVisibility(View.GONE);
                }
                Log.d("TAG", "onBindViewHolder1: " + contentDetails.getTotalCount());
                Log.d("TAG", "onBindViewHolder2: " + getCountDetails(contentDetails));
                Log.d("Content: ", String.valueOf(EMMigrateStatus.getItemsTransferred(contentDetails.getContentType())) + "ContentDetailsAdapter: " + contentDetails.getTotalCount());
//                if (EMMigrateStatus.getItemsTransferred(contentDetails.getContentType()) < contentDetails.getTotalCount() || (IS_MMDS && (contentDetails.getContentType() == EMDataType.EM_DATA_TYPE_SMS_MESSAGES) && !"Android".equalsIgnoreCase(EasyMigrateActivity.remotePlatform) && EasyMigrateActivity.iosSMSFailed)) {
                if ((contentDetails.getTotalCount() - EMMigrateStatus.getItemsNotTransferred(contentDetails.getContentType())) < contentDetails.getTotalCount() || (IS_MMDS && (contentDetails.getContentType() == EMDataType.EM_DATA_TYPE_SMS_MESSAGES) && !"Android".equalsIgnoreCase(EasyMigrateActivity.remotePlatform) && EasyMigrateActivity.iosSMSFailed)) {
                    holder.content_result.setImageResource(R.drawable.fail2);
                    holder.content_count.setTextColor(mContext.getResources().getColor(R.color.lightGreen));
                    holder.content_view.setVisibility(View.VISIBLE);
                    holder.mainLayout.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            contentSelectionInterface.selectedDataTypeInfo(contentDetails.getContentType());
                        }
                    });
                }
            }
        } catch (Exception e) {
            DLog.log("Exception in adapter "+e.getMessage());
        }
    }

    @Override
    public int getItemCount() {
        return contentDetailsList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public CustomTextView content_name, content_count, message_setting, content_installapps;
        public ImageView content_image, content_result, content_view;
        public CheckBox content_select;
        public LinearLayout mainLayout, ll_content_installapps;
        public Button btn_content_installapps;


        public MyViewHolder(View view) {
            super(view);
            content_name = (CustomTextView) view.findViewById(R.id.content_name);
            content_count = (CustomTextView) view.findViewById(R.id.content_progresscount);
            content_select = (CheckBox) view.findViewById(R.id.content_select);
            content_image = (ImageView) view.findViewById(R.id.content_image);
            content_result = (ImageView) view.findViewById(R.id.content_result);
            content_view = (ImageView) view.findViewById(R.id.content_view);
            mainLayout = (LinearLayout) view.findViewById(R.id.mainLayout);
            message_setting = (CustomTextView) view.findViewById(R.id.message_setting);
            ll_content_installapps = (LinearLayout) view.findViewById(R.id.ll_content_installapps);
            btn_content_installapps = (Button) view.findViewById(R.id.btn_content_installapps);
        }
    }


    private String getCountDetails(ContentDetails contentDetails) {
        StringBuffer s = new StringBuffer();
        int count = contentDetails.getTotalCount();
//        if (EMDataType.EM_DATA_TYPE_APP == contentDetails.getContentType() || EMDataType.EM_DATA_TYPE_SMS_MESSAGES == contentDetails.getContentType()) {
//            count = Integer.parseInt(getCountDetails(contentDetails));
//        }
        long fileSize = contentDetails.getTotalSizeOfEntries();
        if (!contentDetails.isPermissionGranted() || adapterType == PERMISSIONS) {
            return "";
        } else if (adapterType == CONTENT_SELECTION) {
            //Not doing anything,count and size already fetched above.
        } else if (adapterType == SUMMARY) {
            if (contentDetails.getContentType() == EMDataType.EM_DATA_TYPE_SETTINGS)
                count = EMMigrateStatus.getItemsTransferred(contentDetails.getContentType());
            if (count > contentDetails.getTotalCount()) {
                count = contentDetails.getTotalCount();
            }
            fileSize = EMMigrateStatus.getTransferedFilesSize(contentDetails.getContentType());
        }
        s.append(count);
        String size = EMUtility.readableFileSize(fileSize);
        if (contentDetails.getTotalSizeOfEntries() != -1 && !size.isEmpty()) {
            s.append("(" + size + ")");
        }
//        Log.d("TAG", "getCountDetails: " + s.toString());
//        Log.d("TAG", "getCountDetails2: " + count);
        return String.valueOf(s);
    }

    private int getContentType(int position) {
        List<Integer> nameList = new ArrayList<Integer>(contentDetailsList.keySet());
        return nameList.get(position);
    }
}

