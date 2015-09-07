package org.allin.enq.activity;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.Bundle;
import android.provider.Settings;

import android.support.v4.app.NotificationCompat;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


import com.google.gson.internal.LinkedTreeMap;

import org.allin.enq.model.Group;
import org.allin.enq.service.EnqService;
import org.allin.enq.service.EnqServiceListener;
import org.allin.enq.service.EnqRestApiInfo;

import org.allin.enq.R;
import org.allin.enq.util.GroupListAdapter;


import java.util.List;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;
import retrofit.RetrofitError;


public class MainActivity extends Activity {

    @Bind(R.id.VgroupsList) ListView groupListView;
    @Bind(R.id.VrefreshButton) Button refreshButtonView;
    @Bind(R.id.VserviceState) TextView serviceStateView;
    @Bind(R.id.groupsProgressBar) ProgressBar groupsProgressBar;

    EnqService mService = null;
    Boolean mBound = false;
    WifiManager wifiManager = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);


        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        Intent intent = new Intent(this, EnqService.class);
        startService(intent);
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
                    groupListView.setVisibility(View.INVISIBLE);
                    groupsProgressBar.setVisibility(View.VISIBLE);
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
                    groupListView.setVisibility(View.VISIBLE);
                    groupsProgressBar.setVisibility(View.INVISIBLE);
                    refreshButtonView.setEnabled(true);
                    groupListView.setAdapter(new GroupListAdapter(groups, getApplicationContext()));

                }
            });

        }

        @Override
        public void OnGroupsNotFound(RetrofitError e) {
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
        public void OnClientNotEnqueued(RetrofitError e) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "Lo sentimos, no se pudo asignar al grupo. Intentelo de nuevo", Toast.LENGTH_SHORT).show();
                }
            });

        }

        @Override
        public void OnServerCall(LinkedTreeMap map) {

            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(getBaseContext())
                            .setSmallIcon(R.drawable.abc_btn_radio_material)
                            .setContentTitle("EnQ")
                            .setContentText("Ya puede acercarse a la caja numero " + map.get("paydesk"));
            int mNotificationId = 001;
// Gets an instance of the NotificationManager service
            NotificationManager mNotifyMgr =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
// Builds the notification and issues it.
            mNotifyMgr.notify(mNotificationId, mBuilder.build());
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
                    serviceStateView.setBackgroundColor(getResources().getColor(R.color.quiet_green));
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

              Group selectedGroup = (Group) groupListView.getItemAtPosition(position);

              mService.enqueueIn(selectedGroup);

        }
    }

}
