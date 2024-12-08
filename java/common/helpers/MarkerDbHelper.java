package common.helpers;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.campustouring.CustomMarkerContract;

public class MarkerDbHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "CustomMarkerDB.db";
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + CustomMarkerContract.MarkerEntry.TABLE_NAME + " (" +
                    CustomMarkerContract.MarkerEntry._ID + " INTEGER PRIMARY KEY," +
                    CustomMarkerContract.MarkerEntry.COLUMN_NAME_NAME + " TEXT," +
                    CustomMarkerContract.MarkerEntry.COLUMN_NAME_SHORTNAME + " TEXT," +
                    CustomMarkerContract.MarkerEntry.COLUMN_NAME_LINK + " TEXT," +
                    CustomMarkerContract.MarkerEntry.COLUMN_NAME_LATITUDE + " TEXT," +
                    CustomMarkerContract.MarkerEntry.COLUMN_NAME_LONGITUDE + " TEXT," +
                    CustomMarkerContract.MarkerEntry.COLUMN_NAME_ISDEFAULTMARKER + " INTEGER)";
    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + CustomMarkerContract.MarkerEntry.TABLE_NAME;

    public MarkerDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}