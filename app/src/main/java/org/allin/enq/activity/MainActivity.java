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
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.Layout;
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


public class MainActivity extends EnqActivity {

    @Bind(R.id.groups_list_view) ListView groupListView;
    @Bind(R.id.refresh_button) Button refreshButton;
    @Bind(R.id.service_state_text_view) TextView serviceStateTextView;
    @Bind(R.id.groups_loading_progress_bar) ProgressBar groupsLoadingProgressBar;

    EnqService mService = null;
    Boolean mBound = false;
    WifiManager wifiManager = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        setupActionBar(R.id.main_activity_toolbar, "EnQ");

        serviceStateTextView.setTypeface(comfortaa_regular);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        Intent intent = new Intent(this, EnqService.class);
        startService(intent);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

    }

    @Override
    protected void onStart() {
        super.onStart();
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mBound) {
                    groupListView.setVisibility(View.INVISIBLE);
                    groupsLoadingProgressBar.setVisibility(View.VISIBLE);
                    mService.checkForEnqServer();
                    refreshButton.setEnabled(false);
                }

            }
        });
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {

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
                    groupsLoadingProgressBar.setVisibility(View.INVISIBLE);
                    refreshButton.setEnabled(true);
                    groupListView.setAdapter(new GroupListAdapter(mService,groups, getApplicationContext()));

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

            final Intent timeoutIntent = new Intent(EnqService.NOTIFICATION_ACTION);
            extendIntent.putExtra(EnqService.NOTIFICATION_ACTION_EXTRA,EnqService.TIMEOUT);

            PendingIntent confirmPendingIntent = PendingIntent.getBroadcast(getBaseContext(), 1, confirmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            PendingIntent extendPendingIntent = PendingIntent.getBroadcast(getBaseContext(), 2, extendIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            PendingIntent cancelPendingIntent = PendingIntent.getBroadcast(getBaseContext(), 3, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT);

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

            final NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            mNotifyMgr.notify(R.integer.notification_id, mBuilder.build());
            Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mNotifyMgr.cancel(R.integer.notification_id);
                    sendBroadcast(timeoutIntent);
                }
            }, callTimeout);


        }

        @Override
        public void OnServiceException(Exception e) {

        }

        @Override
        public void OnServerFound(final EnqRestApiInfo enqRestApiInfo) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    refreshButton.setEnabled(true);
                    serviceStateTextView.setBackgroundColor(getResources().getColor(R.color.quiet_green));
                    serviceStateTextView.setText(enqRestApiInfo.getName());
                }
            });

            mService.findGroups();
        }

        @Override
        public void OnServerNotFound(Exception e) {
            mService.checkForEnqServer();
        }
    }

//    public class QueueSelectedListener implements AdapterView.OnItemClickListener {
//
//        @Override
//        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//
//              Group selectedGroup = (Group) groupListView.getItemAtPosition(position);
//
//              mService.enqueueIn(selectedGroup);
//
//        }
//    }

}
