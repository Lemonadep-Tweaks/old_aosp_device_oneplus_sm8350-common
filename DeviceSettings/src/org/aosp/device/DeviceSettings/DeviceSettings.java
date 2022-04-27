/*
* Copyright (C) 2016 The OmniROM Project
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 2 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program. If not, see <http://www.gnu.org/licenses/>.
*
*/
package org.aosp.device.DeviceSettings;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.provider.Settings;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragment;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;

import org.aosp.device.DeviceSettings.Doze.DozeSettingsActivity;
import org.aosp.device.DeviceSettings.ModeSwitch.*;
import org.aosp.device.DeviceSettings.Preference.CustomSeekBarPreference;
import org.aosp.device.DeviceSettings.Preference.SwitchPreference;
import org.aosp.device.DeviceSettings.Preference.VibratorStrengthPreference;
import org.aosp.device.DeviceSettings.Services.VolumeService;
import org.aosp.device.DeviceSettings.Utils.*;

public class DeviceSettings extends PreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    public static final String KEY_MUTE_MEDIA = "mute_media";
    public static final String KEY_AUTO_HBM_SWITCH = "auto_hbm";
    public static final String KEY_AUTO_HBM_THRESHOLD = "auto_hbm_threshold";
    public static final String KEY_DC_SWITCH = "dc";
    public static final String KEY_HBM_SWITCH = "hbm";
    public static final String KEY_GAME_SWITCH = "game_mode";
    public static final String KEY_EDGE_TOUCH = "edge_touch";
    public static final String KEY_VIBSTRENGTH = "vib_strength";

    private static final String KEY_ENABLE_DOLBY_ATMOS = "enable_dolby_atmos";
    private static final String PREF_DOZE = "advanced_doze_settings";

    private static final String FILE_LEVEL = "/sys/devices/platform/soc/88c000.i2c/i2c-10/10-005a/leds/vibrator/level";
    public static final String DEFAULT = "3";

    private DolbySwitch mDolbySwitch;
    private Preference mDozeSettings;

    private static ListPreference mNrModeSwitcher;
    private static TwoStatePreference mDCModeSwitch;
    private static TwoStatePreference mHBMModeSwitch;
    private static TwoStatePreference mGameModeSwitch;
    private static TwoStatePreference mEdgeTouchSwitch;
    private static TwoStatePreference mAutoHBMSwitch;
    private static TwoStatePreference mMuteMedia;
    private static TwoStatePreference mEnableDolbyAtmos;

    private VibratorStrengthPreference mVibratorStrengthPreference;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getContext());
        addPreferencesFromResource(R.xml.main);

        mMuteMedia = (TwoStatePreference) findPreference(KEY_MUTE_MEDIA);
        mMuteMedia.setChecked(PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean(DeviceSettings.KEY_MUTE_MEDIA, false));
        mMuteMedia.setOnPreferenceChangeListener(this);

        mAutoHBMSwitch = (TwoStatePreference) findPreference(KEY_AUTO_HBM_SWITCH);
        mAutoHBMSwitch.setChecked(PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean(DeviceSettings.KEY_AUTO_HBM_SWITCH, false));
        mAutoHBMSwitch.setOnPreferenceChangeListener(this);

        mDCModeSwitch = (TwoStatePreference) findPreference(KEY_DC_SWITCH);
        mDCModeSwitch.setEnabled(DCModeSwitch.isSupported());
        mDCModeSwitch.setChecked(DCModeSwitch.isCurrentlyEnabled(this.getContext()));
        mDCModeSwitch.setOnPreferenceChangeListener(new DCModeSwitch());

        mHBMModeSwitch = (TwoStatePreference) findPreference(KEY_HBM_SWITCH);
        mHBMModeSwitch.setEnabled(HBMModeSwitch.isSupported());
        mHBMModeSwitch.setChecked(PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean(DeviceSettings.KEY_HBM_SWITCH, false));
        mHBMModeSwitch.setOnPreferenceChangeListener(this);

        mDolbySwitch = new DolbySwitch(this.getContext());
        mEnableDolbyAtmos = (TwoStatePreference) findPreference(KEY_ENABLE_DOLBY_ATMOS);
        mEnableDolbyAtmos.setChecked(mDolbySwitch.isCurrentlyEnabled());
        mEnableDolbyAtmos.setOnPreferenceChangeListener(this);

        mDozeSettings = (Preference)findPreference(PREF_DOZE);
        mDozeSettings.setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(getActivity().getApplicationContext(), DozeSettingsActivity.class);
            startActivity(intent);
            return true;
        });

        mGameModeSwitch = (TwoStatePreference) findPreference(KEY_GAME_SWITCH);
        if (GameModeSwitch.isSupported()) {
            mGameModeSwitch.setEnabled(true);
        } else {
            mGameModeSwitch.setEnabled(false);
            mGameModeSwitch.setSummary(getString(R.string.unsupported_feature));
        }
        mGameModeSwitch.setChecked(GameModeSwitch.isCurrentlyEnabled(this.getContext()));
        mGameModeSwitch.setOnPreferenceChangeListener(new GameModeSwitch());

        mEdgeTouchSwitch = (TwoStatePreference) findPreference(KEY_EDGE_TOUCH);
        if (EdgeTouchSwitch.isSupported()) {
            mEdgeTouchSwitch.setEnabled(true);
        } else {
            mEdgeTouchSwitch.setEnabled(false);
            mEdgeTouchSwitch.setSummary(getString(R.string.unsupported_feature));
        }
        mEdgeTouchSwitch.setChecked(EdgeTouchSwitch.isCurrentlyEnabled(this.getContext()));
        mEdgeTouchSwitch.setOnPreferenceChangeListener(new EdgeTouchSwitch());

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        mVibratorStrengthPreference =  (VibratorStrengthPreference) findPreference(KEY_VIBSTRENGTH);
        if (Utils.fileWritable(FILE_LEVEL)) {
            mVibratorStrengthPreference.setValue(sharedPrefs.getInt(KEY_VIBSTRENGTH,
                Integer.parseInt(Utils.getFileValue(FILE_LEVEL, DEFAULT))));
            mVibratorStrengthPreference.setOnPreferenceChangeListener(this);
        } else {
            mVibratorStrengthPreference.setEnabled(false);
            mVibratorStrengthPreference.setSummary(getString(R.string.unsupported_feature));
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
          if (preference == mMuteMedia) {
            Boolean enabled = (Boolean) newValue;
            VolumeService.setEnabled(getContext(), enabled);
        } else if (preference == mAutoHBMSwitch) {
            Boolean enabled = (Boolean) newValue;
            SharedPreferences.Editor prefChange = PreferenceManager.getDefaultSharedPreferences(getContext()).edit();
            prefChange.putBoolean(KEY_AUTO_HBM_SWITCH, enabled).commit();
            Utils.enableService(getContext());
        } else if (preference == mHBMModeSwitch) {
            Boolean enabled = (Boolean) newValue;
            Utils.writeValue(HBMModeSwitch.getFile(), enabled ? "5" : "0");
            Intent hbmIntent = new Intent(this.getContext(),
                    org.aosp.device.DeviceSettings.Services.HBMModeService.class);
            if (enabled) {
                this.getContext().startService(hbmIntent);
            } else {
                this.getContext().stopService(hbmIntent);
            }
        } else if (preference == mEnableDolbyAtmos) {
            mDolbySwitch.setEnabled((Boolean) newValue);
        } else if (preference == mVibratorStrengthPreference) {
    	    int value = Integer.parseInt(newValue.toString());
            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
            sharedPrefs.edit().putInt(KEY_VIBSTRENGTH, value).commit();
            Utils.writeValue(FILE_LEVEL, String.valueOf(value));
            VibrationUtils.doHapticFeedback(getContext(), VibrationEffect.EFFECT_CLICK);
        }
        return true;
    }

    public static boolean isHBMModeService(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(DeviceSettings.KEY_HBM_SWITCH, false);
    }

    public static boolean isAUTOHBMEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(DeviceSettings.KEY_AUTO_HBM_SWITCH, false);
    }

    public static void restoreVibStrengthSetting(Context context) {
        if (Utils.fileWritable(FILE_LEVEL)) {
            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
            int value = sharedPrefs.getInt(KEY_VIBSTRENGTH,
                Integer.parseInt(Utils.getFileValue(FILE_LEVEL, DEFAULT)));
            Utils.writeValue(FILE_LEVEL, String.valueOf(value));
        }
    }
}