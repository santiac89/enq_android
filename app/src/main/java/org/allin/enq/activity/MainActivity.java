package org.allin.enq.activity;


import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.view.View;
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
import java.util.List;
import java.util.Map;
import butterknife.Bind;
import butterknife.ButterKnife;
import retrofit.RetrofitError;
import static org.allin.enq.service.NotificationButtonsReceiver.*;

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
        public void OnServerCall(JsonObject obj) {

            Integer paydeskNumber = obj.get("paydesk").getAsInt();
            Integer callTimeout = obj.get("call_timeout").getAsInt();
            Integer reenqueueCount = obj.get("reenqueue_count").getAsInt();

            NotificationButtonsReceiver receiver = new NotificationButtonsReceiver( getApplicationContext(), mService );
            getApplicationContext().registerReceiver(receiver, new IntentFilter(NOTIFICATION_ACTION));

            PendingIntent confirmPendingIntent = receiver.createPendingIntent(CONFIRM);
            PendingIntent cancelPendingIntent = receiver.createPendingIntent(CANCEL);
            PendingIntent timeoutPendingIntent = receiver.createPendingIntent(TIMEOUT, reenqueueCount);

            PendingIntent watitingActivityIntent = PendingIntent.getActivity(getApplicationContext(), 0,new Intent(getApplicationContext(),WaitingActivity.class),PendingIntent.FLAG_UPDATE_CURRENT);

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getBaseContext());

            notificationBuilder
                .setSmallIcon(R.drawable.icon_bw)
                .setContentTitle("EnQ")
                .setContentText("Ya puede acercarse a la caja número " + paydeskNumber.toString())
                .addAction(
                        new NotificationCompat.Action(R.drawable.ic_done_black_24dp, "", confirmPendingIntent)
                )
                .addAction(
                        new NotificationCompat.Action(R.drawable.ic_clear_black_24dp, "", cancelPendingIntent)
                ).setContentIntent(watitingActivityIntent);

            if (reenqueueCount < mService.getReenqueueLimit()) {

                PendingIntent extendPendingIntent = receiver.createPendingIntent(EXTEND);

                notificationBuilder.addAction(
                        new NotificationCompat.Action(R.drawable.ic_alarm_add_black_24dp, "", extendPendingIntent)
                );
            }

            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notificationManager.notify(R.integer.notification_id, notificationBuilder.build());

            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

            if (Build.VERSION.SDK_INT >= 19)
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, callTimeout, timeoutPendingIntent);
            else
                alarmManager.set(AlarmManager.RTC_WAKEUP, callTimeout, timeoutPendingIntent);
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

}
