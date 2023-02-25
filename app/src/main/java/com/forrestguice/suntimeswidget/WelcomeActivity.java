/**
    Copyright (C) 2022 Forrest Guice
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

package com.forrestguice.suntimeswidget;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import com.forrestguice.suntimeswidget.views.Toast;
import android.widget.ToggleButton;

import com.forrestguice.suntimeswidget.alarmclock.AlarmClockItem;
import com.forrestguice.suntimeswidget.alarmclock.AlarmClockItemImportTask;
import com.forrestguice.suntimeswidget.alarmclock.AlarmDatabaseAdapter;
import com.forrestguice.suntimeswidget.alarmclock.AlarmNotifications;
import com.forrestguice.suntimeswidget.alarmclock.AlarmSettings;
import com.forrestguice.suntimeswidget.alarmclock.ui.AlarmListDialog;
import com.forrestguice.suntimeswidget.calculator.core.Location;
import com.forrestguice.suntimeswidget.getfix.BuildPlacesTask;
import com.forrestguice.suntimeswidget.getfix.PlacesListFragment;
import com.forrestguice.suntimeswidget.settings.AppSettings;
import com.forrestguice.suntimeswidget.settings.WidgetSettings;
import com.forrestguice.suntimeswidget.settings.WidgetTimezones;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.TimeZone;

public class WelcomeActivity extends AppCompatActivity
{
    public static final String EXTRA_PAGE = "page";

    private ViewPager pager;
    private WelcomeFragmentAdapter pagerAdapter;
    private Button nextButton, prevButton;
    private LinearLayout indicatorLayout;
    private AppSettings.LocaleInfo localeInfo;

    private static final SuntimesUtils utils = new SuntimesUtils();

    @Override
    protected void attachBaseContext(Context newBase)
    {
        Context context = AppSettings.initLocale(newBase, localeInfo = new AppSettings.LocaleInfo());
        super.attachBaseContext(context);
        WidgetSettings.initDefaults(context);
        SuntimesUtils.initDisplayStrings(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        setResult(RESULT_CANCELED, getResultData());
        AppSettings.setTheme(this, AppSettings.loadThemePref(this));
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= 21)
        {
            Window window = getWindow();
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
        }

        setContentView(R.layout.layout_activity_welcome);

        Intent intent = getIntent();
        int page = 0;
        if (intent.hasExtra(EXTRA_PAGE)) {
            page = intent.getIntExtra(EXTRA_PAGE, 0);
            intent.removeExtra(EXTRA_PAGE);
        }

        pagerAdapter = new WelcomeFragmentAdapter(getSupportFragmentManager());
        pager = (ViewPager) findViewById(R.id.container);
        pager.setAdapter(pagerAdapter);
        pager.addOnPageChangeListener(pagerChangeListener);
        pager.setOffscreenPageLimit(pagerAdapter.getCount()-1);   // don't recreate page fragments (retain state)

        prevButton = (Button) findViewById(R.id.button_prev);
        if (prevButton != null) {
            prevButton.setOnClickListener(onPrevPressed);
        }

        nextButton = (Button) findViewById(R.id.button_next);
        if (nextButton != null) {
            nextButton.setOnClickListener(onNextPressed);
        }

        indicatorLayout = (LinearLayout) findViewById(R.id.indicator_layout);
        pagerChangeListener.onPageSelected(0);

        if (page != 0)
        {
            final int selectedPage = page;
            pager.postDelayed(new Runnable() {
                @Override
                public void run() {
                    pager.setCurrentItem(selectedPage, true);
                }
            }, getResources().getInteger(R.integer.anim_welcome_pause_duration));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem)
    {
        switch (menuItem.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default: return (super.onOptionsItemSelected(menuItem));
        }
    }

    @Override
    public void onBackPressed() {
        onPrevPressed.onClick(prevButton);
    }

    private View.OnClickListener onPrevPressed = new View.OnClickListener() {
        @Override
        public void onClick(View v)
        {
            if (pager.getCurrentItem() <= 0) {
                onSkip();
            } else {
                onPrev();
            }
        }
    };

    private View.OnClickListener onNextPressed = new View.OnClickListener()
    {
        @Override
        public void onClick(View v) {
            if ((pager.getCurrentItem() + 1) < pagerAdapter.getCount()) {
                onNext();
            } else {
                onDone();
            }
        }
    };

    private ViewPager.OnPageChangeListener pagerChangeListener = new ViewPager.OnPageChangeListener()
    {
        private int previousPosition = 0;

        @Override
        public void onPageSelected(int position)
        {
            Log.d("DEBUG", "onPageSelected: " + position);
            if (saveSettings(getSupportFragmentManager(), previousPosition))
            {
                updateViews(getSupportFragmentManager(), position);
                setIndicator(position);
                prevButton.setText(getString((position != 0) ? R.string.welcome_action_prev : R.string.welcome_action_skip));
                nextButton.setText(getString((position != pagerAdapter.getCount()-1) ? R.string.welcome_action_next : R.string.welcome_action_done));
                previousPosition = position;

            } else {
                Log.d("DEBUG", "onPageSelected: reverting to " + previousPosition);
                pager.setCurrentItem(previousPosition);
            }
        }

        @Override
        public void onPageScrolled(int position, float offset, int offsetPixels) {}

        @Override
        public void onPageScrollStateChanged(int arg0) {}
    };

    @SuppressLint("ResourceType")
    private void setIndicator(int position)
    {
        int[] colorAttrs = { R.attr.text_accentColor, R.attr.text_disabledColor };
        TypedArray typedArray = obtainStyledAttributes(colorAttrs);
        int activeColor = ContextCompat.getColor(WelcomeActivity.this, typedArray.getResourceId(0, R.color.text_accent_dark));
        int inactiveColor = ContextCompat.getColor(WelcomeActivity.this, typedArray.getResourceId(1, R.color.text_disabled_dark));
        typedArray.recycle();

        TextView[] indicators = new TextView[pagerAdapter.getCount()];
        for (int i=0; i<indicators.length; i++)
        {
            indicators[i] = new TextView(this);
            indicators[i].setTextSize(36);
            indicators[i].setTextColor((i == position) ? activeColor : inactiveColor);
            indicators[i].setText("\u2022");
        }

        indicatorLayout.removeAllViews();
        for (TextView indicator : indicators) {
            indicatorLayout.addView(indicator);
        }
    }

    private void onNext() {
        pager.setCurrentItem(pager.getCurrentItem() + 1, true);
    }

    private void onPrev() {
        pager.setCurrentItem(pager.getCurrentItem() - 1, true);
    }

    private void onSkip()
    {
        AppSettings.setFirstLaunch(WelcomeActivity.this, false);
        setResult(RESULT_CANCELED, getResultData());
        finish();
    }

    private void onDone()
    {
        saveSettings(getSupportFragmentManager(), pager.getCurrentItem());
        AppSettings.setFirstLaunch(WelcomeActivity.this, false);
        setResult(RESULT_OK, getResultData());
        finish();
    }

    private void saveSettings()
    {
        FragmentManager fragments = getSupportFragmentManager();
        for (int i=0; i<pagerAdapter.getCount(); i++) {
            saveSettings(fragments, i);
        }
    }
    private boolean saveSettings(FragmentManager fragments, int position)
    {
        WelcomeFragment page = getPageFragment(fragments, pager, position);
        if (page != null) {
            if (page.validateInput(WelcomeActivity.this)) {
                return page.saveSettings(WelcomeActivity.this);
            } else return false;
        }
        return false;
    }
    private boolean validateInput(FragmentManager fragments, int position)
    {
        WelcomeFragment page = getPageFragment(fragments, pager, position);
        if (page != null) {
            return page.validateInput(WelcomeActivity.this);
        }
        return true;
    }
    private void updateViews(FragmentManager fragments, int position)
    {
        WelcomeFragment page = getPageFragment(fragments, pager, position);
        if (page != null) {
            page.updateViews(WelcomeActivity.this);
        }
    }

    public static WelcomeFragment getPageFragment(FragmentManager fragments, ViewPager pager, int position) {
        // https://stackoverflow.com/questions/54279509/how-to-get-elements-of-fragments-created-by-viewpager-in-mainactivity/54280113#54280113
        return (WelcomeFragment) fragments.findFragmentByTag("android:switcher:" + pager.getId() + ":" + position);
    }

    public void showAbout( View v )
    {
        startActivity(new Intent(this, AboutActivity.class));
        overridePendingTransition(R.anim.transition_next_in, R.anim.transition_next_out);
    }

    public void setNeedsRecreateFlag() {
        Log.d("DEBUG", "setNeedsRecreateFlag");
        getIntent().putExtra(SuntimesSettingsActivity.RECREATE_ACTIVITY, true);
        setResult(RESULT_OK, getResultData());
    }

    public Intent getResultData() {
        boolean value = getIntent().getBooleanExtra(SuntimesSettingsActivity.RECREATE_ACTIVITY, false);
        Log.d("DEBUG", "getResultData: needsRecreate? " + value);
        return new Intent().putExtra(SuntimesSettingsActivity.RECREATE_ACTIVITY, value);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * WelcomeFragmentAdapter
     */
    private class WelcomeFragmentAdapter extends FragmentPagerAdapter
    {
        public WelcomeFragmentAdapter(FragmentManager fragments)
        {
            super(fragments);
        }

        @Override
        public Fragment getItem(int position)
        {
            switch (position)
            {
                case 5: return WelcomeFragment.newInstance(R.layout.layout_welcome_legal);
                case 4: return WelcomeAlarmsFragment.newInstance();
                case 3: return WelcomeTimeZoneFragment.newInstance(WelcomeActivity.this);
                case 2: return WelcomeLocationFragment.newInstance();
                case 1: return WelcomeAppearanceFragment.newInstance();
                case 0: default: return WelcomeFirstPageFragment.newInstance();
            }
        }

        @Override
        public int getCount() {
            return 6;
        }
    }

    /**
     * WelcomeFragment
     */
    public static class WelcomeFragment extends Fragment
    {
        public static final String ARG_LAYOUT_RESID = "layoutResID";

        public WelcomeFragment() {}

        public static WelcomeFragment newInstance(int layoutResID)
        {
            WelcomeFragment fragment = new WelcomeFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_LAYOUT_RESID, layoutResID);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
        {
            View view = inflater.inflate(getLayoutResID(), container, false);
            initViews(getActivity(), view);
            updateViews(getActivity());
            return view;
        }

        @Override
        public void onResume()
        {
            super.onResume();
            updateViews(getActivity());
        }

        @Override
        public void setUserVisibleHint(boolean value)
        {
            super.setUserVisibleHint(value);
            if (isResumed()) {
                updateViews(getActivity());
            }
        }

        public void initViews(Context context, View view)
        {
            if (view != null)
            {
                int[] textViews = new int[] { R.id.text0, R.id.text1, R.id.text2, R.id.text3 };
                for (int resID : textViews) {
                    TextView text = (TextView) view.findViewById(resID);
                    if (text != null) {
                        text.setText(SuntimesUtils.fromHtml(text.getText().toString()));
                    }
                }

                textViews = new int[] { R.id.link0, R.id.link1, R.id.link2, R.id.link3 };
                for (int resID : textViews) {
                    TextView text = (TextView) view.findViewById(resID);
                    if (text != null) {
                        text.setText(SuntimesUtils.fromHtml(text.getText().toString()));
                        text.setMovementMethod(LinkMovementMethod.getInstance());
                    }
                }
            }
        }

        public void updateViews(Context context) {
            /* EMPTY */
        }

        public boolean validateInput(Context context) {
            return true;
        }

        public boolean saveSettings(Context context) {
            return true;
        }

        public int getLayoutResID() {
            return getArguments().getInt(ARG_LAYOUT_RESID);
        }

        public int getPreferredIndex() {
            return 0;
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////


    /**
     * WelcomeFirstPageFragment
     */
    public static class WelcomeFirstPageFragment extends WelcomeFragment
    {
        public WelcomeFirstPageFragment() {}

        public static WelcomeFirstPageFragment newInstance()
        {
            WelcomeFirstPageFragment fragment = new WelcomeFirstPageFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_LAYOUT_RESID, R.layout.layout_welcome_app);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void initViews(Context context, View view) {
            super.initViews(context, view);
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////


    /**
     * WelcomeLocationFragment
     */
    public static class WelcomeLocationFragment extends WelcomeFragment
    {
        public static final int IMPORT_REQUEST = 1100;

        private Button button_addPlaces, button_importPlaces, button_lookupLocation;
        private ProgressBar progress_addPlaces;
        private View layout_permissions;

        public WelcomeLocationFragment() {}

        public static WelcomeLocationFragment newInstance()
        {
            WelcomeLocationFragment fragment = new WelcomeLocationFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_LAYOUT_RESID, R.layout.layout_welcome_location);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void initViews(Context context, View view)
        {
            super.initViews(context, view);

            button_addPlaces = (Button) view.findViewById(R.id.button_build_places);
            if (button_addPlaces != null) {
                button_addPlaces.setOnClickListener(onAddPlacesClicked);
            }

            button_importPlaces = (Button) view.findViewById(R.id.button_import_places);
            if (button_importPlaces != null) {
                button_importPlaces.setOnClickListener(onImportPlacesClicked);
            }

            button_lookupLocation = (Button) view.findViewById(R.id.button_lookup_location);
            if (button_lookupLocation != null) {
                button_lookupLocation.setOnClickListener(onLookupLocationClicked);
            }

            layout_permissions = view.findViewById(R.id.layout_permissions);
            if (layout_permissions != null) {
                layout_permissions.setVisibility(View.GONE);   // toggled visible by locationConfig
            }

            LocationConfigDialog locationConfig = getLocationConfigDialog();
            if (locationConfig != null) {
                locationConfig.setDialogListener(locationConfigListener);
            }

            progress_addPlaces = (ProgressBar) view.findViewById(R.id.progress_build_places);
            toggleProgress(false);
        }

        private final LocationConfigDialog.LocationConfigDialogListener locationConfigListener = new LocationConfigDialog.LocationConfigDialogListener()
        {
            @Override
            public void onEditModeChanged(LocationConfigView.LocationViewMode mode)
            {
                switch (mode) {
                    case MODE_CUSTOM_ADD:
                    case MODE_CUSTOM_EDIT:
                        togglePermissionsText(true); break;
                    default: togglePermissionsText(false); break;
                }
            }
        };

        protected void togglePermissionsText(boolean value) {
            if (layout_permissions != null) {
                layout_permissions.setVisibility(value ? View.VISIBLE : View.GONE);
            }
        }

        private final View.OnClickListener onLookupLocationClicked = new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                LocationConfigDialog locationConfig = getLocationConfigDialog();
                if (locationConfig != null) {
                    locationConfig.addCurrentLocation(getContext());
                }
                if (button_lookupLocation != null) {
                    button_lookupLocation.setEnabled(false);
                    button_lookupLocation.setVisibility(View.GONE);
                }
            }
        };

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data)
        {
            super.onActivityResult(requestCode, resultCode, data);
            switch (requestCode)
            {
                case IMPORT_REQUEST:
                    if (resultCode == Activity.RESULT_OK)
                    {
                        Uri uri = (data != null ? data.getData() : null);
                        if (uri != null) {
                            importPlaces(getActivity(), uri);
                        }
                    } else {
                        reloadLocationList();
                    }
                    break;
            }
        }

        private final View.OnClickListener onImportPlacesClicked = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(BuildPlacesTask.buildPlacesOpenFileIntent(), IMPORT_REQUEST);
            }
        };
        protected void importPlaces(Context context, @NonNull Uri uri)
        {
            BuildPlacesTask task = new BuildPlacesTask(context);
            task.setTaskListener(importPlacesListener);
            task.execute(false, uri);
        }
        private final BuildPlacesTask.TaskListener importPlacesListener = new BuildPlacesTask.TaskListener()
        {
            @Override
            public void onStarted() {
                setRetainInstance(true);
                toggleControlsEnabled(false);
                toggleControlsVisible(false);
                setLocationViewMode(LocationConfigView.LocationViewMode.MODE_DISABLED);
                toggleProgress(true);
            }

            @Override
            public void onFinished(Integer result)
            {
                setRetainInstance(false);
                toggleProgress(false);
                toggleControlsEnabled(true);
                toggleControlsVisible(true);
                if (result > 0)
                {
                    Context context = getActivity();
                    if (context != null && button_importPlaces != null) {
                        button_importPlaces.setText(context.getString(R.string.locationbuild_toast_success, result.toString()));
                        button_importPlaces.setEnabled(false);
                    }
                }
                setLocationViewMode(LocationConfigView.LocationViewMode.MODE_CUSTOM_SELECT);
                reloadLocationList();
            }
        };

        private final View.OnClickListener onAddPlacesClicked = new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                BuildPlacesTask task = new BuildPlacesTask(getActivity());
                task.setTaskListener(buildPlacesListener);
                task.execute();
            }
        };
        private final BuildPlacesTask.TaskListener buildPlacesListener = new BuildPlacesTask.TaskListener()
        {
            @Override
            public void onStarted()
            {
                setRetainInstance(true);
                toggleControlsEnabled(false);
                toggleControlsVisible(false);
                setLocationViewMode(LocationConfigView.LocationViewMode.MODE_DISABLED);
                toggleProgress(true);
            }

            @Override
            public void onFinished(Integer result)
            {
                setRetainInstance(false);
                toggleProgress(false);
                toggleControlsEnabled(true);
                toggleControlsVisible(true);

                if (result > 0)
                {
                    Context context = getActivity();
                    if (context != null && button_addPlaces != null) {
                        button_addPlaces.setText(context.getString(R.string.locationbuild_toast_success, result.toString()));
                        button_addPlaces.setEnabled(false);
                    }
                }

                setLocationViewMode(LocationConfigView.LocationViewMode.MODE_CUSTOM_SELECT);
                reloadLocationList();
            }
        };

        protected void toggleControlsEnabled(boolean value)
        {
            if (button_addPlaces != null) {
                button_addPlaces.setEnabled(value);
            }
            if (button_importPlaces != null) {
                button_importPlaces.setEnabled(value);
            }
            if (button_lookupLocation != null) {
                button_lookupLocation.setEnabled(value);
            }
        }

        protected void toggleControlsVisible(boolean visible)
        {
            if (button_addPlaces != null) {
                button_addPlaces.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
            }
            if (button_importPlaces != null) {
                button_importPlaces.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
            }
            if (button_lookupLocation != null) {
                button_lookupLocation.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
            }
        }

        protected void toggleProgress(boolean visible) {
            if (progress_addPlaces != null) {
                progress_addPlaces.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
            }
        }

        @Nullable
        private LocationConfigDialog getLocationConfigDialog()
        {
            if (isAdded()) {
                FragmentManager fragments = getChildFragmentManager();
                if (fragments != null) {
                    return (LocationConfigDialog) fragments.findFragmentByTag("LocationConfigDialog");
                }
            }
            return null;
        }

        protected void reloadLocationList()
        {
            LocationConfigDialog locationConfig = getLocationConfigDialog();
            if (locationConfig != null) {
                locationConfig.getDialogContent().populateLocationList();
                locationConfig.getDialogContent().clickLocationSpinner();
            }
        }

        protected void setLocationViewMode( LocationConfigView.LocationViewMode value)
        {
            LocationConfigDialog locationConfig = getLocationConfigDialog();
            if (locationConfig != null) {
                locationConfig.getDialogContent().setMode(value);
            }
        }

        @Override
        public boolean validateInput(Context context)
        {
            LocationConfigDialog locationConfig = getLocationConfigDialog();
            if (locationConfig != null) {
                return locationConfig.getDialogContent().validateInput();
            }
            return super.validateInput(context);
        }

        @Override
        public boolean saveSettings(Context context)
        {
            LocationConfigDialog locationConfig = getLocationConfigDialog();
            if (locationConfig != null)
            {
                boolean saved = locationConfig.getDialogContent().saveSettings(context);
                Log.d("DEBUG", "saveSettings: location " + saved);
                return saved;
            }
            return false;
        }
    }

    /**
     * WelcomeTimeZoneFragment
     */
    public static class WelcomeTimeZoneFragment extends WelcomeFragment
    {
        private Spinner timeFormatSpinner;
        private TextView timeZoneWarning, timeZoneWarningNote;
        private Button timeZoneSuggestButton;

        public WelcomeTimeZoneFragment()
        {
            setArguments(new Bundle());
            setLongitude(Double.parseDouble(WidgetSettings.PREF_DEF_LOCATION_LONGITUDE));
        }

        public static WelcomeTimeZoneFragment newInstance(Context context)
        {
            WelcomeTimeZoneFragment fragment = new WelcomeTimeZoneFragment();
            Bundle args = fragment.getArguments();
            args.putInt(ARG_LAYOUT_RESID, R.layout.layout_welcome_timezone);
            fragment.setArguments(args);

            Location location = WidgetSettings.loadLocationPref(context, 0);
            fragment.setLongitude(location.getLongitudeAsDouble());
            fragment.setLongitudeLabel(location.getLabel());
            return fragment;
        }

        public double getLongitude() {
            return getArguments().getDouble(TimeZoneDialog.KEY_LONGITUDE);
        }
        public void setLongitude(double value) {
            getArguments().putDouble(TimeZoneDialog.KEY_LONGITUDE, value);
        }

        public String getLongitudeLabel() {
            return getArguments().getString(LocationConfigView.KEY_LOCATION_LABEL);
        }
        public void setLongitudeLabel( String value ) {
            getArguments().putString(LocationConfigView.KEY_LOCATION_LABEL, value);
        }

        public void toggleWarning(boolean visible)
        {
            if (timeZoneWarning != null) {
                timeZoneWarning.setVisibility(visible ? View.VISIBLE : View.GONE);
            }
            //if (timeZoneWarningNote != null) {
            //    timeZoneWarningNote.setVisibility(visible ? View.VISIBLE : View.GONE);
            //}
            if (timeZoneSuggestButton != null) {
                timeZoneSuggestButton.setVisibility(visible ? View.VISIBLE : View.GONE);
            }
        }

        protected TimeZoneDialog getTimeZoneDialog() {
            FragmentManager fragments = getChildFragmentManager();
            return fragments != null ? (TimeZoneDialog) fragments.findFragmentByTag("TimeZoneDialog") : null;
        }

        @Override
        public void initViews(Context context, View view)
        {
            super.initViews(context, view);

            TimeZoneDialog tzConfig = getTimeZoneDialog();
            if (tzConfig != null) {
                tzConfig.setTimeFormatMode(WidgetSettings.loadTimeFormatModePref(context, 0));
                tzConfig.setDialogListener(timeZoneDialogListener);
            }

            timeZoneWarning = (TextView) view.findViewById(R.id.warning_timezone);
            timeZoneWarningNote = (TextView) view.findViewById(R.id.warning_timezone_note);

            if (timeZoneWarning != null)
            {
                ImageSpan warningIcon = SuntimesUtils.createWarningSpan(context, context.getResources().getDimension(R.dimen.warningIcon_size));
                timeZoneWarning.setText(SuntimesUtils.createSpan(context, timeZoneWarning.getText().toString(), SuntimesUtils.SPANTAG_WARNING, warningIcon));
            }

            timeZoneSuggestButton = (Button) view.findViewById(R.id.button_suggest_timezone);
            if (timeZoneSuggestButton != null) {
                timeZoneSuggestButton.setOnClickListener(timeZoneSuggestButtonListener);
            }

            timeFormatSpinner = (Spinner) view.findViewById(R.id.appwidget_general_timeformatmode);
            if (timeFormatSpinner != null)
            {
                final WidgetSettings.TimeFormatMode timeFormat = WidgetSettings.loadTimeFormatModePref(context, 0);
                final ArrayAdapter<WidgetSettings.TimeFormatMode> adapter = new ArrayAdapter<>(context, R.layout.layout_listitem_oneline,
                        new WidgetSettings.TimeFormatMode[] {WidgetSettings.TimeFormatMode.MODE_SYSTEM, WidgetSettings.TimeFormatMode.MODE_12HR, WidgetSettings.TimeFormatMode.MODE_24HR});
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                timeFormatSpinner.setAdapter(adapter);
                timeFormatSpinner.setOnItemSelectedListener(onTimeFormatSelected);
                timeFormatSpinner.post(new Runnable() {
                    @Override
                    public void run() {
                        timeFormatSpinner.setSelection(adapter.getPosition(timeFormat), false);
                    }
                });
            }
        }

        @Override
        public void updateViews(Context context)
        {
            Location location = WidgetSettings.loadLocationPref(context, 0);
            setLongitude(location.getLongitudeAsDouble());
            setLongitudeLabel(location.getLabel());

            TimeZoneDialog tzConfig = getTimeZoneDialog();
            if (tzConfig != null) {
                tzConfig.setLongitude(getLongitude());
                tzConfig.updatePreview(context);
            }
        }

        protected void updateWarningNote(Context context, TimeZone tz)
        {
            if (timeZoneWarningNote != null)
            {
                long zoneOffsetMillis = tz.getOffset(System.currentTimeMillis());
                long lonOffsetMillis = Math.round(getLongitude() * (24 * 60 * 60 * 1000) / 360d);
                long offset = zoneOffsetMillis - lonOffsetMillis;
                String offsetDisplay = (offset < 0 ? "-" : "+") + utils.timeDeltaLongDisplayString(offset);

                TypedArray typedArray = context.obtainStyledAttributes(new int[] { R.attr.tagColor_warning, R.attr.text_primaryColor });
                int warningColor = ContextCompat.getColor(context, typedArray.getResourceId(0, R.color.warningTag_dark));
                int normalColor = ContextCompat.getColor(context, typedArray.getResourceId(1, R.color.text_primary_dark));
                typedArray.recycle();

                int highlightColor = normalColor;
                if (Math.abs(offset / 1000 / 60 / 60) >= WidgetTimezones.WARNING_TOLERANCE_HOURS) {
                    highlightColor = warningColor;
                }

                String location = getLongitudeLabel();
                String note = context.getString(R.string.timezoneWarningNote, tz.getID(), offsetDisplay, location);
                SpannableString noteDisplay = SuntimesUtils.createBoldColorSpan(null, note, offsetDisplay, highlightColor);
                noteDisplay = SuntimesUtils.createBoldColorSpan(noteDisplay, note, location, normalColor);
                timeZoneWarningNote.setText(noteDisplay);
            }
        }

        private TimeZoneDialog.TimeZoneDialogListener timeZoneDialogListener = new TimeZoneDialog.TimeZoneDialogListener()
        {
            @Override
            public void onSelectionChanged( TimeZone tz ) {
                toggleWarning(WidgetTimezones.isProbablyNotLocal(tz, getLongitude(), Calendar.getInstance(tz).getTime()));
                updateWarningNote(getContext(), tz);
            }
        };

        private AdapterView.OnItemSelectedListener onTimeFormatSelected = new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                Activity context = getActivity();
                TimeZoneDialog tzConfig = getTimeZoneDialog();
                if (tzConfig != null && context != null) {
                    tzConfig.setTimeFormatMode((WidgetSettings.TimeFormatMode) parent.getAdapter().getItem(position));
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };

        private View.OnClickListener timeZoneSuggestButtonListener = new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                TimeZoneDialog tzConfig = getTimeZoneDialog();
                if (tzConfig != null)
                {
                    Calendar now = Calendar.getInstance();
                    double longitude = getLongitude();
                    String label = getLongitudeLabel();
                    Log.d("DEBUG", "longitude label: " + label);

                    boolean foundItem = false;
                    String tzID = WidgetSettings.PREF_DEF_TIMEZONE_CUSTOM;
                    WidgetTimezones.TimeZoneItemAdapter adapter = tzConfig.getTimeZoneItemAdapter();
                    WidgetTimezones.TimeZoneItem[] recommendations = null;
                    if (adapter != null)
                    {
                        if (label != null)
                        {
                            WidgetTimezones.TimeZoneItem[] items = adapter.values();
                            for (WidgetTimezones.TimeZoneItem item : items)
                            {
                                if (item.getID().contains(label) || item.getDisplayString().contains(label)) {
                                    tzID = item.getID();
                                    foundItem = true;
                                    break;
                                }
                            }
                        }

                        if (!foundItem) {
                            recommendations = adapter.findItems(longitude);
                        }
                    }

                    if (!foundItem)
                    {
                        tzID = WidgetSettings.PREF_DEF_TIMEZONE_CUSTOM;
                        TimeZone tz = WidgetTimezones.getTimeZone(tzID, longitude);
                        if (WidgetTimezones.isProbablyNotLocal(tz, longitude, now.getTime()))
                        {
                            if (recommendations != null && recommendations[0] != null) {
                                tzID = recommendations[0].getID();
                            }
                        }
                    }
                    tzConfig.setCustomTimeZone(tzID);
                }
            }
        };

        @Override
        public boolean saveSettings(Context context)
        {
            if (isAdded())
            {
                TimeZoneDialog tzConfig = getTimeZoneDialog();
                if (tzConfig != null) {
                    tzConfig.saveSettings(context);
                }

                WidgetSettings.TimeFormatMode timeFormat = (WidgetSettings.TimeFormatMode) timeFormatSpinner.getSelectedItem();
                WidgetSettings.saveTimeFormatModePref(context, 0, timeFormat);
                Log.d("DEBUG", "saveSettings: timezone");
                return true;
            }
            return false;
        }
    }

    /**
     * WelcomeAlarmsFragment
     */
    public static class WelcomeAlarmsFragment extends WelcomeFragment
    {
        public static final int IMPORT_REQUEST = 1200;

        public WelcomeAlarmsFragment() {}

        protected TextView batteryOptimizationText;
        protected Button importAlarmsButton;
        private ProgressBar progress_importAlarms;

        public static WelcomeAlarmsFragment newInstance()
        {
            WelcomeAlarmsFragment fragment = new WelcomeAlarmsFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_LAYOUT_RESID, R.layout.layout_welcome_alarms);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void initViews(final Context context, View view)
        {
            CheckBox launcherIconCheck = (CheckBox) view.findViewById(R.id.check_alarms_showlauncher);
            if (launcherIconCheck != null)
            {
                launcherIconCheck.setChecked(AlarmSettings.loadPrefShowLauncher(context));
                launcherIconCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        AlarmSettings.savePrefShowLauncher(context, isChecked);
                    }
                });
            }

            CheckBox reminderNotificationCheck = (CheckBox) view.findViewById(R.id.check_alarms_showreminders);
            if (reminderNotificationCheck != null)
            {
                long reminderMillis = AlarmSettings.loadPrefAlarmUpcoming(context);
                reminderNotificationCheck.setChecked(reminderMillis > 0);
                reminderNotificationCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
                    {
                        long reminderMillis = (isChecked ? AlarmSettings.PREF_DEF_ALARM_UPCOMING : 0);
                        AlarmSettings.savePrefAlarmUpcomingReminder(context, reminderMillis);
                    }
                });
            }

            batteryOptimizationText = (TextView) view.findViewById(R.id.text_optWhiteList);

            Button batteryOptimizationButton = (Button) view.findViewById(R.id.button_optWhiteList);
            if (batteryOptimizationButton != null)
            {
                batteryOptimizationButton.setVisibility((Build.VERSION.SDK_INT >= 23) ? View.VISIBLE : View.GONE);
                batteryOptimizationButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        SuntimesSettingsActivity.createBatteryOptimizationAlertDialog(context).show();
                    }
                });
            }

            progress_importAlarms = (ProgressBar) view.findViewById(R.id.progress_import_alarms);
            importAlarmsButton = (Button) view.findViewById(R.id.button_import_alarms);
            if (importAlarmsButton != null) {
                importAlarmsButton.setOnClickListener(onImportAlarmsClicked);
            }
        }

        @Override
        public void updateViews(Context context)
        {
            if (batteryOptimizationText != null)
            {
                batteryOptimizationText.setVisibility((Build.VERSION.SDK_INT >= 23) ? View.VISIBLE : View.GONE);
                batteryOptimizationText.setText(AlarmSettings.batteryOptimizationMessage(context));
            }
        }

        protected void toggleControlsEnabled(boolean value)
        {
            if (importAlarmsButton != null) {
                importAlarmsButton.setEnabled(value);
            }
        }

        protected void toggleControlsVisible(boolean visible)
        {
            if (importAlarmsButton != null) {
                importAlarmsButton.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
            }
        }

        protected void toggleProgress(boolean visible) {
            if (progress_importAlarms != null) {
                progress_importAlarms.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
            }
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data)
        {
            super.onActivityResult(requestCode, resultCode, data);
            switch (requestCode)
            {
                case IMPORT_REQUEST:
                    if (resultCode == Activity.RESULT_OK)
                    {
                        Uri uri = (data != null ? data.getData() : null);
                        if (uri != null) {
                            importAlarms(getActivity(), uri);
                        }
                    }
                    break;
            }
        }

        protected AlarmClockItemImportTask importTask = null;
        private final View.OnClickListener onImportAlarmsClicked = new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                if (importTask != null) {
                    Log.e("ImportAlarms", "Already busy importing/exporting! ignoring request");
                }
                AlarmListDialog.importAlarms(WelcomeAlarmsFragment.this, getContext(), getLayoutInflater(), IMPORT_REQUEST);
            }
        };

        protected void importAlarms(final Context context, @NonNull Uri uri)
        {
            if (importTask != null) {
                Log.e("ImportAlarms", "Already busy importing/exporting! ignoring request");
            }
            importTask = new AlarmClockItemImportTask(context);
            importTask.setTaskListener(importAlarmsListener);
            importTask.execute(uri);
        }

        private final AlarmClockItemImportTask.TaskListener importAlarmsListener =  new AlarmClockItemImportTask.TaskListener()
        {
            @Override
            public void onStarted()
            {
                setRetainInstance(true);
                toggleProgress(true);
                toggleControlsEnabled(false);
                toggleControlsVisible(false);
            }

            @Override
            public void onFinished(AlarmClockItemImportTask.TaskResult result)
            {
                if (result.getResult())
                {
                    final Context context = getContext();
                    final AlarmClockItem[] items = result.getItems();
                    AlarmDatabaseAdapter.AlarmUpdateTask task = new AlarmDatabaseAdapter.AlarmUpdateTask(context, true, true);
                    task.setTaskListener(new AlarmDatabaseAdapter.AlarmItemTaskListener()
                    {
                        @Override
                        public void onFinished(Boolean result, AlarmClockItem[] items)
                        {
                            setRetainInstance(false);
                            importTask = null;
                            toggleProgress(false);
                            toggleControlsVisible(true);

                            if (result)
                            {
                                String plural = getResources().getQuantityString(R.plurals.alarmPlural, items.length, items.length);
                                importAlarmsButton.setText(getString(R.string.importalarms_toast_success, plural));

                                for (AlarmClockItem item : items) {
                                    if (item.enabled) {
                                        context.sendBroadcast( AlarmNotifications.getAlarmIntent(context, AlarmNotifications.ACTION_SCHEDULE, item.getUri()) );
                                    }
                                }
                            }
                        }
                    });
                    task.execute(items);

                } else {
                    setRetainInstance(false);
                    importTask = null;
                    toggleProgress(false);
                    toggleControlsEnabled(true);
                    if (isAdded())
                    {
                        Uri uri = result.getUri();   // import failed
                        String path = ((uri != null) ? uri.toString() : "<path>");
                        String failureMessage = getString(R.string.msg_import_failure, path);
                        Toast.makeText(getActivity(), failureMessage, Toast.LENGTH_LONG).show();
                    }
                }
            }
        };

    }

    /**
     * WelcomeAppearanceFragment
     */
    public static class WelcomeAppearanceFragment extends WelcomeFragment
    {
        protected ToggleButton[] buttons = null;
        protected TextView previewDate;

        public WelcomeAppearanceFragment() {}

        public static WelcomeAppearanceFragment newInstance()
        {
            WelcomeAppearanceFragment fragment = new WelcomeAppearanceFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_LAYOUT_RESID, R.layout.layout_welcome_appearance);
            fragment.setArguments(args);
            return fragment;
        }

        protected void setChecked(@Nullable RadioButton button, boolean value) {
            if (button != null) {
                button.setChecked(value);
            }
        }

        protected void setCheckedChangeListener(@Nullable RadioButton button, CompoundButton.OnCheckedChangeListener listener) {
            if (button != null) {
                button.setOnCheckedChangeListener(listener);
            }
        }

        protected String themeID = null, darkThemeID = null, lightThemeID = null;
        protected AppSettings.TextSize textSize;

        @Override
        public void initViews(Context context, View view)
        {
            super.initViews(context, view);

            RadioButton smallText = (RadioButton) view.findViewById(R.id.radio_text_small);
            RadioButton normalText = (RadioButton) view.findViewById(R.id.radio_text_normal);
            RadioButton largeText = (RadioButton) view.findViewById(R.id.radio_text_large);
            RadioButton xlargeText = (RadioButton) view.findViewById(R.id.radio_text_xlarge);

            textSize = AppSettings.TextSize.valueOf(AppSettings.loadTextSizePref(context));
            switch (textSize)
            {
                case SMALL: setChecked(smallText, true); break;
                case LARGE: setChecked(largeText, true); break;
                case XLARGE: setChecked(xlargeText, true); break;
                case NORMAL: default: setChecked(normalText, true); break;
            }
            setCheckedChangeListener(smallText, onTextSizeChecked(context, AppSettings.TextSize.SMALL));
            setCheckedChangeListener(normalText, onTextSizeChecked(context, AppSettings.TextSize.NORMAL));
            setCheckedChangeListener(largeText, onTextSizeChecked(context, AppSettings.TextSize.LARGE));
            setCheckedChangeListener(xlargeText, onTextSizeChecked(context, AppSettings.TextSize.XLARGE));

            AppSettings.AppThemeInfo themeInfo = AppSettings.loadThemeInfo(context);
            themeID = themeInfo.getThemeName();
            darkThemeID = AppSettings.loadThemeDarkPref(context);
            lightThemeID = AppSettings.loadThemeLightPref(context);
            AppSettings.AppThemeInfo darkThemeInfo = AppSettings.loadThemeInfo(darkThemeID);

            previewDate = (TextView) view.findViewById(R.id.text_date);
            updatePreview(context, themeInfo.getDisplayString(context));

            ToggleButton systemThemeButton = (ToggleButton) view.findViewById(R.id.button_theme_system);
            ToggleButton systemTheme1Button = (ToggleButton) view.findViewById(R.id.button_theme_system1);
            ToggleButton darkThemeButton = (ToggleButton) view.findViewById(R.id.button_theme_dark);
            ToggleButton lightThemeButton = (ToggleButton) view.findViewById(R.id.button_theme_light);
            buttons = new ToggleButton[] {systemThemeButton, systemTheme1Button, darkThemeButton, lightThemeButton};
            for (ToggleButton button : buttons) {
                if (button != null) {
                    button.setChecked(false);
                }
            }

            if (systemThemeButton != null) {
                if (setChecked(systemThemeButton, AppSettings.THEME_SYSTEM.equals(themeID) && AppSettings.THEME_DEFAULT.equals(darkThemeID))) {
                    updatePreview(context, themeInfo.getDisplayString(context));
                }
                systemThemeButton.setOnClickListener(onThemeButtonClicked(AppSettings.THEME_SYSTEM, null, null));
            }

            if (systemTheme1Button != null) {
                if (setChecked(systemTheme1Button, AppSettings.THEME_SYSTEM.equals(themeID) && AppSettings.THEME_SYSTEM1.equals(darkThemeID))) {
                    updatePreview(context, darkThemeInfo.getDisplayString(context));
                }
                systemTheme1Button.setOnClickListener(onThemeButtonClicked(AppSettings.THEME_SYSTEM, AppSettings.THEME_SYSTEM1, AppSettings.THEME_SYSTEM1));
            }

            if (darkThemeButton != null) {
                if (setChecked(darkThemeButton, AppSettings.THEME_DARK.equals(themeID))) {
                    updatePreview(context, themeInfo.getDisplayString(context));
                }
                darkThemeButton.setOnClickListener(onThemeButtonClicked(AppSettings.THEME_DARK, null, null));
            }

            if (lightThemeButton != null) {
                if (setChecked(lightThemeButton, AppSettings.THEME_LIGHT.equals(themeID))) {
                    updatePreview(context, themeInfo.getDisplayString(context));
                }
                lightThemeButton.setOnClickListener(onThemeButtonClicked(AppSettings.THEME_LIGHT, null, null));
            }
        }

        protected void updatePreview(Context context, String themeDisplay)
        {
            if (previewDate != null) {
                previewDate.setText(themeDisplay.replace(" ", "\n"));
            }
        }

        protected boolean setChecked(ToggleButton button, boolean value)
        {
            if (value) {
                for (ToggleButton b : buttons) {
                    if (b != null) {
                        b.setChecked(b == button);
                    }
                }
                return true;
            } else return false;
        }

        private CompoundButton.OnCheckedChangeListener onTextSizeChecked(final Context context, final AppSettings.TextSize textSize)
        {
            return new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked)
                    {
                        WelcomeAppearanceFragment.this.textSize = textSize;
                        AppSettings.saveTextSizePref(context, textSize);
                        recreate(getActivity());
                    }
                }
            };
        }

        private View.OnClickListener onThemeButtonClicked(final String themeID, final String lightThemeID, final String darkThemeID) {
            return new View.OnClickListener() {
                @Override
                public void onClick(View v)
                {
                    Activity activity = getActivity();
                    WelcomeAppearanceFragment.this.themeID = themeID;
                    WelcomeAppearanceFragment.this.lightThemeID = lightThemeID;
                    WelcomeAppearanceFragment.this.darkThemeID = darkThemeID;
                    AppSettings.saveThemeLightPref(activity, lightThemeID);
                    AppSettings.saveThemeDarkPref(activity, darkThemeID);
                    AppSettings.setThemePref(activity, themeID);
                    AppSettings.setTheme(activity, AppSettings.loadThemePref(activity));
                    recreate(getActivity());
                }
            };
        }

        @Override
        public int getPreferredIndex() {
            return 1;
        }

        private void recreate(Activity activity)
        {
            if (activity != null)
            {
                if (activity instanceof  WelcomeActivity) {
                    WelcomeActivity activity1 = (WelcomeActivity)activity;
                    activity1.setNeedsRecreateFlag();
                }
                activity.finish();
                activity.overridePendingTransition(R.anim.transition_restart_in, R.anim.transition_restart_out);
                activity.startActivity(activity.getIntent()
                        .putExtra(EXTRA_PAGE, getPreferredIndex()));
            }
        }

        @Override
        public boolean saveSettings(Context context)
        {
            AppSettings.saveThemeLightPref(context, lightThemeID);
            AppSettings.saveThemeDarkPref(context, darkThemeID);
            AppSettings.saveTextSizePref(context, textSize);
            AppSettings.setThemePref(context, themeID);
            return true;
        }
    }

}