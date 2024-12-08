package com.example.campustouring;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import common.helpers.MarkerDbHelper;

public class CustomMarkerContract {
    MarkerDbHelper dbHelper;

    public CustomMarkerContract(Context context) {
        dbHelper = new MarkerDbHelper(context);
    }

    public static class MarkerEntry implements BaseColumns {
        public static final String TABLE_NAME = "CustomMarkersTable";
        public static final String COLUMN_NAME_NAME = "name";
        public static final String COLUMN_NAME_SHORTNAME = "shortName";
        public static final String COLUMN_NAME_LINK = "link";
        public static final String COLUMN_NAME_LATITUDE = "latitude";
        public static final String COLUMN_NAME_LONGITUDE = "longitude";
        public static final String COLUMN_NAME_ISDEFAULTMARKER = "isDefaultMarker";
    }

    public static class MarkerEntryObj {
        public MarkerEntryObj(String name, String shortName, String link, Double latitude, Double longitude) {
            this.name = name;
            this.shortName = shortName;
            this.link = link;
            this.latitude = latitude;
            this.longitude = longitude;
        }
        public MarkerEntryObj(long _id, String name, String shortName, String link, Double latitude, Double longitude, int isDefaultMarker) {
            this._id = _id;
            this.name = name;
            this.shortName = shortName;
            this.link = link;
            this.latitude = latitude;
            this.longitude = longitude;
            this.isDefaultMarker = isDefaultMarker;
        }

        public MarkerEntryObj(String name, String shortName, String link, Double latitude, Double longitude, int isDefaultMarker) {
            this.name = name;
            this.shortName = shortName;
            this.link = link;
            this.latitude = latitude;
            this.longitude = longitude;
            this.isDefaultMarker = isDefaultMarker;
        }

        long _id;
        String name;
        String shortName;
        String link;
        Double latitude;
        Double longitude;
        int isDefaultMarker;

        public boolean isEmpty() {
            if (this._id == 0.0 && this.name.length() == 0 && this.shortName.length() == 0 && this.link.length() == 0 && this.latitude == 0.0 && this.longitude == 0){
                return true;
            }
            return false;
        }
    }

    public MarkerEntryObj readSingleFromDb(long index) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String[] projection = {
                CustomMarkerContract.MarkerEntry._ID,
                CustomMarkerContract.MarkerEntry.COLUMN_NAME_NAME,
                CustomMarkerContract.MarkerEntry.COLUMN_NAME_SHORTNAME,
                CustomMarkerContract.MarkerEntry.COLUMN_NAME_LINK,
                CustomMarkerContract.MarkerEntry.COLUMN_NAME_LATITUDE,
                CustomMarkerContract.MarkerEntry.COLUMN_NAME_LONGITUDE,
                CustomMarkerContract.MarkerEntry.COLUMN_NAME_ISDEFAULTMARKER
        };

        String selection = CustomMarkerContract.MarkerEntry._ID + " = ?";
        String[] selectionArgs = {String.valueOf(index)};

        Cursor cursor = db.query(
                CustomMarkerContract.MarkerEntry.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null
        );
        MarkerEntryObj marker;

        if (cursor.moveToNext()) {
            marker = new MarkerEntryObj(
                    cursor.getLong(cursor.getColumnIndexOrThrow(MarkerEntry._ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(MarkerEntry.COLUMN_NAME_NAME)),
                    cursor.getString(cursor.getColumnIndexOrThrow(MarkerEntry.COLUMN_NAME_SHORTNAME)),
                    cursor.getString(cursor.getColumnIndexOrThrow(MarkerEntry.COLUMN_NAME_LINK)),
                    cursor.getDouble(cursor.getColumnIndexOrThrow(MarkerEntry.COLUMN_NAME_LATITUDE)),
                    cursor.getDouble(cursor.getColumnIndexOrThrow(MarkerEntry.COLUMN_NAME_LONGITUDE)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(MarkerEntry.COLUMN_NAME_ISDEFAULTMARKER))
                    );

        }
        else{
            return null;
        }
        cursor.close();

        return marker;
    }

