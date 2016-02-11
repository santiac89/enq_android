package org.allin.enq.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.os.Vibrator;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import org.allin.enq.R;
import org.allin.enq.service.EnqService;
import org.allin.enq.util.EnqActivity;

import java.io.IOException;

import butterknife.Bind;
import butterknife.ButterKnife;

public class CallReceivedActivity extends EnqActivity {

    public static final String CONFIRM = "confirm";
    public static final String EXTEND = "extend";
    public static final String CANCEL = "cancel";

    private EnqService enqService = null;
    private Boolean timeoutFired = false;

    private Runnable timeoutCallbackRunnable = new Runnable() {
        @Override
        public void run() {
            if (!enqService.clientReachedReenqueueLimit())
                enqService.startWaitingForCall();
            timeoutFired = true;
            finish();
        }
    };

    private Handler timeoutHandler = new Handler();
    private CountDownTimer countDownTimer;
    private MediaPlayer mp;
    private Boolean isInitialized = false;
    private Boolean responseSent = false;
    @Bind(R.id.call_received_confirm_button) Button confirmButton;
    @Bind(R.id.call_received_cancel_button) Button cancelButton;
    @Bind(R.id.call_received_extend_button) Button extendButton;
    @Bind(R.id.call_received_paydesk_number_text_view) TextView paydeskNumberTextView;
    @Bind(R.id.call_received_client_number_text_view) TextView clientNumberTextView;
    @Bind(R.id.remaining_to_response_text_view) TextView remainingTimeToResponseTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call_received);
        ButterKnife.bind(this);
        wakeUp();
    }

    @Override
    protected void onStart() {
        super.onStart();

        Intent intent = new Intent(this, EnqService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);


    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(mConnection);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (responseSent || timeoutFired) return;

        if (enqService.clientReachedReenqueueLimit()) {
            timeoutHandler.removeCallbacks(timeoutCallbackRunnable);
            enqService.sendCallResponse(CANCEL);
            // TODO  POR QUE GAROMPA FALLA ACA Y NO MANDA MENSAJE?
        } else {
            timeoutHandler.removeCallbacks(timeoutCallbackRunnable);
            enqService.sendCallResponse(EXTEND);
            enqService.startWaitingForCall();

        }
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
        long[] pattern = {0, 500, 500, 500, 500, 500, 500, 500};
        vibrator.vibrate(pattern, -1);

        mp = MediaPlayer.create(getApplicationContext(), R.raw.bell);
        mp.setLooping(false);
        mp.start();
        mp.setOnCompletionListener(new SoundtrackPlayerListener());
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    public void startCountdown() {

        countDownTimer = new CountDownTimer(enqService.getCallTimeout(), 1000) {

            public void onTick(long millisUntilFinished) {
                Long seconds = millisUntilFinished / 1000;
                remainingTimeToResponseTextView.setText(seconds.toString());
            }

            public void onFinish() {
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

            if (isInitialized) return;

            isInitialized = true;

            paydeskNumberTextView.setText(enqService.getPaydeskNumber().toString());
            clientNumberTextView.setText(enqService.getClientNumber().toString());
            extendButton.setText("EXTENDER (" +enqService.getNextEstimatedTime()+ "')");

            confirmButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    timeoutHandler.removeCallbacks(timeoutCallbackRunnable);
                    enqService.sendCallResponse(CONFIRM);
                    responseSent = true;
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
                    responseSent = true;
                    finish();
                }
            });

            extendButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    timeoutHandler.removeCallbacks(timeoutCallbackRunnable);
                    enqService.sendCallResponse(EXTEND);
                    responseSent = true;
                    enqService.startWaitingForCall();
                    finish();
                }
            });

            if (enqService.clientReachedReenqueueLimit()) {
                extendButton.setVisibility(View.GONE);
            }

            timeoutHandler.postDelayed(timeoutCallbackRunnable, enqService.getCallTimeout());
            startCountdown();


        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            enqService = null;
        }
    };

    private class SoundtrackPlayerListener implements MediaPlayer.OnCompletionListener{

        int loopTimes = 0;

        @Override
        public void onCompletion(MediaPlayer mp) {

            if (loopTimes < 2) {
                loopTimes++;
                mp.seekTo(0);
                mp.start();
                return;
            }

            mp.reset();
            mp.release();
            mp = null;
        }
    }




}
