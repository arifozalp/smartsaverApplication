package com.example.smartsaver.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "smartsaver.db";
    public static final int DATABASE_VERSION = 2; // Versiyonu 2 yaptık ki upgrade çalışsın

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Kullanıcı tablosu
        db.execSQL("CREATE TABLE User (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT," +  // EKLENDİ
                "email TEXT UNIQUE," +
                "password TEXT," +
                "balance REAL DEFAULT 0)");

        // Para transferleri tablosu
        db.execSQL("CREATE TABLE MoneyTransfer (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "sender_id INTEGER," +
                "receiver_email TEXT," +
                "amount REAL," +
                "date TEXT)");

        // Kullanıcının yatırımları
        db.execSQL("CREATE TABLE Investment (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "user_id INTEGER," +
                "stock_code TEXT," +
                "invested_amount REAL," +
                "profit REAL," +
                "date TEXT)");

        // Hisse senedi bilgileri
        db.execSQL("CREATE TABLE Stock (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "code TEXT," +
                "name TEXT," +
                "short_term_change REAL," +
                "long_term_change REAL," +
                "quarterly_status TEXT)");

        // Yatırım öneri geçmişi
        db.execSQL("CREATE TABLE SuggestionHistory (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "user_id INTEGER," +
                "date TEXT," +
                "risk_level TEXT," +
                "recommendation TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Veritabanı güncelleme işlemi (geliştirme aşamasında sıfırlanıyor)
        db.execSQL("DROP TABLE IF EXISTS User");
        db.execSQL("DROP TABLE IF EXISTS MoneyTransfer");
        db.execSQL("DROP TABLE IF EXISTS Investment");
        db.execSQL("DROP TABLE IF EXISTS Stock");
        db.execSQL("DROP TABLE IF EXISTS SuggestionHistory");
        onCreate(db);
    }

    public double getBalance(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        double balance = 0;
        Cursor cursor = db.rawQuery("SELECT balance FROM User WHERE email = ?", new String[]{email});
        if (cursor.moveToFirst()) {
            balance = cursor.getDouble(0);
        }
        cursor.close();
        return balance;
    }

    public boolean insertUser(String name, String email, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("email", email);
        values.put("password", password);
        long result = db.insert("User", null, values);
        return result != -1;
    }

}
