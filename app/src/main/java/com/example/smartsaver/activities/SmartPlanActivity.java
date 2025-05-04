package com.example.smartsaver.activities;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smartsaver.R;
import com.example.smartsaver.database.DBHelper;

import java.util.ArrayList;
import java.util.List;

public class SmartPlanActivity extends AppCompatActivity {

    private EditText inputAmount, inputDuration, inputRisk;
    private Spinner goalSpinner;
    private ImageButton btnContinue;

    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_smart_plan);

        inputAmount = findViewById(R.id.inputAmount);
        inputDuration = findViewById(R.id.inputDuration);
        inputRisk = findViewById(R.id.inputRisk);
        goalSpinner = findViewById(R.id.goalSpinner);
        btnContinue = findViewById(R.id.btnContinue);

        userEmail = getIntent().getStringExtra("user_email");

        String[] goals = {"Select a goal", "Buy a house", "Car", "Vacation", "Emergency Fund"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, goals);
        goalSpinner.setAdapter(adapter);

        btnContinue.setOnClickListener(v -> handleContinue());
    }

    private void handleContinue() {
        String amountStr = inputAmount.getText().toString().trim();
        String durationStr = inputDuration.getText().toString().trim();
        String riskStr = inputRisk.getText().toString().trim().toLowerCase();
        String goal = goalSpinner.getSelectedItem().toString();

        if (TextUtils.isEmpty(amountStr) || TextUtils.isEmpty(durationStr) || TextUtils.isEmpty(riskStr) || goal.equals("Select a goal")) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!riskStr.equals("low") && !riskStr.equals("medium") && !riskStr.equals("high")) {
            Toast.makeText(this, "Risk must be: low, medium, or high", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!durationStr.matches("\\d+\\s+(days|months|years)")) {
            Toast.makeText(this, "Duration must be in format like '5 months'", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amountStr);

        DBHelper dbHelper = new DBHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String riskCondition;
        switch (riskStr) {
            case "high":
                riskCondition = "change_percent < 2";
                break;
            case "medium":
                riskCondition = "change_percent >= 2 AND change_percent < 4";
                break;
            case "low":
                riskCondition = "change_percent >= 4";
                break;
            default:
                Toast.makeText(this, "Invalid risk level", Toast.LENGTH_SHORT).show();
                return;
        }

        String query = "SELECT symbol, name, price, change_percent FROM stocks WHERE price <= ? AND " + riskCondition;
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(amount)});

        String selectedSymbol = null, selectedName = null;
        double selectedPrice = 0;

        if (cursor.getCount() == 0) {
            Toast.makeText(this, "No available stocks found for your amount. Try a higher amount.", Toast.LENGTH_LONG).show();
            return;
        }

        if (cursor.getCount() == 1 && cursor.moveToFirst()) {
            selectedSymbol = cursor.getString(0);
            selectedName = cursor.getString(1);
            selectedPrice = cursor.getDouble(2);
            Toast.makeText(this, "Only one stock available for your budget.", Toast.LENGTH_LONG).show();
        } else {
            List<String[]> matchingStocks = new ArrayList<>();
            while (cursor.moveToNext()) {
                matchingStocks.add(new String[]{
                        cursor.getString(0), // symbol
                        cursor.getString(1), // name
                        String.valueOf(cursor.getDouble(2)) // price
                });
            }

            int randomIndex = (int) (Math.random() * matchingStocks.size());
            String[] selected = matchingStocks.get(randomIndex);
            selectedSymbol = selected[0];
            selectedName = selected[1];
            selectedPrice = Double.parseDouble(selected[2]);
        }

        cursor.close();
        db.close();

        StringBuilder recommendationNote = new StringBuilder();
        recommendationNote.append("Hold it for the next ").append(durationStr).append(" and review performance before selling.\n");

        switch (riskStr) {
            case "low":
                recommendationNote.append("This is a high-return strategy with elevated growth potential.");
                break;
            case "medium":
                recommendationNote.append("This selection balances risk and reward.");
                break;
            case "high":
                recommendationNote.append("This is a low-volatility option suitable for risk-averse investors.");
                break;
        }

        Intent intent = new Intent(this, SuggestionResultActivity.class);
        intent.putExtra("user_email", userEmail);
        intent.putExtra("amount", amount);
        intent.putExtra("duration", durationStr);
        intent.putExtra("risk", riskStr);
        intent.putExtra("goal", goal);
        intent.putExtra("stock_symbol", selectedSymbol);
        intent.putExtra("stock_name", selectedName);
        intent.putExtra("stock_price", selectedPrice);
        intent.putExtra("stock_note", recommendationNote.toString());
        startActivity(intent);
    }
}