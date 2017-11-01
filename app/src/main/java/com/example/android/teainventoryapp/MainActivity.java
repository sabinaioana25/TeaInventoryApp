package com.example.android.teainventoryapp;

import android.app.LoaderManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import static com.example.android.teainventoryapp.data.TeaContract.TeaEntry;

public class MainActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    //Identifier for tea data loader
    private static final int TEA_LOADER = 0;

    //Adapter for GridView
    TeaCursorAdapter mCursorAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Setup FAB to open EditorActivity
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, EditorActivity.class);
                startActivity(intent);
            }
        });

        // Find GridView to populate tea data
        GridView teaGridView = (GridView) findViewById(R.id.grid);

        // Find and set empty view on the ListView, so that it only shows when the list has no items
        View emptyView = findViewById(R.id.empty_view);
        teaGridView.setEmptyView(emptyView);

        // Setup an Adapter to create a list item for each row of tea data in the Cursor.
        // There is no tea data yet (until the loader finishes) so pass in null for the Cursor.
        mCursorAdapter = new TeaCursorAdapter(this, null);
        teaGridView.setAdapter(mCursorAdapter);

        teaGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {

                // Create a new intent to go to grid item
                Intent intent = new Intent(MainActivity.this, EditorActivity.class);

                // From the content URI that represents the specific tea that was clicked on
                Uri currentTeaUri = ContentUris.withAppendedId(TeaEntry.CONTENT_URI, id);

                // Set the URI on the data field of the intent
                intent.setData(currentTeaUri);

                // Launch the {@link EditorActivity} to display the data for the current tea
                startActivity(intent);
            }
        });

        // Kick off the loader
        getLoaderManager().initLoader(TEA_LOADER, null, this);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    /**
     * Insert tea inventory
     */
    private void insertTea() {
        insertTeaProduct(R.drawable.asset_1, "grean tea", "teavana", 20, 5);
        insertTeaProduct(R.drawable.main_image, "Oolong", "Teavana", 150, 5);
        insertTeaProduct(R.drawable.caramel_truffle_tea, "Caramel Truffle", "Teavana", 200, 6);

    }

    private void insertTeaProduct(int imageId, String type, String brand, int quantity, int price) {
        // Create a ContentValues object where columns are the keys and attributes are the values
        ContentValues values = new ContentValues();

        Uri imageUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE +
                "://" + getResources().getResourcePackageName(imageId) +
                '/' + getResources().getResourceTypeName(imageId) + '/' +
                getResources().getResourceEntryName(imageId));
        values.put(TeaEntry.COLUMN_TEA_IMAGE, imageUri.toString());
        values.put(TeaEntry.COLUMN_TEA_TYPE, type);
        values.put(TeaEntry.COLUMN_TEA_BRAND, brand);
        values.put(TeaEntry.COLUMN_TEA_QUANTITY, quantity);
        values.put(TeaEntry.COLUMN_TEA_PRICE, price);

        // Insert a new row into the provider using the ContentResolver.
        // Use the {@link ProductEntry#CONTENT_URI} to indicate that we want to insert
        // into the products database table.
        // Receive the new content URI that will allow us to access this data in the future.
        Uri newUri = getContentResolver().insert(TeaEntry.CONTENT_URI, values);

        // Show a toast message depending on whether or not the insertion was successful.
        if (newUri == null) {
            Toast.makeText(this, "Error with saving product", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Prompt the user to confirm that they want to delete everything
     */
    private void deleteAllConfirmation() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you sure you want to delete all inventory products?");
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int id) {
                // User clicked the "Delete" button, to delete the product.
                int rowsDeleted = getContentResolver().delete(TeaEntry.CONTENT_URI, null, null);
                Log.v("MainActivity", rowsDeleted + " rows deleted from product database");
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the product.
                if (dialogInterface != null) {
                    dialogInterface.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_main.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            case R.id.action_insert_inventory_data:
                insertTea();
                return true;
            case R.id.action_clear_inventory_data:
                deleteAllConfirmation();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        // Define a projection that specifies the column from the table we care about
        String[] projection = {
                TeaEntry._ID,
                TeaEntry.COLUMN_TEA_IMAGE,
                TeaEntry.COLUMN_TEA_TYPE,
                TeaEntry.COLUMN_TEA_BRAND,
                TeaEntry.COLUMN_TEA_QUANTITY,
                TeaEntry.COLUMN_TEA_PRICE};

        // This loader will execute the ContentProvider's query method on a background thread
        return new CursorLoader(this,       // Parent activity context
                TeaEntry.CONTENT_URI,       // Provider content URI to query
                projection,                 // Columns to include in the resulting Cursor
                null,                       // No selection clause
                null,                       // No selection arguments
                null);                      // Default sort order
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mCursorAdapter.swapCursor(data);

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mCursorAdapter.swapCursor(null);
    }
}