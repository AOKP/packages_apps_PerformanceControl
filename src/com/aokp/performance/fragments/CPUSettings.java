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

package com.aokp.performance.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

import com.aokp.performance.R;
import com.aokp.performance.util.Constants;
import com.aokp.performance.util.CMDProcessor;
import com.aokp.performance.util.Helpers;

public class CPUSettings extends Fragment
        implements SeekBar.OnSeekBarChangeListener, Constants {

    private SeekBar mMaxSlider;
    private SeekBar mMinSlider;
    private Spinner mGovernor;
    private Spinner mIo;
    private Switch mSetOnBoot;
    private TextView mCurFreq;
    private TextView mMaxSpeedText;
    private TextView mMinSpeedText;
    private String[] mAvailableFrequencies;
    private Activity mActivity;

    private String mMaxFreqSetting;
    private String mMinFreqSetting;

    private CurCPUThread mCurCPUThread;
    private SharedPreferences mPreferences;

    private boolean mIsTegra3 = false;
    private int mNumOfCpu = 1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup root,
            Bundle savedInstanceState) {
        mActivity = getActivity();
        View view = inflater.inflate(R.layout.cpu_settings, root, false);

        mIsTegra3 = new File(TEGRA_MAX_FREQ_PATH).exists();

        mPreferences = PreferenceManager.getDefaultSharedPreferences(mActivity);

        mAvailableFrequencies = new String[0];
        String availableFrequenciesLine = Helpers.readOneLine(STEPS_PATH);
        if (availableFrequenciesLine != null) {
            mAvailableFrequencies = availableFrequenciesLine.split(" ");
	    Arrays.sort(mAvailableFrequencies, new Comparator<String>() {
                @Override
                public int compare(String object1, String object2) {
                return Integer.valueOf(object1).compareTo(Integer.valueOf(object2));
                }
            });
        }
        int frequenciesNum = mAvailableFrequencies.length - 1;

        String currentGovernor = Helpers.readOneLine(GOVERNOR_PATH);
        String currentIo = Helpers.getIOScheduler();
        String curMaxSpeed = Helpers.readOneLine(MAX_FREQ_PATH);
        String curMinSpeed = Helpers.readOneLine(MIN_FREQ_PATH);

        if (mIsTegra3) {
            String curTegraMaxSpeed = Helpers.readOneLine(TEGRA_MAX_FREQ_PATH);
            int curTegraMax = 0;
            try {
                curTegraMax = Integer.parseInt(curTegraMaxSpeed);
                if (curTegraMax > 0) {
                    curMaxSpeed = Integer.toString(curTegraMax);
                }
            } catch (NumberFormatException ex) {
                curTegraMax = 0;
            }
        }

        String numOfCpus = Helpers.readOneLine(NUM_OF_CPUS_PATH);
        String[] cpuCount = numOfCpus.split("-");
        if (cpuCount.length > 1) {
            try {
                int cpuStart = Integer.parseInt(cpuCount[0]);
                int cpuEnd = Integer.parseInt(cpuCount[1]);

                mNumOfCpu = cpuEnd - cpuStart + 1;

                if (mNumOfCpu < 0)
                    mNumOfCpu = 1;
            } catch (NumberFormatException ex) {
                mNumOfCpu = 1;
            }
        }

        mCurFreq = (TextView) view.findViewById(R.id.current_speed);

        mMaxSlider = (SeekBar) view.findViewById(R.id.max_slider);
        mMaxSlider.setMax(frequenciesNum);
        mMaxSpeedText = (TextView) view.findViewById(R.id.max_speed_text);
        mMaxSpeedText.setText(toMHz(curMaxSpeed));
        mMaxSlider.setProgress(Arrays.asList(mAvailableFrequencies).indexOf(curMaxSpeed));
        mMaxSlider.setOnSeekBarChangeListener(this);

        mMinSlider = (SeekBar) view.findViewById(R.id.min_slider);
        mMinSlider.setMax(frequenciesNum);
        mMinSpeedText = (TextView) view.findViewById(R.id.min_speed_text);
        mMinSpeedText.setText(toMHz(curMinSpeed));
        mMinSlider.setProgress(Arrays.asList(mAvailableFrequencies).indexOf(curMinSpeed));
        mMinSlider.setOnSeekBarChangeListener(this);

        mGovernor = (Spinner) view.findViewById(R.id.pref_governor);
        String[] availableGovernors = Helpers.readOneLine(GOVERNORS_LIST_PATH).split(" ");
        ArrayAdapter<CharSequence> governorAdapter = new ArrayAdapter<CharSequence> (mActivity,
                android.R.layout.simple_spinner_item);
        governorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        for (int i = 0; i < availableGovernors.length; i++) {
            governorAdapter.add(availableGovernors[i]);
        }
        mGovernor.setAdapter(governorAdapter);
        mGovernor.setSelection(Arrays.asList(availableGovernors).indexOf(currentGovernor));
        mGovernor.post(new Runnable() {
            public void run() {
                mGovernor.setOnItemSelectedListener(new GovListener());
            }
        });
        mIo = (Spinner) view.findViewById(R.id.pref_io);
        String[] availableIo = Helpers.getAvailableIOSchedulers();
        ArrayAdapter<CharSequence> ioAdapter = new ArrayAdapter<CharSequence> (mActivity,
                android.R.layout.simple_spinner_item);
        ioAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        for (int i = 0; i < availableIo.length; i++) {
            ioAdapter.add(availableIo[i]);
        }
        mIo.setAdapter(ioAdapter);
        mIo.setSelection(Arrays.asList(availableIo).indexOf(currentIo));
        mIo.post(new Runnable() {
            public void run() {
                mIo.setOnItemSelectedListener(new IOListener());
            }
        });
        mSetOnBoot = (Switch) view.findViewById(R.id.cpu_sob);
        mSetOnBoot.setChecked(mPreferences.getBoolean(CPU_SOB, false));
        mSetOnBoot.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton v, boolean checked) {
                final SharedPreferences.Editor editor = mPreferences.edit();
                editor.putBoolean(CPU_SOB, checked);
                editor.commit();
            }
        });

        return view;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar,int progress, boolean fromUser) {
        if(fromUser) {
            switch(seekBar.getId()) {
                case R.id.max_slider:
                    setMaxSpeed(seekBar, progress);
                    break;
                case R.id.min_slider:
                    setMinSpeed(seekBar, progress);
                    break;
            }
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        // we have a break now, write the values..

        for (int i = 0; i < mNumOfCpu; i++) {
            new CMDProcessor().su.runWaitFor("busybox echo " + mMaxFreqSetting + " > "
                    + MAX_FREQ_PATH.replace("cpu0", "cpu" + i));
            new CMDProcessor().su.runWaitFor("busybox echo " + mMinFreqSetting + " > "
                    + MIN_FREQ_PATH.replace("cpu0", "cpu" + i));
        }
        
        if (mIsTegra3) {
            new CMDProcessor().su.runWaitFor("busybox echo " + mMaxFreqSetting + " > "
                    + TEGRA_MAX_FREQ_PATH);
        }
    }

    public class GovListener implements OnItemSelectedListener {
        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            String selected = parent.getItemAtPosition(pos).toString();
            
            // do this on all cpu's since MSM can have different governors on each cpu
            //  and it doesn't hurt other devices to do it
            for (int i = 0; i < mNumOfCpu; i++) {
                new CMDProcessor().su.runWaitFor("busybox echo " + selected + " > "
                        + GOVERNOR_PATH.replace("cpu0", "cpu" + i));
            }

            final SharedPreferences.Editor editor = mPreferences.edit();
            editor.putString(PREF_GOV, selected);
            editor.commit();
        }

        public void onNothingSelected(AdapterView<?> parent) {
            // Do nothing.
        }
    }

    public class IOListener implements OnItemSelectedListener {
        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            String selected = parent.getItemAtPosition(pos).toString();
            new CMDProcessor().su.runWaitFor("busybox echo " + selected + " > " + IO_SCHEDULER_PATH);
            final SharedPreferences.Editor editor = mPreferences.edit();
            editor.putString(PREF_IO, selected);
            editor.commit();
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
            mMinSpeedText.setText(toMHz(current));
            mMinFreqSetting = current;
        }
        mMaxSpeedText.setText(toMHz(current));
        mMaxFreqSetting = current;
        final SharedPreferences.Editor editor = mPreferences.edit();
        editor.putString(PREF_MAX_CPU, current);
        editor.commit();
    }

    public void setMinSpeed(SeekBar seekBar, int progress) {
        String current = "";
        current = mAvailableFrequencies[progress];
        int maxSliderProgress = mMaxSlider.getProgress();
        if (progress >= maxSliderProgress) {
            mMaxSlider.setProgress(progress);
            mMaxSpeedText.setText(toMHz(current));
            mMaxFreqSetting = current;
        }
        mMinSpeedText.setText(toMHz(current));
        mMinFreqSetting = current;
        final SharedPreferences.Editor editor = mPreferences.edit();
        editor.putString(PREF_MIN_CPU, current);
        editor.commit();
    }

    private String toMHz(String mhzString) {
        return new StringBuilder().append(Integer.valueOf(mhzString) / 1000).append(" MHz").toString();
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
                    mCurCPUHandler.sendMessage(mCurCPUHandler.obtainMessage(0, curFreq));
                }
            } catch (InterruptedException e) {
                return;
            }
        }
    };

    protected Handler mCurCPUHandler = new Handler() {
        public void handleMessage(Message msg) {
            mCurFreq.setText(toMHz((String) msg.obj));
        }
    };
}
