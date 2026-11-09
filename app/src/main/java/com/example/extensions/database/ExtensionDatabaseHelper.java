package com.example.extensions.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.extensions.models.ExtensionItem;
import com.example.extensions.models.ExtensionSource;
import com.example.extensions.models.ExtensionType;
import com.example.extensions.models.InstallationHistoryEntry;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class ExtensionDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "neu_extensions.db";
    private static final int DATABASE_VERSION = 1;

    // Table: extensions
    private static final String TABLE_EXTENSIONS = "extensions";
    private static final String COL_EXT_ID = "id";
    private static final String COL_EXT_NAME = "name";
    private static final String COL_EXT_DESCRIPTION = "description";
    private static final String COL_EXT_TYPE = "type";
    private static final String COL_EXT_VERSION = "version";
    private static final String COL_EXT_AUTHOR = "author";
    private static final String COL_EXT_SOURCE_URL = "source_url";
    private static final String COL_EXT_INSTALL_DIR = "install_directory";
    private static final String COL_EXT_ENABLED = "enabled";
    private static final String COL_EXT_VALIDATION = "validation_status";
    private static final String COL_EXT_DEPENDENCIES = "dependencies_json";
    private static final String COL_EXT_PERMISSIONS = "permissions_json";
    private static final String COL_EXT_CHECKSUM = "checksum_sha256";
    private static final String COL_EXT_INSTALLED_AT = "installed_timestamp";
    private static final String COL_EXT_UPDATED_AT = "last_updated_timestamp";
    private static final String COL_EXT_UPDATE_AVAIL = "update_available";
    private static final String COL_EXT_LATEST_VER = "latest_version";

    // Table: installation_history
    private static final String TABLE_HISTORY = "installation_history";
    private static final String COL_HIST_ID = "id";
    private static final String COL_HIST_EXT_ID = "extension_id";
    private static final String COL_HIST_EXT_NAME = "extension_name";
    private static final String COL_HIST_ACTION = "action";
    private static final String COL_HIST_STATUS = "status";
    private static final String COL_HIST_VERSION = "version";
    private static final String COL_HIST_DETAILS = "details";
    private static final String COL_HIST_TIMESTAMP = "timestamp";

    // Table: extension_sources
    private static final String TABLE_SOURCES = "extension_sources";
    private static final String COL_SRC_ID = "id";
    private static final String COL_SRC_NAME = "name";
    private static final String COL_SRC_URL = "url";
    private static final String COL_SRC_TYPE = "type";
    private static final String COL_SRC_ENABLED = "enabled";
    private static final String COL_SRC_TRUSTED = "trusted";
    private static final String COL_SRC_LAST_SYNC = "last_sync_timestamp";

    private static volatile ExtensionDatabaseHelper instance;

    public static synchronized ExtensionDatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new ExtensionDatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private ExtensionDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_EXTENSIONS + " ("
                + COL_EXT_ID + " TEXT PRIMARY KEY, "
                + COL_EXT_NAME + " TEXT NOT NULL, "
                + COL_EXT_DESCRIPTION + " TEXT, "
                + COL_EXT_TYPE + " TEXT NOT NULL, "
                + COL_EXT_VERSION + " TEXT, "
                + COL_EXT_AUTHOR + " TEXT, "
                + COL_EXT_SOURCE_URL + " TEXT, "
                + COL_EXT_INSTALL_DIR + " TEXT, "
                + COL_EXT_ENABLED + " INTEGER DEFAULT 1, "
                + COL_EXT_VALIDATION + " TEXT, "
                + COL_EXT_DEPENDENCIES + " TEXT, "
                + COL_EXT_PERMISSIONS + " TEXT, "
                + COL_EXT_CHECKSUM + " TEXT, "
                + COL_EXT_INSTALLED_AT + " INTEGER, "
                + COL_EXT_UPDATED_AT + " INTEGER, "
                + COL_EXT_UPDATE_AVAIL + " INTEGER DEFAULT 0, "
                + COL_EXT_LATEST_VER + " TEXT"
                + ");");

        db.execSQL("CREATE TABLE " + TABLE_HISTORY + " ("
                + COL_HIST_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_HIST_EXT_ID + " TEXT, "
                + COL_HIST_EXT_NAME + " TEXT, "
                + COL_HIST_ACTION + " TEXT, "
                + COL_HIST_STATUS + " TEXT, "
                + COL_HIST_VERSION + " TEXT, "
                + COL_HIST_DETAILS + " TEXT, "
                + COL_HIST_TIMESTAMP + " INTEGER"
                + ");");

        db.execSQL("CREATE TABLE " + TABLE_SOURCES + " ("
                + COL_SRC_ID + " TEXT PRIMARY KEY, "
                + COL_SRC_NAME + " TEXT, "
                + COL_SRC_URL + " TEXT UNIQUE, "
                + COL_SRC_TYPE + " TEXT, "
                + COL_SRC_ENABLED + " INTEGER DEFAULT 1, "
                + COL_SRC_TRUSTED + " INTEGER DEFAULT 0, "
                + COL_SRC_LAST_SYNC + " INTEGER"
                + ");");

        // Seed default trusted sources
        seedDefaultSources(db);
    }

    private void seedDefaultSources(SQLiteDatabase db) {
        ContentValues cv = new ContentValues();
        cv.put(COL_SRC_ID, "src_official_skills");
        cv.put(COL_SRC_NAME, "Official Android & AI Skills");
        cv.put(COL_SRC_URL, "https://github.com/aistudio-skills/official-skills");
        cv.put(COL_SRC_TYPE, "GITHUB");
        cv.put(COL_SRC_ENABLED, 1);
        cv.put(COL_SRC_TRUSTED, 1);
        cv.put(COL_SRC_LAST_SYNC, System.currentTimeMillis());
        db.insert(TABLE_SOURCES, null, cv);

        ContentValues cv2 = new ContentValues();
        cv2.put(COL_SRC_ID, "src_community_agents");
        cv2.put(COL_SRC_NAME, "NEU Community Agents Hub");
        cv2.put(COL_SRC_URL, "https://github.com/neucode-hub/community-agents");
        cv2.put(COL_SRC_TYPE, "GITHUB");
        cv2.put(COL_SRC_ENABLED, 1);
        cv2.put(COL_SRC_TRUSTED, 1);
        cv2.put(COL_SRC_LAST_SYNC, System.currentTimeMillis());
        db.insert(TABLE_SOURCES, null, cv2);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Migration support
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EXTENSIONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HISTORY);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SOURCES);
        onCreate(db);
    }

    // ==================== EXTENSIONS CRUD ====================

    public synchronized boolean insertOrUpdateExtension(ExtensionItem item) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_EXT_ID, item.getId());
        cv.put(COL_EXT_NAME, item.getName());
        cv.put(COL_EXT_DESCRIPTION, item.getDescription());
        cv.put(COL_EXT_TYPE, item.getType() != null ? item.getType().name() : ExtensionType.SKILL.name());
        cv.put(COL_EXT_VERSION, item.getVersion());
        cv.put(COL_EXT_AUTHOR, item.getAuthor());
        cv.put(COL_EXT_SOURCE_URL, item.getSourceUrl());
        cv.put(COL_EXT_INSTALL_DIR, item.getInstallDirectory());
        cv.put(COL_EXT_ENABLED, item.isEnabled() ? 1 : 0);
        cv.put(COL_EXT_VALIDATION, item.getValidationStatus());
        cv.put(COL_EXT_DEPENDENCIES, new JSONArray(item.getDependencies()).toString());
        cv.put(COL_EXT_PERMISSIONS, new JSONArray(item.getPermissions()).toString());
        cv.put(COL_EXT_CHECKSUM, item.getChecksumSha256());
        cv.put(COL_EXT_INSTALLED_AT, item.getInstalledTimestamp());
        cv.put(COL_EXT_UPDATED_AT, item.getLastUpdatedTimestamp());
        cv.put(COL_EXT_UPDATE_AVAIL, item.isUpdateAvailable() ? 1 : 0);
        cv.put(COL_EXT_LATEST_VER, item.getLatestVersion());

        long result = db.insertWithOnConflict(TABLE_EXTENSIONS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        return result != -1;
    }

    public synchronized List<ExtensionItem> getAllExtensions() {
        List<ExtensionItem> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_EXTENSIONS, null, null, null, null, null, COL_EXT_NAME + " ASC");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                list.add(cursorToExtension(cursor));
            }
            cursor.close();
        }
        return list;
    }

    public synchronized List<ExtensionItem> getExtensionsByType(ExtensionType type) {
        List<ExtensionItem> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_EXTENSIONS, null, COL_EXT_TYPE + " = ?",
                new String[]{type.name()}, null, null, COL_EXT_NAME + " ASC");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                list.add(cursorToExtension(cursor));
            }
            cursor.close();
        }
        return list;
    }

    public synchronized ExtensionItem getExtensionById(String id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_EXTENSIONS, null, COL_EXT_ID + " = ?",
                new String[]{id}, null, null, null);
        ExtensionItem item = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                item = cursorToExtension(cursor);
            }
            cursor.close();
        }
        return item;
    }

    public synchronized boolean deleteExtension(String id) {
        SQLiteDatabase db = getWritableDatabase();
        int rows = db.delete(TABLE_EXTENSIONS, COL_EXT_ID + " = ?", new String[]{id});
        return rows > 0;
    }

    public synchronized boolean setExtensionEnabled(String id, boolean enabled) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_EXT_ENABLED, enabled ? 1 : 0);
        cv.put(COL_EXT_UPDATED_AT, System.currentTimeMillis());
        int rows = db.update(TABLE_EXTENSIONS, cv, COL_EXT_ID + " = ?", new String[]{id});
        return rows > 0;
    }

    private ExtensionItem cursorToExtension(Cursor c) {
        ExtensionItem item = new ExtensionItem();
        item.setId(c.getString(c.getColumnIndexOrThrow(COL_EXT_ID)));
        item.setName(c.getString(c.getColumnIndexOrThrow(COL_EXT_NAME)));
        item.setDescription(c.getString(c.getColumnIndexOrThrow(COL_EXT_DESCRIPTION)));
        try {
            item.setType(ExtensionType.valueOf(c.getString(c.getColumnIndexOrThrow(COL_EXT_TYPE))));
        } catch (Exception e) {
            item.setType(ExtensionType.SKILL);
        }
        item.setVersion(c.getString(c.getColumnIndexOrThrow(COL_EXT_VERSION)));
        item.setAuthor(c.getString(c.getColumnIndexOrThrow(COL_EXT_AUTHOR)));
        item.setSourceUrl(c.getString(c.getColumnIndexOrThrow(COL_EXT_SOURCE_URL)));
        item.setInstallDirectory(c.getString(c.getColumnIndexOrThrow(COL_EXT_INSTALL_DIR)));
        item.setEnabled(c.getInt(c.getColumnIndexOrThrow(COL_EXT_ENABLED)) == 1);
        item.setValidationStatus(c.getString(c.getColumnIndexOrThrow(COL_EXT_VALIDATION)));
        item.setChecksumSha256(c.getString(c.getColumnIndexOrThrow(COL_EXT_CHECKSUM)));
        item.setInstalledTimestamp(c.getLong(c.getColumnIndexOrThrow(COL_EXT_INSTALLED_AT)));
        item.setLastUpdatedTimestamp(c.getLong(c.getColumnIndexOrThrow(COL_EXT_UPDATED_AT)));
        item.setUpdateAvailable(c.getInt(c.getColumnIndexOrThrow(COL_EXT_UPDATE_AVAIL)) == 1);
        item.setLatestVersion(c.getString(c.getColumnIndexOrThrow(COL_EXT_LATEST_VER)));

        // Parse JSON lists
        String depStr = c.getString(c.getColumnIndexOrThrow(COL_EXT_DEPENDENCIES));
        if (depStr != null && !depStr.isEmpty()) {
            try {
                JSONArray arr = new JSONArray(depStr);
                List<String> deps = new ArrayList<>();
                for (int i = 0; i < arr.length(); i++) deps.add(arr.getString(i));
                item.setDependencies(deps);
            } catch (JSONException ignored) {}
        }

        String permStr = c.getString(c.getColumnIndexOrThrow(COL_EXT_PERMISSIONS));
        if (permStr != null && !permStr.isEmpty()) {
            try {
                JSONArray arr = new JSONArray(permStr);
                List<String> perms = new ArrayList<>();
                for (int i = 0; i < arr.length(); i++) perms.add(arr.getString(i));
                item.setPermissions(perms);
            } catch (JSONException ignored) {}
        }

        return item;
    }

    // ==================== HISTORY CRUD ====================

    public synchronized void recordHistory(InstallationHistoryEntry entry) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_HIST_EXT_ID, entry.getExtensionId());
        cv.put(COL_HIST_EXT_NAME, entry.getExtensionName());
        cv.put(COL_HIST_ACTION, entry.getAction());
        cv.put(COL_HIST_STATUS, entry.getStatus());
        cv.put(COL_HIST_VERSION, entry.getVersion());
        cv.put(COL_HIST_DETAILS, entry.getDetails());
        cv.put(COL_HIST_TIMESTAMP, entry.getTimestamp());
        db.insert(TABLE_HISTORY, null, cv);
    }

    public synchronized List<InstallationHistoryEntry> getAllHistory() {
        List<InstallationHistoryEntry> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_HISTORY, null, null, null, null, null, COL_HIST_TIMESTAMP + " DESC");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                InstallationHistoryEntry e = new InstallationHistoryEntry();
                e.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COL_HIST_ID)));
                e.setExtensionId(cursor.getString(cursor.getColumnIndexOrThrow(COL_HIST_EXT_ID)));
                e.setExtensionName(cursor.getString(cursor.getColumnIndexOrThrow(COL_HIST_EXT_NAME)));
                e.setAction(cursor.getString(cursor.getColumnIndexOrThrow(COL_HIST_ACTION)));
                e.setStatus(cursor.getString(cursor.getColumnIndexOrThrow(COL_HIST_STATUS)));
                e.setVersion(cursor.getString(cursor.getColumnIndexOrThrow(COL_HIST_VERSION)));
                e.setDetails(cursor.getString(cursor.getColumnIndexOrThrow(COL_HIST_DETAILS)));
                e.setTimestamp(cursor.getLong(cursor.getColumnIndexOrThrow(COL_HIST_TIMESTAMP)));
                list.add(e);
            }
            cursor.close();
        }
        return list;
    }

    // ==================== SOURCES CRUD ====================

    public synchronized boolean insertSource(ExtensionSource src) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_SRC_ID, src.getId());
        cv.put(COL_SRC_NAME, src.getName());
        cv.put(COL_SRC_URL, src.getUrl());
        cv.put(COL_SRC_TYPE, src.getType());
        cv.put(COL_SRC_ENABLED, src.isEnabled() ? 1 : 0);
        cv.put(COL_SRC_TRUSTED, src.isTrusted() ? 1 : 0);
        cv.put(COL_SRC_LAST_SYNC, src.getLastSyncTimestamp());
        long res = db.insertWithOnConflict(TABLE_SOURCES, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        return res != -1;
    }

    public synchronized List<ExtensionSource> getAllSources() {
        List<ExtensionSource> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_SOURCES, null, null, null, null, null, COL_SRC_NAME + " ASC");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                ExtensionSource s = new ExtensionSource();
                s.setId(cursor.getString(cursor.getColumnIndexOrThrow(COL_SRC_ID)));
                s.setName(cursor.getString(cursor.getColumnIndexOrThrow(COL_SRC_NAME)));
                s.setUrl(cursor.getString(cursor.getColumnIndexOrThrow(COL_SRC_URL)));
                s.setType(cursor.getString(cursor.getColumnIndexOrThrow(COL_SRC_TYPE)));
                s.setEnabled(cursor.getInt(cursor.getColumnIndexOrThrow(COL_SRC_ENABLED)) == 1);
                s.setTrusted(cursor.getInt(cursor.getColumnIndexOrThrow(COL_SRC_TRUSTED)) == 1);
                s.setLastSyncTimestamp(cursor.getLong(cursor.getColumnIndexOrThrow(COL_SRC_LAST_SYNC)));
                list.add(s);
            }
            cursor.close();
        }
        return list;
    }

    public synchronized boolean deleteSource(String id) {
        SQLiteDatabase db = getWritableDatabase();
        int rows = db.delete(TABLE_SOURCES, COL_SRC_ID + " = ?", new String[]{id});
        return rows > 0;
    }
}
