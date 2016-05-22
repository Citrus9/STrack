package pl.pwr.citrus.strack.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

import java.util.HashMap;

import android.util.Log;

public class DatabaseAdapter extends SQLiteOpenHelper {

    private static DatabaseAdapter sSingleton;
    private SQLiteDatabase mDb;

    private static final String DATABASE_NAME = "stores";
    private static final int SCHEMA_VERSION = 1; //bez tablicy groupMembers  i groupList

    public static final String STORE_KEY_ROWID = "_id";
    public static final String STORE_TABLE = "store_table";
    public static final String STORE_DELETED = "store_deleted";
    public static final String STORE_POSITION = "store_position";
    public static final String STORE_NAME = "store_name";
    public static final String STORE_GROCERY = "store_grocery";
    public static final String STORE_HOUSEHOLD = "store_household";
    public static final String STORE_COSMETICS = "store_cosmetics";
    public static final String STORE_DISTANCE = "store_distance";

    private final String sort_order = "ASC"; // ASC or DESC

    // String to create database table
    private static final String DATABASE_CREATE_STORES =
            "CREATE TABLE " + STORE_TABLE + " (" +
                    STORE_KEY_ROWID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    STORE_DELETED + " INTEGER, " +
                    STORE_POSITION + " INTEGER, " +
                    STORE_NAME + " TEXT, " +
                    STORE_GROCERY + " INTEGER, " +
                    STORE_HOUSEHOLD + " INTEGER, " +
                    STORE_COSMETICS + " INTEGER);";

    // Methods to setup database singleton and connections
    synchronized public static DatabaseAdapter getInstance(Context ctxt) {
        if (sSingleton == null) {
            sSingleton = new DatabaseAdapter(ctxt);
        }
        return sSingleton;
    }

    public DatabaseAdapter(Context ctxt) {
        super(ctxt, DATABASE_NAME, null, SCHEMA_VERSION);
        //sSingleton = this;
    }

    public DatabaseAdapter openConnection() throws SQLException {
        if (mDb == null) {
            mDb = sSingleton.getWritableDatabase();
        }
        return this;
    }

    public synchronized void closeConnection() {
        if (sSingleton != null) {
            sSingleton.close();
            mDb.close();
            sSingleton = null;
            mDb = null;
        }
    }

    public void insertStore(String name, int groc, int house, int cosm) {
        int item_Position = getMaxMealColumnData();
        ContentValues contentValues = new ContentValues();

        contentValues.put(STORE_DELETED, 1);
        contentValues.put(STORE_POSITION, (item_Position + 1));
        contentValues.put(STORE_NAME, name);
        contentValues.put(STORE_GROCERY, groc);
        contentValues.put(STORE_HOUSEHOLD, house);
        contentValues.put(STORE_COSMETICS, cosm);

        try {
            if (mDb.insert(STORE_TABLE,null, contentValues) < 0) {
                return;
            }
        } catch (Exception ex) {
            Log.d("Exception in importing", ex.getMessage().toString());
        }
    }

    public boolean updateStore(long productId, String name, int groc, int house, int cosm) {

        ContentValues contentValues = new ContentValues();

        contentValues.put(STORE_NAME, name);
        contentValues.put(STORE_GROCERY, groc);
        contentValues.put(STORE_HOUSEHOLD, house);
        contentValues.put(STORE_COSMETICS, cosm);

        return  mDb.update(STORE_TABLE, contentValues, STORE_KEY_ROWID + "=" + productId,
                null) > 0;
    }

    public boolean updateStoreName(long productId, String name) {

        ContentValues contentValues = new ContentValues();

        contentValues.put(STORE_NAME, name);

        return  mDb.update(STORE_TABLE, contentValues, STORE_KEY_ROWID + "=" + productId,
                null) > 0;
    }

    public boolean deleteStore(long productId) {

        ContentValues contentValues = new ContentValues();

        contentValues.put(STORE_DELETED, 2);

        return  mDb.update(STORE_TABLE, contentValues, STORE_KEY_ROWID + "=" + productId,
                null) > 0;
    }

    public boolean undeleteStore(long productId) {

        ContentValues contentValues = new ContentValues();

        contentValues.put(STORE_DELETED, 1);

        return mDb.update(STORE_TABLE, contentValues, STORE_KEY_ROWID + "=" + productId,
                null) > 0;
    }

    // initial database load with dummy records
    @Override
    public void onCreate(SQLiteDatabase mDb) {
        try {
            mDb.beginTransaction();

            mDb.execSQL(DATABASE_CREATE_STORES);

            mDb.setTransactionSuccessful();
        } finally {
            mDb.endTransaction();
        }

    }

    public Cursor getStoreRecord(long rowId) throws SQLException {
        Cursor mLetterCursor = mDb.query(true, STORE_TABLE, null,
                STORE_KEY_ROWID + "=" + rowId, null, null, null, null, null);
        if (mLetterCursor != null) {
            mLetterCursor.moveToFirst();
        }
        return mLetterCursor;
    }

    @Override
    public void onUpgrade(SQLiteDatabase mDb, int oldVersion, int newVersion) {
        // DON'T DO IT ON PRODUCTION VERSION
        mDb.execSQL("DROP TABLE IF EXISTS " + STORE_TABLE);
        onCreate(mDb);
    }


    public int getMaxMealColumnData() {

        mDb = sSingleton.getWritableDatabase();
        SQLiteStatement stmt = mDb
                .compileStatement("SELECT MAX("+ STORE_POSITION +") FROM "+ STORE_TABLE);

        return (int) stmt.simpleQueryForLong();
    }

    public Cursor getAllMealRecords() {

        return mDb.query(STORE_TABLE, null,
                STORE_DELETED + "=" + 1, null, null, null,
                STORE_NAME + " " + sort_order);
    }

    public String getStoreNameById(long store_id) {

        final SQLiteStatement stmt = mDb
                .compileStatement("SELECT "+ STORE_NAME +" FROM "+ STORE_TABLE + " WHERE " + STORE_KEY_ROWID + " = " +store_id);

        return (String) stmt.simpleQueryForString();
    }
}