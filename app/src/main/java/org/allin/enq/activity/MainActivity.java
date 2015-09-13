package org.allin.enq.activity;


import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Typeface;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonObject;

import org.allin.enq.R;
import org.allin.enq.model.Group;
import org.allin.enq.service.EnqRestApiInfo;
import org.allin.enq.service.EnqService;
import org.allin.enq.service.EnqServiceListener;
import org.allin.enq.service.NotificationButtonsReceiver;
import org.allin.enq.util.GroupListAdapter;

import java.io.BufferedWriter;
import java.util.List;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;
import retrofit.RetrofitError;


public class MainActivity extends ActionBarActivity {

    @Bind(R.id.VgroupsList) ListView groupListView;
    @Bind(R.id.VrefreshButton) Button refreshButtonView;
    @Bind(R.id.VserviceState) TextView serviceStateView;
    @Bind(R.id.groupsProgressBar) ProgressBar groupsProgressBar;

    TextView conectando;
    TextView enq_title;
    EnqService mService = null;
    Boolean mBound = false;
    WifiManager wifiManager = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //ActionBar actionBar = getSupportActionBar();
        //actionBar.setIcon(R.drawable.icon_bw);
        //actionBar.setDisplayShowHomeEnabled(true);

        Toolbar toolbar = (Toolbar) findViewById(R.id.appbar);
        setSupportActionBar(toolbar);

        //custom fonts
        Typeface comfortaa_b = Typeface.createFromAsset(getAssets(), "fonts/Comfortaa-Bold.ttf");
        Typeface comfortaa_r = Typeface.createFromAsset(getAssets(), "fonts/Comfortaa-Regular.ttf");
        conectando= (TextView) findViewById(R.id.VserviceState);
        conectando.setTypeface(comfortaa_r);
        enq_title= (TextView) findViewById(R.id.title_enq);
        enq_title.setTypeface(comfortaa_b);

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
        public void OnServerCall(JsonObject obj, BufferedWriter socketWriter) {

            getApplicationContext().registerReceiver(new NotificationButtonsReceiver(socketWriter), new IntentFilter(EnqService.NOTIFICATION_ACTION));

            Integer paydeskNumber = obj.get("paydesk").getAsInt();
            Integer callTimeout = obj.get("call_timeout").getAsInt();

            Intent confirmIntent = new Intent(EnqService.NOTIFICATION_ACTION);
            confirmIntent.putExtra(EnqService.NOTIFICATION_ACTION_EXTRA,EnqService.CONFIRM);

            Intent extendIntent = new Intent(EnqService.NOTIFICATION_ACTION);
            extendIntent.putExtra(EnqService.NOTIFICATION_ACTION_EXTRA,EnqService.EXTEND);

            Intent cancelIntent = new Intent(EnqService.NOTIFICATION_ACTION);
            cancelIntent.putExtra(EnqService.NOTIFICATION_ACTION_EXTRA,EnqService.CANCEL);

            PendingIntent confirmPendingIntent = PendingIntent.getBroadcast(getBaseContext(),EnqService.REQUEST_CODE,confirmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            PendingIntent extendPendingIntent = PendingIntent.getBroadcast(getBaseContext(),EnqService.REQUEST_CODE,extendIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            PendingIntent cancelPendingIntent = PendingIntent.getBroadcast(getBaseContext(),EnqService.REQUEST_CODE,cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(getBaseContext())
                    .setSmallIcon(R.drawable.icon_bw)
                    .setContentTitle("EnQ")
                    .setContentText("Ya puede acercarse a la caja número " + paydeskNumber.toString() )
                    .addAction(
                        new NotificationCompat.Action(R.drawable.ic_done_black_24dp, "Confirmar", confirmPendingIntent)
                    )
                    .addAction(
                        new NotificationCompat.Action(R.drawable.ic_alarm_add_black_24dp, "Extender", extendPendingIntent)
                    )
                    .addAction(
                        new NotificationCompat.Action(R.drawable.ic_clear_black_24dp, "Cancelar", cancelPendingIntent)
                    );

            NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            mNotifyMgr.notify(R.integer.notification_id, mBuilder.build());
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
