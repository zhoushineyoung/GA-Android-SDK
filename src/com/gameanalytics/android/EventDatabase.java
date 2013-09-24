/* 
   Game Analytics Android Wrapper
   Copyright (c) 2013 Tim Wicksteed <tim@twicecircled.com>
   http:/www.gameanalytics.com

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package com.gameanalytics.android;

import java.util.ArrayList;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class EventDatabase {

	private SQLiteDatabase db;

	public EventDatabase(Context context) {
		this.db = new OpenHelper(context).getWritableDatabase();
	}

	// DATABASE SCHEMA
	// Common
	protected final static String TABLENAME = "events";
	protected final static String ROW_ID = "_id";
	protected final static String TYPE = "type";
	protected final static String USER_ID = "user_id";
	protected final static String SESSION_ID = "session_id";
	protected final static String BUILD = "build";
	protected final static String EVENT_ID = "event_id";
	protected final static String AREA = "area";
	protected final static String X = "x";
	protected final static String Y = "y";
	protected final static String Z = "z";
	private static int MAXIMUM_EVENT_STORAGE = 0; // Default is 0 (unlimited)

	// Design
	protected final static String VALUE = "value";

	// Business
	protected final static String CURRENCY = "currency";
	protected final static String AMOUNT = "amount";

	// User
	protected final static String GENDER = "gender";
	protected final static String BIRTH_YEAR = "birth_year";
	protected final static String FRIEND_COUNT = "friend_count";

	// Optional user events added in V1.10
	protected final static String PLATFORM = "platform";
	protected final static String DEVICE = "device";
	protected final static String OS_MAJOR = "os_major";
	protected final static String OS_MINOR = "os_minor";
	protected final static String SDK_VERSION = "sdk_version";
	protected final static String INSTALL_PUBLISHER = "install_publisher";
	protected final static String INSTALL_SITE = "install_site";
	protected final static String INSTALL_CAMPAIGN = "install_campaign";
	protected final static String INSTALL_AD = "install_ad";
	protected final static String INSTALL_KEYWORD = "install_keyword";

	// Quality
	protected final static String MESSAGE = "message";

	protected final static String CREATE_TABLE = "create table " + TABLENAME
			+ " (" + ROW_ID + " integer primary key autoincrement not null,"
			+ TYPE + " text," + USER_ID + " text," + SESSION_ID + " text,"
			+ BUILD + " text," + EVENT_ID + " text," + AREA + " text," + X
			+ " num," + Y + " num," + Z + " num," + VALUE + " num," + CURRENCY
			+ " text," + AMOUNT + " num," + GENDER + " text," + BIRTH_YEAR
			+ " num," + FRIEND_COUNT + " num," + MESSAGE + " text," + PLATFORM
			+ " text," + DEVICE + " text," + OS_MAJOR + " text," + OS_MINOR
			+ " text," + SDK_VERSION + " text," + INSTALL_PUBLISHER + " text,"
			+ INSTALL_SITE + " text," + INSTALL_CAMPAIGN + " text,"
			+ INSTALL_AD + " text," + INSTALL_KEYWORD + " text" + ");";

	// Database operations (SYNCHRONIZED)
	// The following methods are synchronized so that extra events won't be
	// added to the database while the current lot are being pulled out and
	// sent.
	synchronized protected ArrayList<?>[] getEvents() {
		// Get all events
		Cursor cursor = db.query(TABLENAME, null, null, null, null, null,
				ROW_ID);

		// Create ArrayLists
		ArrayList<DesignEvent> designEvents = new ArrayList<DesignEvent>();
		ArrayList<UserEvent> userEvents = new ArrayList<UserEvent>();
		ArrayList<BusinessEvent> businessEvents = new ArrayList<BusinessEvent>();
		ArrayList<QualityEvent> qualityEvents = new ArrayList<QualityEvent>();

		// Columns
		String type;
		String userId;
		String sessionId;
		String build;
		String eventId;
		String area;
		float x;
		float y;
		float z;
		float value;
		String currency;
		int amount;
		char gender;
		int birthYear;
		int friendCount;
		String message;
		String platform;
		String device;
		String osMajor;
		String osMinor;
		String sdkVersion;
		String installPublisher;
		String installSite;
		String installCampaign;
		String installAd;
		String installKeyword;

		// Populate ArrayLists
		if (cursor.moveToFirst()) {
			while (!cursor.isAfterLast()) {
				type = cursor.getString(1);
				userId = cursor.getString(2);
				sessionId = cursor.getString(3);
				build = cursor.getString(4);
				eventId = cursor.getString(5);
				area = cursor.getString(6);
				x = cursor.getFloat(7);
				y = cursor.getFloat(8);
				z = cursor.getFloat(9);

				if (type.equals(GameAnalytics.DESIGN)) {
					value = cursor.getFloat(10);
					designEvents.add(new DesignEvent(userId, sessionId, build,
							eventId, area, x, y, z, value));
				} else if (type.equals(GameAnalytics.BUSINESS)) {
					currency = cursor.getString(11);
					amount = cursor.getInt(12);
					businessEvents.add(new BusinessEvent(userId, sessionId,
							build, eventId, area, x, y, z, currency, amount));
				} else if (type.equals(GameAnalytics.USER)) {
					gender = cursor.getString(13).toCharArray()[0];
					birthYear = cursor.getInt(14);
					friendCount = cursor.getInt(15);
					platform = cursor.getString(17);
					device = cursor.getString(18);
					osMajor = cursor.getString(19);
					osMinor = cursor.getString(20);
					sdkVersion = cursor.getString(21);
					installPublisher = cursor.getString(22);
					installSite = cursor.getString(23);
					installCampaign = cursor.getString(24);
					installAd = cursor.getString(25);
					installKeyword = cursor.getString(26);
					userEvents.add(new UserEvent(userId, sessionId, build,
							eventId, area, x, y, z, gender, birthYear,
							friendCount, platform, device, osMajor, osMinor,
							sdkVersion, installPublisher, installSite,
							installCampaign, installAd, installKeyword));
				} else if (type.equals(GameAnalytics.QUALITY)) {
					message = cursor.getString(16);
					qualityEvents.add(new QualityEvent(userId, sessionId,
							build, eventId, area, x, y, z, message));
				}
				cursor.moveToNext();
			}
		}
		cursor.close();

		// Delete events from database
		db.delete(TABLENAME, null, null);

		// Return ArrayLists
		return new ArrayList<?>[] { designEvents, businessEvents, userEvents,
				qualityEvents };
	}

	synchronized private void insert(ContentValues values) {
		if (MAXIMUM_EVENT_STORAGE == 0 || !isFull()) {
			db.insert(TABLENAME, null, values);
		}
	}

	protected void addDesignEvent(String userId, String sessionId,
			String build, String eventId, String area, float x, float y,
			float z, float value) {
		final ContentValues values = new ContentValues();
		values.put(TYPE, GameAnalytics.DESIGN);
		values.put(USER_ID, userId);
		values.put(SESSION_ID, sessionId);
		values.put(BUILD, build);
		values.put(EVENT_ID, eventId);
		values.put(AREA, area);
		values.put(X, x);
		values.put(Y, y);
		values.put(Z, z);
		values.put(VALUE, value);
		// Do insert on seperate thread so that if synchronization locks up the
		// method, main thread can return.
		new Thread() {
			public void run() {
				insert(values);
			}
		}.start();
	}

	protected void addBusinessEvent(String userId, String sessionId,
			String build, String eventId, String area, float x, float y,
			float z, String currency, int amount) {
		final ContentValues values = new ContentValues();
		values.put(TYPE, GameAnalytics.BUSINESS);
		values.put(USER_ID, userId);
		values.put(SESSION_ID, sessionId);
		values.put(BUILD, build);
		values.put(EVENT_ID, eventId);
		values.put(AREA, area);
		values.put(X, x);
		values.put(Y, y);
		values.put(Z, z);
		values.put(CURRENCY, currency);
		values.put(AMOUNT, amount);
		// Do insert on seperate thread so that if synchronization locks up the
		// method, main thread can return.
		new Thread() {
			public void run() {
				insert(values);
			}
		}.start();
	}

	protected void addUserEvent(String userId, String sessionId, String build,
			String eventId, String area, float x, float y, float z,
			char gender, int birthYear, int friendCount, String platform,
			String device, String osMajor, String osMinor, String sdkVersion,
			String installPublisher, String installSite,
			String installCampaign, String installAd, String installKeyword) {
		final ContentValues values = new ContentValues();
		values.put(TYPE, GameAnalytics.USER);
		values.put(USER_ID, userId);
		values.put(SESSION_ID, sessionId);
		values.put(BUILD, build);
		values.put(EVENT_ID, eventId);
		values.put(AREA, area);
		values.put(X, x);
		values.put(Y, y);
		values.put(Z, z);
		values.put(GENDER, String.valueOf(gender));
		values.put(BIRTH_YEAR, birthYear);
		values.put(FRIEND_COUNT, friendCount);
		values.put(PLATFORM, platform);
		values.put(DEVICE, device);
		values.put(OS_MAJOR, osMajor);
		values.put(OS_MINOR, osMinor);
		values.put(SDK_VERSION, sdkVersion);
		values.put(INSTALL_PUBLISHER, installPublisher);
		values.put(INSTALL_SITE, installSite);
		values.put(INSTALL_CAMPAIGN, installCampaign);
		values.put(INSTALL_AD, installAd);
		values.put(INSTALL_KEYWORD, installKeyword);
		// Do insert on seperate thread so that if synchronization locks up the
		// method, main thread can return.
		new Thread() {
			public void run() {
				insert(values);
			}
		}.start();
	}

	protected void addQualityEvent(String userId, String sessionId,
			String build, String eventId, String area, float x, float y,
			float z, String message) {
		final ContentValues values = new ContentValues();
		values.put(TYPE, GameAnalytics.QUALITY);
		values.put(USER_ID, userId);
		values.put(SESSION_ID, sessionId);
		values.put(BUILD, build);
		values.put(EVENT_ID, eventId);
		values.put(AREA, area);
		values.put(X, x);
		values.put(Y, y);
		values.put(Z, z);
		values.put(MESSAGE, message);
		// Do insert on seperate thread so that if synchronization locks up the
		// method, main thread can return.
		new Thread() {
			public void run() {
				insert(values);
			}
		}.start();
	}

	private boolean isFull() {
		Cursor cursor = db.query(TABLENAME, new String[] { ROW_ID }, null,
				null, null, null, null);
		if (cursor.getCount() >= MAXIMUM_EVENT_STORAGE) {
			return true;
		} else {
			return false;
		}
	}

	protected void setMaximumEventStorage(int maximumEventStorage) {
		MAXIMUM_EVENT_STORAGE = maximumEventStorage;
	}

	protected void clear() {
		db.delete(TABLENAME, null, null);
	}

	// OPENHELPER CLASS
	private class OpenHelper extends SQLiteOpenHelper {

		// Database details
		private final static String DB_NAME = "GameAnalytics";
		private final static int DB_VERSION = 1;

		public OpenHelper(Context context) {
			super(context, DB_NAME, null, DB_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			// Database is created for the first time
			// Create tables:
			GALog.i("Creating database to store events.");
			db.execSQL(CREATE_TABLE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// Version 1 - ORIGINAL
			// Version 2 - Added optional user fields
			if (newVersion > oldVersion) {
				if (oldVersion == 1) {
					db.execSQL("ALTER TABLE " + TABLENAME + " ADD COLUMN "
							+ PLATFORM + " text," + DEVICE + " text,"
							+ OS_MAJOR + " text," + OS_MINOR + " text,"
							+ SDK_VERSION + " text," + INSTALL_PUBLISHER
							+ " text," + INSTALL_SITE + " text,"
							+ INSTALL_CAMPAIGN + " text," + INSTALL_AD
							+ " text," + INSTALL_KEYWORD + " text");
				}
			}
		}
	}
}