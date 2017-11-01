package com.example.android.teainventoryapp.data;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import static com.example.android.teainventoryapp.data.TeaContract.*;

/**
 * Created by Sabina on 10/29/2017.
 */

public class TeaDbHelper extends SQLiteOpenHelper {

    public static final String LOG_TAG = TeaDbHelper.class.getSimpleName();

    // Name of database file
    private static final String DATABASE_NAME = "tea.db";

    // Database version
    private static final int DATABASE_VERSION = 1;

    /**
     * Constructs a new instance of {@link TeaDbHelper}
     * @param context
     */
    public TeaDbHelper (Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Called when database created for the first time
    @Override
    public void onCreate(SQLiteDatabase db) {

        // Create a String that contains the SQL statement to create the teas tables
        String SQL_CREATE_TEA_TABLE = "CREATE TABLE " + TeaEntry.TABLE_NAME + " ("
                + TeaEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + TeaEntry.COLUMN_TEA_IMAGE + " TEXT NOT NULL, "
                + TeaEntry.COLUMN_TEA_TYPE + " TEXT NOT NULL, "
                + TeaEntry.COLUMN_TEA_BRAND + " TEXT, "
                + TeaEntry.COLUMN_TEA_QUANTITY + " INTEGER NOT NULL, "
                + TeaEntry.COLUMN_TEA_PRICE + " INTEGER NOT NULL DEFAULT 0);";
        // Execut the SQL daabase
        db.execSQL(SQL_CREATE_TEA_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}
