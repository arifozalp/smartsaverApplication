package com.example.smartsaver.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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

    private EditText recipientInput, amountInput;
    private ImageButton sendButton;

    private int userId;
    private String userEmail;

    private final String BASE_URL = "http://10.0.2.2:3000";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer);

        recipientInput = findViewById(R.id.recipientInput);
        amountInput = findViewById(R.id.amountInput);
        sendButton = findViewById(R.id.sendButton);

        userId = getIntent().getIntExtra("user_id", -1);
        userEmail = getIntent().getStringExtra("user_email");

        sendButton.setOnClickListener(v -> {
            String receiverEmail = recipientInput.getText().toString().trim();
            String amountStr = amountInput.getText().toString().trim();

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

            fetchTargetUserIdAndSend(receiverEmail, amount);
        });
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
                error -> {
                    Toast.makeText(this, "Recipient not found", Toast.LENGTH_SHORT).show();
                }
        );

        Volley.newRequestQueue(this).add(request);
    }

    private void sendTransferRequest(int targetUserId, double amount) {
        String url = BASE_URL + "/transactions";

        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    Toast.makeText(this, "Transfer Successful", Toast.LENGTH_SHORT).show();
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
                Map<String, String> params = new HashMap<>();
                params.put("user_id", String.valueOf(userId));
                params.put("target_user_id", String.valueOf(targetUserId));
                params.put("type", "transfer");
                params.put("stock_code", "");
                params.put("price", "1");
                params.put("amount", String.valueOf(amount));
                params.put("date", new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()));
                return params;
            }
        };

        Volley.newRequestQueue(this).add(request);
    }

    private void updateBalanceAfterTransfer() {
        String url = BASE_URL + "/user_profiles/" + userId;

        JsonObjectRequest balanceRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        double updatedBalance = response.getDouble("balance");
                        Toast.makeText(this, "Updated Balance: ₺" + updatedBalance, Toast.LENGTH_SHORT).show();
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
}
