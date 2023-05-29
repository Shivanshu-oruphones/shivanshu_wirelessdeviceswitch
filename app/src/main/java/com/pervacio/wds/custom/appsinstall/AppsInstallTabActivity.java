package com.pervacio.wds.custom.appsinstall;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.pervacio.wds.R;
import com.pervacio.wds.app.DLog;

import static com.pervacio.wds.custom.appsinstall.AppInstallConstants.appsStatusStringMap;
import static com.pervacio.wds.custom.appsinstall.AppInstallConstants.appsSuggestionStringMap;
import static com.pervacio.wds.custom.appsinstall.InstallAppsUtils.prepareRestoredAppData;
import static com.pervacio.wds.custom.utils.Constants.launchingAppsInstallFirstTime;

/**
 * Created by Satyanarayana Chidurala on 22/07/2021.
 */

public class AppsInstallTabActivity extends AppCompatActivity {
    private static final String TAG = "AppsInstallTabActivity";

    private CustomViewPager viewPager;
    private TabsPagerAdapter mAdapter;
    private TabLayout mTablayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_install_apps_tab);
        DLog.log(TAG+" enter onCreate");
        setTitle(getResources().getString(R.string.app_install_applications_title_str));
        appsStatusStringMap.put(AppInstallationsCategory.AppsStatus.PENDING,getResources().getString(R.string.app_install_pending_apps_title_str));
        appsStatusStringMap.put(AppInstallationsCategory.AppsStatus.FAILED,getResources().getString(R.string.app_install_failed_apps_title_str));
        appsStatusStringMap.put(AppInstallationsCategory.AppsStatus.INSTALLED,getResources().getString(R.string.app_install_installed_apps_title_str));
        appsSuggestionStringMap.put(AppInstallationsCategory.AppsStatus.PENDING,getResources().getString(R.string.app_install_pending_apps_suggestion_str));
        appsSuggestionStringMap.put(AppInstallationsCategory.AppsStatus.FAILED,getResources().getString(R.string.app_install_failed_apps_suggestion_str));
        appsSuggestionStringMap.put(AppInstallationsCategory.AppsStatus.INSTALLED,getResources().getString(R.string.app_install_installed_apps_suggestion_str));


        if(launchingAppsInstallFirstTime){
            prepareRestoredAppData();
            launchingAppsInstallFirstTime=false;
        }

        viewPager = (CustomViewPager) findViewById(R.id.pager);
        mAdapter = new TabsPagerAdapter(getSupportFragmentManager());
        TabsPagerAdapter.currentItem=0;
        viewPager.setAdapter(mAdapter);
        viewPager.setOffscreenPageLimit(3);
        mTablayout = (TabLayout) findViewById(R.id.tab_layout);
        mTablayout.setupWithViewPager(viewPager);
        mTablayout.setTabMode(TabLayout.MODE_FIXED);
//        mTablayout.setSelectedTabIndicatorHeight(5);
//        mTablayout.setSelectedTabIndicatorHeight(3);

//        updateTabs(0);
        mTablayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
                int position = tab.getPosition();
                TabsPagerAdapter.currentItem = position;
                // new added
                refreshTab(tab.getPosition());// create a method

            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
//                    setIconToTab(1, R.drawable.test_deselected);
                }
            }
            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                // on changing the page
                // make respected tab selected
                //  actionBar.setSelectedNavigationItem(position);
                DLog.log(TAG+" enter onPageSelected "+position);
            }
            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2) {
            }
            @Override
            public void onPageScrollStateChanged(int arg0) {
            }
        });

        viewPager.setSwipeEnabled(true);
        setTextsToTab();
        viewPager.setCurrentItem(TabsPagerAdapter.currentItem);

    }

    private void refreshTab(int position) {
        DLog.log(TAG+" enter  refreshTab position "+position);
        InstallAppsFragment installAppsFragment = (InstallAppsFragment) viewPager.getAdapter().instantiateItem(viewPager, viewPager.getCurrentItem());
        installAppsFragment.setUpAppInfoModelListAdapter(AppInstallConstants.categoryList[position]);

    }
    private void setTextsToTab(){
        int position=0;
        TabLayout.Tab tab;
//        for (AppInstallationsCategory.AppsStatus appsStatus : AppInstallConstants.categoryList){
        for (int i =0; i<AppInstallConstants.categoryList.length;i++){
            tab = mTablayout.getTabAt(position);
            if (tab != null) {
                tab.setText(appsStatusStringMap.get(AppInstallConstants.categoryList[position]));
            }
            position++;
        }
    }

    private void setTitle(String title){
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null ){
            supportActionBar.setDisplayHomeAsUpEnabled(true);
            supportActionBar.setDisplayShowHomeEnabled(true);
            supportActionBar.setTitle(title);
            supportActionBar.setHomeAsUpIndicator(R.drawable.back);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem aMenuItem) {
        switch (aMenuItem.getItemId()) {
            // If home icon is clicked return to main Activity
            case android.R.id.home:
//                if (mBackButton!=null && mBackButton.isEnabled())
                onBackPressed();
                break;
            default:
                break;
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        DLog.log(TAG+" enter onBackPressed");
        finish();
    }
}
