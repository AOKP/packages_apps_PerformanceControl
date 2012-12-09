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

import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.*;
import android.widget.*;
import android.widget.AdapterView.OnItemSelectedListener;
import com.brewcrewfoo.performance.R;
import com.brewcrewfoo.performance.activities.PCSettings;
import com.brewcrewfoo.performance.util.CMDProcessor;
import com.brewcrewfoo.performance.util.Constants;
import com.brewcrewfoo.performance.util.Helpers;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

public class CPUSettings extends Fragment implements
        SeekBar.OnSeekBarChangeListener, Constants {

    private SeekBar mMaxSlider;
    private SeekBar mMinSlider;
    private Spinner mGovernor;
    private Spinner mIo;
    private Switch mSetOnBoot;
    private TextView mCurFreq;
    private TextView mMaxSpeedText;
    private TextView mMinSpeedText;
    private String[] mAvailableFrequencies;
    private String[] mAvailableGovernors;
    private String[] mAvailableIo;

    private String mMaxFreqSetting;
    private String mMinFreqSetting;
    private String mCurrentGovernor;
    private String mCurrentIo;
    private String mCurMaxSpeed;
    private String mCurMinSpeed;

    private CurCPUThread mCurCPUThread;
    private SharedPreferences mPreferences;

    private boolean mIsTegra3 = false;
    private int mFrequenciesNum;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPreferences = PreferenceManager
                .getDefaultSharedPreferences(getActivity());

        mIsTegra3 = new File(TEGRA_MAX_FREQ_PATH).exists();
        mAvailableFrequencies = new String[0];

        String availableFrequenciesLine = Helpers.readOneLine(STEPS_PATH);
        if (availableFrequenciesLine != null) {
            mAvailableFrequencies = availableFrequenciesLine.split(" ");
            Arrays.sort(mAvailableFrequencies, new Comparator<String>() {
                @Override
                public int compare(String object1, String object2) {
                    return Integer.valueOf(object1).compareTo(
                            Integer.valueOf(object2));
                }
            });
        }

        mFrequenciesNum = mAvailableFrequencies.length - 1;
        mAvailableGovernors = Helpers.readOneLine(GOVERNORS_LIST_PATH).split(
                " ");
        mAvailableIo = Helpers.getAvailableIOSchedulers();

        mCurrentGovernor = Helpers.readOneLine(GOVERNOR_PATH);
        mCurrentIo = Helpers.getIOScheduler();
        mCurMaxSpeed = Helpers.readOneLine(MAX_FREQ_PATH);
        mCurMinSpeed = Helpers.readOneLine(MIN_FREQ_PATH);

        if (mIsTegra3) {
            String curTegraMaxSpeed = Helpers.readOneLine(TEGRA_MAX_FREQ_PATH);
            int curTegraMax = 0;
            try {
                curTegraMax = Integer.parseInt(curTegraMaxSpeed);
                if (curTegraMax > 0) {
                    mCurMaxSpeed = Integer.toString(curTegraMax);
                }
            } catch (NumberFormatException ex) {
                curTegraMax = 0;
            }
        }

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup root,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.cpu_settings, root, false);

        mCurFreq = (TextView) view.findViewById(R.id.current_speed);

        mMaxSlider = (SeekBar) view.findViewById(R.id.max_slider);
        mMaxSlider.setMax(mFrequenciesNum);
        mMaxSpeedText = (TextView) view.findViewById(R.id.max_speed_text);
        mMaxSpeedText.setText(Helpers.toMHz(mCurMaxSpeed));
        mMaxSlider.setProgress(Arrays.asList(mAvailableFrequencies).indexOf(
                mCurMaxSpeed));
        mMaxSlider.setOnSeekBarChangeListener(this);

        mMinSlider = (SeekBar) view.findViewById(R.id.min_slider);
        mMinSlider.setMax(mFrequenciesNum);
        mMinSpeedText = (TextView) view.findViewById(R.id.min_speed_text);
        mMinSpeedText.setText(Helpers.toMHz(mCurMinSpeed));
        mMinSlider.setProgress(Arrays.asList(mAvailableFrequencies).indexOf(
                mCurMinSpeed));
        mMinSlider.setOnSeekBarChangeListener(this);

        mGovernor = (Spinner) view.findViewById(R.id.pref_governor);
        ArrayAdapter<CharSequence> governorAdapter = new ArrayAdapter<CharSequence>(
                getActivity(), android.R.layout.simple_spinner_item);
        governorAdapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        for (int i = 0; i < mAvailableGovernors.length; i++) {
            governorAdapter.add(mAvailableGovernors[i]);
        }
        mGovernor.setAdapter(governorAdapter);
        mGovernor.setSelection(Arrays.asList(mAvailableGovernors).indexOf(
                mCurrentGovernor));
        mGovernor.post(new Runnable() {
            public void run() {
                mGovernor.setOnItemSelectedListener(new GovListener());
            }
        });

        mIo = (Spinner) view.findViewById(R.id.pref_io);
        ArrayAdapter<CharSequence> ioAdapter = new ArrayAdapter<CharSequence>(
                getActivity(), android.R.layout.simple_spinner_item);
        ioAdapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        for (int i = 0; i < mAvailableIo.length; i++) {
            ioAdapter.add(mAvailableIo[i]);
        }
        mIo.setAdapter(ioAdapter);
        mIo.setSelection(Arrays.asList(mAvailableIo).indexOf(mCurrentIo));
        mIo.post(new Runnable() {
            public void run() {
                mIo.setOnItemSelectedListener(new IOListener());
            }
        });

        mSetOnBoot = (Switch) view.findViewById(R.id.cpu_sob);
        mSetOnBoot.setChecked(mPreferences.getBoolean(CPU_SOB, false));
        mSetOnBoot
                .setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton v,
                                                 boolean checked) {
                        final SharedPreferences.Editor editor = mPreferences
                                .edit();
                        editor.putBoolean(CPU_SOB, checked);
                        editor.commit();
                    }
                });

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.cpu_settings_menu, menu);
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
    public void onProgressChanged(SeekBar seekBar, int progress,
                                  boolean fromUser) {
        if (fromUser) {
            if (seekBar.getId() == R.id.max_slider) {
                setMaxSpeed(seekBar, progress);
            } else if (seekBar.getId() == R.id.min_slider) {
                setMinSpeed(seekBar, progress);
            }
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        // we have a break now, write the values..

        for (int i = 0; i < Helpers.getNumOfCpus(); i++) {
            new CMDProcessor().su.runWaitFor("busybox echo " + mMaxFreqSetting
                    + " > " + MAX_FREQ_PATH.replace("cpu0", "cpu" + i));
            new CMDProcessor().su.runWaitFor("busybox echo " + mMinFreqSetting
                    + " > " + MIN_FREQ_PATH.replace("cpu0", "cpu" + i));
        }

        if (mIsTegra3) {
            new CMDProcessor().su.runWaitFor("busybox echo " + mMaxFreqSetting
                    + " > " + TEGRA_MAX_FREQ_PATH);
        }
    }

    public class GovListener implements OnItemSelectedListener {
        public void onItemSelected(AdapterView<?> parent, View view, int pos,
                                   long id) {
            String selected = parent.getItemAtPosition(pos).toString();

            // do this on all cpu's since MSM can have different governors on
            // each cpu
            // and it doesn't hurt other devices to do it
            for (int i = 0; i < Helpers.getNumOfCpus(); i++) {
                new CMDProcessor().su.runWaitFor("busybox echo " + selected
                        + " > " + GOVERNOR_PATH.replace("cpu0", "cpu" + i));
            }
            updateSharedPrefs(PREF_GOV, selected);
        }

        public void onNothingSelected(AdapterView<?> parent) {
            // Do nothing.
        }
    }

    public class IOListener implements OnItemSelectedListener {
        public void onItemSelected(AdapterView<?> parent, View view, int pos,
                                   long id) {
            String selected = parent.getItemAtPosition(pos).toString();
            new CMDProcessor().su.runWaitFor("busybox echo " + selected + " > "
                    + IO_SCHEDULER_PATH);
            updateSharedPrefs(PREF_IO, selected);
        }

        public void onNothingSelected(AdapterView<?> parent) {
            // Do nothing.
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mCurCPUThread == null) {
            mCurCPUThread = new CurCPUThread();
            mCurCPUThread.start();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Helpers.updateAppWidget(getActivity());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mCurCPUThread != null) {
            if (mCurCPUThread.isAlive()) {
                mCurCPUThread.interrupt();
                try {
                    mCurCPUThread.join();
                } catch (InterruptedException e) {
                }
            }
        }
    }

    public void setMaxSpeed(SeekBar seekBar, int progress) {
        String current = "";
        current = mAvailableFrequencies[progress];
        int minSliderProgress = mMinSlider.getProgress();
        if (progress <= minSliderProgress) {
            mMinSlider.setProgress(progress);
            mMinSpeedText.setText(Helpers.toMHz(current));
            mMinFreqSetting = current;
        }
        mMaxSpeedText.setText(Helpers.toMHz(current));
        mMaxFreqSetting = current;
        updateSharedPrefs(PREF_MAX_CPU, current);
    }

    public void setMinSpeed(SeekBar seekBar, int progress) {
        String current = "";
        current = mAvailableFrequencies[progress];
        int maxSliderProgress = mMaxSlider.getProgress();
        if (progress >= maxSliderProgress) {
            mMaxSlider.setProgress(progress);
            mMaxSpeedText.setText(Helpers.toMHz(current));
            mMaxFreqSetting = current;
        }
        mMinSpeedText.setText(Helpers.toMHz(current));
        mMinFreqSetting = current;
        updateSharedPrefs(PREF_MIN_CPU, current);
    }

    protected class CurCPUThread extends Thread {
        private boolean mInterrupt = false;

        public void interrupt() {
            mInterrupt = true;
        }

        @Override
        public void run() {
            try {
                while (!mInterrupt) {
                    sleep(500);
                    final String curFreq = Helpers.readOneLine(CUR_CPU_PATH);
                    mCurCPUHandler.sendMessage(mCurCPUHandler.obtainMessage(0,
                            curFreq));
                }
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    ;

    protected Handler mCurCPUHandler = new Handler() {
        public void handleMessage(Message msg) {
            mCurFreq.setText(Helpers
                    .toMHz((String) msg.obj));
        }
    };

    private void updateSharedPrefs(String var, String value) {
        final SharedPreferences.Editor editor = mPreferences.edit();
        editor.putString(var, value);
        editor.commit();
    }
}
