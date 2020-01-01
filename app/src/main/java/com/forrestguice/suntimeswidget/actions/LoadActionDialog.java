/**
    Copyright (C) 2019 Forrest Guice
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

package com.forrestguice.suntimeswidget.actions;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.support.annotation.NonNull;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.forrestguice.suntimeswidget.R;
import com.forrestguice.suntimeswidget.SuntimesUtils;
import com.forrestguice.suntimeswidget.settings.WidgetActions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * LoadActionDialog
 */
public class LoadActionDialog extends EditActionDialog
{
    @Override
    public String getIntentID()
    {
        if (spin_intentID != null) {
            ActionDisplay selected = (ActionDisplay)spin_intentID.getSelectedItem();
            return selected != null ? selected.id : null;
        } else return null;
    }

    @Override
    public String getIntentTitle()
    {
        if (spin_intentID != null) {
            ActionDisplay selected = (ActionDisplay)spin_intentID.getSelectedItem();
            return selected != null ? selected.title : null;
        } else return null;
    }

    private Spinner spin_intentID;

    @Override
    protected void initViews(Context context, View dialogContent)
    {
        spin_intentID = (Spinner) dialogContent.findViewById(R.id.spin_intentid);
        initAdapter(context);

        ImageButton button_menu = (ImageButton) dialogContent.findViewById(R.id.edit_intent_menu);
        button_menu.setOnClickListener(onMenuButtonClicked);

        super.initViews(context, dialogContent);
    }

    protected void initAdapter(Context context)
    {
        ArrayList<ActionDisplay> ids = new ArrayList<>();
        Set<String> intentIDs = WidgetActions.loadActionLaunchList(context, 0);
        for (String id : intentIDs)
        {
            String title = WidgetActions.loadActionLaunchPref(context, 0, id, WidgetActions.PREF_KEY_ACTION_LAUNCH_TITLE);
            String desc = WidgetActions.loadActionLaunchPref(context, 0, id, WidgetActions.PREF_KEY_ACTION_LAUNCH_DESC);
            if (title != null && !title.trim().isEmpty()) {
                ids.add(new ActionDisplay(id, title, desc));
            }
        }

        Collections.sort(ids, new Comparator<ActionDisplay>() {
            @Override
            public int compare(ActionDisplay o1, ActionDisplay o2) {
                return o1.title.compareTo(o2.title);
            }
        });

        ActionDisplayAdapter adapter = new ActionDisplayAdapter(context, R.layout.layout_listitem_timezone, ids.toArray(new ActionDisplay[0]));
        //adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spin_intentID.setAdapter(adapter);
    }

    @Override
    protected int getLayoutID() {
        return R.layout.layout_dialog_intent_load;
    }

