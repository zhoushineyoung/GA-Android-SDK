package com.gameanalytics.android;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBOpenHelper extends SQLiteOpenHelper {

	// Database details
	private final static String DB_NAME = "GameAnalytics";
	private final static int DB_VERSION = 4;

	public DBOpenHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		// Database is created for the first time
		// Create tables:
		GALog.i("Creating database to store events.");
		db.execSQL(EventDatabase.CREATE_TABLE);

		// From version 1.14.0 onwards, we use Google AID if available.
		// Set preference when creating table to avoid changing user IDs of
		// existing users.
		GameAnalytics.setNewUser(true);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// Version 1 - ORIGINAL
		// Version 2 - Added optional user fields
		// Version 3 - Added severity column
		if (newVersion > oldVersion) {
			String addColumn = "ALTER TABLE " + EventDatabase.TABLENAME + " ADD COLUMN ";
			String text = " text";
			if (oldVersion <= 1) {
				db.execSQL(addColumn + EventDatabase.PLATFORM + text);
				db.execSQL(addColumn + EventDatabase.DEVICE + text);
				db.execSQL(addColumn + EventDatabase.OS_MAJOR + text);
				db.execSQL(addColumn + EventDatabase.OS_MINOR + text);
				db.execSQL(addColumn + EventDatabase.SDK_VERSION + text);
				db.execSQL(addColumn + EventDatabase.INSTALL_PUBLISHER + text);
				db.execSQL(addColumn + EventDatabase.INSTALL_SITE + text);
				db.execSQL(addColumn + EventDatabase.INSTALL_CAMPAIGN + text);
				db.execSQL(addColumn + EventDatabase.INSTALL_ADGROUP + text);
				db.execSQL(addColumn + EventDatabase.INSTALL_AD + text);
				db.execSQL(addColumn + EventDatabase.INSTALL_KEYWORD + text);
				db.execSQL(addColumn + EventDatabase.GAME_KEY + text);
				db.execSQL(addColumn + EventDatabase.SECRET_KEY + text);
				db.execSQL(addColumn + EventDatabase.ANDROID_ID + text);
			}
			if (oldVersion <= 2) {
				db.execSQL(addColumn + EventDatabase.SEVERITY + text);
			}
			if (oldVersion <= 3) {
				db.execSQL(addColumn + EventDatabase.GOOGLE_AID + text);
			}
		}
	}
}