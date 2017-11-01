package com.example.android.teainventoryapp.data;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by Sabina on 10/29/2017.
 */

public class TeaContract {

    // To prevent someone from accidentally instantianting contract class
    private TeaContract() {
    }

    // Content authority
    public static final String CONTENT_AUTHORITY = "com.example.android.teainventory";

    // Create base URIs to content with content provider
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    // Possible path to tea data
    public static final String PATH_TEA = "teas";

    /**
     * Inner class to define constant values for database table
     * Each entry is a single tea item
     */
    public static final class TeaEntry implements BaseColumns {

        // Content URI to access data
        public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_TEA);

        // MIME type of the {@link #CONTENT_URI} for a list of teas
        public static final String CONTENT_LIST_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE +
                "/" + CONTENT_AUTHORITY + "/" + PATH_TEA;

        // MIME type for a single tea
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE +
                "/" + CONTENT_AUTHORITY + "/" + PATH_TEA;

        // Name of the database
        public final static String TABLE_NAME = "teas";

        // Unique ID number (int)
        public final static String _ID = BaseColumns._ID;

        // Tea image
        public static final String COLUMN_TEA_IMAGE = "image";

        // Tea type
        public static final String COLUMN_TEA_TYPE = "type";

        // Tea brand
        public static final String COLUMN_TEA_BRAND = "brand";

        // Quantity
        public static final String COLUMN_TEA_QUANTITY = "quantity";

        // Tea price
        public static final String COLUMN_TEA_PRICE = "price";

    }
}