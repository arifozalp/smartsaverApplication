package com.example.smartsaver.backgroundServices;

import static androidx.constraintlayout.widget.Constraints.TAG;

import android.app.*;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.smartsaver.R;

import org.json.JSONObject;

import java.util.Locale;

public class TransferMonitorService extends Service {
    private static final String CH_MONITOR = "transfer_monitor";
    private static final String CH_ALERT   = "transfer_alerts";
    public static final String EXTRA_USER_ID = "user_id";
    private static final int    NOTIF_ID     = 1001;

    private static final String PREF  = "transfer_prefs";
    private static final String KEY   = "last_incoming_id";

    private static final long   INTERVAL = 3_000;

    private final Handler handler = new Handler();
    private int userId;

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        userId = intent.getIntExtra(EXTRA_USER_ID, -1);
        if (userId == -1) stopSelf();

        createChannels();
        startForeground(NOTIF_ID, buildForegroundNotif("Dinleniyor…"));

        handler.post(pollRunnable);
        return START_STICKY;
    }

    private final Runnable pollRunnable = new Runnable() {
        @Override public void run() {
            pollOnce();
            handler.postDelayed(this, INTERVAL);
        }
    };

    private void pollOnce() {
        SharedPreferences sp = getSharedPreferences(PREF, MODE_PRIVATE);
        long lastId = sp.getLong(KEY, 0);

        String url = "http://10.0.2.2:3000/incoming/" + userId + "?since_id=" + lastId;

        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                res -> {
                    try {
                        if (res.getBoolean("new")) {
                            JSONObject t = res.getJSONObject("transfer");
                            long id   = t.getLong("id");
                            String s  = t.getString("sender_name");
                            double a  = t.getDouble("amount");

                            showTransferNotification(s, a);
                            sp.edit().putLong(KEY, id).apply();
                        }
                    } catch (Exception e) {
                    }
                },
                err -> Log.e(TAG, "HTTP error", err)
        );
        Volley.newRequestQueue(this).add(req);
    }

    private void createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = getSystemService(NotificationManager.class);

            NotificationChannel monitor = new NotificationChannel(
                    CH_MONITOR, "Transfer Monitor",
                    NotificationManager.IMPORTANCE_MIN);
            monitor.setSound(null, null);
            monitor.setShowBadge(false);
            nm.createNotificationChannel(monitor);

            NotificationChannel alert = new NotificationChannel(
                    CH_ALERT, "Transfer Alerts",
                    NotificationManager.IMPORTANCE_HIGH);
            alert.enableVibration(true);
            alert.setSound(Settings.System.DEFAULT_NOTIFICATION_URI,
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .build());
            nm.createNotificationChannel(alert);
        }
    }

    private Notification buildForegroundNotif(String text) {
        return new NotificationCompat.Builder(this, CH_MONITOR)
                .setSmallIcon(R.drawable.logo_turtle)
                .setContentTitle(text)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setOngoing(true)
                .setSilent(true)
                .build();
    }

    private void showTransferNotification(String senderEmail, double amount) {
        String title = "Transfer Received";
        String body  = String.format("$%.2f from %s", amount, senderEmail);

        // 3) Bildirimi oluştur
        NotificationCompat.Builder nb = new NotificationCompat.Builder(this, CH_ALERT)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL);

        NotificationManagerCompat.from(this)
                .notify((int) System.currentTimeMillis(), nb.build());
    }

    @Override public IBinder onBind(Intent intent) { return null; }
    @Override public void onDestroy() {
        Log.d(TAG, "Service destroyed");
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
