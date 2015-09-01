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
import android.widget.Button;
import android.widget.TextView;

import org.allin.enq.R;
import org.allin.enq.service.EnqService;

import butterknife.Bind;
import butterknife.ButterKnife;


public class WaitingActivity extends Activity {

    EnqService mService = null;

    @Bind(R.id.estimatedTimeTextView) TextView estimatedTimeTextView;
    @Bind(R.id.numberTextView) TextView numberTextView;
    @Bind(R.id.cancelButton) Button cancelButton;
    @Bind(R.id.changeGroupButton) Button changeGroupButton;
    @Bind(R.id.moreTimeButton) Button moreTimeButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waiting);

        ButterKnife.bind(this);

        Intent intent = new Intent(this, EnqService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

    }

    @Override
    protected void onStart() {
        super.onStart();
        estimatedTimeTextView.setText(getIntent().getStringExtra("estimated"));
        numberTextView.setText(getIntent().getStringExtra("number"));
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