    public List<MarkerEntryObj> readAllFromDb() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String[] projection = {
                CustomMarkerContract.MarkerEntry._ID,
                CustomMarkerContract.MarkerEntry.COLUMN_NAME_NAME,
                CustomMarkerContract.MarkerEntry.COLUMN_NAME_SHORTNAME,
                CustomMarkerContract.MarkerEntry.COLUMN_NAME_LINK,
                CustomMarkerContract.MarkerEntry.COLUMN_NAME_LATITUDE,
                CustomMarkerContract.MarkerEntry.COLUMN_NAME_LONGITUDE,
                CustomMarkerContract.MarkerEntry.COLUMN_NAME_ISDEFAULTMARKER
        };


        Cursor cursor = db.query(
                CustomMarkerContract.MarkerEntry.TABLE_NAME,
                projection,
                null,
                null,
                null,
                null,
                null
        );
        List<MarkerEntryObj> markerList = new ArrayList<>();

        while (cursor.moveToNext()) {
            MarkerEntryObj marker = new MarkerEntryObj(
                    cursor.getLong(cursor.getColumnIndexOrThrow(MarkerEntry._ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(MarkerEntry.COLUMN_NAME_NAME)),
                    cursor.getString(cursor.getColumnIndexOrThrow(MarkerEntry.COLUMN_NAME_SHORTNAME)),
                    cursor.getString(cursor.getColumnIndexOrThrow(MarkerEntry.COLUMN_NAME_LINK)),
                    cursor.getDouble(cursor.getColumnIndexOrThrow(MarkerEntry.COLUMN_NAME_LATITUDE)),
                    cursor.getDouble(cursor.getColumnIndexOrThrow(MarkerEntry.COLUMN_NAME_LONGITUDE)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(MarkerEntry.COLUMN_NAME_ISDEFAULTMARKER))
            );
            marker._id = cursor.getLong(cursor.getColumnIndexOrThrow(MarkerEntry._ID));

            markerList.add(marker);
        }
        cursor.close();

        return markerList;
    }

    public void deleteOneFromDb(int localIndex) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        String selection = CustomMarkerContract.MarkerEntry._ID + " = ?";
        String[] selectionArgs = {String.valueOf(localIndex)};

        db.delete(CustomMarkerContract.MarkerEntry.TABLE_NAME, selection, selectionArgs);
    }

    public int updateMarker(MarkerEntryObj marker) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(CustomMarkerContract.MarkerEntry.COLUMN_NAME_NAME, marker.name);
        values.put(CustomMarkerContract.MarkerEntry.COLUMN_NAME_SHORTNAME, marker.shortName);
        values.put(CustomMarkerContract.MarkerEntry.COLUMN_NAME_LINK, marker.link);
        values.put(CustomMarkerContract.MarkerEntry.COLUMN_NAME_LATITUDE, marker.latitude);
        values.put(CustomMarkerContract.MarkerEntry.COLUMN_NAME_LONGITUDE, marker.longitude);
        values.put(CustomMarkerContract.MarkerEntry.COLUMN_NAME_ISDEFAULTMARKER, marker.isDefaultMarker);

        String selection = CustomMarkerContract.MarkerEntry._ID + " = ?";
        String[] selectionArgs = {String.valueOf(marker._id)};
        Log.d("DATA MARKER UPDATE", "updateMarker ID: " + marker._id);
        return db.update(
                CustomMarkerContract.MarkerEntry.TABLE_NAME,
                values,
                selection,
                selectionArgs
                );
    }

    public void saveNewMarker(MarkerEntryObj marker) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(CustomMarkerContract.MarkerEntry.COLUMN_NAME_NAME, marker.name);
        values.put(CustomMarkerContract.MarkerEntry.COLUMN_NAME_SHORTNAME, marker.shortName);
        values.put(CustomMarkerContract.MarkerEntry.COLUMN_NAME_LINK, marker.link);
        values.put(CustomMarkerContract.MarkerEntry.COLUMN_NAME_LATITUDE, marker.latitude);
        values.put(CustomMarkerContract.MarkerEntry.COLUMN_NAME_LONGITUDE, marker.longitude);
        values.put(CustomMarkerContract.MarkerEntry.COLUMN_NAME_ISDEFAULTMARKER, marker.isDefaultMarker);

        db.insert(MarkerEntry.TABLE_NAME, null, values);
    }

    public void clearDb() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(MarkerEntry.TABLE_NAME, null, null);
    }
    public void dropTable(){
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS " + MarkerEntry.TABLE_NAME);
    }
}