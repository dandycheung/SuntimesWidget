/**
    Copyright (C) 2014-2023 Forrest Guice
    This file is part of SuntimesWidget.

    SuntimesWidget is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    SuntimesWidget is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with SuntimesWidget.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.forrestguice.suntimeswidget.settings.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.text.style.ImageSpan;
import android.util.Log;

import com.forrestguice.suntimeswidget.R;
import com.forrestguice.suntimeswidget.SuntimesSettingsActivity;
import com.forrestguice.suntimeswidget.SuntimesUtils;
import com.forrestguice.suntimeswidget.alarmclock.AlarmNotifications;
import com.forrestguice.suntimeswidget.alarmclock.AlarmSettings;
import com.forrestguice.suntimeswidget.settings.AppSettings;
import com.forrestguice.suntimeswidget.views.Toast;

import static com.forrestguice.suntimeswidget.settings.AppSettings.findPermission;

/**
 * Alarm Prefs
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class AlarmPrefsFragment extends PreferenceFragment
{
    private static SuntimesUtils utils = new SuntimesUtils();

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        AppSettings.initLocale(getActivity());
        Log.i(SuntimesSettingsActivity.LOG_TAG, "AlarmPrefsFragment: Arguments: " + getArguments());

        PreferenceManager.setDefaultValues(getActivity(), R.xml.preference_alarms, false);
        addPreferencesFromResource(R.xml.preference_alarms);

        Activity activity = getActivity();
        if (AlarmSettings.loadPrefPowerOffAlarms(activity)) {
            checkPermissions(activity, true);
        }
    }

    private final BroadcastReceiver updateAlarmPrefsReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            Uri data = intent.getData();
            Log.d(SuntimesSettingsActivity.LOG_TAG, "updateAlarmPrefsReceiver.onReceive: " + data + " :: " + action);

            if (action != null)
            {
                if (action.equals(AlarmNotifications.ACTION_UPDATE_UI))
                {
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            AlarmPrefsFragment.this.setBootCompletedPrefEnabled(true);
                            initPref_alarms_bootCompleted(AlarmPrefsFragment.this);
                        }
                    }, 500);
                } else Log.e(SuntimesSettingsActivity.LOG_TAG, "updateAlarmPrefsReceiver.onReceive: unrecognized action: " + action);
            } else Log.e(SuntimesSettingsActivity.LOG_TAG, "updateAlarmPrefsReceiver.onReceive: null action!");
        }
    };

    @Override
    public void onResume()
    {
        super.onResume();
        getActivity().registerReceiver(updateAlarmPrefsReceiver, AlarmNotifications.getUpdateBroadcastIntentFilter(false));
        initPref_alarms(AlarmPrefsFragment.this);
    }

    @Override
    public void onPause() {
        getActivity().unregisterReceiver(updateAlarmPrefsReceiver);
        super.onPause();
    }

    public static final int REQUEST_PERMISSION_POWEROFFALARMS = 100;
    protected boolean checkPermissions(Activity activity, boolean requestIfMissing)
    {
        AlarmSettings.PowerOffAlarmInfo info = AlarmSettings.loadPowerOffAlarmInfo(activity);
        if (ContextCompat.checkSelfPermission(activity, info.getPermission()) != PackageManager.PERMISSION_GRANTED) {
            if (requestIfMissing) {
                ActivityCompat.requestPermissions(activity, new String[]{info.getPermission()}, REQUEST_PERMISSION_POWEROFFALARMS);
            }
            return false;
        } else return true;
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {}

    protected void setBootCompletedPrefEnabled(boolean value)
    {
        final Preference pref = findPreference(AlarmSettings.PREF_KEY_ALARM_BOOTCOMPLETED);
        if (pref != null) {
            pref.setEnabled(value);
        }
    }

    @SuppressLint("ResourceType")
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private static void initPref_alarms(final AlarmPrefsFragment fragment)
    {
        final Context context = fragment.getActivity();
        if (context == null) {
            return;
        }

        int[] colorAttrs = { R.attr.tagColor_warning, R.attr.text_accentColor };
        TypedArray typedArray = context.obtainStyledAttributes(colorAttrs);
        int colorWarning = ContextCompat.getColor(context, typedArray.getResourceId(0, R.color.warningTag_dark));
        int accentColor = ContextCompat.getColor(context, typedArray.getResourceId(1,  R.color.text_accent_dark));
        typedArray.recycle();

        Preference batteryOptimization = fragment.findPreference(AlarmSettings.PREF_KEY_ALARM_BATTERYOPT);
        if (batteryOptimization != null)
        {
            if (Build.VERSION.SDK_INT >= 23)
            {
                batteryOptimization.setOnPreferenceClickListener(onBatteryOptimizationClicked(context));
                batteryOptimization.setSummary(AlarmSettings.batteryOptimizationMessage(context));

            } else {
                PreferenceCategory alarmsCategory = (PreferenceCategory)fragment.findPreference(AlarmSettings.PREF_KEY_ALARM_CATEGORY);
                removePrefFromCategory(batteryOptimization, alarmsCategory);  // battery optimization is api 23+
            }
        }

        Preference autostartPref = fragment.findPreference(AlarmSettings.PREF_KEY_ALARM_AUTOSTART);
        if (autostartPref != null)
        {
            if (AlarmSettings.hasAutostartSettings(context))
            {
                autostartPref.setOnPreferenceClickListener(onAutostartPrefClicked(context));
                autostartPref.setSummary(AlarmSettings.autostartMessage(context));

            } else {
                PreferenceCategory alarmsCategory = (PreferenceCategory)fragment.findPreference(AlarmSettings.PREF_KEY_ALARM_CATEGORY);
                removePrefFromCategory(autostartPref, alarmsCategory);
            }
        }

        Preference notificationPrefs = fragment.findPreference(AlarmSettings.PREF_KEY_ALARM_NOTIFICATIONS);
        if (notificationPrefs != null)
        {
            boolean notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled();
            notificationPrefs.setOnPreferenceClickListener(onNotificationPrefsClicked(context));

            if (notificationsEnabled)
            {
                String enabledString = context.getString(R.string.configLabel_alarms_notifications_on);
                if (isDeviceSecure(context) && !notificationsOnLockScreen(context))
                {
                    String disabledString = context.getString(R.string.configLabel_alarms_notifications_off);
                    String summaryString = context.getString(R.string.configLabel_alarms_notifications_summary1, disabledString);
                    notificationPrefs.setSummary(SuntimesUtils.createColorSpan(null, summaryString, disabledString, colorWarning));
                } else {
                    notificationPrefs.setSummary(context.getString(R.string.configLabel_alarms_notifications_summary0, enabledString));
                }
            } else {
                String disabledString = context.getString(R.string.configLabel_alarms_notifications_off);
                notificationPrefs.setSummary(SuntimesUtils.createColorSpan(null, disabledString, disabledString, colorWarning));
            }
        }

        Preference volumesPrefs = fragment.findPreference(AlarmSettings.PREF_KEY_ALARM_VOLUMES);
        if (volumesPrefs != null) {
            volumesPrefs.setOnPreferenceClickListener(onVolumesPrefsClicked(context));
        }

        Preference powerOffAlarmsPref = fragment.findPreference(AlarmSettings.PREF_KEY_ALARM_POWEROFFALARMS);
        if (powerOffAlarmsPref != null)
        {
            powerOffAlarmsPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
            {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue)
                {
                    Activity activity = fragment.getActivity();
                    boolean enabled = (Boolean)newValue;
                    if (enabled && activity != null) {
                        fragment.checkPermissions(activity, true);
                    }
                    return true;
                }
            });

            AlarmSettings.PowerOffAlarmInfo info = AlarmSettings.loadPowerOffAlarmInfo(context);
            powerOffAlarmsPref.setSummary(context.getString(R.string.configLabel_alarms_poweroffalarms_summary, findPermission(context, info.getPermission())));
        }

        initPref_alarms_bootCompleted(fragment);

        Preference showLauncher = fragment.findPreference(AlarmSettings.PREF_KEY_ALARM_SHOWLAUNCHER);
        if (showLauncher != null)
        {
            showLauncher.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
            {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue)
                {
                    if (context != null)
                    {
                        AlarmSettings.setShowLauncherIcon(context, (Boolean)newValue);
                        Toast.makeText(context, context.getString(R.string.reboot_required_message), Toast.LENGTH_LONG).show();
                        return true;
                    }
                    return false;
                }
            });
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private static void initPref_alarms_bootCompleted(final AlarmPrefsFragment fragment)
    {
        final Context context = fragment.getActivity();
        if (context == null) {
            return;
        }

        final Preference bootCompletedPref = fragment.findPreference(AlarmSettings.PREF_KEY_ALARM_BOOTCOMPLETED);
        if (bootCompletedPref != null)
        {
            bootCompletedPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
            {
                @Override
                public boolean onPreferenceClick(Preference preference)
                {
                    AlertDialog.Builder confirm = new AlertDialog.Builder(context)
                            .setMessage(context.getString(R.string.configLabel_alarms_bootcompleted_action_confirm))
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton(context.getString(R.string.dialog_ok), new DialogInterface.OnClickListener()
                            {
                                public void onClick(DialogInterface dialog, int whichButton)
                                {
                                    bootCompletedPref.setEnabled(false);
                                    context.sendBroadcast(new Intent(AlarmNotifications.getAlarmIntent(context, AlarmNotifications.ACTION_SCHEDULE, null)));
                                }
                            })
                            .setNegativeButton(context.getString(R.string.dialog_cancel), null);
                    confirm.show();
                    return true;
                }
            });

            AlarmSettings.BootCompletedInfo bootCompletedInfo = AlarmSettings.loadPrefLastBootCompleted(context);
            long lastRunMillis = bootCompletedInfo.getTimeMillis();
            String lastBootCompleted = utils.calendarDateTimeDisplayString(context, lastRunMillis).toString();
            String afterDelay = (lastRunMillis >= 0 ? utils.timeDeltaLongDisplayString(0, bootCompletedInfo.getAtElapsedMillis(), true).getValue() : "");
            String took = (lastRunMillis >= 0 ? bootCompletedInfo.getDurationMillis() + "ms": "");
            CharSequence infoSpan = (lastRunMillis >= 0 ? context.getString(R.string.configLabel_alarms_bootcompleted_info, lastBootCompleted, afterDelay, took)
                    : context.getString(R.string.configLabel_alarms_bootcompleted_info_never));
            bootCompletedPref.setSummary(context.getString(R.string.configLabel_alarms_bootcompleted_summary, infoSpan));
        }
    }

    /***
     * Android 4 and under can enable/disable notifications per app .. the setting is located in App details.
     * Android 5 adds the ability to display notifications on the lock screen (global) .. global lock screen setting is in "Sound Settings".
     * Android 7 extends the ability to display notifications on the lock screen (per app) .. app lock screen setting is in App details.
     * Android 8 adds the ability to enable/disable notifications per channel. .. TODO
     */
    private static Preference.OnPreferenceClickListener onNotificationPrefsClicked(final Context context)
    {
        final boolean notificationsOnLockScreen = notificationsOnLockScreen(context);
        return new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference)
            {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                {
                    openNotificationSettings(context);

                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    if (!notificationsOnLockScreen) {
                        openSoundSettings(context);
                    } else {
                        openNotificationSettings(context);
                    }

                } else {
                    openNotificationSettings(context);
                }
                return false;
            }
        };
    }

    /**
     * https://stackoverflow.com/questions/32366649/any-way-to-link-to-the-android-notification-settings-for-my-app
     * @param context
     */
    public static void openNotificationSettings(@NonNull Context context)
    {
        Intent intent = new Intent();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
            intent.putExtra("app_package", context.getPackageName());                           // Android 5-7
            intent.putExtra("app_uid", context.getApplicationInfo().uid);                       // Android 5-7
            intent.putExtra("android.provider.extra.APP_PACKAGE", context.getPackageName());    // Android 8+

        } else {
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
        }
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(SuntimesSettingsActivity.LOG_TAG, "Failed to open notification settings! " + e);
            Toast.makeText(context, e.getClass().getSimpleName() + "!", Toast.LENGTH_SHORT).show();
        }
    }

    private static Preference.OnPreferenceClickListener onAutostartPrefClicked(final Context context)
    {
        return new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                AlarmSettings.openAutostartSettings(context);
                return false;
            }
        };
    }

    private static Preference.OnPreferenceClickListener onVolumesPrefsClicked(final Context context)
    {
        return new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference)
            {
                openSoundSettings(context);
                return false;
            }
        };
    }

    public static void openSoundSettings(@NonNull Context context)
    {
        Intent intent = new Intent();
        intent.setAction("android.settings.SOUND_SETTINGS");
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(SuntimesSettingsActivity.LOG_TAG, "Failed to open sound settings! " + e);
            Toast.makeText(context, e.getClass().getSimpleName() + "!", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * https://stackoverflow.com/questions/43438978/get-status-of-setting-control-notifications-on-your-lock-screen
     * @param context
     * @return true notifications allowed on lock screen (global setting)
     */
    public static boolean notificationsOnLockScreen(Context context)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {                  // per app "on lock screen" setting introduce in Android7
            return (Settings.Secure.getInt(context.getContentResolver(), "lock_screen_show_notifications", -1) > 0);    // TODO

        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {    // global "on lock screen" setting introduced in Android5
            return (Settings.Secure.getInt(context.getContentResolver(), "lock_screen_show_notifications", -1) > 0);

        } else {
            return true;
        }
    }

    private static Preference.OnPreferenceClickListener onBatteryOptimizationClicked(final Context context)
    {
        return new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                createBatteryOptimizationAlertDialog(context).show();
                return false;
            }
        };
    }

    /**
     * This alert dialog explains the importance of disabling battery optimization for alarm functionality
     * to work correctly and requests that the user take action. An extra warning is displayed
     * for device's whose manufacturer has been known to break alarm functionality anyway, and directs
     * those users to the online help.
     *
     * If this build of the app includes the `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` permission, a
     * request to whitelist the app is made directly. Otherwise the battery optimization list is
     * shown instead (and the user must find and select the app).
     */
    @SuppressLint("ResourceType")
    public static AlertDialog.Builder createBatteryOptimizationAlertDialog(final Context context)
    {
        final boolean isIgnoringOptimizations = AlarmSettings.isIgnoringBatteryOptimizations(context);
        final boolean isAggressive = AlarmSettings.aggressiveBatteryOptimizations(context);

        String message =
                isIgnoringOptimizations ? "[i] " + context.getString(R.string.configLabel_alarms_optWhiteList_listed)
                        : (isAggressive ? "[w] " + context.getString(R.string.configLabel_alarms_optWhiteList_unlisted_aggressive)
                        : "[w] " + context.getString(R.string.configLabel_alarms_optWhiteList_unlisted));
        if (!isIgnoringOptimizations) {
            message += "\n\n" + context.getString(R.string.help_battery_optimization, context.getString(R.string.app_name));
        }
        if (isAggressive) {
            message += "\n\n[w] " + context.getString(R.string.help_battery_optimization_aggressive, Build.MANUFACTURER);
        }

        int iconSize = (int) context.getResources().getDimension(R.dimen.helpIcon_size);
        int[] iconAttrs = { R.attr.tagColor_warning, R.attr.icActionAbout, R.attr.icActionWarning };
        TypedArray typedArray = context.obtainStyledAttributes(iconAttrs);
        int warningColor = ContextCompat.getColor(context, typedArray.getResourceId(0, R.color.text_accent_dark));
        ImageSpan iconInfo = SuntimesUtils.createImageSpan(context, typedArray.getResourceId(1, R.drawable.ic_action_about), iconSize, iconSize, 0);
        ImageSpan iconWarn = SuntimesUtils.createImageSpan(context, typedArray.getResourceId(2, R.drawable.ic_action_warning), iconSize, iconSize, warningColor);
        typedArray.recycle();

        SuntimesUtils.ImageSpanTag[] tags = {
                new SuntimesUtils.ImageSpanTag("[i]", iconInfo),
                new SuntimesUtils.ImageSpanTag("[w]", iconWarn)
        };
        CharSequence messageSpan = SuntimesUtils.createSpan(context, message, tags);

        return new AlertDialog.Builder(context)
                .setMessage(messageSpan)
                .setPositiveButton(context.getString(R.string.configLabel_alarms_optWhiteList),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                dialog.dismiss();
                                if (isIgnoringOptimizations) {
                                    AlarmSettings.openBatteryOptimizationSettings(context);
                                } else {
                                    if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(context, Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)) {
                                        AlarmSettings.requestIgnoreBatteryOptimization(context);
                                    } else AlarmSettings.openBatteryOptimizationSettings(context);
                                }
                            }
                        })
                .setNeutralButton(context.getString(R.string.configAction_onlineHelp),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                String url = context.getString(R.string.help_battery_optimization_url);
                                /*if (isAggressive) {
                                    url += "-" + Build.MANUFACTURER;
                                }*/
                                context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                            }
                        });
        //.setNegativeButton(context.getString(R.string.dialog_cancel), null);
    }

    protected static boolean isDeviceSecure(Context context)
    {
        KeyguardManager keyguard = (KeyguardManager)context.getSystemService(Context.KEYGUARD_SERVICE);
        if (keyguard != null)
        {
            if (Build.VERSION.SDK_INT >= 23) {
                return keyguard.isDeviceSecure();

            } else if (Build.VERSION.SDK_INT >= 16) {
                return keyguard.isKeyguardSecure();

            } else return false;
        } else return false;
    }

    public static void removePrefFromCategory(Preference pref, PreferenceCategory category)
    {
        if (pref != null && category != null) {
            category.removePreference(pref);
        }
    }

}
