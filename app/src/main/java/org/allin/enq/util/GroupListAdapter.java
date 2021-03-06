package org.allin.enq.util;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.TextView;

import org.allin.enq.model.Group;
import org.allin.enq.R;
import org.allin.enq.service.EnqService;

import java.util.List;

/**
 * Created by Santi on 21/07/2015.
 */
public class GroupListAdapter implements ListAdapter
{
    private Context appContext;
    private List<Group> groupsList;
    private EnqService service;
    private Typeface comfortaa_regular;

    public GroupListAdapter(EnqService service, List<Group> groupsList, Context appContext) {
        comfortaa_regular = Typeface.createFromAsset(appContext.getAssets(), "fonts/Comfortaa-Regular.ttf");
        this.groupsList = groupsList;
        this.appContext = appContext;
        this.service = service;

        this.groupsList.add(0,new Group());
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public boolean isEnabled(int position) {
        return true;
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {

    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {

    }

    @Override
    public int getCount() {
        return groupsList.size();
    }

    @Override
    public Object getItem(int position) {
        return groupsList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) appContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            if (position == 0)
                convertView = inflater.inflate(R.layout.header_list_item, parent, false);
            else
                convertView = inflater.inflate(R.layout.list_item, parent, false);
        }

        if (position == 0) {

            TextView estimatedTimeColumnLabel = (TextView) convertView .findViewById(R.id.estimated_time_text_view);
            estimatedTimeColumnLabel.setTypeface(comfortaa_regular);

        } else {

            Button groupButton = (Button) convertView .findViewById(R.id.group_button);
            TextView estimatedTimeTextView = (TextView) convertView .findViewById(R.id.estimated_time_text_view);

            final Group currentGroup = groupsList.get(position);

            groupButton.setTypeface(comfortaa_regular);
            groupButton.setText(currentGroup.getName());
            groupButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    service.enqueueInGroup(currentGroup);
                }
            });

            estimatedTimeTextView.setTypeface(comfortaa_regular);
            Integer timeInMinutes = currentGroup.getEstimatedTime() / 60000;
            estimatedTimeTextView.setText(timeInMinutes.toString().concat("'"));

        }

        return convertView ;

    }

    @Override
    public int getItemViewType(int position) {
        return 1;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return groupsList.size() == 0;
    }
}

