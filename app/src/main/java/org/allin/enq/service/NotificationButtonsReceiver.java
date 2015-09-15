package org.allin.enq.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import org.allin.enq.R;

import java.util.Random;

import static org.allin.enq.service.EnqService.*;

/**
 * Created by santiagocarullo on 9/12/15.
 */
public class NotificationButtonsReceiver extends BroadcastReceiver {

    public static final String NOTIFICATION_ACTION = "org.allin.enq.NOTIFICATION_ACTION";
    public static final String NOTIFICATION_ACTION_EXTRA = "notification_action";
    public static final String REENQUEUE_COUNT_EXTRA = "notification_reenqueue_count";
    public static final String CONFIRM = "confirm";
    public static final String EXTEND = "extend";
    public static final String CANCEL = "cancel";
    public static final String TIMEOUT = "client_triggered_timeout";

    private Integer DEFAULT_REENQUEUE_COUNT_EXTRA = 3;
    private EnqService enqService;
    private Context context;

    public NotificationButtonsReceiver(Context context, EnqService service) {
        this.enqService = service;
        this.context = context;
    }

    public PendingIntent createPendingIntent(String action) {
        return createPendingIntent(action, DEFAULT_REENQUEUE_COUNT_EXTRA);
    }

    public PendingIntent createPendingIntent(String action, Integer reenqueueCount) {
        Intent intent = new Intent(NOTIFICATION_ACTION);
        intent.putExtra(NOTIFICATION_ACTION_EXTRA, action);
        intent.putExtra(REENQUEUE_COUNT_EXTRA, reenqueueCount);
        return PendingIntent.getBroadcast(context, new Random().nextInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        String action = intent.getStringExtra(NOTIFICATION_ACTION_EXTRA);
        Integer reenqueueCount = intent.getIntExtra(REENQUEUE_COUNT_EXTRA, DEFAULT_REENQUEUE_COUNT_EXTRA);

        switch (action) {
            case CONFIRM:
                enqService.sendResponse(action);
                // TODO Mostrar pantalla de confirmado
            break;

            case EXTEND:
                enqService.sendResponse(action);
                enqService.waitForServerCall();
            break;

            case CANCEL:
                enqService.sendResponse(action);
                // TODO RESET ALL!
            break;

            case TIMEOUT:
                if (reenqueueCount < enqService.getReenqueueLimit())
                    enqService.waitForServerCall();
                else
                   return;
            break;
        }

        notificationManager.cancel(R.integer.notification_id);
        context.unregisterReceiver(this);
    }
}
