package com.example.android.teainventoryapp;

import android.app.Activity;
import android.support.v7.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import static com.example.android.teainventoryapp.data.TeaContract.TeaEntry;

/**
 * Created by Sabina on 10/29/2017.
 */


public class EditorActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    public static final String LOG_TAG = EditorActivity.class.getSimpleName();

    // Identifier for tea data
    private static final int EXISTING_TEA_LOADER = 0;

    // Content URI for existing tea (and if null, it's a new tea type)
    private Uri mCurrentTeaUri;

    // The request code to store image from the Gallery
    private static final int PICK_IMAGE_REQUEST = 0;

    /**
     * URI for the product image
     */
    private Uri mImageUri;

    // Image field to add image
    private ImageView mImageView;

    // Edit Text fields
    private EditText mTypeView;
    private EditText mBrandView;
    private EditText mQuantityView;
    private EditText mPriceView;

    // Buttons
    private Button mOrderButton;
    private Button mMinusButton;
    private Button mPlusButton;

    private boolean mTeaHasChanged = false;

    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mTeaHasChanged = true;
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        // Examine the intent that was used to launch this activity,
        // in order to figure out if we're creating a new tea or editing an existing one.
        Intent intent = getIntent();
        mCurrentTeaUri = intent.getData();

        // If the intent DOES NOT contain a tea content URI, then we know that we are
        // creating a new one.
        if (mCurrentTeaUri == null) {
            // This is a new tea, so change the app bar to say "add tea"
            setTitle(getString(R.string.editor_add_tea_type));

            // Invalidate the options menu, so the "Delete" menu option can be hidden.
            // (It doesn't make sense to delete an item that hasn't been created yet.)
            invalidateOptionsMenu();
        } else {
            // Otherwise this is an existing tea item, so change the app bar to say "Edit tea"
            setTitle(getString(R.string.editor_update_tea_type));

            // Initialize a loader to read the product data from the database
            // and display the current values in the editor
            getLoaderManager().initLoader(EXISTING_TEA_LOADER, null, this);
        }

        // Find all the relevant data views
        mImageView = (ImageView) findViewById(R.id.editor_tea_image_view);
        mTypeView = (EditText) findViewById(R.id.editor_type_view);
        mBrandView = (EditText) findViewById(R.id.editor_brand_view);
        mQuantityView = (EditText) findViewById(R.id.editor_quantity_view);
        mPriceView = (EditText) findViewById(R.id.editor_price_view);

        // Identify all button views
        mOrderButton = (Button) findViewById(R.id.editor_order_button);
        mPlusButton = (Button) findViewById(R.id.editor_plus_button);
        mMinusButton = (Button) findViewById(R.id.editor_minus_button);

        // Setup OnTouchListeners on all the input fields, so we can determine if the user
        // has touched or modified them. This will let us know if there are unsaved changes
        // or not, if the user tries to leave the editor without saving.
        mTypeView.setOnTouchListener(mTouchListener);
        mBrandView.setOnTouchListener(mTouchListener);
        mQuantityView.setOnTouchListener(mTouchListener);
        mPriceView.setOnTouchListener(mTouchListener);
        mImageView.setOnTouchListener(mTouchListener);

        // Add and subtract buttons
        mPlusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int quantityAdded = Integer.parseInt(mQuantityView.getText().toString().trim());
                quantityAdded += 1;
                mQuantityView.setText(Integer.toString(quantityAdded));
            }
        });

        mMinusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int quantitySubstracted = Integer.parseInt(mQuantityView.getText().toString().trim());
                quantitySubstracted -= 1;
                mQuantityView.setText(Integer.toString(quantitySubstracted));
            }
        });

        mOrderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Get text for type and brand to put in e-mail
                String typeEmail = mTypeView.getText().toString().trim();
                String brandEmail = mBrandView.getText().toString().trim();

                // Create e-mail message
                String emailMessage = "This is a request for the following items: " +
                        "\n" + typeEmail +
                        " - " + brandEmail;

                // Send intent
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                emailIntent.setData(Uri.parse("mailto:"));
                emailIntent.putExtra(Intent.EXTRA_SUBJECT,
                        getString(R.string.order_email_subject));
                emailIntent.putExtra(Intent.EXTRA_TEXT, emailMessage);

                if (emailIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(emailIntent);
                }
            }
        });

        mImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openImageSelector();
            }
        });

    }

    private boolean saveTea() {

        // Read from input fields
        // Use trim to eliminate leading or trailing white spaces
        String typeString = mTypeView.getText().toString().trim();
        String brandString = mBrandView.getText().toString().trim();
        String quantityString = mQuantityView.getText().toString().trim();
        String priceString = mPriceView.getText().toString().trim();

        if (mCurrentTeaUri == null && mImageUri == null &&
                TextUtils.isEmpty(typeString)
                && TextUtils.isEmpty(brandString) && TextUtils.isEmpty(quantityString)
                && TextUtils.isEmpty(priceString)) {
            // Since no fields were modified, we can return early without creating a new product.
            // No need to create ContentValues and no need to do any ContentProvider operations.
            return true;
        } else if (TextUtils.isEmpty(typeString)) {
            Toast.makeText(this, "Please enter a valid type",
                    Toast.LENGTH_SHORT).show();
            return false;
        } else if (TextUtils.isEmpty(brandString)) {
            Toast.makeText(this, "Please enter a valid brand", Toast.LENGTH_SHORT).show();
            return false;
        } else if (TextUtils.isEmpty(quantityString)) {
            Toast.makeText(this, "Please enter a non-null quantity", Toast.LENGTH_SHORT).show();
            return false;
        } else if (TextUtils.isEmpty(priceString)) {
            Toast.makeText(this, "Please enter a non-null price", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Create a ContentValues object where column names are the keys,
        // and product attributes from the editor are the values.
        ContentValues values = new ContentValues();
        values.put(TeaEntry.COLUMN_TEA_TYPE, typeString);
        values.put(TeaEntry.COLUMN_TEA_BRAND, brandString);
        values.put(TeaEntry.COLUMN_TEA_QUANTITY, quantityString);
        values.put(TeaEntry.COLUMN_TEA_PRICE, priceString);

        if (mImageUri == null) {
            mImageUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE
                    + "://" + getResources().getResourcePackageName(R.drawable.asset_1)
                    + '/' + getResources().getResourceTypeName(R.drawable.asset_1)
                    + '/' + getResources().getResourceEntryName(R.drawable.asset_1));
        }
        values.put(TeaEntry.COLUMN_TEA_IMAGE, mImageUri.toString());

        // Determine if this is a new or existing product by checking if mCurrentTeaUri is null or not
        if (mCurrentTeaUri == null) {
            // This is a NEW product, so insert a new product into the provider,
            // returning the content URI for the new product.
            Uri newUri = getContentResolver().insert(TeaEntry.CONTENT_URI, values);
            supportInvalidateOptionsMenu();

            // Show a toast message depending on whether or not the insertion was successful.
            if (newUri == null) {
                // If the new content URI is null, then there was an error with insertion.
                Toast.makeText(this, "Error with saving the product",
                        Toast.LENGTH_SHORT).show();
                return false;
            } else {
                // Otherwise, the insertion was successful and we can display a toast.
                Toast.makeText(this, "Tea saved", Toast.LENGTH_SHORT).show();
                return true;
            }
        } else {
            // Otherwise this is an EXISTING product, so update the product with content URI: mCurrentTeaUri
            // and pass in the new ContentValues. Pass in null for the selection and selection args
            // because mCurrentProductUri will already identify the correct row in the database that
            // we want to modify.
            int rowsAffected = getContentResolver().update(mCurrentTeaUri, values, null, null);

            // Show a toast message depending on whether the updated was successful or not
            if (rowsAffected == 0) {
                // If no rows were affected, then there was an error with the update
                Toast.makeText(this, "Could not perform update", Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the update was successful and we can display a confirmation toast
                Toast.makeText(this, "Update successful", Toast.LENGTH_SHORT).show();
            }
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_editor.xml file
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    /**
     * This method is called after invalidateOptionsMenu(), so that the
     * menu can be updated (some menu items can be hidden or made visible).
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        // If this is a new product, hide the "Delete" menu item.
        if (mCurrentTeaUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Save the tea to the database
            case R.id.action_save:
                if (saveTea()) finish();
                return true;

            // Respond to a click on the "delete" menu option
            case R.id.action_delete:
                showDeleteConfirmationDialog();
                return true;

            case R.id.home:
                // Respond to a click on the "Up" button in the app bar
                if (!mTeaHasChanged) {
                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    return true;
                }

                // Otherwise if there are unsaved changes, setup a dialog to warn the user.
                // Create a click listener to handle the user confirming that
                // changes should be discarded.
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int id) {
                                // User clicked "Discard" button, navigate to parent activity.
                                NavUtils.navigateUpFromSameTask(EditorActivity.this);
                            }
                        };

                // Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {

        // If the tea hasn't changed, continue with handling back button press
        if (!mTeaHasChanged) {
            super.onBackPressed();
            return;
        }

        // Otherwise if there are unsaved changes, setup a dialog to warn the user.
        // Create a click listener to handle the user confirming that changes should be discarded.
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int id) {
                        // User clicked "Discard" button, close the current activity.
                        finish();
                    }
                };

        // Show dialog that there are unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {

        if (mCurrentTeaUri == null) {
            return null;
        }

        // Define a projection with all items
        String[] projection = {
                TeaEntry._ID,
                TeaEntry.COLUMN_TEA_IMAGE,
                TeaEntry.COLUMN_TEA_TYPE,
                TeaEntry.COLUMN_TEA_BRAND,
                TeaEntry.COLUMN_TEA_QUANTITY,
                TeaEntry.COLUMN_TEA_PRICE};

        // Loader will execute the ContentProvider's query method on a background thread
        return new CursorLoader(this,   // Parent activity context
                mCurrentTeaUri,         // Query the content URI for the current pet
                projection,             // Columns to include in the resulting Cursor
                null,                   // No selection clause
                null,                   // No selection arguments
                null);                  // Default sort order);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // Bail early if the cursor is null or there is less than one row in the cursor
        if (cursor == null || cursor.getCount() < 1) {
            return;
        }

        // Proceed with moving to the first row of the cursor and reading data from it
        if (cursor.moveToFirst()) {
            // Find the column of tea attributes that we're interested in
            int imageColumnIndex = cursor.getColumnIndex(TeaEntry.COLUMN_TEA_IMAGE);
            int typeColumnIndex = cursor.getColumnIndex(TeaEntry.COLUMN_TEA_TYPE);
            int brandColumnIndex = cursor.getColumnIndex(TeaEntry.COLUMN_TEA_BRAND);
            int quantityColumnIndex = cursor.getColumnIndex(TeaEntry.COLUMN_TEA_QUANTITY);
            int priceColumnIndex = cursor.getColumnIndex(TeaEntry.COLUMN_TEA_PRICE);

            String image = cursor.getString(imageColumnIndex);
            String type = cursor.getString(typeColumnIndex);
            String brand = cursor.getString(brandColumnIndex);
            int quantity = cursor.getInt(quantityColumnIndex);
            int price = cursor.getInt(priceColumnIndex);

            // Update views
            mImageUri = Uri.parse(image);
            mImageView.setImageURI(mImageUri);
            mTypeView.setText(type);
            mBrandView.setText(brand);
            mQuantityView.setText(Integer.toString(quantity));
            mPriceView.setText(Integer.toString(price));
        }
        cursor.close();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mImageView.setImageResource(R.drawable.asset_1);
        mTypeView.setText("");
        mBrandView.setText("");
        mQuantityView.setText("");
        mPriceView.setText("");
    }

    private void showUnsavedChangesDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {
        // Create an AlertDialog.Builder and set the message
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.alert_discard_changes);
        builder.setPositiveButton("Discard", discardButtonClickListener);

        builder.setNegativeButton("Keep editing", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int id) {
                if (dialogInterface != null) {
                    dialogInterface.dismiss();
                }
            }
        });

        // Create and display the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Delete this tea?");
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialogInterface, int id) {
                // User clicked the "delete" button, so delete the tea
                deleteTea();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int id) {
                // User cancelled the deletion, so dismiss the dialog
                if (dialogInterface != null) {
                    dialogInterface.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void deleteTea() {
        // Only perform the delete operation if this is an existing entry
        if (mCurrentTeaUri != null) {
            int rowsDeleted = getContentResolver().delete(mCurrentTeaUri, null, null);

            // Show a toast message depending on whether the deletion was successful or not
            if (rowsDeleted == 0) {
                Toast.makeText(this, "Error deleting this tea", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Successfully deleted tea", Toast.LENGTH_SHORT).show();
            }
        }
        finish();
    }

    public void openImageSelector() {
        Intent intent;

        if (Build.VERSION.SDK_INT < 19) {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
        } else {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
        }

        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select picture"), PICK_IMAGE_REQUEST);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        // The ACTION_OPEN_DOCUMENT intent was sent with the request code READ_REQUEST_CODE.
        // If the request code seen here doesn't match, it's the response to some other intent,
        // and the below code shouldn't run at all.
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {

            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.  Pull that uri using "resultData.getData()"

            if (data != null) {
                mImageUri = data.getData();
                Log.i(LOG_TAG, "Uri: " + mImageUri.toString());

                mImageView.setImageBitmap(getBitmapFromUri(mImageUri));
            }
        }
    }

    public Bitmap getBitmapFromUri(Uri uri) {
        if (uri == null || uri.toString().isEmpty()) {
            return null;
        }

        // Get the dimensions of the View
        int targetW = mImageView.getWidth();
        int targetH = mImageView.getHeight();

        InputStream input = null;
        try {
            input = this.getContentResolver().openInputStream(uri);

            // Get the dimensions of the bitmap
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(input, null, bmOptions);
            input.close();

            int photoW = bmOptions.outWidth;
            int photoH = bmOptions.outHeight;

            // Determine how much to scale down the image
            int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

            // Decode the image file into a Bitmap sized to fill the View
            bmOptions.inJustDecodeBounds = false;
            bmOptions.inSampleSize = scaleFactor;
            bmOptions.inPurgeable = true;

            Bitmap bitmap = BitmapFactory.decodeStream(input, null, bmOptions);
            input.close();
            return bitmap;
        } catch (FileNotFoundException fne) {
            Log.e(LOG_TAG, "Failed to load image", fne);
            return null;
        } catch (Exception e) {
            Log.e(LOG_TAG, "Failed to load image", e);
            return null;
        } finally {
            try {
                input.close();
            } catch (IOException e) {
            }

        }
    }

    private void hideOrderButton() {
        Button orderButton = (Button) findViewById(R.id.editor_order_button);
        orderButton.setVisibility(orderButton.GONE);
    }
}