    protected View.OnClickListener onMenuButtonClicked = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            showOverflowMenu(getContext(), v);
        }
    };

    protected void showOverflowMenu(Context context, View parent)
    {
        PopupMenu menu = new PopupMenu(context, parent);
        MenuInflater inflater = menu.getMenuInflater();
        inflater.inflate(R.menu.editintent1, menu.getMenu());
        menu.setOnMenuItemClickListener(onMenuItemClicked);
        SuntimesUtils.forceActionBarIcons(menu.getMenu());
        prepareOverflowMenu(context, menu.getMenu());
        menu.show();
    }

    protected void prepareOverflowMenu(Context context, Menu menu)
    {
        MenuItem deleteItem = menu.findItem(R.id.deleteAction);
        if (deleteItem != null) {
            deleteItem.setEnabled(getIntentID() != null);
        }
    }

    protected PopupMenu.OnMenuItemClickListener onMenuItemClicked = new PopupMenu.OnMenuItemClickListener()
    {
        @Override
        public boolean onMenuItemClick(MenuItem menuItem)
        {
            switch (menuItem.getItemId())
            {
                case R.id.addAction:
                    addAction();
                    return true;

                case R.id.clearAction:
                    clearActions();
                    return true;

                case R.id.deleteAction:
                    deleteAction();
                    return true;

                default:
                    return false;
            }
        }
    };

    private void addAction()
    {
        final Context context = getContext();
        final SaveActionDialog saveDialog = new SaveActionDialog();
        saveDialog.setOnAcceptedListener(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                saveDialog.getEdit().saveIntent(context, 0, saveDialog.getIntentID(), saveDialog.getIntentTitle(), saveDialog.getIntentDesc());
                Toast.makeText(context, context.getString(R.string.saveaction_toast, saveDialog.getIntentTitle(), saveDialog.getIntentID()), Toast.LENGTH_SHORT).show();
                initAdapter(getContext());
                updateViews(getContext());
            }
        });
        saveDialog.show(getFragmentManager(), EditActionView.DIALOGTAG_SAVE);
    }

    private void clearActions()
    {
        final Context context = getContext();
        if (context != null)
        {
            AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());
            dialog.setMessage(context.getString(R.string.clearactions_dialog_msg))
                    .setNegativeButton(context.getString(android.R.string.cancel), null)
                    .setPositiveButton(context.getString(R.string.clearactions_dialog_ok),
                            new DialogInterface.OnClickListener()
                            {
                                @Override
                                public void onClick(DialogInterface dialog, int which)
                                {
                                    WidgetActions.deletePrefs(context, 0);
                                    WidgetActions.initDefaults(context);
                                    Toast.makeText(context, context.getString(R.string.clearactions_toast), Toast.LENGTH_SHORT).show();
                                    initAdapter(getContext());
                                    updateViews(getContext());
                                }
                            });
            dialog.show();
        }
    }

    private void deleteAction()
    {
        Context context = getContext();
        final String intentID = getIntentID();
        if (intentID != null && context != null)
        {
            AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());
            String title = WidgetActions.loadActionLaunchPref(context, 0, intentID, WidgetActions.PREF_KEY_ACTION_LAUNCH_TITLE);
            dialog.setMessage(context.getString(R.string.delaction_dialog_msg, title, intentID))
                    .setNegativeButton(context.getString(R.string.delaction_dialog_cancel), null)
                    .setPositiveButton(context.getString(R.string.delaction_dialog_ok),
                    new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            WidgetActions.deleteActionLaunchPref(getContext(), 0, intentID);
                            initAdapter(getContext());
                            updateViews(getContext());
                        }
                    });
            dialog.show();
        }
    }

    /**
     * ActionDisplay
     */
    public static class ActionDisplay
    {
        public String id, title, desc;
        public ActionDisplay(String id, String title, String desc)
        {
            this.id = id;
            this.title = title;
            this.desc = desc;
        }
        public String toString() {
             return title;
        }
    }

    /**
     * ActionDisplayAdapter
     */
    public static class ActionDisplayAdapter extends ArrayAdapter<ActionDisplay>
    {
        private int resourceID, dropDownResourceID;
        private int[] colors;

        public ActionDisplayAdapter(@NonNull Context context, int resource) {
            super(context, resource);
            init(context, resource);
        }

        public ActionDisplayAdapter(@NonNull Context context, int resource, @NonNull ActionDisplay[] objects) {
            super(context, resource, objects);
            init(context, resource);
        }

        public ActionDisplayAdapter(@NonNull Context context, int resource, @NonNull List<ActionDisplay> objects) {
            super(context, resource, objects);
            init(context, resource);
        }

        private void init(@NonNull Context context, int resource) {
            resourceID = dropDownResourceID = resource;
            colors = context.getResources().getIntArray(R.array.utcOffsetColors);
        }

        @Override
        public void setDropDownViewResource(int resID) {
            super.setDropDownViewResource(resID);
            dropDownResourceID = resID;
        }

        @Override
        public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
            return getItemView(position, convertView, parent, true, dropDownResourceID);
        }

        @Override
        @NonNull
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            return getItemView(position, convertView, parent, false, resourceID);
        }

        private View getItemView(int position, View convertView, @NonNull ViewGroup parent, boolean colorize, int resID)
        {
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            View view = layoutInflater.inflate(resID, parent, false);

            ActionDisplay item = getItem(position);
            if (item == null) {
                Log.w("getItemView", "item at position " + position + " is null.");
                return view;
            }

            TextView primaryText = (TextView)view.findViewById(android.R.id.text1);
            primaryText.setText(item.toString());

            TextView secondaryText = (TextView)view.findViewById(android.R.id.text2);
            if (secondaryText != null) {
                secondaryText.setText(item.desc);
            }

            ImageView icon = (ImageView) view.findViewById(android.R.id.icon);
            if (icon != null)
            {
                if (colorize)
                {
                    GradientDrawable d = (GradientDrawable) icon.getBackground().mutate();
                    d.setColor(getColorForPosition(position));
                    d.invalidateSelf();
                    icon.setVisibility(View.VISIBLE);

                } else {
                    icon.setVisibility(View.GONE);
                }
            }

            return view;
        }

        private int getColorForPosition(int position) {
            if (position < 0) {
                position = 0;
            }
            return colors[position % colors.length];
        }
    }

}
