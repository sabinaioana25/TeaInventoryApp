package com.example.android.teainventoryapp.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import static com.example.android.teainventoryapp.data.TeaContract.CONTENT_AUTHORITY;
import static com.example.android.teainventoryapp.data.TeaContract.PATH_TEA;
import static com.example.android.teainventoryapp.data.TeaContract.TeaEntry;

/**
 * Created by Sabina on 10/29/2017.
 */

public class TeaProvider extends ContentProvider {

    public static final String LOG_TAG = TeaProvider.class.getSimpleName();

    // URI matcher code for the tea table
    private static final int TEAS = 100;

    // URI matcher code for single entry
    private static final int TEA_ID = 101;

    // Database helper
    private TeaDbHelper mDbHelper;

    // UriMatcher object to match content URI for corresponding code
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    // Static initializer
    static {
        // The content URI for multiple rows
        sUriMatcher.addURI(CONTENT_AUTHORITY, PATH_TEA, TEAS);

        // The content URI for a single row
        sUriMatcher.addURI(CONTENT_AUTHORITY, PATH_TEA + "/#", TEA_ID);
    }

    @Override
    public boolean onCreate() {
        mDbHelper = new TeaDbHelper(getContext());
        return true;
    }


    @Override
    public Cursor query(Uri uri,
                        String[] projection,
                        String selection,
                        String[] selectionArgs,
                        String sortOrder) {

        // Get readable database
        SQLiteDatabase database = mDbHelper.getReadableDatabase();

        // This cursor will hold the result of the query
        Cursor cursor;

        // Figure out if the URI matcher can match the URI to a specific code
        int match = sUriMatcher.match(uri);
        switch (match) {
            case TEAS:
                // Query the Tea table.
                // The cursor could contain multiple rows of the Tea table
                cursor = database.query(TeaEntry.TABLE_NAME,
                        projection, selection, selectionArgs, null, null, sortOrder);
                break;
            case TEA_ID:
                // Extract out the ID from the URI.
                //
                // For every "?" in the selection, we need to have an element in the selection
                // arguments that will fill in the "?". Since we have 1 question mark in the
                // selection, we have 1 String in the selection arguments' String array
                selection = TeaEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};

                // This will perform a query on the lipstick table where the _id equals 3 to return a
                // Cursor containing that row of the table
                cursor = database.query(TeaEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }

        // Set notification URI on the Cursor,
        // so we know what content URI the Cursor was created for.
        // If the data at this URI changes, then we know we need to update the Cursor.
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        // Return the cursor
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case TEAS:
                return TeaEntry.CONTENT_LIST_TYPE;
            case TEA_ID:
                return TeaEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unknwn URI " + uri + " with match " + match);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case TEAS:
                return insertTea(uri, contentValues);
            default:
                throw new IllegalArgumentException("Insertion is not supported for " + uri);
        }
    }

    /**
     * Insert tea into the database with the given content values. Return the new content URI
     * for that specific row in the database.
     */
    private Uri insertTea(Uri uri, ContentValues values) {
        // Check that image  is not null
        String image = values.getAsString(TeaEntry.COLUMN_TEA_IMAGE);
        if (image == null) {
            throw new IllegalArgumentException("Tea requires a valid image");
        }

        // Check that the tea type is not null
        String type = values.getAsString(TeaEntry.COLUMN_TEA_TYPE);
        if (type == null || type.length() == 0) {
            throw new IllegalArgumentException("Tea must have a type");
        }

        // Check that the tea brand is not null
        String brand = values.getAsString(TeaEntry.COLUMN_TEA_BRAND);
        if (brand == null || brand.length() == 0) {
            throw new IllegalArgumentException("Tea must have a brand");
        }

        // Check quantity isn't below 0
        Integer quantity = values.getAsInteger(TeaEntry.COLUMN_TEA_QUANTITY);
        if (quantity != null && quantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }

        // Check that price doesn't have a negative value
        Integer price = values.getAsInteger(TeaEntry.COLUMN_TEA_PRICE);
        if (price != null && price < 0) {
            throw new IllegalArgumentException("Price must be positive");
        }

        // Get writeable database
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        // Insert the new item with given values
        long id = db.insert(TeaEntry.TABLE_NAME, null, values);
        // If the ID is -1, then the insertion failed log an error and return null
        if (id == -1) {
            Log.e(LOG_TAG, "Failed to insert row for " + uri);
            return null;
        }

        // Notify all listeners that the data has changed for the pet content URI
        getContext().getContentResolver().notifyChange(uri, null);

        // Return the new URI with the ID (of the newly inserted row) appended at the end
        return ContentUris.withAppendedId(uri, id);
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String selection, String[] selectionArgs) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case TEAS:
                return updateTea(uri, contentValues, selection, selectionArgs);
            case TEA_ID:
                // For the Tea entry code, extract out the ID from the URI,
                // so we know which row to update. Selection will be "_id=?" and selection
                // arguments will be a String array containing the actual ID.
                selection = TeaEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                return updateTea(uri, contentValues, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Update not supported for " + uri);
        }
    }

    private int updateTea(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

        // If there are no values to update, then don't try to update the database
        if (values.size() == 0) {
            return 0;
        }
        // If the type key is present check that the type value is not null.
        if (values.containsKey(TeaEntry.COLUMN_TEA_TYPE)) {
            String typeName = values.getAsString(TeaEntry.COLUMN_TEA_TYPE);
            if (typeName == null) {
                throw new IllegalArgumentException("Tea must have a type inserted");
            }
        }

        // If brand is valid
        if (values.containsKey(TeaEntry.COLUMN_TEA_BRAND)) {
            String brandName = values.getAsString(TeaEntry.COLUMN_TEA_BRAND);
            if (brandName == null) {
                throw new IllegalArgumentException("Tea must have a brand");
            }
        }

        // If quantity is valid
        if (values.containsKey(TeaEntry.COLUMN_TEA_QUANTITY)) {
            Integer quantity = values.getAsInteger(TeaEntry.COLUMN_TEA_QUANTITY);
            if (quantity == null || quantity < 0) {
                throw new IllegalArgumentException("Quantity must be valid");
            }
        }

        // If price is valid
        if (values.containsKey(TeaEntry.COLUMN_TEA_PRICE)) {
            Integer price = values.getAsInteger(TeaEntry.COLUMN_TEA_PRICE);
            if (price != 0 && price < 0) {
                throw new IllegalArgumentException("Price cannot be a negative value");
            }
        }

        // Otherwise, get writable database to update the data
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Perform the update on the database and get the number of rows affected
        int rowsUpdated = database.update(TeaEntry.TABLE_NAME, values, selection, selectionArgs);

        // If one or more rows are updated, then notify all listeners that the data has changed at the given URI
        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        // Return number of rows updated
        return rowsUpdated;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {

        // Get writeable database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Track number of rows that were deletd
        int rowsDeleted;

        final int match = sUriMatcher.match(uri);
        switch (match) {
            case TEAS:
                // Delete all rows that match the selection and selectionArgs
                rowsDeleted = database.delete(TeaEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case TEA_ID:
                // Delete a single row given by the ID in the URI
                selection = TeaEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                rowsDeleted = database.delete(TeaEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Deletion is not supported for " + uri);
        }

        // If 1 or more rows were deleted, then notify all listeners that the data at the
        // given URI has changed

        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        // Return the number of rows deleted
        return rowsDeleted;
    }
}