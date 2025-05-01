package com.example.smartsaver.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "smartsaver.db";
    public static final int DATABASE_VERSION = 1;

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Kullanıcı tablosu
        db.execSQL("CREATE TABLE User (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "email TEXT UNIQUE," +
                "password TEXT," +
                "balance REAL DEFAULT 0)");

        // Para transferleri tablosu
        db.execSQL("CREATE TABLE Transaction (" +
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

        // Hisse senedi bilgileri (statik veya JSON'dan alınan verilerle doldurulabilir)
        db.execSQL("CREATE TABLE Stock (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "code TEXT," +
                "name TEXT," +
                "short_term_change REAL," +
                "long_term_change REAL," +
                "quarterly_status TEXT)");

        // Yatırım öneri geçmişi (opsiyonel)
        db.execSQL("CREATE TABLE SuggestionHistory (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "user_id INTEGER," +
                "date TEXT," +
                "risk_level TEXT," +
                "recommendation TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Sürüm güncellemesi durumunda tabloları sıfırla (geliştirme aşamasında)
        db.execSQL("DROP TABLE IF EXISTS User");
        db.execSQL("DROP TABLE IF EXISTS Transaction");
        db.execSQL("DROP TABLE IF EXISTS Investment");
        db.execSQL("DROP TABLE IF EXISTS Stock");
        db.execSQL("DROP TABLE IF EXISTS SuggestionHistory");
        onCreate(db);
    }
}
