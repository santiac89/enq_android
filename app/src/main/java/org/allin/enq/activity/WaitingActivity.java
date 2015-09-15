package org.allin.enq.activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import org.allin.enq.R;
import org.allin.enq.service.EnqService;

import butterknife.Bind;
import butterknife.ButterKnife;


public class WaitingActivity extends EnqActivity {

    EnqService mService = null;

    @Bind(R.id.estimated_text_view) TextView estimatedTimeTextView;
    @Bind(R.id.number_text_view) TextView numberTextView;
    @Bind(R.id.cancel_button) Button cancelButton;
    @Bind(R.id.change_group_button) Button changeGroupButton;
    @Bind(R.id.more_time_button) Button moreTimeButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waiting);

        ButterKnife.bind(this);

        setupActionBar(R.id.waiting_activity_toolbar, "Llamada en espera");

        Intent intent = new Intent(this, EnqService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

    }

    @Override
    protected void onStart() {
        super.onStart();
        estimatedTimeTextView.setText(getIntent().getStringExtra("estimated"));
        numberTextView.setText(getIntent().getStringExtra("number"));

    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {

            EnqService.EnqServiceBinder binder = (EnqService.EnqServiceBinder) service;
            mService = binder.getService();

            if (!mService.isWaitingForServerCall()) mService.waitForServerCall();

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };
}
