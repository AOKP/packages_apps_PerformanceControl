/*Performance Control - An Android CPU Control application
Copyright (C) 2012  James Roberts

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package com.aokp.performance.activities;

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.Fragment;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;

import com.aokp.performance.R;
import com.aokp.performance.fragments.CPUInfo;
import com.aokp.performance.fragments.CPUSettings;
import com.aokp.performance.fragments.Advanced;
import com.aokp.performance.fragments.TimeInState;
import com.aokp.performance.fragments.VoltageControlSettings;
import com.aokp.performance.util.Constants;

public class MainActivity extends Activity implements Constants {

    ViewPager viewPager;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        ActionBar actionBar = getActionBar();

        ColorDrawable actionBarDrawable = new ColorDrawable();
        actionBarDrawable.setColor(getResources().getColor(R.color.pc_gray));
        actionBar.setBackgroundDrawable(actionBarDrawable);

        viewPager = (ViewPager) findViewById(R.id.viewpager);
        TitleAdapter titleAdapter = new TitleAdapter(getFragmentManager());
        viewPager.setAdapter(titleAdapter);
        viewPager.setCurrentItem(0);

        PagerTabStrip pagerTabStrip = (PagerTabStrip) findViewById(R.id.pagerTabStrip);
        pagerTabStrip.setBackgroundColor(getResources().getColor(R.color.pc_light_gray));
        pagerTabStrip.setTabIndicatorColor(getResources().getColor(R.color.pc_blue));
        pagerTabStrip.setDrawFullUnderline(true);
        pagerTabStrip.setTextColor(getResources().getColor(R.color.pc_white));
    }
    
    class TitleAdapter extends FragmentPagerAdapter {
        private String titles[] = new String[] { "CPU SETTINGS", "VOLTAGE SETTINGS",
                "ADVANCED SETTINGS", "TIME IN STATE", "CPU INFO" };
        private Fragment frags[] = new Fragment[titles.length];
        
        public TitleAdapter(FragmentManager fm) {
            super(fm);
            frags[0] = new CPUSettings();
            frags[1] = new VoltageControlSettings();
            frags[2] = new Advanced();
            frags[3] = new TimeInState();
            frags[4] = new CPUInfo();
        }
        
        @Override
        public CharSequence getPageTitle(int position) {
            return titles[position];
        }
        
        @Override
        public Fragment getItem(int position) {
            return frags[position];
        }
        
        @Override
        public int getCount() {
            return frags.length;
        }
    }
}
