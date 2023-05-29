package com.pervacio.wds.custom.appsinstall;




import android.content.Context;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import static com.pervacio.wds.custom.appsinstall.AppInstallConstants.categoryList;

/**
 * Created by Satyanarayana Chidurala on 22/07/2021.
 */
public class TabsPagerAdapter extends FragmentPagerAdapter {
    Context context;
    public TabsPagerAdapter(FragmentManager fm, Context context) {
        super(fm);
        this.context = context;
    }
    public TabsPagerAdapter(FragmentManager fm/*, Context context*/) {
        super(fm);
//        this.context = context;
    }
    public static int currentItem = 0;
    @Override
    public Fragment getItem(int index) {
        return new InstallAppsFragment(context, categoryList[index]);


        /*switch (index) {
            case 0:
//                return new TestFragment();
                return new InstallAppsFragment(context, AppInstallationsCategory.AppsStatus.FAILED);
            case 1:
//                return new CategoriesFragment();
                return new InstallAppsFragment(context, AppInstallationsCategory.AppsStatus.PENDING);
            case 2:
//                return new HistoryFragment();
                return new InstallAppsFragment(context, AppInstallationsCategory.AppsStatus.INSTALLED);
        }*/
//        return null;
    }

    @Override
    public int getCount() {
        // get item count - equal to number of tabs
//        return ProductFlowUtil.isCountryGermany()?2:3;
        return 3;
    }

 /*   @Override
    public int getItemPosition(Object object) {
        // POSITION_NONE makes it possible to reload the PagerAdapter
//        viewPager.getAdapter().notifyDataSetChanged();
        return POSITION_NONE;
    }*/

}