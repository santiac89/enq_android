package org.allin.enq.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.io.BufferedWriter;
import java.io.IOException;
import static org.allin.enq.service.EnqService.*;

/**
 * Created by santiagocarullo on 9/12/15.
 */
public class NotificationButtonsReceiver extends BroadcastReceiver {

    private BufferedWriter socketWriter;

    public NotificationButtonsReceiver(BufferedWriter socketWriter) {
        this.socketWriter = socketWriter;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getStringExtra(NOTIFICATION_ACTION_EXTRA);

        try {
            socketWriter.write(action);
            socketWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        switch (action) {
            case CONFIRM:
                break;
            case EXTEND:
                break;
            case CANCEL:
                break;
        }

        context.unregisterReceiver(this);
    }
}
