package com.dm.assist;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.dm.assist.model.Campaign;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class DBHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 5;
    private static final String DATABASE_NAME = "campaign_db";
    private static final String TABLE_JSON_DATA = "json_data";
    private static final String KEY_ID = "id";
    private static final String KEY_JSON_DATA = "json_data";

    private static final String TOKEN_ID = "4752d258-876d-476d-a01d-a6b59e308437";


    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_JSON_DATA_TABLE = "CREATE TABLE " + TABLE_JSON_DATA + "("
                + KEY_ID + " TEXT PRIMARY KEY,"
                + KEY_JSON_DATA + " TEXT" + ")";
        db.execSQL(CREATE_JSON_DATA_TABLE);
        initializeTokens(db);
    }

    private ContentValues build_token_cv(long tokens)
    {
        try {
            ContentValues values = new ContentValues();
            values.put(KEY_ID, TOKEN_ID);
            JSONObject obj = new JSONObject();
            obj = obj.put("ai_tokens", tokens); // Initialize user with 10000 tokens.  Move this server side attached to google account if app takes off.
            values.put(KEY_JSON_DATA, obj.toString());
            return values;
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    public void initializeTokens(SQLiteDatabase db)
    {
        db.insert(TABLE_JSON_DATA, null, build_token_cv(10000));
    }

    public long getTokens()
    {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_JSON_DATA, new String[]{KEY_ID, KEY_JSON_DATA}, KEY_ID + "=?",
                new String[]{TOKEN_ID}, null, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            try {
                JSONObject jsonData = new JSONObject(cursor.getString(1));
                return jsonData.getLong("ai_tokens");
            } catch (JSONException e) {
                e.printStackTrace();
            } finally {
                cursor.close();
            }
        }
        return -1;
    }

    /**
     * You can add tokens when user makes in app purchases or watches ads
     * You can add negative tokens when user makes ChatGPT requests.
     *
     * Tokens are ChatGPT tokens, but we call them AI tokens.
     * We charge 2x what ChatGPT charges for tokens (they do $0.01 = 5000 tokens,
     * we do $0.01 = 2500 tokens.  We say each video ad is $0.01 (unless we can pull
     * a more accurate number out of the ad service)
     * @param toAdd
     */
    public void addTokens(long toAdd)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        long tokens = getTokens(); //TODO FIXME HACK: This may need to use the db we have here or have concurrency issues.  But this all belongs server side anyway.
        ContentValues values = build_token_cv(tokens + toAdd);
        db.update(TABLE_JSON_DATA, values, KEY_ID + "=?", new String[]{TOKEN_ID});
        db.close();
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_JSON_DATA);
        onCreate(db);
    }

    public void addCampaign(Campaign c) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(KEY_ID, c.id);
            values.put(KEY_JSON_DATA, c.toJson().toString());
            db.insert(TABLE_JSON_DATA, null, values);
            db.close();
        }
        catch(JSONException exc)
        {
            exc.printStackTrace();
        }
    }

    public Campaign getCampaign(String idval) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_JSON_DATA, new String[]{KEY_ID, KEY_JSON_DATA}, KEY_ID + "=?",
                new String[]{idval}, null, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            try {
                JSONObject jsonData = new JSONObject(cursor.getString(1));
                cursor.close();
                return Campaign.fromJson(idval, jsonData);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public List<Campaign> getAllCampaigns() {
        String selectQuery = "SELECT * FROM " + TABLE_JSON_DATA;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        List<Campaign> campaigns = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                try {
                    String idval = cursor.getString(0);
                    if (idval.equals(TOKEN_ID))
                        continue; //Ewww.  This obviously needs to go into a different ideally serverside codebase.
                    JSONObject jsonData = new JSONObject(cursor.getString(1));
                    campaigns.add(Campaign.fromJson(idval,jsonData));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        return campaigns;
    }

    public int updateCampaign(Campaign c) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(KEY_JSON_DATA, c.toJson().toString());

            return db.update(TABLE_JSON_DATA, values, KEY_ID + "=?", new String[]{c.id});
        }
        catch(JSONException exc)
        {
            exc.printStackTrace();
            return 0;
        }
    }

    public void deleteCampaign(String id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_JSON_DATA, KEY_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }
}