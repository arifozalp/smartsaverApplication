package com.example.smartsaver.activities;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.smartsaver.R;

public class DashboardActivity extends AppCompatActivity {

    private TextView   welcomeText;
    private TextView   balanceText;
    private LinearLayout btnTransfer;
    private LinearLayout btnSmartPlan;
    private LinearLayout btnStocks;
    private LinearLayout btnMyStats;

    private int    userId;
    private String userEmail;
    private static final String BASE_URL = "http://10.0.2.2:3000";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        welcomeText   = findViewById(R.id.welcomeText);
        balanceText   = findViewById(R.id.balanceText);
        btnTransfer   = findViewById(R.id.btnTransfer);
        btnSmartPlan  = findViewById(R.id.btnSmartPlan);
        btnStocks     = findViewById(R.id.btnStocks);
        btnMyStats    = findViewById(R.id.btnMyStats);

        userId    = getIntent().getIntExtra("user_id",   -1);
        userEmail = getIntent().getStringExtra("user_email");

        if (userId < 0 || userEmail == null) {
            Toast.makeText(this, "Kullanıcı bilgisi alınamadı", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        fetchUserBalance();

        btnTransfer.setOnClickListener(v -> {
            Intent i = new Intent(this, TransferActivity.class);
            i.putExtra("user_id",    userId);
            i.putExtra("user_email", userEmail);
            startActivityForResult(i, 1001);
        });

        btnSmartPlan.setOnClickListener(v -> {
            Intent i = new Intent(this, SmartPlanActivity.class);
            i.putExtra("user_id",    userId);
            i.putExtra("user_email", userEmail);
            startActivity(i);
        });

        btnStocks.setOnClickListener(v -> {
            Intent i = new Intent(this, StockListActivity.class);
            i.putExtra("user_id",    userId);
            i.putExtra("user_email", userEmail);
            startActivity(i);
        });

        btnMyStats.setOnClickListener(v -> {
            Intent i = new Intent(this, MyStatsActivity.class);
            i.putExtra("user_id",    userId);
            i.putExtra("user_email", userEmail);
            startActivity(i);
        });
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("Exit")
                .setMessage("Are you sure you want to log out?")
                .setCancelable(false)
                .setNegativeButton("No", (d, w) -> d.dismiss())
                .setPositiveButton("Yes", (d, w) -> {
                    Intent i = new Intent(this, LoginActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK |
                            Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                    finish();
                })
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchUserBalance();
    }

    private void fetchUserBalance() {
        String url = BASE_URL + "/user_profiles/" + userId;
        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    try {
                        double bal = response.getDouble("balance");
                        String fullName = response.getString("full_name");
                        welcomeText.setText("Welcome, " + fullName);
                        balanceText.setText(String.format("Balance: $%.2f", bal));
                    } catch (Exception e) {
                        Toast.makeText(this, "Veri çözümleme hatası", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "Sunucu bağlantısı başarısız", Toast.LENGTH_SHORT).show()
        );
        Volley.newRequestQueue(this).add(req);
    }
}
