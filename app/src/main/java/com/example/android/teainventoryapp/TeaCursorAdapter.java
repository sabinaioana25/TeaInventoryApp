package com.example.android.teainventoryapp;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v4.widget.CursorAdapter;

import static com.example.android.teainventoryapp.data.TeaContract.TeaEntry;

/**
 * Created by Sabina on 10/29/2017.
 */

public class TeaCursorAdapter extends CursorAdapter {

    // Constructor
    public TeaCursorAdapter(Context context, Cursor c) {
        super(context, c, 0);
    }

    // Make new grid item view
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.grid_item, parent, false);
    }

    // Bind data to grid item
    @Override
    public void bindView(View view, final Context context, Cursor cursor) {

        // Find text views we want modified
        TextView typeView = view.findViewById(R.id.grid_type);
        TextView quantityView = view.findViewById(R.id.grid_quantity);
        TextView priceView = view.findViewById(R.id.grid_price);
        Button saleButton = view.findViewById(R.id.grid_sale_button);

        // Find image view to modify
        ImageView imageView = view.findViewById(R.id.grid_image);

        // Find columns on tea table for each attribute
        int idColumnIndex = cursor.getColumnIndex(TeaEntry._ID);
        int imageColumnIndex = cursor.getColumnIndex(TeaEntry.COLUMN_TEA_IMAGE);
        int typeColumnIndex = cursor.getColumnIndex(TeaEntry.COLUMN_TEA_TYPE);
        int quantityColumnIndex = cursor.getColumnIndex(TeaEntry.COLUMN_TEA_QUANTITY);
        int priceColumnIndex = cursor.getColumnIndex(TeaEntry.COLUMN_TEA_PRICE);

        // Read Tea attributes from Cursor of current entry
        String teaImage = cursor.getString(imageColumnIndex);
        String teaType = cursor.getString(typeColumnIndex);
        final int teaQuantity = cursor.getInt(quantityColumnIndex);
        int teaPrice = cursor.getInt(priceColumnIndex);

        // Update TextViews
        typeView.setText(teaType);
        quantityView.setText("Quantity: " + teaQuantity);
        priceView.setText("Price: " + teaPrice + "GBP");
        imageView.setImageURI(Uri.parse(teaImage));

        // Get row ID
        final int rowID = cursor.getInt(idColumnIndex);

        saleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri currentTeaUri = ContentUris.withAppendedId(TeaEntry.CONTENT_URI, rowID);
                makeSale(view, teaQuantity, currentTeaUri);

            }
        });
    }

    private void makeSale(View view, int quantity, Uri uriTea) {
        if (quantity > 0) {
            quantity--;

            ContentValues contentValues = new ContentValues();
            contentValues.put(TeaEntry.COLUMN_TEA_QUANTITY, quantity);
            mContext.getContentResolver().update(uriTea, contentValues, null, null);
        } else {
            Toast.makeText(view.getContext(), "This tea is not in stock", Toast.LENGTH_SHORT).show();
        }
    }
}