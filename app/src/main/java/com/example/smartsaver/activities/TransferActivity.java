package com.example.smartsaver.activities;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.example.smartsaver.R;
import com.example.smartsaver.database.DBHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class TransferActivity extends AppCompatActivity {

    private EditText receiverEmailInput, amountInput;
    private Button sendButton;

    private DBHelper dbHelper;
    private String senderEmail;
    private static final String USERS_URL = "https://raw.githubusercontent.com/arifozalp/smartsaverApplication/refs/heads/master/data/users.json";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer);

        receiverEmailInput = findViewById(R.id.receiverEmailInput);
        amountInput = findViewById(R.id.amountInput);
        sendButton = findViewById(R.id.sendButton);

        dbHelper = new DBHelper(this);
        senderEmail = getIntent().getStringExtra("user_email");

        sendButton.setOnClickListener(v -> {
            String receiverEmail = receiverEmailInput.getText().toString().trim();
            String amountStr = amountInput.getText().toString().trim();

            if (TextUtils.isEmpty(receiverEmail) || TextUtils.isEmpty(amountStr)) {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount;
            try {
                amount = Double.parseDouble(amountStr);
                if (amount <= 0) {
                    Toast.makeText(this, "Enter a valid amount", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Amount must be a number", Toast.LENGTH_SHORT).show();
                return;
            }

            // GitHub API üzerinden alıcının var olup olmadığını kontrol et
            checkReceiverExists(receiverEmail, amount);
        });
    }

    private void checkReceiverExists(String receiverEmail, double amount) {
        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, USERS_URL, null,
                response -> {
                    boolean found = false;
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            JSONObject user = response.getJSONObject(i);
                            String email = user.getString("email");
                            if (email.equalsIgnoreCase(receiverEmail)) {
                                found = true;
                                break;
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    if (found) {
                        processTransfer(receiverEmail, amount);
                    } else {
                        Toast.makeText(this, "Receiver not found in system", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "Connection error", Toast.LENGTH_SHORT).show()
        );

        Volley.newRequestQueue(this).add(request);
    }

    private void processTransfer(String receiverEmail, double amount) {
        double senderBalance = dbHelper.getBalance(senderEmail);

        if (senderBalance < amount) {
            Toast.makeText(this, "Insufficient balance", Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        db.execSQL("UPDATE User SET balance = balance - ? WHERE email = ?", new Object[]{amount, senderEmail});
        db.execSQL("INSERT INTO Transaction (sender_id, receiver_email, amount, date) VALUES ((SELECT id FROM User WHERE email = ?), ?, ?, datetime('now'))",
                new Object[]{senderEmail, receiverEmail, amount});

        Toast.makeText(this, "Transfer successful", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, DashboardActivity.class).putExtra("user_email", senderEmail));
        finish();
    }
}
