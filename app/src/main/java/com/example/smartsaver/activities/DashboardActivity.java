package com.example.smartsaver.activities;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smartsaver.R;
import com.example.smartsaver.database.DBHelper;

public class DashboardActivity extends AppCompatActivity {

    private TextView welcomeText, balanceText;
    private Button btnTransfer, btnSmartPlan, btnStocks, btnMyStats;

    private DBHelper dbHelper;
    private String userEmail;
    private double userBalance;

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

        dbHelper = new DBHelper(this);

        // Email bilgisi LoginActivity'den gelir
        userEmail = getIntent().getStringExtra("user_email");
        welcomeText.setText("Welcome, " + userEmail);

        // Veritabanından bakiye çek
        userBalance = getBalance(userEmail);
        balanceText.setText("Balance: ₺" + String.format("%.2f", userBalance));

        // Buton tıklamaları
        btnTransfer.setOnClickListener(v -> {
            Intent intent = new Intent(this, TransferActivity.class);
            intent.putExtra("user_email", userEmail);
            startActivity(intent);
        });

        btnSmartPlan.setOnClickListener(v -> {
            Intent intent = new Intent(this, SmartPlanActivity.class);
            intent.putExtra("user_email", userEmail);
            startActivity(intent);
        });

        btnStocks.setOnClickListener(v -> {
            Intent intent = new Intent(this, StockListActivity.class);
            startActivity(intent);
        });

        btnMyStats.setOnClickListener(v -> {
            Intent intent = new Intent(this, MyStatsActivity.class);
            intent.putExtra("user_email", userEmail);
            startActivity(intent);
        });
    }

    private double getBalance(String email) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT balance FROM User WHERE email=?", new String[]{email});
        double balance = 0;
        if (cursor.moveToFirst()) {
            balance = cursor.getDouble(0);
        }
        cursor.close();
        return balance;
    }
}
