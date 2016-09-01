package com.example.yardenbourg.mindhivepodcast;

/**
 * Created by Yardenbourg on 13/08/16.
 */

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import java.util.ArrayList;

/**
 * Custom Adapter for the ViewPager
 */
public class ViewPagerAdapter extends FragmentPagerAdapter {

    final int PAGE_COUNT = 2;
    private ArrayList<String> titlesList = new ArrayList<>();
    private ArrayList<Fragment> fragmentList = new ArrayList<>();

    public ViewPagerAdapter(FragmentManager manager) {
        super(manager);
    }

    @Override
    public Fragment getItem(int position) {
        return fragmentList.get(position);
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return titlesList.get(position);
    }

    /**
     * Returning the size of the fragmentList, in this case, 2.
     * @return
     */
    @Override
    public int getCount() {
        return PAGE_COUNT;
    }

    /**
     * Adding the Fragment and it's title to their respective ArrayLists.
     * @param fragment
     * @param title
     */
    public void addFragment(Fragment fragment, String title) {

        fragmentList.add(fragment);
        titlesList.add(title);
    }
}
