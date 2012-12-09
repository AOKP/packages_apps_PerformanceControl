/*
 * Performance Control - An Android CPU Control application Copyright (C) 2012
 * Jared Rummler Copyright (C) 2012 James Roberts
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

import android.app.*;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.SeekBar.OnSeekBarChangeListener;
import com.brewcrewfoo.performance.R;
import com.brewcrewfoo.performance.activities.PCSettings;
import com.brewcrewfoo.performance.util.CMDProcessor;
import com.brewcrewfoo.performance.util.Constants;
import com.brewcrewfoo.performance.util.Helpers;
import com.brewcrewfoo.performance.util.Voltage;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class VoltageControlSettings extends Fragment implements Constants {

    public static final int DIALOG_EDIT_VOLT = 0;
    private List<Voltage> mVoltages;
    private ListAdapter mAdapter;
    private static SharedPreferences mPreferences;
    private Voltage mVoltage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPreferences = PreferenceManager
                .getDefaultSharedPreferences(getActivity());

        mAdapter = new ListAdapter(getActivity());
        mVoltages = getVolts(mPreferences);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup root,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.voltage_settings, root, false);

        final ListView listView = (ListView) view.findViewById(R.id.ListView);
        final Switch setOnBoot = (Switch) view.findViewById(R.id.applyAtBoot);

        if (mVoltages.isEmpty()) {
            ((TextView) view.findViewById(R.id.emptyList))
                    .setVisibility(View.VISIBLE);
            ((LinearLayout) view.findViewById(R.id.BottomBar))
                    .setVisibility(View.GONE);
        }

        setOnBoot.setChecked(mPreferences.getBoolean(VOLTAGE_SOB, false));
        setOnBoot
                .setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView,
                                                 boolean isChecked) {
                        final SharedPreferences.Editor editor = mPreferences
                                .edit();
                        editor.putBoolean(VOLTAGE_SOB, isChecked);
                        editor.commit();
                    }
                });

        ((Button) view.findViewById(R.id.applyBtn))
                .setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View arg0) {
                        final StringBuilder sb = new StringBuilder();
                        for (final Voltage volt : mVoltages) {
                            sb.append(volt.getSavedMV() + " ");
                        }
                        for (int i = 0; i < Helpers.getNumOfCpus(); i++) {
                            new CMDProcessor().su.runWaitFor("busybox echo "
                                    + sb.toString()
                                    + " > "
                                    + Helpers.getVoltagePath().replace("cpu0",
                                    "cpu" + i));
                        }
                        final List<Voltage> volts = getVolts(mPreferences);
                        mVoltages.clear();
                        mVoltages.addAll(volts);
                        mAdapter.notifyDataSetChanged();
                    }
                });

        mAdapter.setListItems(mVoltages);
        listView.setAdapter(mAdapter);
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                mVoltage = mVoltages.get(position);
                showDialog(DIALOG_EDIT_VOLT);
            }
        });

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.voltage_control_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.app_settings) {
            Intent intent = new Intent(getActivity(), PCSettings.class);
            startActivity(intent);
        }
        return true;
    }

    public static List<Voltage> getVolts(final SharedPreferences preferences) {
        final List<Voltage> volts = new ArrayList<Voltage>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(
                    Helpers.getVoltagePath()), 256);
            String line = "";
            while ((line = br.readLine()) != null) {
                final String[] values = line.split("\\s+");
                if (values != null) {
                    if (values.length >= 2) {
                        final String freq = values[0].replace("mhz:", "");
                        final String currentMv = values[1];
                        final String savedMv = preferences.getString(freq,
                                currentMv);
                        final Voltage voltage = new Voltage();
                        voltage.setFreq(freq);
                        voltage.setCurrentMV(currentMv);
                        voltage.setSavedMV(savedMv);
                        volts.add(voltage);
                    }
                }
            }
            br.close();
        } catch (FileNotFoundException e) {
            Log.d(TAG, Helpers.getVoltagePath() + " does not exist");
        } catch (IOException e) {
            Log.d(TAG, "Error reading " + Helpers.getVoltagePath());
        }
        return volts;
    }

    private static final int[] STEPS = new int[]{600, 625, 650, 675, 700,
            725, 750, 775, 800, 825, 850, 875, 900, 925, 950, 975, 1000, 1025,
            1050, 1075, 1100, 1125, 1150, 1175, 1200, 1225, 1250, 1275, 1300,
            1325, 1350, 1375, 1400, 1425, 1450, 1475, 1500, 1525, 1550, 1575,
            1600};

    private static int getNearestStepIndex(final int value) {
        int index = 0;
        for (int i = 0; i < STEPS.length; i++) {
            if (value > STEPS[i]) {
                index++;
            } else {
                break;
            }
        }
        return index;
    }

    protected void showDialog(final int id) {
        AlertDialog dialog = null;
        switch (id) {
            case DIALOG_EDIT_VOLT:
                final LayoutInflater factory = LayoutInflater.from(getActivity());
                final View voltageDialog = factory.inflate(R.layout.voltage_dialog,
                        null);

                final EditText voltageEdit = (EditText) voltageDialog
                        .findViewById(R.id.voltageEdit);
                final SeekBar voltageSeek = (SeekBar) voltageDialog
                        .findViewById(R.id.voltageSeek);
                final TextView voltageMeter = (TextView) voltageDialog
                        .findViewById(R.id.voltageMeter);

                final String savedMv = mVoltage.getSavedMV();
                final int savedVolt = Integer.parseInt(savedMv);
                voltageEdit.setText(savedMv);
                voltageEdit.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void afterTextChanged(Editable arg0) {
                        // TODO Auto-generated method stub
                    }

                    @Override
                    public void beforeTextChanged(CharSequence arg0, int arg1,
                                                  int arg2, int arg3) {
                        // TODO Auto-generated method stub
                    }

                    @Override
                    public void onTextChanged(CharSequence arg0, int arg1,
                                              int arg2, int arg3) {
                        final String text = voltageEdit.getText().toString();
                        int value = 0;
                        try {
                            value = Integer.parseInt(text);
                        } catch (NumberFormatException nfe) {
                            return;
                        }
                        voltageMeter.setText(text + " mV");
                        final int index = getNearestStepIndex(value);
                        voltageSeek.setProgress(index);
                    }

                });

                voltageMeter.setText(savedMv + " mV");
                voltageSeek.setMax(40);
                voltageSeek.setProgress(getNearestStepIndex(savedVolt));
                voltageSeek
                        .setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
                            @Override
                            public void onProgressChanged(SeekBar sb, int progress,
                                                          boolean fromUser) {
                                if (fromUser) {
                                    final String volt = Integer
                                            .toString(STEPS[progress]);
                                    voltageMeter.setText(volt + " mV");
                                    voltageEdit.setText(volt);
                                }
                            }

                            @Override
                            public void onStartTrackingTouch(SeekBar seekBar) {
                                // TODO Auto-generated method stub

                            }

                            @Override
                            public void onStopTrackingTouch(SeekBar seekBar) {
                                // TODO Auto-generated method stub

                            }

                        });

                dialog = new AlertDialog.Builder(getActivity())
                        .setTitle(
                                mVoltage.getFreq()
                                        + getResources().getString(
                                        R.string.ps_volt_mhz_voltage))
                        .setView(voltageDialog)
                        .setPositiveButton(
                                getResources().getString(R.string.ps_volt_save),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                                        int whichButton) {
                                        removeDialog(id);
                                        final String value = voltageEdit.getText()
                                                .toString();
                                        SharedPreferences.Editor editor = mPreferences
                                                .edit();
                                        editor.putString(mVoltage.getFreq(), value);
                                        editor.commit();
                                        mVoltage.setSavedMV(value);
                                        mAdapter.notifyDataSetChanged();
                                    }
                                })
                        .setNegativeButton(null,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                                        int whichButton) {
                                        removeDialog(id);
                                    }
                                }).create();
                break;
            default:
                break;
        }

        if (dialog != null) {
            FragmentManager fm = getActivity().getFragmentManager();
            FragmentTransaction ftr = fm.beginTransaction();
            CustomDialogFragment newFragment = CustomDialogFragment
                    .newInstance(dialog);
            DialogFragment fragmentDialog = (DialogFragment) fm
                    .findFragmentByTag("" + id);
            if (fragmentDialog != null) {
                ftr.remove(fragmentDialog);
                ftr.commit();
            }
            newFragment.show(fm, "" + id);
        }
    }

    protected void removeDialog(int pDialogId) {
        FragmentManager fm = getActivity().getFragmentManager();
        FragmentTransaction ftr = fm.beginTransaction();
        DialogFragment fragmentDialog = null;
        fragmentDialog = (DialogFragment) fm.findFragmentByTag("" + pDialogId);
        if (fragmentDialog != null) {
            FragmentTransaction f = ftr.remove(fragmentDialog);
            f.commit();
        }
    }

    public static class CustomDialogFragment extends DialogFragment {
        private Dialog mDialog;

        public static CustomDialogFragment newInstance(Dialog dialog) {
            CustomDialogFragment frag = new CustomDialogFragment();
            frag.mDialog = dialog;
            return frag;
        }

        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return mDialog;
        }
    }

    public class ListAdapter extends BaseAdapter {
        private LayoutInflater mInflater;
        private List<Voltage> results;

        public ListAdapter(Context context) {
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return results.size();
        }

        @Override
        public Object getItem(int position) {
            return results.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView,
                            ViewGroup parent) {

            final ViewHolder holder;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.list_volt, null);
                holder = new ViewHolder();
                holder.mFreq = (TextView) convertView.findViewById(R.id.Freq);
                holder.mCurrentMV = (TextView) convertView
                        .findViewById(R.id.mVCurrent);
                holder.mSavedMV = (TextView) convertView
                        .findViewById(R.id.mVSaved);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            final Voltage voltage = mVoltages.get(position);
            holder.setFreq(voltage.getFreq());
            holder.setCurrentMV(voltage.getCurrentMv());
            holder.setSavedMV(voltage.getSavedMV());
            return convertView;
        }

        public void setListItems(List<Voltage> mVoltages) {
            results = mVoltages;
        }

        public class ViewHolder {
            private TextView mFreq;
            private TextView mCurrentMV;
            private TextView mSavedMV;

            public void setFreq(final String freq) {
                mFreq.setText(freq + " MHz");
            }

            public void setCurrentMV(final String currentMv) {
                mCurrentMV.setText(getResources().getString(
                        R.string.ps_volt_current_voltage)
                        + currentMv + " mV");
            }

            public void setSavedMV(final String savedMv) {
                mSavedMV.setText(getResources().getString(
                        R.string.ps_volt_setting_to_apply)
                        + savedMv + " mV");
            }
        }
    }
}
