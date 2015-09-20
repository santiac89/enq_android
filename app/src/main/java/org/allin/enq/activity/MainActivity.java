package org.allin.enq.activity;


import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
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

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import retrofit.RetrofitError;

public class MainActivity extends EnqActivity {

    @Bind(R.id.groups_list_view) ListView groupListView;
    @Bind(R.id.groups_list_swipe_refresh_layout) SwipeRefreshLayout groupsListSwipeRefreshLayout;
    @Bind(R.id.no_groups_found_text_view) TextView noGroupsFoundTextView;


    EnqService enqService = null;
    Boolean mBound = false;
    WifiManager wifiManager = null;
    Integer serverNotFoundRetries = 0;
    Integer serverNotFoundMaxRetries = 3;
    Integer groupsNotFoundRetries = 0;
    Integer groupsNotFoundMaxRetries = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = new Intent(this, EnqService.class);
        startService(intent);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        setupActivity(R.id.main_activity_toolbar, "Tipo de atención");

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
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
                if (mBound) {
                    noGroupsFoundTextView.setVisibility(View.INVISIBLE);
                    groupListView.setAdapter(new EmptyListAdapter());
                    enqService.checkForServer();
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
            mBound = true;

            if (enqService.isWaitingForServerCall()) {
                Intent intent = new Intent(getApplicationContext(),WaitingActivity.class);
                startActivity(intent);
            }
            
            if (!wifiManager.isWifiEnabled()) {

                promptForWifiNetwork();

            } else {

                groupsListSwipeRefreshLayout.setRefreshing(true);
                enqService.checkForServer();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
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
                            MainActivity.this.finish();
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
                    groupListView.setAdapter(new GroupListAdapter(enqService, groups, getApplicationContext()));
                    groupsListSwipeRefreshLayout.setRefreshing(false);
                }
            });
        }

        @Override
        public void OnGroupsNotFound(RetrofitError e) {
            if (groupsNotFoundRetries < groupsNotFoundMaxRetries) {
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
            Intent intent = new Intent(getApplicationContext(),WaitingActivity.class);
            startActivity(intent);
        }

        @Override
        public void OnClientNotEnqueued(RetrofitError e) {
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
            if (serverNotFoundRetries < serverNotFoundMaxRetries) {
                serverNotFoundRetries++;
                enqService.checkForServer();
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
