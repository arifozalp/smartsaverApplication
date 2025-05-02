package com.example.smartsaver.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.smartsaver.R;

public class DashboardActivity extends AppCompatActivity {

    private TextView welcomeText, balanceText;
    private Button btnTransfer, btnSmartPlan, btnStocks, btnMyStats;

    private int userId;
    private String userEmail;
    private final String BASE_URL = "http://10.0.2.2:3000/users/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        welcomeText = findViewById(R.id.welcomeText);
        balanceText = findViewById(R.id.balanceText);
        btnTransfer = findViewById(R.id.btnTransfer);
        btnSmartPlan = findViewById(R.id.btnSmartPlan);
        btnStocks = findViewById(R.id.btnStocks);
        btnMyStats = findViewById(R.id.btnMyStats);

        // Login ekranından gelen kullanıcı bilgileri
        userId = getIntent().getIntExtra("user_id", -1); // kullanıcı ID'si taşınmalı
        userEmail = getIntent().getStringExtra("user_email");

        welcomeText.setText("Welcome, " + userEmail);

        if (userId != -1) {
            fetchUserData(userId);
        } else {
            Toast.makeText(this, "Kullanıcı bilgisi alınamadı", Toast.LENGTH_SHORT).show();
        }

        btnTransfer.setOnClickListener(v -> {
            Intent intent = new Intent(this, TransferActivity.class);
            intent.putExtra("user_id", userId);
            intent.putExtra("user_email", userEmail);
            startActivity(intent);
        });

        btnSmartPlan.setOnClickListener(v -> {
            Intent intent = new Intent(this, SmartPlanActivity.class);
            intent.putExtra("user_id", userId);
            intent.putExtra("user_email", userEmail);
            startActivity(intent);
        });

        btnStocks.setOnClickListener(v -> {
            Intent intent = new Intent(this, StockListActivity.class);
            startActivity(intent);
        });

        btnMyStats.setOnClickListener(v -> {
            Intent intent = new Intent(this, MyStatsActivity.class);
            intent.putExtra("user_id", userId);
            startActivity(intent);
        });
    }

    private void fetchUserData(int id) {
        String url = BASE_URL + id;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        String email = response.getString("email");
                        double balance = response.getDouble("balance");

                        welcomeText.setText("Welcome, " + email);
                        balanceText.setText("Balance: ₺" + String.format("%.2f", balance));
                    } catch (Exception e) {
                        Toast.makeText(this, "JSON çözümleme hatası", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "Sunucu bağlantı hatası", Toast.LENGTH_SHORT).show()
        );

        Volley.newRequestQueue(this).add(request);
    }
}
