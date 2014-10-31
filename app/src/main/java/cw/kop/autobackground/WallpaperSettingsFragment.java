/*
 * Copyright (C) Winson Chiu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cw.kop.autobackground;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import cw.kop.autobackground.settings.AppSettings;

public class WallpaperSettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {

    private static final String TAG = WallpaperSettingsFragment.class.getName();
    private static final long CONVERT_MILLES_TO_MIN = 60000;
    private SwitchPreference intervalPref;
    private EditTextPreference frameRatePref;
    private Context appContext;
    private PendingIntent pendingIntent;
    private AlarmManager alarmManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_wallpaper);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        appContext = activity;

        Intent intent = new Intent();
        intent.setAction(LiveWallpaperService.UPDATE_WALLPAPER);
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        pendingIntent = PendingIntent.getBroadcast(appContext, 0, intent, 0);

        alarmManager = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
    }

    @Override
    public void onDetach() {
        appContext = null;
        super.onDetach();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        frameRatePref = (EditTextPreference) findPreference("animation_frame_rate");
        intervalPref = (SwitchPreference) findPreference("use_interval");

        return inflater.inflate(R.layout.fragment_list, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        frameRatePref.setSummary(AppSettings.getAnimationFrameRate() + " FPS");

        if (!AppSettings.useAdvanced()) {
            PreferenceCategory wallpaperPreferences = (PreferenceCategory) findPreference("title_wallpaper_settings");

            wallpaperPreferences.removePreference(findPreference("fill_images"));
            wallpaperPreferences.removePreference(findPreference("preserve_context"));
            wallpaperPreferences.removePreference(findPreference("scale_images"));
            wallpaperPreferences.removePreference(findPreference("show_album_art"));

            PreferenceCategory intervalPreferences = (PreferenceCategory) findPreference("title_interval_settings");


            intervalPreferences.removePreference(findPreference("reset_on_manual_cycle"));
            intervalPreferences.removePreference(findPreference("force_interval"));
            intervalPreferences.removePreference(findPreference("when_locked"));

            PreferenceCategory transitionPreferences = (PreferenceCategory) findPreference("title_transition_settings");

            transitionPreferences.removePreference(findPreference("transition_speed"));
            transitionPreferences.removePreference(findPreference("reverse_overshoot"));
            transitionPreferences.removePreference(findPreference("overshoot_intensity"));
            transitionPreferences.removePreference(findPreference("reverse_overshoot_vertical"));
            transitionPreferences.removePreference(findPreference("overshoot_intensity_vertical"));
            transitionPreferences.removePreference(findPreference("reverse_spin_in"));
            transitionPreferences.removePreference(findPreference("spin_in_angle"));
            transitionPreferences.removePreference(findPreference("reverse_spin_out"));
            transitionPreferences.removePreference(findPreference("spin_out_angle"));

            PreferenceCategory animationPreferences = (PreferenceCategory) findPreference("title_animation_settings");

            animationPreferences.removePreference(findPreference("animation_speed"));
            animationPreferences.removePreference(findPreference("animation_speed_vertical"));
            animationPreferences.removePreference(findPreference("scale_animation_speed"));
            animationPreferences.removePreference(frameRatePref);
            animationPreferences.removePreference(findPreference("animation_safety_adv"));

            getPreferenceScreen().removePreference(findPreference("title_gesture_settings"));

        }

        if (AppSettings.getIntervalDuration() > 0) {
            intervalPref.setSummary("Change every " + (AppSettings.getIntervalDuration() / CONVERT_MILLES_TO_MIN) + " minutes");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

    }

    private void showDialogIntervalMenu() {

        AppSettings.setIntervalDuration(0);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(appContext);

        dialogBuilder.setItems(R.array.interval_entry_menu, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                switch (which) {
                    case 0:
                        AppSettings.setIntervalDuration(0);
                        break;
                    case 1:
                        AppSettings.setIntervalDuration(5 * CONVERT_MILLES_TO_MIN);
                        break;
                    case 2:
                        AppSettings.setIntervalDuration(15 * CONVERT_MILLES_TO_MIN);
                        break;
                    case 3:
                        AppSettings.setIntervalDuration(30 * CONVERT_MILLES_TO_MIN);
                        break;
                    case 4:
                        AppSettings.setIntervalDuration(AlarmManager.INTERVAL_HOUR);
                        break;
                    case 5:
                        AppSettings.setIntervalDuration(2 * AlarmManager.INTERVAL_HOUR);
                        break;
                    case 6:
                        AppSettings.setIntervalDuration(6 * AlarmManager.INTERVAL_HOUR);
                        break;
                    case 7:
                        AppSettings.setIntervalDuration(AlarmManager.INTERVAL_HALF_DAY);
                        break;
                    default:
                }

                if (AppSettings.getIntervalDuration() > 0) {
                    intervalPref.setSummary("Change every " + (AppSettings.getIntervalDuration() / CONVERT_MILLES_TO_MIN) + " minutes");
                }
                else {
                    intervalPref.setSummary("Change on return");
                }

                setIntervalAlarm();

            }
        });

        AlertDialog dialog = dialogBuilder.create();

        dialog.show();
    }

    private void showDialogIntervalForInput() {

        AppSettings.setIntervalDuration(0);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(appContext);
        dialogBuilder.setMessage("Update Interval");

        View dialogView = View.inflate(appContext, R.layout.numeric_dialog, null);

        dialogBuilder.setView(dialogView);

        final EditText inputField = (EditText) dialogView.findViewById(R.id.input_field);

        inputField.setHint("Enter number of minutes");

        dialogBuilder.setPositiveButton(R.string.ok_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                int inputValue = Integer.parseInt(inputField.getText().toString());

                if (inputField.getText().toString().equals("") || inputValue < 0) {
                    intervalPref.setChecked(false);
                    return;
                }
                AppSettings.setIntervalDuration(Integer.parseInt(inputField.getText().toString()) * CONVERT_MILLES_TO_MIN);
                setIntervalAlarm();
                if (inputValue > 0) {
                    intervalPref.setSummary("Change every " + (AppSettings.getIntervalDuration() / CONVERT_MILLES_TO_MIN) + " minutes");
                }
                else {
                    intervalPref.setSummary("Change on return");
                }
            }
        });
        dialogBuilder.setNegativeButton(R.string.cancel_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                intervalPref.setChecked(false);
            }
        });

        AlertDialog dialog = dialogBuilder.create();

        dialog.show();
    }

    private void setIntervalAlarm() {

        if (AppSettings.useInterval() && AppSettings.getIntervalDuration() > 0) {
            alarmManager.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis() + AppSettings.getIntervalDuration(), AppSettings.getIntervalDuration(), pendingIntent);
            Log.i("WSD", "Interval Set: " + AppSettings.getIntervalDuration());
        }
        else {
            alarmManager.cancel(pendingIntent);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        if (!((Activity) appContext).isFinishing()) {
            Preference pref = findPreference(key);

            if (pref instanceof EditTextPreference) {
                EditTextPreference editPref = (EditTextPreference) pref;
                if (editPref.getText().equals("0") || editPref.getText().equals("")) {
                    editPref.setText("1");
                }
                editPref.setSummary(editPref.getText());
            }

            if (key.equals("use_interval")) {
                if (AppSettings.useInterval()) {
                    if (AppSettings.useAdvanced()) {
                        showDialogIntervalForInput();
                    }
                    else {
                        showDialogIntervalMenu();
                    }
                }
                else {
                    intervalPref.setSummary("Change image after certain period");
                    setIntervalAlarm();
                }
                Log.i("WSF", "Interval Set: " + AppSettings.useInterval());
            }

            if (key.equals("animation_frame_rate")) {
                frameRatePref.setSummary(AppSettings.getAnimationFrameRate() + " FPS");
            }

            if (key.equals("show_album_art")) {
                Intent musicReceiverIntent = new Intent(appContext, MusicReceiverService.class);

                if (AppSettings.showAlbumArt()) {
                    appContext.startService(musicReceiverIntent);
                    Log.i(TAG, "Starting MusicReceiverService");
                }
                else {
                    appContext.stopService(musicReceiverIntent);
                    Log.i(TAG, "Stopping MusicReceiverService");
                }
            }
        }
    }
}
