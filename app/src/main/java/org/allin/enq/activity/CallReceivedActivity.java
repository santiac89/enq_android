package org.allin.enq.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import org.allin.enq.R;
import org.allin.enq.api.EnqCallInfo;
import org.allin.enq.service.EnqService;
import org.allin.enq.util.EnqActivity;

import butterknife.Bind;
import butterknife.ButterKnife;

public class CallReceivedActivity extends EnqActivity {

    public static final String CONFIRM = "confirm";
    public static final String EXTEND = "extend";
    public static final String CANCEL = "cancel";

    private EnqService enqService = null;
    private Runnable timeoutCallbackRunnable = new Runnable() {
        @Override
        public void run() {
            if (!enqService.clientReachedReenqueueLimit())
                enqService.startWaitingForCall();

            finish();
        }
    };

    private Handler timeoutHandler = new Handler();

    @Bind(R.id.call_received_confirm_button) Button confirmButton;
    @Bind(R.id.call_received_cancel_button) Button cancelButton;
    @Bind(R.id.call_received_extend_button) Button extendButton;
    @Bind(R.id.call_received_paydesk_number_text_view) TextView paydeskNumberTextView;
    @Bind(R.id.call_received_client_number_text_view) TextView clientNumberTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = new Intent(this, EnqService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        setContentView(R.layout.activity_call_received);

        ButterKnife.bind(this);

        wakeUp();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mConnection);
    }

    private void wakeUp() {

        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN |
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_FULLSCREEN |
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(1000);

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

            paydeskNumberTextView.setText(enqService.getPaydeskNumber().toString());
            clientNumberTextView.setText(enqService.getClientNumber().toString());

            confirmButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    timeoutHandler.removeCallbacks(timeoutCallbackRunnable);
                    enqService.sendCallResponse(CONFIRM);
                    Intent confirmedIntent = new Intent(getApplicationContext(),ConfirmedActivity.class);
                    startActivity(confirmedIntent);
                    finish();
                }
            });

            cancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    timeoutHandler.removeCallbacks(timeoutCallbackRunnable);
                    enqService.sendCallResponse(CANCEL);
                    finish();
                }
            });

            extendButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    timeoutHandler.removeCallbacks(timeoutCallbackRunnable);
                    enqService.sendCallResponse(EXTEND);
                    enqService.startWaitingForCall();
                    finish();
                }
            });

            if (enqService.clientReachedReenqueueLimit()) {
                extendButton.setVisibility(View.GONE);
            }

            timeoutHandler.postDelayed(timeoutCallbackRunnable, enqService.getCallTimeout());


        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };



}
