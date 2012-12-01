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

package com.brewcrewfoo.performance.util;

public class Voltage {
    private String mFreq;
    private String mCurrentMv;
    private String mSavedMv;

    public void setFreq(final String freq) {
        this.mFreq = freq;
    }

    public String getFreq() {
        return mFreq;
    }

    public void setCurrentMV(final String currentMv) {
        this.mCurrentMv = currentMv;
    }

    public String getCurrentMv() {
        return mCurrentMv;
    }

    public void setSavedMV(final String savedMv) {
        this.mSavedMv = savedMv;
    }

    public String getSavedMV() {
        return mSavedMv;
    }
}
