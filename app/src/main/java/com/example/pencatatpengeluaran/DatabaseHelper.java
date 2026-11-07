package com.example.pencatatpengeluaran;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "pengeluaran.db";
    private static final int DB_VERSION = 1;
    private static final String TABLE = "expenses";

    public static class Expense {
        public final long id;
        public final String description;
        public final long amount;
        public final long createdAt;

        public Expense(long id, String description, long amount, long createdAt) {
            this.id = id;
            this.description = description;
            this.amount = amount;
            this.createdAt = createdAt;
        }
    }

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "description TEXT NOT NULL, "
                + "amount INTEGER NOT NULL, "
                + "created_at INTEGER NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE);
        onCreate(db);
    }

    public void insert(String description, long amount) {
        ContentValues values = new ContentValues();
        values.put("description", description);
        values.put("amount", amount);
        values.put("created_at", System.currentTimeMillis());
        getWritableDatabase().insert(TABLE, null, values);
    }

    public void delete(long id) {
        getWritableDatabase().delete(TABLE, "id = ?", new String[]{String.valueOf(id)});
    }

    public List<Expense> getAll() {
        List<Expense> list = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query(TABLE, null, null, null,
                null, null, "created_at DESC")) {
            while (c.moveToNext()) {
                list.add(new Expense(
                        c.getLong(c.getColumnIndexOrThrow("id")),
                        c.getString(c.getColumnIndexOrThrow("description")),
                        c.getLong(c.getColumnIndexOrThrow("amount")),
                        c.getLong(c.getColumnIndexOrThrow("created_at"))));
            }
        }
        return list;
    }

    public long getTotalSince(long sinceMillis) {
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT COALESCE(SUM(amount), 0) FROM " + TABLE + " WHERE created_at >= ?",
                new String[]{String.valueOf(sinceMillis)})) {
            return c.moveToFirst() ? c.getLong(0) : 0;
        }
    }
}
