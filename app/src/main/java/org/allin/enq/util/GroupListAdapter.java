package org.allin.enq.util;

import android.content.Context;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import org.allin.enq.model.Group;
import org.allin.enq.R;

import java.util.List;

/**
 * Created by Santi on 21/07/2015.
 */
public class GroupListAdapter implements ListAdapter
{
    private Context appContext;

    private List<Group> groupsList;

    public GroupListAdapter(List<Group> groupsList, Context appContext) {
        this.groupsList = groupsList;
        this.appContext = appContext;
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
            convertView = inflater.inflate(R.layout.list_item, parent, false);
        }

        TextView groupName = (TextView) convertView .findViewById(R.id.VgroupName);
        ImageView queueImage = (ImageView) convertView .findViewById(R.id.VgroupImage);
        TextView estimatedTime = (TextView) convertView .findViewById(R.id.VestimatedTime);

        Group currentGroup = groupsList.get(position);

        groupName.setText(currentGroup.getName());


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
