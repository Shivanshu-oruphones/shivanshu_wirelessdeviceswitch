package com.pervacio.wds.custom.appsinstall;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.pervacio.wds.R;
import com.pervacio.wds.app.DLog;
import com.pervacio.wds.app.EMGlobals;
import com.pervacio.wds.custom.APPI;
import com.pervacio.wds.custom.appmigration.AppInfoModel;
import com.pervacio.wds.custom.appmigration.AppMigrateUtils;

import java.util.List;

import static com.pervacio.wds.custom.appsinstall.AppInstallationsCategory.AppsStatus.FAILED;
import static com.pervacio.wds.custom.appsinstall.AppInstallationsCategory.AppsStatus.INSTALLED;
import static com.pervacio.wds.custom.appsinstall.AppInstallationsCategory.AppsStatus.PENDING;
import static com.pervacio.wds.custom.appsinstall.InstallAppsUtils.*;

public class InstallAppsFragment extends Fragment implements InstallAppDetailsChildAdapter.AppInstallSelectionListener{
    static EMGlobals emGlobals = new EMGlobals();
    private static final String TAG = "InstallAppsFragment";
    View view;
    private RecyclerView mRecyclerView;
    List<AppInfoModel> appInfoModelList ;
    private InstallAppDetailsChildAdapter mCustomAdapter;
    Context context;
    AppInstallationsCategory.AppsStatus appsStatus;
    private int selectedAppPosition=-1;
    private String selectedPackageName;
    private TextView suggestion_tv,center_tv;
    GoogleSignInClient mGoogleSignInClient;
    public static final int RC_SIGN_IN =101;
    private String file_path;
    private String package_name;

