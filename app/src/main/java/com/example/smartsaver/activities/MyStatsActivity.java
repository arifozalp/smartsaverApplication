package com.example.smartsaver.activities;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smartsaver.R;
import com.example.smartsaver.database.DBHelper;

public class MyStatsActivity extends AppCompatActivity {

    private TextView totalInvestment, totalProfit, positiveCount, negativeCount;
    private DBHelper dbHelper;
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_stats);

        totalInvestment = findViewById(R.id.totalInvestment);
        totalProfit = findViewById(R.id.totalProfit);
        positiveCount = findViewById(R.id.positiveCount);
        negativeCount = findViewById(R.id.negativeCount);

        dbHelper = new DBHelper(this);
        userEmail = getIntent().getStringExtra("user_email");

        loadStats();
    }

    private void loadStats() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Kullanıcı ID’sini bul
        Cursor userCursor = db.rawQuery("SELECT id FROM User WHERE email = ?", new String[]{userEmail});
        if (!userCursor.moveToFirst()) return;
        int userId = userCursor.getInt(0);
        userCursor.close();

        // Tüm yatırımları al
        Cursor cursor = db.rawQuery("SELECT invested_amount, profit FROM Investment WHERE user_id = ?", new String[]{String.valueOf(userId)});

        double total = 0, profit = 0;
        int profitCount = 0, lossCount = 0;

        while (cursor.moveToNext()) {
            double invested = cursor.getDouble(0);
            double gain = cursor.getDouble(1);

            total += invested;
            profit += gain;

            if (gain >= 0) profitCount++;
            else lossCount++;
        }

        cursor.close();

        totalInvestment.setText("Total Investment: ₺" + String.format("%.2f", total));
        totalProfit.setText("Total Profit: ₺" + String.format("%.2f", profit));
        positiveCount.setText("Profitable Stocks: " + profitCount);
        negativeCount.setText("Loss Stocks: " + lossCount);
    }
}
