package com.jihoon.callStateProject;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper
{
    static final String DATABASE_NAME = "contact.db";
    public DBHelper(Context context, int version)
    {
        super(context, DATABASE_NAME, null, version);
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE callState (name TEXT, phonenum TEXT, email TEXT, memo TEXT);");
        db.execSQL("CREATE TABLE checkBox (name TEXT, phonenum TEXT, email TEXT, memo TEXT);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS callState;");
        onCreate(db);
    }
}
