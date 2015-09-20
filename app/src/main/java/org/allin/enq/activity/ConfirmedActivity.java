package org.allin.enq.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.allin.enq.R;
import org.allin.enq.api.EnqCallInfo;
import org.allin.enq.service.EnqService;
import org.allin.enq.util.EnqActivity;

import butterknife.Bind;
import butterknife.ButterKnife;

public class ConfirmedActivity extends EnqActivity {

    public static String CONFIRMED_ACTIVITY_EXTRA = "confirmed_activity_extra";

    @Bind(R.id.confirmed_client_number_text_view) TextView clientNumberTextView;
    @Bind(R.id.confirmed_paydesk_number_text_view) TextView paydeskNumberTextView;
    @Bind(R.id.confirmed_close_button) Button closeButton;

    private EnqService enqService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirmed);
        ButterKnife.bind(this);

        setupActivity(R.id.confirmed_activity_toolbar, "Turno");

        Intent intent = new Intent(this, EnqService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unbindService(mConnection);
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

            clientNumberTextView.setText(enqService.getClientNumber().toString());
            paydeskNumberTextView.setText(enqService.getPaydeskNumber().toString());

            closeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };
}
