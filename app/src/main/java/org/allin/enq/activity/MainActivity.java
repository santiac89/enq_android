package org.allin.enq.activity;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import org.allin.enq.R;
import org.allin.enq.model.Group;
import org.allin.enq.service.EnqService;
import org.allin.enq.service.EnqServiceListener;
import org.allin.enq.util.EmptyListAdapter;
import org.allin.enq.util.EnqActivity;
import org.allin.enq.util.GroupListAdapter;

import java.io.IOException;
import java.util.List;
import butterknife.Bind;
import butterknife.ButterKnife;

public class MainActivity extends EnqActivity {

    @Bind(R.id.groups_list_view) ListView groupListView;
    @Bind(R.id.groups_list_swipe_refresh_layout) SwipeRefreshLayout groupsListSwipeRefreshLayout;
    @Bind(R.id.no_groups_found_text_view) TextView noGroupsFoundTextView;

    private final Integer SERVER_NOT_FOUND_MAX_RETRIES = 3;
    private final Integer GROUPS_NOT_FOUND_MAX_RETRIES = 3;

    EnqService enqService = null;
    WifiManager wifiManager = null;
    ConnectivityManager connManager = null;
    Integer serverNotFoundRetries = 0;
    Integer groupsNotFoundRetries = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        bindEnqService(mConnection);

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        setupActivity();

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mConnection);
    }

    @Override
    protected void onStart() {
        super.onStart();


        groupsListSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {

                NetworkInfo wifiState = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

                if (!wifiManager.isWifiEnabled() || !wifiState.isConnected()) {
                    groupsListSwipeRefreshLayout.setRefreshing(false);
                    promptForWifiNetwork();
                } else {
                    noGroupsFoundTextView.setVisibility(View.INVISIBLE);
                    groupListView.setAdapter(new EmptyListAdapter());
                    enqService.findService();
                }
            }
        });


        groupListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                boolean enable = false;
                if (groupListView != null && groupListView.getChildCount() > 0) {
                    // check if the first item of the list is visible
                    boolean firstItemVisible = groupListView.getFirstVisiblePosition() == 0;
                    // check if the top of the first item is visible
                    boolean topOfFirstItemVisible = groupListView.getChildAt(0).getTop() == 0;
                    // enabling or disabling the refresh layout
                    enable = firstItemVisible && topOfFirstItemVisible;
                }
                groupsListSwipeRefreshLayout.setEnabled(enable);
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

            }
        });

    }


    /**
     * Connection with the background EnqService
     */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {

            EnqService.EnqServiceBinder binder = (EnqService.EnqServiceBinder) service;
            enqService = binder.getService();
            enqService.setListener(new MyServiceListener());

            NetworkInfo wifiState = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

            if (enqService.isWaitingForServerCall()) {
                Intent intent = new Intent(getApplicationContext(),WaitingActivity.class);
                startActivity(intent);
            }

            if (!wifiManager.isWifiEnabled() || !wifiState.isConnected()) {

                promptForWifiNetwork();

            } else {

                groupsListSwipeRefreshLayout.setRefreshing(true);
                enqService.findService();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        }
    };


    /**
     * Creates a modal to ask the user to turn on Wi-Fi and connect to the server network
     */
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
                        dialog.cancel();
                    }
                }
            );

        AlertDialog alert = builder.create();
        alert.show();

    }

    /**
     *  EnqService events listener for MainActivity
     */
    public class MyServiceListener implements EnqServiceListener
    {

        @Override
        public void OnGroupsFound(final List<Group> groups) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                if (groups.size() == 0) {
                    noGroupsFoundTextView.setVisibility(View.VISIBLE);
                    groupListView.setAdapter(new EmptyListAdapter());
                } else {
                    groupListView.setAdapter(new GroupListAdapter(enqService, groups, getApplicationContext()));
                }

                groupsListSwipeRefreshLayout.setRefreshing(false);
                }
            });
        }

        @Override
        public void OnGroupsNotFound() {
            if (groupsNotFoundRetries < GROUPS_NOT_FOUND_MAX_RETRIES) {
                groupsNotFoundRetries++;
                enqService.findGroups();
            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                    noGroupsFoundTextView.setVisibility(View.VISIBLE);
                    groupListView.setAdapter(new EmptyListAdapter());
                    groupsListSwipeRefreshLayout.setRefreshing(false);
                    }
                });

                groupsNotFoundRetries = 0;
            }
        }

        @Override
        public void OnClientEnqueued() {
            enqService.startWaitingForCall();
            Intent intent = new Intent(MainActivity.this, WaitingActivity.class);
            startActivity(intent);
        }

        @Override
        public void OnClientNotEnqueued(IOException e) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), R.string.couldnt_assign_group, Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void OnServiceException(Exception e) {

        }

        @Override
        public void OnServerFound() {
          enqService.findGroups();
        }

        @Override
        public void OnServerNotFound(Exception e) {
            if (serverNotFoundRetries < SERVER_NOT_FOUND_MAX_RETRIES) {
                serverNotFoundRetries++;
                enqService.findService();
            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                    groupListView.setAdapter(new EmptyListAdapter());
                    groupsListSwipeRefreshLayout.setRefreshing(false);
                    noGroupsFoundTextView.setVisibility(View.VISIBLE);
                    }
                });
                serverNotFoundRetries = 0;
            }
        }
    }

}
