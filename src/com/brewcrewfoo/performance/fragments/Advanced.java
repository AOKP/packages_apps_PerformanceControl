/*
 * Performance Control - An Android CPU Control application Copyright (C) 2012
 * James Roberts
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.brewcrewfoo.performance.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.*;
import android.preference.Preference.OnPreferenceChangeListener;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import com.brewcrewfoo.performance.R;
import com.brewcrewfoo.performance.activities.PCSettings;
import com.brewcrewfoo.performance.util.CMDProcessor;
import com.brewcrewfoo.performance.util.Constants;
import com.brewcrewfoo.performance.util.Helpers;

import java.io.File;

public class Advanced extends PreferenceFragment implements
        OnSharedPreferenceChangeListener, OnPreferenceChangeListener, Constants {

    private Preference mDirtyRatio;
    private Preference mDirtyBackground;
    private Preference mDirtyExpireCentisecs;
    private Preference mDirtyWriteback;
    private Preference mMinFreeK;
    private Preference mOvercommit;
    private Preference mSwappiness;
    private Preference mVfs;
    private ListPreference mFreeMem;
    private ListPreference mReadAhead;
    private CheckBoxPreference mFastCharge;

    private SharedPreferences mPreferences;
    protected Context mContext;

    private int mSeekbarProgress;
    private EditText settingText;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPreferences = PreferenceManager
                .getDefaultSharedPreferences(getActivity());
        mPreferences.registerOnSharedPreferenceChangeListener(this);
        addPreferencesFromResource(R.layout.advanced);

        final int minFree = getMinFreeValue();
        final String values[] = getResources().getStringArray(
                R.array.minfree_values);
        String closestValue = mPreferences.getString(PREF_MINFREE, values[0]);

        if (minFree < 37)
            closestValue = values[0];
        else if (minFree < 62)
            closestValue = values[1];
        else if (minFree < 77)
            closestValue = values[2];
        else if (minFree < 90)
            closestValue = values[3];
        else
            closestValue = values[4];

        mFreeMem = (ListPreference) findPreference(PREF_MINFREE);
        mFreeMem.setValue(closestValue);
        mFreeMem.setSummary(getString(R.string.ps_free_memory, minFree + "mb"));

        mReadAhead = (ListPreference) findPreference(PREF_READ_AHEAD);
        mReadAhead.setValue(Helpers.readOneLine(READ_AHEAD_PATH));
        mReadAhead.setSummary(getString(R.string.ps_read_ahead,
                Helpers.readOneLine(READ_AHEAD_PATH) + " kb"));

        mFastCharge = (CheckBoxPreference) findPreference(PREF_FASTCHARGE);
        mFastCharge.setChecked(mPreferences.getBoolean(PREF_FASTCHARGE, false));

        mDirtyRatio = (Preference) findPreference(PREF_DIRTY_RATIO);
        mDirtyBackground = (Preference) findPreference(PREF_DIRTY_BACKGROUND);
        mDirtyExpireCentisecs = (Preference) findPreference(PREF_DIRTY_EXPIRE);
        mDirtyWriteback = (Preference) findPreference(PREF_DIRTY_WRITEBACK);
        mMinFreeK = (Preference) findPreference(PREF_MIN_FREE_KB);
        mOvercommit = (Preference) findPreference(PREF_OVERCOMMIT);
        mSwappiness = (Preference) findPreference(PREF_SWAPPINESS);
        mVfs = (Preference) findPreference(PREF_VFS);

        mDirtyRatio.setSummary(Helpers.readOneLine(DIRTY_RATIO_PATH));
        mDirtyBackground.setSummary(Helpers.readOneLine(DIRTY_BACKGROUND_PATH));
        mDirtyExpireCentisecs
                .setSummary(Helpers.readOneLine(DIRTY_EXPIRE_PATH));
        mDirtyWriteback.setSummary(Helpers.readOneLine(DIRTY_WRITEBACK_PATH));
        mMinFreeK.setSummary(Helpers.readOneLine(MIN_FREE_PATH));
        mOvercommit.setSummary(Helpers.readOneLine(OVERCOMMIT_PATH));
        mSwappiness.setSummary(Helpers.readOneLine(SWAPPINESS_PATH));
        mVfs.setSummary(Helpers.readOneLine(VFS_CACHE_PRESSURE_PATH));

        boolean fChargeExists = new File(FASTCHARGE_PATH)
                .exists();
        if (!fChargeExists) {
            PreferenceCategory kernelCat = (PreferenceCategory) findPreference("kernel");
            getPreferenceScreen().removePreference(kernelCat);
        }

        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.advanced_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.app_settings) {
            Intent intent = new Intent(getActivity(), PCSettings.class);
            startActivity(intent);
        }
        return true;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
                                         Preference preference) {
        String key = preference.getKey();
        if (PREF_FASTCHARGE.equals(key)) {
            if (mPreferences.getBoolean(PREF_FASTCHARGE, false)) {
                String warningMessage = getString(R.string.fast_charge_warning);

                new AlertDialog.Builder(getActivity())
                        .setMessage(warningMessage)
                        .setNegativeButton("Cancel",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int which) {
                                        mPreferences
                                                .edit()
                                                .putBoolean(PREF_FASTCHARGE,
                                                        false).apply();
                                        mFastCharge.setChecked(false);
                                    }
                                })
                        .setPositiveButton("OK",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int which) {
                                        mPreferences
                                                .edit()
                                                .putBoolean(PREF_FASTCHARGE,
                                                        true).apply();
                                        mFastCharge.setChecked(true);
                                    }
                                }).create().show();
                return true;
            }
        } else if (preference == mDirtyRatio) {
            String title = getString(R.string.dirty_ratio_title);
            int currentProgress = Integer.parseInt(Helpers
                    .readOneLine(DIRTY_RATIO_PATH));
            int max = 100;
            openDialog(currentProgress, title, max, mDirtyRatio,
                    DIRTY_RATIO_PATH, PREF_DIRTY_RATIO);
            return true;
        } else if (preference == mDirtyBackground) {
            String title = getString(R.string.dirty_background_title);
            int currentProgress = Integer.parseInt(Helpers
                    .readOneLine(DIRTY_BACKGROUND_PATH));
            int max = 100;
            openDialog(currentProgress, title, max, mDirtyBackground,
                    DIRTY_BACKGROUND_PATH, PREF_DIRTY_BACKGROUND);
            return true;
        } else if (preference == mDirtyExpireCentisecs) {
            String title = getString(R.string.dirty_expire_title);
            int currentProgress = Integer.parseInt(Helpers
                    .readOneLine(DIRTY_EXPIRE_PATH));
            int max = 500;
            openDialog(currentProgress, title, max, mDirtyExpireCentisecs,
                    DIRTY_EXPIRE_PATH, PREF_DIRTY_EXPIRE);
            return true;
        } else if (preference == mDirtyWriteback) {
            String title = getString(R.string.dirty_writeback_title);
            int currentProgress = Integer.parseInt(Helpers
                    .readOneLine(DIRTY_WRITEBACK_PATH));
            int max = 500;
            openDialog(currentProgress, title, max, mDirtyWriteback,
                    DIRTY_WRITEBACK_PATH, PREF_DIRTY_WRITEBACK);
            return true;
        } else if (preference == mMinFreeK) {
            String title = getString(R.string.min_free_title);
            int currentProgress = Integer.parseInt(Helpers
                    .readOneLine(MIN_FREE_PATH));
            int max = 8192;
            openDialog(currentProgress, title, max, mMinFreeK, MIN_FREE_PATH,
                    PREF_MIN_FREE_KB);
            return true;
        } else if (preference == mOvercommit) {
            String title = getString(R.string.overcommit_title);
            int currentProgress = Integer.parseInt(Helpers
                    .readOneLine(OVERCOMMIT_PATH));
            int max = 100;
            openDialog(currentProgress, title, max, mOvercommit,
                    OVERCOMMIT_PATH, PREF_OVERCOMMIT);
            return true;
        } else if (preference == mSwappiness) {
            String title = getString(R.string.swappiness_title);
            int currentProgress = Integer.parseInt(Helpers
                    .readOneLine(SWAPPINESS_PATH));
            int max = 100;
            openDialog(currentProgress, title, max, mSwappiness,
                    SWAPPINESS_PATH, PREF_SWAPPINESS);
            return true;
        } else if (preference == mVfs) {
            String title = getString(R.string.vfs_title);
            int currentProgress = Integer.parseInt(Helpers
                    .readOneLine(VFS_CACHE_PRESSURE_PATH));
            int max = 200;
            openDialog(currentProgress, title, max, mVfs,
                    VFS_CACHE_PRESSURE_PATH, PREF_VFS);
            return true;
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public void onSharedPreferenceChanged(
            final SharedPreferences sharedPreferences, String key) {
        if (key.equals(PREF_MINFREE)) {
            String values = mPreferences.getString(key, null);
            if (!values.equals(null))
                new CMDProcessor().su.runWaitFor("busybox echo " + values
                        + " > " + MINFREE_PATH);
            mFreeMem.setSummary(getString(R.string.ps_free_memory,
                    getMinFreeValue() + "mb"));
        } else if (key.equals(PREF_READ_AHEAD)) {
            String values = mPreferences.getString(key, null);
            if (!values.equals(null))
                new CMDProcessor().su.runWaitFor("busybox echo " + values
                        + " > " + READ_AHEAD_PATH);
            mReadAhead.setSummary(getString(R.string.ps_read_ahead,
                    Helpers.readOneLine(READ_AHEAD_PATH) + " kb"));
        }
    }

    private int getMinFreeValue() {
        int emptyApp = 0;
        String MINFREE_LINE = Helpers.readOneLine(MINFREE_PATH);
        String EMPTY_APP = MINFREE_LINE
                .substring(MINFREE_LINE.lastIndexOf(",") + 1);

        if (!EMPTY_APP.equals(null) || !EMPTY_APP.equals("")) {
            try {
                int mb = Integer.parseInt(EMPTY_APP.trim()) * 4 / 1024;
                emptyApp = (int) Math.ceil(mb);
            } catch (NumberFormatException nfe) {
                Log.i(TAG, "error processing " + EMPTY_APP);
            }
        }
        return emptyApp;
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return false;
    }

    public void openDialog(int currentProgress, String title, final int max,
                           final Preference pref, final String path, final String key) {
        Resources res = getActivity().getResources();
        String cancel = res.getString(R.string.cancel);
        String ok = res.getString(R.string.ok);
        LayoutInflater factory = LayoutInflater.from(getActivity());
        final View alphaDialog = factory.inflate(R.layout.seekbar_dialog, null);

        final SeekBar seekbar = (SeekBar) alphaDialog
                .findViewById(R.id.seek_bar);
        seekbar.setProgress(currentProgress);
        seekbar.setMax(max);

        settingText = (EditText) alphaDialog.findViewById(R.id.setting_text);
        settingText
                .setOnEditorActionListener(new TextView.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId,
                                                  KeyEvent event) {
                        if (actionId == EditorInfo.IME_ACTION_DONE) {
                            int val = Integer.valueOf(settingText.getText()
                                    .toString());
                            seekbar.setProgress(val);
                            return true;
                        }
                        return false;
                    }
                });
        settingText.setText(Integer.toString(currentProgress));
        settingText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    int val = Integer.parseInt(s.toString());
                    if (val > max) {
                        s.replace(0, s.length(), Integer.toString(max), 0, 2);
                    }
                } catch (NumberFormatException ex) {
                }
            }
        });

        OnSeekBarChangeListener seekBarChangeListener = new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekbar, int progress,
                                          boolean fromUser) {
                mSeekbarProgress = seekbar.getProgress();
                settingText.setText(Integer.toString(mSeekbarProgress));
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekbar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekbar) {
            }
        };
        seekbar.setOnSeekBarChangeListener(seekBarChangeListener);

        new AlertDialog.Builder(getActivity())
                .setTitle(title)
                .setView(alphaDialog)
                .setNegativeButton(cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                // nothing
                            }
                        })
                .setPositiveButton(ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int val = Integer.valueOf(settingText.getText()
                                .toString());
                        seekbar.setProgress(val);
                        int newProgress = seekbar.getProgress();
                        pref.setSummary(Integer.toString(newProgress));
                        new CMDProcessor().su.runWaitFor("busybox echo "
                                + newProgress + " > " + path);
                        final SharedPreferences.Editor editor = mPreferences
                                .edit();
                        editor.putInt(key, newProgress);
                        editor.commit();
                    }
                }).create().show();
    }

}
