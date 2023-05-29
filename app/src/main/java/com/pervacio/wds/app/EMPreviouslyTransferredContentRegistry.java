package com.pervacio.wds.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.HashSet;
import java.util.Set;

public class EMPreviouslyTransferredContentRegistry {
    static EMPreviouslyTransferredContentRegistry emPreviouslyTransferredContentRegistry = null;

    public static EMPreviouslyTransferredContentRegistry getInstanceOfEMPreviouslyTransferredContentRegistry() {
        if (emPreviouslyTransferredContentRegistry == null) {
            emPreviouslyTransferredContentRegistry = new EMPreviouslyTransferredContentRegistry();
        }
        return emPreviouslyTransferredContentRegistry;
    }

    public void initialize(String aOtherDeviceId, Context aContext) {
        mDbHelper = new PreviouslyTransferredItemsDbHelper(aContext);
        mOtherDeviceId = aOtherDeviceId;

        if (mOtherDeviceId == null)
            return;

        // Query the database and build the in-memory list for this device
        String[] projection = {
                _ID,
                OTHER_DEVICE_ID,
                TRANSFERRED_ITEM
        };

        String selection = OTHER_DEVICE_ID + " = ?";
        String[] selectionArgs = { aOtherDeviceId };

        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        Cursor cursor = db.query(
                TABLE_NAME,                               // The table to query
                projection,                               // The columns to return
                selection,                                // The columns for the WHERE clause
                selectionArgs,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                null                                      // The sort order
        );

        while(cursor.moveToNext()) {
            String transferredItem = cursor.getString(cursor.getColumnIndexOrThrow(TRANSFERRED_ITEM));
            mPreviouslyTransferredItems.add(transferredItem);
        }

        cursor.close();

    }

    // Clear the in-memory register so we won't detect previously transferred content
    // Note that this doesn't clear any persisted data - TODO: should it?
    void clearPreviouslyTransferredItems() {
        mPreviouslyTransferredItems = new HashSet<>();
    }

    synchronized void addToPreviouslyTransferredItem(String aItem) {
        mPreviouslyTransferredItems.add(aItem);

        if (mOtherDeviceId == null)
            return;

        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(OTHER_DEVICE_ID, mOtherDeviceId);
        values.put(TRANSFERRED_ITEM, aItem);
        db.insert(TABLE_NAME, null, values);
    }

    synchronized boolean itemHasBeenPreviouslyTransferred(String aItem) {
        return mPreviouslyTransferredItems.contains(aItem);
    }

    boolean anyItemsPreviouslyTransferred() {
        return !(mPreviouslyTransferredItems.isEmpty());
    }

    private Set<String> mPreviouslyTransferredItems = new HashSet<>();
    private String mOtherDeviceId;
    PreviouslyTransferredItemsDbHelper mDbHelper;

    ///////////////////////////////////////////////////////////////////////////////////

    final static private String TABLE_NAME = "PreviouslyTransferredItems";
    final static private String _ID = "_ID";
    final static private String OTHER_DEVICE_ID = "OtherDeviceId";
    final static private String TRANSFERRED_ITEM = "TransferredItem";

    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    _ID + " INTEGER PRIMARY KEY," +
                    OTHER_DEVICE_ID + " TEXT," +
                    TRANSFERRED_ITEM + " TEXT)";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + TABLE_NAME;

    public class PreviouslyTransferredItemsDbHelper extends SQLiteOpenHelper {
        public static final int DATABASE_VERSION = 1;
        public static final String DATABASE_NAME = "previous_transfers.db";

        public PreviouslyTransferredItemsDbHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        public void onCreate(SQLiteDatabase db) {
            db.execSQL(SQL_CREATE_ENTRIES);
        }

        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL(SQL_DELETE_ENTRIES);
            onCreate(db);
        }

        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            onUpgrade(db, oldVersion, newVersion);
        }
    }
}
