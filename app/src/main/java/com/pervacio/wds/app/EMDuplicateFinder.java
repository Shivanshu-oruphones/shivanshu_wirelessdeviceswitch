/*************************************************************************
 *
 * Media Mushroom Limited CONFIDENTIAL
 * __________________
 *
 *  Copyright 2017 Media Mushroom Limited
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Media Mushroom Limited.
 *
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Media Mushroom Limited.
 */

package com.pervacio.wds.app;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;

import android.net.Uri;

import com.pervacio.wds.custom.APPI;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EMDuplicateFinder {
    static EMGlobals emGlobals = new EMGlobals();
    public EMDuplicateFinder(List<String> aColumnNamesToIncludeInHash,
                                Uri aContentProviderUri,
                                ContentResolver aContentResolver) {
        DLog.log(">> EMDuplicateFinder");
        mKeys = aColumnNamesToIncludeInHash;

        mUri = aContentProviderUri;

        // Generate the hashes for each row, add them to the set
        String[] projection = aColumnNamesToIncludeInHash.toArray(new String[aColumnNamesToIncludeInHash.size()]);

        Cursor cursor = null;

        try {
            cursor = emGlobals.getmContext().getContentResolver().query(aContentProviderUri,
                    projection, // Column projection
                    null, // WHERE
                    null, // WHERE arguments
                    null); // Order by

            if (cursor.moveToFirst()) {
                while (!cursor.isAfterLast()) {
                    List<String> values = new ArrayList<String>();
                    for (String key : mKeys) {
                        int columnIndex = cursor.getColumnIndex(key);
                        if (columnIndex >= 0) {
                            if (!cursor.isNull(columnIndex)) {
                                String columnValue = cursor.getString(columnIndex);
                                values.add(columnValue);
                            }
                        }
                    }

                    String hash = generateHash(values);
                    mSetOfHashes.add(hash);

                    cursor.moveToNext();
                }
            }
        } catch (Exception ex) {
            DLog.log(ex);
            // TODO: handle the exception? Most likely the required content provider is not present
        }
        finally {
            if (cursor != null)
                cursor.close();
        }

        DLog.log("<< EMDuplicateFinder");

    }

    String generateHash(List<String> aValues) {
        // TODO: we should probably use a better hash maker than this
        String textToHash = new String();
        for (String value : aValues) {
            textToHash += value; // TODO: we should probably using string builder for improved efficiency
        }

        //DLog.log("Generating hash of: " + textToHash);

        String hash = Integer.toString(textToHash.hashCode());

       // DLog.log("Hash: " + hash);

        return hash;
    }

    public boolean itemExists(ContentValues aContentValues) {
        // Generate a hash of the content values (only use the strings from the projection and use them in order)
        boolean itemExists = false;
        if(!mSetOfHashes.isEmpty()){
            List<String> valueList = new ArrayList<String>();
            for (String key : mKeys) {
                String stringValue = aContentValues.getAsString(key);
                if (stringValue != null) {
                    valueList.add(stringValue);
                }
            }
            String hash = generateHash(valueList);
            itemExists = mSetOfHashes.contains(hash);
        }

       /* if (itemExists)
            DLog.log("Skipping Message as its already exists");*/
        return itemExists;
    }

    Set<String> mSetOfHashes = new HashSet<String>();
    List<String> mKeys;
    Uri mUri;
}
