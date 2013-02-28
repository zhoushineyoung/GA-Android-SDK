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
import android.util.Log;

public class EventDatabase {

	private SQLiteDatabase db;

	public EventDatabase(Context context) {
		this.db = new OpenHelper(context).getWritableDatabase();
	}

	// DATABASE SCHEMA
	// Common
	public final static String TABLENAME = "events";
	public final static String ROW_ID = "_id";
	public final static String TYPE = "type";
	public final static String USER_ID = "user_id";
	public final static String SESSION_ID = "session_id";
	public final static String BUILD = "build";
	public final static String EVENT_ID = "event_id";
	public final static String AREA = "area";
	public final static String X = "x";
	public final static String Y = "y";
	public final static String Z = "z";

	// Design
	public final static String VALUE = "value";

	// Business
	public final static String CURRENCY = "currency";
	public final static String AMOUNT = "amount";

	// User
	public final static String GENDER = "gender";
	public final static String BIRTH_YEAR = "birth_year";
	public final static String FRIEND_COUNT = "friend_count";

	// Quality
	public final static String MESSAGE = "message";

	public final static String CREATE_TABLE = "create table " + TABLENAME
			+ " (" + ROW_ID + " integer primary key autoincrement not null,"
			+ TYPE + " text," + USER_ID + " text," + SESSION_ID + " text,"
			+ BUILD + " text," + EVENT_ID + " text," + AREA + " text," + X
			+ " num," + Y + " num," + Z + " num," + VALUE + " num," + CURRENCY
			+ " text," + AMOUNT + " num," + GENDER + " text," + BIRTH_YEAR
			+ " num," + FRIEND_COUNT + " num" + ");";

	// Database operations (SYNCHRONIZED)
	// The following methods are synchronized so that extra events won't be
	// added to the database while the current lot are being pulled out and
	// sent.
	synchronized public ArrayList<?>[] getEvents() {
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
					businessEvents.add(new BusinessEvent(USER_ID, SESSION_ID,
							BUILD, eventId, area, x, y, z, currency, amount));
				} else if (type.equals(GameAnalytics.USER)) {
					gender = cursor.getString(13).toCharArray()[0];
					birthYear = cursor.getInt(14);
					friendCount = cursor.getInt(15);
					userEvents.add(new UserEvent(USER_ID, SESSION_ID, BUILD,
							eventId, area, x, y, z, gender, birthYear,
							friendCount));
				} else if (type.equals(GameAnalytics.QUALITY)) {
					message = cursor.getString(16);
					qualityEvents.add(new QualityEvent(USER_ID, SESSION_ID,
							BUILD, eventId, area, x, y, z, message));
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
		db.insert(TABLENAME, null, values);
	}

	public void addDesignEvent(String userId, String sessionId, String build,
			String eventId, String area, float x, float y, float z, float value) {
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

	public void addBusinessEvent(String userId, String sessionId, String build,
			String eventId, String area, float x, float y, float z,
			String currency, int amount) {
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

	public void addUserEvent(String userId, String sessionId, String build,
			String eventId, String area, float x, float y, float z,
			char gender, int birthYear, int friendCount) {
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
		// Do insert on seperate thread so that if synchronization locks up the
		// method, main thread can return.
		new Thread() {
			public void run() {
				insert(values);
			}
		}.start();
	}

	public void addQualityEvent(String userId, String sessionId, String build,
			String eventId, String area, float x, float y, float z,
			String message) {
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
			Log.d("GameAnalytics", "Creating database to store events.");
			db.execSQL(CREATE_TABLE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// No persistent data, no need for updrades
		}
	}
}