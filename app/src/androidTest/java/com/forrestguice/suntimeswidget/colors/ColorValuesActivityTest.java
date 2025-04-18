/**
    Copyright (C) 2025 Forrest Guice
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

package com.forrestguice.suntimeswidget.colors;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.forrestguice.suntimeswidget.BehaviorTest;
import com.forrestguice.suntimeswidget.R;
import com.forrestguice.suntimeswidget.RetryRule;
import com.forrestguice.suntimeswidget.SuntimesActivityTestBase;
import com.forrestguice.suntimeswidget.SuntimesSettingsActivity;
import com.forrestguice.suntimeswidget.SuntimesSettingsActivityTest;
import com.forrestguice.suntimeswidget.alarmclock.ui.AlarmDismissActivityTest;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.pressBack;
import static android.support.test.espresso.matcher.RootMatchers.isPlatformPopup;
import static android.support.test.espresso.matcher.ViewMatchers.hasSibling;
import static android.support.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.forrestguice.suntimeswidget.support.espresso.ViewAssertionHelper.assertShown;
import static com.forrestguice.suntimeswidget.support.espresso.matcher.ViewMatchersContrib.navigationButton;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.endsWith;

@LargeTest
@BehaviorTest
@RunWith(AndroidJUnit4.class)
public class ColorValuesActivityTest extends SuntimesActivityTestBase
{
    @Rule
    public ActivityTestRule<SuntimesSettingsActivity> activityRule = new ActivityTestRule<>(SuntimesSettingsActivity.class, false, false);

    @Rule
    public RetryRule retry = new RetryRule(3);

    @Before
    public void beforeTest() throws IOException {
        setAnimationsEnabled(false);
        saveConfigState(getContext());
        overrideConfigState(getContext());
    }
    @After
    public void afterTest() throws IOException {
        setAnimationsEnabled(true);
        restoreConfigState(getContext());
    }

    /////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////

    //@Test
    //public void test_ColorValuesActivity()
    //{
        //Activity activity = activityRule.getActivity();
        //AlarmDismissActivityRobot robot = new AlarmDismissActivityRobot()
        //        .showActivity(activity)    // TODO
        //        .assertActivityShown(activity);
    //}

    @Test
    public void test_ColorValuesActivity_AppColors_Dark()
    {
        activityRule.launchActivity(new Intent(Intent.ACTION_MAIN));
        Activity activity = activityRule.getActivity();
        AppColorsActivityRobot robot = new AppColorsActivityRobot(activity);
        robot.showActivity(activity, true)
                .assertActivityShown(activity);

        robot.showOverflowMenu(activity).sleep(1000)
                .assertOverflowMenuShown()
                .cancelOverflowMenu();
    }

    @Test
    public void test_ColorValuesActivity_AppColors_Light()
    {
        activityRule.launchActivity(new Intent(Intent.ACTION_MAIN));
        Activity activity = activityRule.getActivity();
        AppColorsActivityRobot robot = new AppColorsActivityRobot(activity);
        robot.showActivity(activity, false)
                .assertActivityShown(activity);

        robot.showOverflowMenu(activity).sleep(1000)
                .assertOverflowMenuShown()
                .cancelOverflowMenu();
    }

    @Test
    public void test_ColorValuesActivity_BrightAlarmColors()
    {
        activityRule.launchActivity(new Intent(Intent.ACTION_MAIN));
        Activity activity = activityRule.getActivity();
        ColorValuesActivityRobot robot = brightAlarmColorsActivityRobot(activity)
                .showActivity(activity)
                .assertActivityShown(activity);

        robot.showOverflowMenu(activity).sleep(1000)
                .assertOverflowMenuShown()
                .cancelOverflowMenu();
    }

    @Test
    public void test_ColorValuesActivity_BrightAlarmColors_preview()
    {
        activityRule.launchActivity(new Intent(Intent.ACTION_MAIN));
        final Activity activity = activityRule.getActivity();
        ColorValuesActivityRobot robot0 = brightAlarmColorsActivityRobot(activity);
        robot0.showActivity(activity)
                .assertActivityShown(activity)
                .showOverflowMenu(activity)
                .clickOverflowMenu_preview().sleep(1000);

        AlarmDismissActivityTest.AlarmDismissActivityRobot robot = new AlarmDismissActivityTest.AlarmDismissActivityRobot();
        robot.assertActivityShown(activity)
                .dragDismissButton(activity).sleep(1000);
        robot0.assertActivityShown(activity);
    }

    /////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////

    /**
     * AppColorsActivityRobot
     */
    public static class AppColorsActivityRobot extends ColorValuesActivityRobot
    {
        public AppColorsActivityRobot(final Activity activity)
        {
            super(new ColorValuesActivityRobotConfig()
            {
                @Override
                public String getActivityTitle() {
                    return activity.getString(R.string.configAction_colors);
                }
                @Override
                public boolean supportsPreview() {
                    return false;
                }
            });
            setRobot(this);
        }

        /**
         * shows the "app colors" activity from `settings -> user interface`
         */
        @Override
        public ColorValuesActivityRobot showActivity(Activity activity) {
            return showActivity(activity, true);
        }
        public ColorValuesActivityRobot showActivity(Activity activity, boolean darkMode)
        {
            SuntimesSettingsActivityTest.SettingsActivityRobot robot = new SuntimesSettingsActivityTest.SettingsActivityRobot()
                    .clickHeader_userInterfaceSettings();
            if (darkMode) {
                robot.clickPrefButton_darkAppColors().sleep(1000);
            } else robot.clickPrefButton_lightAppColors().sleep(1000);
            return this;
        }
    }

    /**
     * BrightAlarmColorsActivityRobot
     */
    public static ColorValuesActivityRobot brightAlarmColorsActivityRobot(final Activity activity)
    {
        return new ColorValuesActivityTest.ColorValuesActivityRobot(
                new ColorValuesActivityTest.ColorValuesActivityRobotConfig()
                {
                    @Override
                    public String getActivityTitle() {
                        return activity.getString(R.string.configLabel_alarms_brightMode_colors);
                    }
                    @Override
                    public boolean supportsPreview() {
                        return true;
                    }
                })
        {
            /**
             * shows the "bright alarm colors" activity from `settings -> alarms`
             */
            @Override
            public ColorValuesActivityRobot showActivity(Activity activity)
            {
                new SuntimesSettingsActivityTest.SettingsActivityRobot()
                        .showActivity(activity)
                        .clickHeader_alarmSettings()
                        .clickPref_brightAlarmColors().sleep(1000);
                return this;
            }
        };
    }

    /////////////////////////////////////////////////////////////////////////

    /**
     * ColorValuesActivityRobot
     */
    public static abstract class ColorValuesActivityRobot extends ActivityRobot<ColorValuesActivityRobot>
    {
        protected ColorValuesActivityRobotConfig config;
        public ColorValuesActivityRobot(ColorValuesActivityRobotConfig config) {
            setRobot(this);
            this.config = config;
        }

        public abstract ColorValuesActivityRobot showActivity(Activity activity);

        public ColorValuesActivityRobot clickCancelButton() {
            onView(allOf(withContentDescription(android.R.string.cancel), hasSibling(withText(config.getActivityTitle())))).perform(click());
            return this;
        }
        public ColorValuesActivityRobot clickApplyButton() {
            onView(withContentDescription(android.R.string.cancel)).perform(click());
            return this;
        }
        public ColorValuesActivityRobot clickAddButton() {
            onView(withContentDescription(R.string.configAction_addColors)).perform(click());
            return this;
        }
        public ColorValuesActivityRobot clickEditButton() {
            onView(withContentDescription(R.string.configAction_editColors)).perform(click());
            return this;
        }

        public ColorValuesActivityRobot clickOverflowMenu_preview() {
            onView(withText(R.string.configAction_previewColors)).inRoot(isPlatformPopup()).perform(click());
            return this;
        }
        public ColorValuesActivityRobot cancelOverflowMenu() {
            onView(withText(R.string.configAction_importColors)).inRoot(isPlatformPopup()).perform(pressBack());
            return this;
        }

        /////////////////////////////////////////////////////////////////////////

        public ColorValuesActivityRobot assertActivityShown(Context context) {
            onView(allOf(withText(config.getActivityTitle()), isDescendantOfA(withClassName(endsWith("Toolbar"))))).check(assertShown);
            onView(navigationButton()).check(assertShown);
            onView(withContentDescription(R.string.configAction_chooseColor)).check(assertShown);
            return this;
        }

        public ColorValuesActivityRobot assertOverflowMenuShown() {
            if (config.supportsPreview()) {
                onView(withText(R.string.configAction_previewColors)).check(assertShown);
            }
            onView(withText(R.string.configAction_import)).check(assertShown);
            onView(withText(R.string.configAction_share)).check(assertShown);
            onView(withText(R.string.configAction_deleteColors)).check(assertShown);
            return this;
        }
    }

    public static abstract class ColorValuesActivityRobotConfig
    {
        public abstract String getActivityTitle();
        public abstract boolean supportsPreview();
    }
}
