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

package com.brewcrewfoo.performance.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.IBinder;
import android.preference.PreferenceManager;
import com.brewcrewfoo.performance.R;
import com.brewcrewfoo.performance.fragments.VoltageControlSettings;
import com.brewcrewfoo.performance.util.CMDProcessor;
import com.brewcrewfoo.performance.util.Constants;
import com.brewcrewfoo.performance.util.Helpers;
import com.brewcrewfoo.performance.util.Voltage;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class BootService extends Service implements Constants {

    public static boolean servicesStarted = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            stopSelf();
        }
        new BootWorker(this).execute();
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    class BootWorker extends AsyncTask<Void, Void, Void> {

        Context c;

        public BootWorker(Context c) {
            this.c = c;
        }

        @SuppressWarnings("deprecation")
        @Override
        protected Void doInBackground(Void... args) {
            SharedPreferences preferences = PreferenceManager
                    .getDefaultSharedPreferences(c);

            if (preferences.getBoolean(CPU_SOB, false)) {
                final String max = preferences.getString(PREF_MAX_CPU, null);
                final String min = preferences.getString(PREF_MIN_CPU, null);
                final String gov = preferences.getString(PREF_GOV, null);
                final String io = preferences.getString(PREF_IO, null);

                if (max != null || min != null || gov != null) {
                    boolean mIsTegra3 = new File(TEGRA_MAX_FREQ_PATH).exists();

                    for (int i = 0; i < Helpers.getNumOfCpus(); i++) {
                        if (max != null) {
                            new CMDProcessor().su.runWaitFor("busybox echo "
                                    + max + " > "
                                    + MAX_FREQ_PATH.replace("cpu0", "cpu" + i));
                        }

                        if (min != null) {
                            new CMDProcessor().su.runWaitFor("busybox echo "
                                    + min + " > "
                                    + MIN_FREQ_PATH.replace("cpu0", "cpu" + i));
                        }

                        if (gov != null) {
                            new CMDProcessor().su.runWaitFor("busybox echo "
                                    + gov + " > "
                                    + GOVERNOR_PATH.replace("cpu0", "cpu" + i));
                        }
                    }

                    if (mIsTegra3 && max != null) {
                        new CMDProcessor().su.runWaitFor("busybox echo " + max
                                + " > " + TEGRA_MAX_FREQ_PATH);
                    }
                }

                if (io != null) {
                    new CMDProcessor().su.runWaitFor("busybox echo " + io
                            + " > " + IO_SCHEDULER_PATH);
                }
            }

            if (preferences.getBoolean(VOLTAGE_SOB, false)) {
                final List<Voltage> volts = VoltageControlSettings
                        .getVolts(preferences);
                final StringBuilder sb = new StringBuilder();
                for (final Voltage volt : volts) {
                    sb.append(volt.getSavedMV() + " ");
                }
                for (int i = 0; i < Helpers.getNumOfCpus(); i++) {
                    new CMDProcessor().su.runWaitFor("busybox echo "
                            + sb.toString()
                            + " > "
                            + Helpers.getVoltagePath().replace("cpu0",
                            "cpu" + i));
                }
            }
            boolean FChargeOn = preferences.getBoolean(PREF_FASTCHARGE, false);
            try {
                File fastcharge = new File(FASTCHARGE_PATH);
                FileWriter fwriter = new FileWriter(fastcharge);
                BufferedWriter bwriter = new BufferedWriter(fwriter);
                bwriter.write(FChargeOn ? "1" : "0");
                bwriter.close();
                Intent i = new Intent();
                i.setAction(INTENT_ACTION_FASTCHARGE);
                c.sendBroadcast(i);
            } catch (IOException e) {
            }

            if (FChargeOn) {
                // add notification to warn user they can only charge
                CharSequence contentTitle = c
                        .getText(R.string.fast_charge_notification_title);
                CharSequence contentText = c
                        .getText(R.string.fast_charge_notification_message);

                Notification n = new Notification.Builder(c)
                        .setAutoCancel(true).setContentTitle(contentTitle)
                        .setContentText(contentText)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setWhen(System.currentTimeMillis()).getNotification();

                NotificationManager nm = (NotificationManager) getApplicationContext()
                        .getSystemService(Context.NOTIFICATION_SERVICE);
                nm.notify(1337, n);
            }

            if (preferences.getBoolean(PREF_MINFREE_BOOT, false)) {
                final String values = preferences.getString(PREF_MINFREE, null);
                if (!values.equals(null)) {
                    new CMDProcessor().su.runWaitFor("busybox echo " + values
                            + " > " + MINFREE_PATH);
                }
            }

            if (preferences.getBoolean(PREF_READ_AHEAD_BOOT, false)) {
                final String values = preferences.getString(PREF_READ_AHEAD,
                        null);
                if (!values.equals(null)) {
                    new CMDProcessor().su.runWaitFor("busybox echo " + values
                            + " > " + READ_AHEAD_PATH);
                }
            }

            if (preferences.getBoolean(VM_SOB, false)) {
                new CMDProcessor().su.runWaitFor("busybox echo "
                        + preferences.getInt(PREF_DIRTY_RATIO,
                        Integer.parseInt(Helpers
                                .readOneLine(DIRTY_RATIO_PATH)))
                        + " > " + DIRTY_RATIO_PATH);
                new CMDProcessor().su.runWaitFor("busybox echo "
                        + preferences.getInt(PREF_DIRTY_BACKGROUND, Integer
                        .parseInt(Helpers
                                .readOneLine(DIRTY_BACKGROUND_PATH)))
                        + " > " + DIRTY_BACKGROUND_PATH);
                new CMDProcessor().su.runWaitFor("busybox echo "
                        + preferences.getInt(PREF_DIRTY_EXPIRE, Integer
                        .parseInt(Helpers
                                .readOneLine(DIRTY_EXPIRE_PATH)))
                        + " > " + DIRTY_EXPIRE_PATH);
                new CMDProcessor().su.runWaitFor("busybox echo "
                        + preferences.getInt(PREF_DIRTY_WRITEBACK, Integer
                        .parseInt(Helpers
                                .readOneLine(DIRTY_WRITEBACK_PATH)))
                        + " > " + DIRTY_WRITEBACK_PATH);
                new CMDProcessor().su.runWaitFor("busybox echo "
                        + preferences.getInt(PREF_MIN_FREE_KB, Integer
                        .parseInt(Helpers.readOneLine(MIN_FREE_PATH)))
                        + " > " + MIN_FREE_PATH);
                new CMDProcessor().su
                        .runWaitFor("busybox echo "
                                + preferences.getInt(PREF_OVERCOMMIT, Integer
                                .parseInt(Helpers
                                        .readOneLine(OVERCOMMIT_PATH)))
                                + " > " + OVERCOMMIT_PATH);
                new CMDProcessor().su
                        .runWaitFor("busybox echo "
                                + preferences.getInt(PREF_SWAPPINESS, Integer
                                .parseInt(Helpers
                                        .readOneLine(SWAPPINESS_PATH)))
                                + " > " + SWAPPINESS_PATH);
                new CMDProcessor().su.runWaitFor("busybox echo "
                        + preferences.getInt(PREF_VFS, Integer.parseInt(Helpers
                        .readOneLine(VFS_CACHE_PRESSURE_PATH))) + " > "
                        + VFS_CACHE_PRESSURE_PATH);
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            servicesStarted = true;
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
