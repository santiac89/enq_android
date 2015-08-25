package org.allin.enq.activity;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import org.allin.enq.R;
import org.allin.enq.service.EnqService;


public class WaitingActivity extends Activity {

    EnqService mService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waiting);


        Intent intent = new Intent(this, EnqService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

    }

    @Override
    protected void onStart() {
        super.onStart();

        TextView estimatedTimeView =     (TextView) findViewById(R.id.VestimatedTime);
        TextView numberView = (TextView) findViewById(R.id.Vnumber);

        estimatedTimeView.setText(getIntent().getStringExtra("estimated"));
        numberView.setText(getIntent().getStringExtra("number"));



    }

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {

            EnqService.EnqServiceBinder binder = (EnqService.EnqServiceBinder) service;
            mService = binder.getService();
            mService.waitForServerCall();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };
}
