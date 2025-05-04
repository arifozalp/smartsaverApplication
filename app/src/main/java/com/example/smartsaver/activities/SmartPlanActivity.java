package com.example.smartsaver.activities;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.example.smartsaver.R;
import com.example.smartsaver.database.DBHelper;

public class SmartPlanActivity extends AppCompatActivity {

    private EditText inputAmount;
    private RadioGroup durationGroup, riskGroup;
    private Spinner goalSpinner;
    private Button btnContinue;

    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_smart_plan);

        inputAmount = findViewById(R.id.inputAmount);
        durationGroup = findViewById(R.id.durationGroup);
        riskGroup = findViewById(R.id.riskGroup);
        goalSpinner = findViewById(R.id.goalSpinner);
        btnContinue = findViewById(R.id.btnContinue);

        userEmail = getIntent().getStringExtra("user_email");

        // Spinner veri yüklemesi
        String[] goals = {"Select a goal", "Buy a house", "Car", "Vacation", "Emergency Fund"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, goals);
        goalSpinner.setAdapter(adapter);

        btnContinue.setOnClickListener(v -> handleContinue());
    }

    private void handleContinue() {
        String amountStr = inputAmount.getText().toString().trim();
        int selectedDurationId = durationGroup.getCheckedRadioButtonId();
        int selectedRiskId = riskGroup.getCheckedRadioButtonId();
        String goal = goalSpinner.getSelectedItem().toString();

        if (TextUtils.isEmpty(amountStr) || selectedDurationId == -1 || selectedRiskId == -1 || goal.equals("Select a goal")) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amountStr);
        String duration = ((RadioButton) findViewById(selectedDurationId)).getText().toString();
        String risk = ((RadioButton) findViewById(selectedRiskId)).getText().toString();

        // DB'den hisse önerisi al
        DBHelper dbHelper = new DBHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String priceCondition = "";
        if (amount < 1000)        priceCondition = "price < 100";
        else if (amount < 5000)   priceCondition = "price >= 100 AND price < 300";
        else                      priceCondition = "price >= 300";

        String riskCondition = "";
        switch (risk) {
            case "Low":    riskCondition = "change_percent < 2"; break;
            case "Medium": riskCondition = "change_percent >= 2 AND change_percent < 4"; break;
            case "High":   riskCondition = "change_percent >= 4"; break;
        }

        // SQL sorgusu
        String query = "SELECT symbol, name, price, change_percent FROM stocks WHERE " + priceCondition + " AND " + riskCondition;

        Cursor cursor = db.rawQuery(query, null);
        String selectedSymbol = null, selectedName = null;
        double selectedPrice = 0;

        if (cursor.getCount() == 0) {
            Toast.makeText(this, "No matching stocks found.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (risk.equals("High")) {
            double maxChange = Double.MIN_VALUE;
            while (cursor.moveToNext()) {
                double cp = cursor.getDouble(3);
                if (cp > maxChange) {
                    maxChange = cp;
                    selectedSymbol = cursor.getString(0);
                    selectedName   = cursor.getString(1);
                    selectedPrice  = cursor.getDouble(2);
                }
            }
        } else {
            // Rastgele seçim
            int index = (int) (Math.random() * cursor.getCount());
            cursor.moveToPosition(index);
            selectedSymbol = cursor.getString(0);
            selectedName   = cursor.getString(1);
            selectedPrice  = cursor.getDouble(2);
        }

        cursor.close();
        db.close();

        // Bilgileri diğer ekrana aktar
        Intent intent = new Intent(this, SuggestionResultActivity.class);
        intent.putExtra("user_email", userEmail);
        intent.putExtra("amount", amount);
        intent.putExtra("duration", duration);
        intent.putExtra("risk", risk);
        intent.putExtra("goal", goal);
        intent.putExtra("stock_symbol", selectedSymbol);
        intent.putExtra("stock_name", selectedName);
        intent.putExtra("stock_price", selectedPrice);
        startActivity(intent);
    }

}
