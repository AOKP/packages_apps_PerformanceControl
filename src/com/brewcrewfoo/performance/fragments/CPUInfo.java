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
import android.os.Bundle;
import android.view.*;
import android.widget.TextView;
import com.brewcrewfoo.performance.R;
import com.brewcrewfoo.performance.activities.PCSettings;
import com.brewcrewfoo.performance.util.Constants;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class CPUInfo extends Fragment implements Constants {

    private TextView mKernelInfo;
    private TextView mCPUInfo;
    private TextView mMemInfo;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup root,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.cpu_info, root, false);
        mKernelInfo = (TextView) view.findViewById(R.id.kernel_info);
        mCPUInfo = (TextView) view.findViewById(R.id.cpu_info);
        mMemInfo = (TextView) view.findViewById(R.id.mem_info);
        updateData();
        return view;
    }

    public void updateData() {
        String newInfo = "";
        mKernelInfo.setText(newInfo);
        mCPUInfo.setText(newInfo);
        mMemInfo.setText(newInfo);
        readFile(mKernelInfo, KERNEL_INFO_PATH);
        readFile(mCPUInfo, CPU_INFO_PATH);
        readFile(mMemInfo, MEM_INFO_PATH);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.cpu_info_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.refresh) {
            updateData();
        } else if (item.getItemId() == R.id.app_settings) {
            Intent intent = new Intent(getActivity(), PCSettings.class);
            startActivity(intent);
        }
        return true;
    }

    public void readFile(TextView tView, String fName) {
        FileReader fr = null;
        try {
            fr = new FileReader(fName);
            BufferedReader br = new BufferedReader(fr);
            String line = br.readLine();
            while (null != line) {
                tView.append(line);
                tView.append("\n");
                line = br.readLine();
            }
        } catch (IOException ex) {
        } finally {
            if (null != fr) {
                try {
                    fr.close();
                } catch (IOException e) {
                }
            }
        }
    }
}
