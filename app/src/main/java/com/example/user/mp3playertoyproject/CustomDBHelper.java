package com.example.user.mp3playertoyproject;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class CustomDBHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "musicDB";
    private static final int VERSION = 1;

    public CustomDBHelper(Context context) {
        super(context, DB_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String query = "CREATE TABLE musicTBL("+
                "id VARCHAR(15) PRIMARY KEY,"+
                "artist VARCHAR(15)," +
                "title VARCHAR(15)," +
                "albumArt VARCHAR(15)," +
                "year VARCHAR(15)," +
                "duration VARCHAR(15)," +
                "click INTEGER," +
                "liked INTEGER );";

        db.execSQL(query);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String query = "DROP TABLE IF EXISTS groupTBL;";
        db.execSQL(query);
        onCreate(db);
    }
}
