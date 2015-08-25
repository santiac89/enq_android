package org.allin.enq.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.Bundle;
import android.provider.Settings;

import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import org.allin.enq.model.Group;
import org.allin.enq.service.EnqService;
import org.allin.enq.service.EnqServiceListener;
import org.allin.enq.service.EnqRestApiInfo;

import org.allin.enq.R;
import org.allin.enq.util.GroupListAdapter;


import java.util.List;
import java.util.Map;

import retrofit.RetrofitError;


public class MainActivity extends Activity {

    ListView groupListView = null;
    Button refreshButtonView = null;
    TextView serviceStateView = null;
    EnqService mService = null;
    Boolean mBound = false;
    WifiManager wifiManager = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        groupListView = (ListView) findViewById(R.id.VgroupsList);
        refreshButtonView = (Button) findViewById(R.id.VrefreshButton);
        serviceStateView = (TextView) findViewById(R.id.VserviceState);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        Intent intent = new Intent(this, EnqService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

    }

    @Override
    protected void onStart() {
        super.onStart();
        groupListView.setOnItemClickListener(new QueueSelectedListener());
        refreshButtonView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mBound) {

                        mService.checkForEnqServer();
                        refreshButtonView.setEnabled(false);


                }

            }
        });
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {

            EnqService.EnqServiceBinder binder = (EnqService.EnqServiceBinder) service;
            mService = binder.getService();
            mService.setListener(new MyServiceListener());
            mBound = true;

            if (!wifiManager.isWifiEnabled()) {

                promptForWifiNetwork();

            } else {

                mService.checkForEnqServer();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };


    public void promptForWifiNetwork()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage(R.string.wifioff_dialog_content)
                .setTitle(R.string.wifioff_dialog_title)
                .setCancelable(false)
                .setPositiveButton(R.string.wifioff_dialog_settings,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Intent i = new Intent(Settings.ACTION_WIFI_SETTINGS);
                                startActivity(i);
                            }
                        }
                )
                .setNegativeButton(R.string.wifioff_dialog_cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                MainActivity.this.finish();
                            }
                        }
                );
        AlertDialog alert = builder.create();
        alert.show();

    }

    public class MyServiceListener implements EnqServiceListener
    {

        @Override
        public void OnGroupsFound(final List<Group> groups) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    refreshButtonView.setEnabled(true);
                    groupListView.setAdapter(new GroupListAdapter(groups, getApplicationContext()));

                }
            });

        }

        @Override
        public void OnGroupsNotFound(Exception e) {
            mService.findGroups();
        }

        @Override
        public void OnClientEnqueued(Map<String, String> result) {

            Intent intent = new Intent(getApplicationContext(),WaitingActivity.class);

            intent.putExtra("number",result.get("number"));
            intent.putExtra("estimated",result.get("estimated"));

            startActivity(intent);

        }

        @Override
        public void OnServiceException(Exception e) {

        }

        @Override
        public void OnServerFound(final EnqRestApiInfo enqRestApiInfo) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    refreshButtonView.setEnabled(true);
                    serviceStateView.setBackgroundColor(Color.GREEN);
                    serviceStateView.setText(enqRestApiInfo.getName());
                }
            });

            mService.findGroups();
        }

        @Override
        public void OnServerNotFound(Exception e) {
            mService.checkForEnqServer();
        }
    }

    public class QueueSelectedListener implements AdapterView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

              Group selectedGroup= (Group) groupListView.getItemAtPosition(position);

              mService.enqueueIn(selectedGroup);

        }
    }

}