    public InstallAppsFragment(Context context,AppInstallationsCategory.AppsStatus appsStatus){
        this.context = context;
        this.appsStatus = appsStatus;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_install_apps, container, false);
        mRecyclerView =  view.findViewById(R.id.listview);
        suggestion_tv = view.findViewById(R.id.suggestion_tv);
        center_tv = view.findViewById(R.id.center_tv);
        setUpAppInfoModelListAdapter(appsStatus);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        DLog.log(TAG+" enter onStart");
        setUpAppInfoModelListAdapter(appsStatus);
    }

    public void setUpAppInfoModelListAdapter(AppInstallationsCategory.AppsStatus appsStatus){
        DLog.log(TAG+" enter setUpAppInfoModelListAdapter appsStatus "+appsStatus);
        switch (appsStatus){
            case FAILED:
                appInfoModelList = getInstallationFailedList();
                if(appInfoModelList.isEmpty()) {
                    suggestion_tv.setVisibility(View.GONE);
                    center_tv.setVisibility(View.VISIBLE);
                    center_tv.setText("No Apps");
                }else{
                    suggestion_tv.setVisibility(View.VISIBLE);
                    suggestion_tv.setText(AppInstallConstants.appsSuggestionStringMap.get(FAILED));
                    center_tv.setVisibility(View.GONE);
                }
                break;
            case PENDING:
                appInfoModelList =  getNotInstalledList();
                if(appInfoModelList.isEmpty()) {
                    suggestion_tv.setVisibility(View.GONE);
                    center_tv.setVisibility(View.VISIBLE);
                    center_tv.setText("No Apps");
                }else{
                    suggestion_tv.setVisibility(View.VISIBLE);
                    suggestion_tv.setText(AppInstallConstants.appsSuggestionStringMap.get(PENDING));
                    center_tv.setVisibility(View.GONE);
                }
                break;
            case INSTALLED:
                appInfoModelList = getInstallationPassedList();
                if(appInfoModelList.isEmpty()) {
                    suggestion_tv.setVisibility(View.GONE);
                    center_tv.setVisibility(View.VISIBLE);
                    center_tv.setText("No Apps");
                }else{
                    suggestion_tv.setVisibility(View.VISIBLE);
                    suggestion_tv.setText(AppInstallConstants.appsSuggestionStringMap.get(INSTALLED));
                    center_tv.setVisibility(View.GONE);
                }
                break;
            default:
                break;
        }
        DLog.log(TAG+" enter setUpAppInfoModelListAdapter appsStatus "+appsStatus+" appInfoModelList.size() "+appInfoModelList.size());
        if(!appInfoModelList.isEmpty()){
            mCustomAdapter = new InstallAppDetailsChildAdapter(context, appInfoModelList, this);
            mRecyclerView.setAdapter(mCustomAdapter);
            mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            mCustomAdapter.notifyDataSetChanged();
            mRecyclerView.setVisibility(View.VISIBLE);
        }else{
            mRecyclerView.setVisibility(View.GONE);
        }
    }

    private void updateInstallationStatus(){
        setUpAppInfoModelListAdapter(appsStatus);
    }

    @Override
    public void onResume() {
        super.onResume();
        DLog.log(TAG+" enter onResume ");
        if (AppMigrateUtils.appInfoList.size() != 0 && selectedAppPosition != -1) {
            AppInfoModel appInfoModelLocal = null;
            for (AppInfoModel appInfoModel : AppMigrateUtils.appInfoList) {
                if (appInfoModel.getPackageName().equalsIgnoreCase(selectedPackageName)) {
                    appInfoModelLocal = appInfoModel;
                    break;
                }
            }
            if (appInfoModelLocal != null) {
                String appVersonName = getAppVersion(appInfoModelLocal.getPackageName());
                appInfoModelLocal.setLocalInstallationAttempted(true);
                if (appVersonName.equalsIgnoreCase("NotAvailable")) {
                    appInfoModelLocal.setInstallationDone(false);

                    DLog.log(TAG+" Enter onResume appInstall " + appInfoModelLocal.getName() + " not installed");
                } else {
                    appInfoModelLocal.setInstallationDone(true);
                    DLog.log(TAG+" Enter onResume appInstall " + appInfoModelLocal.getName() + " installed");
                }
            }
            updateInstallationStatus();
        }
    }

    @Override
    public void onAppInstallSelected(String filePath, String packageName, int position) {
        // Install App from here
        DLog.log("enter onAppInstallSelected "+filePath+" "+packageName+" "+position);
        selectedAppPosition = position;
        selectedPackageName = packageName;
        file_path = filePath;
        package_name=packageName;
        boolean installFromPlaystore = false;
        int appPostionInList = -1;
        for(AppInfoModel appInfoModel : AppMigrateUtils.appInfoList){
            appPostionInList = appPostionInList+1;
            if(appInfoModel.getPackageName().equalsIgnoreCase(packageName)){
                break;
            }
        }
        AppInfoModel appInfoModel = AppMigrateUtils.appInfoList.get(appPostionInList);
        if(appInfoModel.isLocalInstallationAttempted()){
            installFromPlaystore = true;
        }else{
            installFromPlaystore = false;
        }

        if(installFromPlaystore){
            if(isGoogleAccountPresent()){
                DLog.log(TAG+" Google Account Present Case");
                installApps( filePath, packageName,  true);
            }  else{
                DLog.log(TAG+" Google Account Not Present Case");
                twoButtonDialog(this.getActivity(), getString(R.string.str_google_signin_alert_title), getString(R.string.str_google_signin_alert_message), new String[]{getString(R.string.cancel), getString(R.string.ok)},
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                DLog.log("AppsInstallActivity Google Account Sign-in Cancelled by user");
//                                finish();
                            }
                        },
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                DLog.log("AppsInstallActivity Google Account Sign-in Selected by user");

                                // Configure sign-in to request the user's ID, email address, and basic
// profile. ID and basic profile are included in DEFAULT_SIGN_IN.
                                GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                        .requestEmail()
                                        .build();
                                // Build a GoogleSignInClient with the options specified by gso.
                                mGoogleSignInClient = GoogleSignIn.getClient(emGlobals.getmContext(), gso);
                                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                                startActivityForResult(signInIntent, RC_SIGN_IN);
                            }
                        });

            }
        }else{
            installApps( filePath, packageName,  false);
        }
    }

    public void twoButtonDialog(Activity activity, String title, String message, String[] buttonText, final View.OnClickListener firstButton, final View.OnClickListener secondButton) {
        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.cutom_alert_dialog_new);
        dialog.setCancelable(false);
        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        TextView BL_alert_head = (TextView) dialog
                .findViewById(R.id.BL_alert_head);
        TextView BL_alert_text = (TextView) dialog
                .findViewById(R.id.BL_alert_text);
        BL_alert_head.setText(title);
        BL_alert_text.setText(message);
        Button BL_alert_ok = (Button) dialog.findViewById(R.id.BL_alert_yes);
        Button BL_alert_no = (Button) dialog.findViewById(R.id.BL_alert_no);
        /*Buttons as per requirements*/
        String firstButtonText = buttonText[0];
        String SecondButtonText = null;
        if(buttonText.length>1){
            SecondButtonText = buttonText[1];
        }
        BL_alert_ok.setVisibility(View.GONE);
        BL_alert_no.setVisibility(View.GONE);
        if(!TextUtils.isEmpty(firstButtonText)){
            BL_alert_ok.setText(firstButtonText);
            BL_alert_ok.setVisibility(View.VISIBLE);
        }
        if(!TextUtils.isEmpty(SecondButtonText)){
            BL_alert_no.setText(SecondButtonText);
            BL_alert_no.setVisibility(View.VISIBLE);
        }
        BL_alert_ok.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dialog.dismiss();
                if(firstButton!=null){
                    firstButton.onClick(v);
                }
            }
        });
        BL_alert_no.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dialog.dismiss();
                if(secondButton!=null){
                    secondButton.onClick(v);
                }
            }
        });

        dialog.show();
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            DLog.log("enter AppsInstallActivity onActivityResult RC_SIGN_IN");
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            Task<GoogleSignInAccount> completedTask = GoogleSignIn.getSignedInAccountFromIntent(data);
//            handleSignInResult(task);
            try {
                GoogleSignInAccount account = completedTask.getResult(ApiException.class);
                if(account !=null){
                    DLog.log("enter AppsInstallActivity onActivityResult RC_SIGN_IN account signin is done into email : "+account.getEmail());
                    Toast.makeText(context.getApplicationContext(),"Present",Toast.LENGTH_LONG).show();
                    installApps( file_path, package_name,  true);
                }
            } catch (ApiException e) {
                e.printStackTrace();
                DLog.log( "signInResult:failed code=" + e.getStatusCode());
            }

        }
    }
}
