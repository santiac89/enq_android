package org.allin.enq.activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.allin.enq.R;
import org.allin.enq.service.EnqService;
import org.allin.enq.util.EnqActivity;

import butterknife.Bind;
import butterknife.ButterKnife;


public class WaitingActivity extends EnqActivity {

    EnqService enqService = null;

    //@Bind(R.id.estimated_text_view) TextView estimatedTimeTextView;
    @Bind(R.id.waiting_client_number_text_view) TextView numberTextView;
    @Bind(R.id.group_name_text_view) TextView groupNameTextView;
    @Bind(R.id.waiting_cancel_button) Button cancelButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waiting);
        ButterKnife.bind(this);
        setupActivity(R.id.waiting_activity_toolbar, "Llamada en espera");

    }


    @Override
    protected void onStop() {
        super.onStop();
        unbindService(mConnection);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        enqService.cancelWaitingForCall();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, EnqService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {

            EnqService.EnqServiceBinder binder = (EnqService.EnqServiceBinder) service;
            enqService = binder.getService();

            if (!enqService.isWaitingForServerCall())
                finish();

            numberTextView.setText(enqService.getClientNumber().toString());
            groupNameTextView.setText(enqService.getGroupName());

            cancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    enqService.cancelWaitingForCall();
                    finish();
                }
            });

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };
}
