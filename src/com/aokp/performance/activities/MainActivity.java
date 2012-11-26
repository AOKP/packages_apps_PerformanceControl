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
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.aokp.performance.R;
import com.aokp.performance.fragments.CPUInfo;
import com.aokp.performance.fragments.CPUSettings;
import com.aokp.performance.fragments.Advanced;
import com.aokp.performance.fragments.TimeInState;
import com.aokp.performance.fragments.VoltageControlSettings;
import com.aokp.performance.util.Constants;
import com.aokp.performance.util.Helpers;

public class MainActivity extends Activity implements Constants {

    SharedPreferences mPreferences;
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
        
        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        
        // If this is the first launch of the application. Check for root.
        boolean firstrun = mPreferences.getBoolean("firstrun", true);
        // Continue to bug the user that options will not work.
        boolean rootWasCanceled = mPreferences.getBoolean("rootcanceled", false);

        // Don't bother AOKP users ;)
        PackageManager pm = getPackageManager();
        boolean rcInstalled = false;
        try {
            pm.getPackageInfo("com.aokp.romcontrol", PackageManager.GET_ACTIVITIES);
            rcInstalled = true;
        } catch (PackageManager.NameNotFoundException e) {
            rcInstalled = false;
        }
        
        // Now that we've decided what to do. Launch the appropriate dialog
        if (firstrun || rootWasCanceled) {
            SharedPreferences.Editor e = mPreferences.edit();
            e.putBoolean("firstrun", false);
            e.commit();
            if (rcInstalled) {
                Helpers.checkSu();
            } else {
                launchFirstRunDialog();
            }
        }
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
    
    private void launchFirstRunDialog() {
        String cancel = getString(R.string.cancel);
        String ok = getString(R.string.ok);
        String title = getString(R.string.first_run_title);
        final String failedTitle = getString(R.string.su_failed_title);
        LayoutInflater factory = LayoutInflater.from(this);
        final View firstRunDialog = factory.inflate(R.layout.su_dialog, null);
        TextView tv = (TextView) firstRunDialog.findViewById(R.id.message);
        tv.setText(R.string.first_run_message);
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(firstRunDialog)
                .setNegativeButton(cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String message = getString(R.string.su_cancel_message);
                SharedPreferences.Editor e = mPreferences.edit();
                e.putBoolean("rootcanceled", true);
                e.commit();
                suResultDialog(failedTitle, message);
            }
        })
        .setPositiveButton(ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                boolean canSu = Helpers.checkSu();
                boolean canBb = Helpers.checkBusybox();
                if (canSu && canBb) {
                    String title = getString(R.string.su_success_title);
                    String message = getString(R.string.su_success_message);
                    SharedPreferences.Editor e = mPreferences.edit();
                    e.putBoolean("rootcanceled", false);
                    e.commit();
                    suResultDialog(title, message);
                }
                if (!canSu || !canBb) {
                    String message = getString(R.string.su_failed_su_or_busybox);
                    SharedPreferences.Editor e = mPreferences.edit();
                    e.putBoolean("rootcanceled", true);
                    e.commit();
                    suResultDialog(failedTitle, message);
                }
            }
        })
        .create()
        .show();
    }
    
    private void suResultDialog(String title, String message) {
        String ok = getString(R.string.ok);
        LayoutInflater factory = LayoutInflater.from(this);
        final View suResultDialog = factory.inflate(R.layout.su_dialog, null);
        TextView tv = (TextView) suResultDialog.findViewById(R.id.message);
        tv.setText(message);
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(suResultDialog)
                .setCancelable(false)
                .setPositiveButton(ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        })
        .create()
        .show();
    }
}
