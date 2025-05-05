package com.example.smartsaver.activities;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.smartsaver.R;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class TransferActivity extends AppCompatActivity {
    private static final String CHANNEL_ID = "transfer_channel";
    private static final int REQUEST_POST_NOTIFICATIONS = 1002;

    private EditText recipientInput, amountInput;
    private ImageButton sendButton;

    private int userId;
    private String userEmail;

    private String lastRecipientEmail;
    private double lastAmount;

    private final String BASE_URL = "http://10.0.2.2:3000";

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (!isGranted) {
                    Toast.makeText(this, "Bildirim izni kapalı — transfer bildirimleri gönderilemeyecek", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer);

        createNotificationChannel();
        ensureNotificationPermission();

        recipientInput = findViewById(R.id.recipientInput);
        amountInput    = findViewById(R.id.amountInput);
        sendButton     = findViewById(R.id.sendButton);

        userId    = getIntent().getIntExtra("user_id", -1);
        userEmail = getIntent().getStringExtra("user_email");

        sendButton.setOnClickListener(v -> {
            String receiverEmail = recipientInput.getText().toString().trim();
            String amountStr     = amountInput.getText().toString().trim();

            if (receiverEmail.isEmpty() || amountStr.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount;
            try {
                amount = Double.parseDouble(amountStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
                return;
            }
            if (amount <= 0) {
                Toast.makeText(this, "Amount must be greater than 0", Toast.LENGTH_SHORT).show();
                return;
            }

            lastRecipientEmail = receiverEmail;
            lastAmount = amount;
            fetchTargetUserIdAndSend(receiverEmail, amount);
        });
    }

    private void ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void fetchTargetUserIdAndSend(String receiverEmail, double amount) {
        String url = BASE_URL + "/get_user_id_by_email?email=" + receiverEmail;

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    try {
                        int targetUserId = response.getInt("id");
                        sendTransferRequest(targetUserId, amount);
                    } catch (Exception e) {
                        Toast.makeText(this, "Error parsing recipient data", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "Recipient not found", Toast.LENGTH_SHORT).show()
        );

        Volley.newRequestQueue(this).add(request);
    }

    private void sendTransferRequest(int targetUserId, double amount) {
        String url = BASE_URL + "/transactions";

        StringRequest request = new StringRequest(Request.Method.POST, url,
                resp -> {
                    // only show notification if we have permission
                    if (NotificationManagerCompat.from(this).areNotificationsEnabled()) {
                        sendTransferNotification(lastRecipientEmail, lastAmount);
                    }
                    updateBalanceAfterTransfer();
                },
                error -> {
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        String errorMsg = new String(error.networkResponse.data);
                        Toast.makeText(this, "Error: " + errorMsg, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Transfer Failed: Network error", Toast.LENGTH_SHORT).show();
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        .format(new Date());
                Map<String, String> params = new HashMap<>();
                params.put("user_id",        String.valueOf(userId));
                params.put("target_user_id", String.valueOf(targetUserId));
                params.put("type",           "transfer");
                params.put("stock_code",     "");
                params.put("price",          "1");
                params.put("amount",         String.valueOf(amount));
                params.put("date",           date);
                return params;
            }
        };

        Volley.newRequestQueue(this).add(request);
    }

    private void updateBalanceAfterTransfer() {
        String url = BASE_URL + "/user_profiles/" + userId;

        JsonObjectRequest balanceRequest = new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    try {
                        double updatedBalance = response.getDouble("balance");
                        Toast.makeText(this, "Updated Balance: $" + updatedBalance, Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(this, "Failed to update balance", Toast.LENGTH_SHORT).show();
                    }
                    finish();
                },
                error -> {
                    Toast.makeText(this, "Failed to fetch updated balance", Toast.LENGTH_SHORT).show();
                    finish();
                }
        );

        Volley.newRequestQueue(this).add(balanceRequest);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel chan = new NotificationChannel(
                    CHANNEL_ID,
                    "Transfers",
                    NotificationManager.IMPORTANCE_HIGH
            );
            chan.setDescription("Notifies when a transfer completes");
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(chan);
        }
    }

    private void sendTransferNotification(String toEmail, double amt) {
        String title = "Transfer Completed";
        String text  = String.format(Locale.getDefault(), "$%.2f sent to %s", amt, toEmail);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat.from(this)
                .notify((int) System.currentTimeMillis(), builder.build());
    }
}
