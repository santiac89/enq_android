package com.example.santi.enq;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.DataSetObserver;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.apache.http.MethodNotSupportedException;
import org.json.JSONException;

import java.io.IOException;
import java.util.List;


public class MainActivity extends Activity {

    ListView queueListView = null;
    EnqService mService = null;
    Boolean mBound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        queueListView = (ListView) findViewById(R.id.VqueueList);

    }

    @Override
    protected void onStart() {
        super.onStart();

        Intent intent = new Intent(this, EnqService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {

            try {
                mService.findQueues(new QueuesFoundListener() {
                    @Override
                    public void OnQueuesFound(final List<Queue> queues) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                queueListView.setAdapter(new QueueListAdapter(queues));
                            }
                        });

                    }
                });
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (MethodNotSupportedException e) {
                e.printStackTrace();
            }


        }

        return super.onOptionsItemSelected(item);
    }


    public class QueueListAdapter implements ListAdapter
    {

       private List<Queue> queueList;

        public QueueListAdapter(List<Queue> queueList) {
            this.queueList = queueList;
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
            return queueList.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
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
                LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.list_item, parent, false);
            }


            TextView queueName = (TextView) convertView .findViewById(R.id.VqueueName);
            ImageView queueImage = (ImageView) convertView .findViewById(R.id.VqueueImage);
            TextView estimatedTime = (TextView) convertView .findViewById(R.id.VestimatedTime);

            Queue selectedQueue = queueList.get(position);

            queueName.setText(selectedQueue.getName());
            //queueImage.setImageBitmap(BitmapFactory.decodeFile(""));
            estimatedTime.setText(selectedQueue.getEstimated().toString());

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
            return queueList.size() == 0;
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            EnqService.EnqServiceBinder binder = (EnqService.EnqServiceBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };
}
