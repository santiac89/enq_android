package org.allin.enq.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.allin.enq.R;
import org.allin.enq.service.EnqService;
import org.allin.enq.util.EnqActivity;
import org.w3c.dom.Text;

import butterknife.Bind;
import butterknife.ButterKnife;

public class ConfirmedActivity extends EnqActivity {

    @Bind(R.id.confirmed_client_number_text_view) TextView clientNumberTextView;
    @Bind(R.id.confirmed_paydesk_number_text_view) TextView paydeskNumberTextView;
    @Bind(R.id.confirmed_close_button) Button closeButton;
    @Bind(R.id.remaining_time_text_view) TextView remainingTimeTextView;

    private EnqService enqService;
    private long timeToArrive = 0;
    private CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirmed);
        ButterKnife.bind(this);
        setupActivity();
        bindEnqService(mConnection);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startCountdown();
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

    public void startCountdown() {

        long remainingSeconds = timeToArrive - SystemClock.elapsedRealtime();

        if (countDownTimer != null || remainingSeconds < 0) return;

        countDownTimer = new CountDownTimer(remainingSeconds, 1000) {

            public void onTick(long millisUntilFinished) {
                long minutes = (millisUntilFinished / 1000) / 60;
                long seconds = (millisUntilFinished / 1000) % 60;
                remainingTimeTextView.setText(String.format("%02d:%02d",minutes,seconds));
            }

            public void onFinish() {
                closeButton.setVisibility(View.VISIBLE);
                remainingTimeTextView.setText("00:00");
            }

        };

        countDownTimer.start();

    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {

            EnqService.EnqServiceBinder binder = (EnqService.EnqServiceBinder) service;
            enqService = binder.getService();

            clientNumberTextView.setText(enqService.getClientNumber().toString());
            paydeskNumberTextView.setText(enqService.getPaydeskNumber().toString());
            timeToArrive = SystemClock.elapsedRealtime() + (enqService.getPaydeskArrivalTimeout() * 1000);

            closeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });

            startCountdown();

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };
}
