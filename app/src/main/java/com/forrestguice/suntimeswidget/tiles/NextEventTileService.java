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

package com.forrestguice.suntimeswidget.tiles;

import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.service.quicksettings.Tile;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.view.ContextThemeWrapper;

import com.forrestguice.suntimeswidget.R;
import com.forrestguice.suntimeswidget.SuntimesUtils;
import com.forrestguice.suntimeswidget.calculator.SuntimesRiseSetDataset;
import com.forrestguice.suntimeswidget.settings.AppSettings;
import com.forrestguice.suntimeswidget.settings.WidgetSettings;

import java.util.Calendar;
import java.util.TimeZone;

@TargetApi(24)
public class NextEventTileService extends ClockTileService
{
    public static final int NEXTEVENTTILE_APPWIDGET_ID = -2;

    @Override
    protected int appWidgetId() {
        return NEXTEVENTTILE_APPWIDGET_ID;
    }

    @Override
    @NonNull @SuppressWarnings("rawtypes")
    protected Class getConfigActivityClass(Context context) {
        return NextEventTileConfigActivity.class;
    }

    @Override
    protected void updateTile(Context context)
    {
        Tile tile = getQsTile();

        SuntimesRiseSetDataset.SearchResult result = findNextEvent(context, true);
        Calendar event = Calendar.getInstance(TimeZone.getDefault());
        event.setTimeInMillis(result.getCalendar().getTimeInMillis());

        WidgetSettings.RiseSetDataMode mode = result.getMode();

        int icon = (mode != null && mode.getTimeMode() == WidgetSettings.TimeMode.NOON) ? R.drawable.ic_noon_tile
                : (result.isRising() ? R.drawable.svg_sunrise : R.drawable.svg_sunset);

        String timeDisplay = utils.calendarTimeShortDisplayString(context, event, false).toString() + " " + (mode != null ? mode.toString() : "null"); // context.getString(result.isRising() ? R.string.sunrise : R.string.sunset);
        tile.setLabel(timeDisplay);
        tile.setIcon(Icon.createWithResource(this, icon));

        updateTileState(context, tile);
        tile.updateTile();
    }

    /**
     * initDataset
     */
    protected SuntimesRiseSetDataset initDataset(Context context)
    {
        if (dataset == null) {
            dataset = new SuntimesRiseSetDataset(context, appWidgetId());
            dataset.calculateData(context);
        }
        return dataset;
    }
    protected SuntimesRiseSetDataset dataset = null;

    /**
     * findNextEvent
     */
    protected SuntimesRiseSetDataset.SearchResult findNextEvent(Context context, boolean reinit)
    {
        if (nextEvent == null || reinit) {
            initDataset(context);
            nextEvent = dataset.findNextEvent();
        }
        return nextEvent;
    }
    private SuntimesRiseSetDataset.SearchResult nextEvent;

    @Override
    protected Dialog createDialog(final Context context)
    {
        findNextEvent(context, true);
        return super.createDialog(context);
    }

    @Override
    protected SpannableStringBuilder formatDialogTitle(Context context)
    {
        SuntimesRiseSetDataset.SearchResult nextEvent = findNextEvent(context, false);
        Calendar event = Calendar.getInstance(TimeZone.getDefault());
        event.setTimeInMillis(nextEvent.getCalendar().getTimeInMillis());
        String timeString = utils.calendarTimeShortDisplayString(context, event, false).toString();
        SpannableString timeDisplay = SuntimesUtils.createBoldSpan(null, timeString, timeString);
        timeDisplay = SuntimesUtils.createRelativeSpan(timeDisplay, timeString, timeString, 1.25f);

        SpannableStringBuilder title = new SpannableStringBuilder();
        title.append(timeDisplay);
        return title;
    }

    protected SpannableStringBuilder formatDialogMessage(Context context)
    {
        Calendar now = now(context);
        SuntimesRiseSetDataset.SearchResult nextEvent = findNextEvent(context, false);
        Calendar event = Calendar.getInstance(TimeZone.getDefault());
        event.setTimeInMillis(nextEvent.getCalendar().getTimeInMillis());

        WidgetSettings.RiseSetDataMode mode = nextEvent.getMode();
        String modeString = (mode != null ? mode.toString() : "null");
        SpannableString modeDisplay = SuntimesUtils.createBoldSpan(null, modeString, modeString);
        modeDisplay = SuntimesUtils.createRelativeSpan(modeDisplay, modeString, modeString, 1.25f);

        String noteValue = utils.timeDeltaLongDisplayString(now.getTimeInMillis(), event.getTimeInMillis()).getValue();
        String noteString = context.getString(event.after(now) ? R.string.hence : R.string.ago, noteValue);
        CharSequence noteDisplay = SuntimesUtils.createBoldSpan(null, noteString, noteValue);

        SpannableStringBuilder message = new SpannableStringBuilder();
        message.append(modeDisplay);
        message.append("\n\n");
        message.append(noteDisplay);
        return message;
    }

    @Override
    protected Drawable dialogIcon(Context context)
    {
        SuntimesRiseSetDataset.SearchResult nextEvent = findNextEvent(context, false);
        WidgetSettings.RiseSetDataMode mode = nextEvent.getMode();
        int icon = (mode != null && mode.getTimeMode() == WidgetSettings.TimeMode.NOON) ? R.drawable.ic_noon_tile
                : (nextEvent.isRising() ? R.drawable.svg_sunrise : R.drawable.svg_sunset);
        Drawable d = ContextCompat.getDrawable(context, icon);

        ContextThemeWrapper contextWrapper = new ContextThemeWrapper(context, AppSettings.loadTheme(context));
        int[] attrs = { R.attr.sunriseColor, R.attr.sunsetColor };
        TypedArray a = contextWrapper.obtainStyledAttributes(attrs);
        int colorId = a.getResourceId(nextEvent.isRising() ? 0 : 1, R.color.text_primary);
        a.recycle();
        DrawableCompat.setTint(d, ContextCompat.getColor(context, colorId));

        return d;
    }

}
