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

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.provider.Settings.Secure;
import android.text.format.Time;
import com.google.gson.Gson;
import com.loopj.twicecircled.android.http.AsyncHttpClient;
import com.loopj.twicecircled.android.http.AsyncHttpResponseHandler;

/**
 * Public singleton class used to interface with the GameAnalytics servers.
 */
public class GameAnalytics {

	// WEBSERVER
	protected static final String API_URL = "http://api.gameanalytics.com/1/";
	protected static final String AUTHORIZATION = "Authorization";
	protected static final String CONTENT_TYPE = "Content-Type";
	protected static final String CONTENT_TYPE_JSON = "application/json";

	// ERRORS
	private static final String BAD_REQUEST = "Not all required fields are present in the data.";
	private static final String BAD_REQUEST_DESC = "When you see this, most likely some required fields are missing from the JSON data you sent. Make sure you include all required fields for the category you are using. Please note that incomplete events are discarded.";
	private static final String NO_GAME = "Game not found";
	private static final String NO_GAME_DESC = "The game key supplied was not recognised. Make sure that you use the game key you were supplied when you signed up in GameAnalytics.initialise().";
	private static final String DATA_NOT_FOUND = "Data not found";
	private static final String DATA_NOT_FOUND_DESC = "No JSON data was sent with the request. Make sure that you are sending some data as either a single JSON object or as an array of JSON objects.";
	private static final String UNAUTHORIZED = "Unauthorized";
	private static final String UNAUTHORIZED_DESC = "The value of the authorization header is not valid. Make sure you use exactly the same secret key as was supplied to you when you created your Game Analytics account.";
	private static final String SIG_NOT_FOUND = "Signature not found in request";
	private static final String SIG_NOT_FOUND_DESC = "The \"Authorization\" header is missing. Make sure that you add a header with the \"Authorization\" key to your API call.";
	private static final String FORBIDDEN_DESC = "Make sure that the URL is valid and that it conforms to the specifications.";
	private static final String GAME_NOT_FOUND = "Game key not found";
	private static final String GAME_NOT_FOUND_DESC = "The game key in the URL does not match any existing games. Make sure that you are using the correct game key (the key which you received when creating your game on the GameAnalytics website).";
	private static final String METHOD_NOT_SUPPORTED = "Method not found";
	private static final String METHOD_NOT_SUPPORTED_DESC = "The URI used to post data to the server was incorrect. This could be because the game key supplied the GameAnalytics during initialise() was blank or null.";
	private static final String INTERNAL_SERVER_ERROR_DESC = "Internal server error. Please bring this error to Game Analytics attention. We are sorry for any inconvenience caused.";
	private static final String NOT_IMPLEMENTED_DESC = "The used HTTP method is not supported. Please only use the POST method for submitting data.";

	// DEBUGGING
	/**
	 * Used as the parameter for setDebugLogLevel(int level). VERBOSE logging
	 * will post to the debug log when every event is created and batched off to
	 * the server.
	 */
	public static final int VERBOSE = 0;
	/**
	 * Used as the parameter for setDebugLogLevel(int level). RELEASE logging
	 * will only post to the debug logs in the event of a warning or error.
	 */
	public static final int RELEASE = 1;
	protected static int LOGGING = RELEASE;

	// CATEGORIES
	protected static final String DESIGN = "design";
	protected static final String USER = "user";
	protected static final String QUALITY = "quality";
	protected static final String BUSINESS = "business";

	// APP/DEVELOPER SPECIFIC
	private static String GAME_KEY;
	private static String SECRET_KEY;
	private static String USER_ID;
	private static String SESSION_ID;
	private static String BUILD;
	private static String AREA;
	private static int SEND_EVENT_INTERVAL = 20000; // Default is 20 secs
	private static int NETWORK_POLL_INTERVAL = 60000; // Default is 60 secs
	private static int SESSION_TIME_OUT = 20000; // Default is 20 secs

	// PRECONFIGURED EVENTS
	private static final String FPS_EVENT_NAME = "FPS";
	private static final String USER_INFO_EVENT_NAME = "userInfo";
	private static final String REFERRAL_EVENT_NAME = "referral";
	private static final String ANDROID = "Android";
	private static final String SDK_VERSION = "GA Android SDK 1.10";

	// OTHER
	private static AsyncHttpClient CLIENT;
	private static BatchThread CURRENT_THREAD;
	private static EventDatabase EVENT_DATABASE;
	private static Context CONTEXT;
	private static boolean INITIALISED = false;
	private static boolean SESSION_STARTED = false;
	private static boolean CACHE_LOCALLY = true;
	private static boolean AUTO_BATCH = true;
	private static long SESSION_END_TIME;
	private static long START_FPS_TIME;
	private static int FPS_FRAMES;

	/**
	 * Initialise the GameAnalytics wrapper. It is recommended that you call
	 * this method from the entry activity of your application's onCreate()
	 * method. Uses the value of 'android:versionName' from the
	 * AndroidManifest.xml as the build version of the application by default.
	 * 
	 * @param context
	 *            the calling activity
	 * @param secretKey
	 *            secret key supplied when you registered at GameAnalytics
	 * @param gameKey
	 *            game key supplied when you registered at GameAnalytics
	 */
	public static void initialise(Context context, String secretKey,
			String gameKey) {
		// Get build version from AndroidManifest.xml
		String build;
		try {
			build = context.getPackageManager().getPackageInfo(
					context.getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
			GALog.w("Warning: android:versionName tag is not set correctly in Android Manifest.");
			build = "unknown";
		}
		// Pass on to full initialise method
		initialise(context, secretKey, gameKey, build);
	}

	/**
	 * Initialise the GameAnalytics wrapper. It is recommended that you call
	 * this method from the entry activity of your application's onCreate()
	 * method.
	 * 
	 * @param context
	 *            the calling activity
	 * @param secretKey
	 *            secret key supplied when you registered at GameAnalytics
	 * @param gameKey
	 *            game key supplied when you registered at GameAnalytics
	 * @param build
	 *            optional - leave out to use 'android:versionName' from
	 *            manifest file by default
	 */
	public static void initialise(Context context, String secretKey,
			String gameKey, String build) {
		// Get user id
		USER_ID = md5(Secure.getString(context.getContentResolver(),
				Secure.ANDROID_ID));
		// Set game and secret keys and build
		SECRET_KEY = secretKey;
		GAME_KEY = gameKey + "/";
		BUILD = build;

		// Initialise other variables
		CLIENT = new AsyncHttpClient();
		EVENT_DATABASE = new EventDatabase(context);

		// Set boolean initialised, newEvent() can only be called after
		// initialise() and startSession()
		INITIALISED = true;
	}

	/**
	 * Checks whether Game Analytics has been initialised.
	 * 
	 * @return true if initialised, otherwise false
	 */
	public static boolean isInitialised() {
		return INITIALISED;
	}

	/**
	 * Checks whether a Game Analytics session has been started.
	 * 
	 * @return true if session started, otherwise false
	 */
	public static boolean isSessionStarted() {
		return SESSION_STARTED;
	}

	/**
	 * Call this method in every activity's onResume() method to ensure correct
	 * session logging.
	 * 
	 * @param context
	 *            the calling activity
	 */
	public static void startSession(Context context) {
		// Update current context
		CONTEXT = context;
		AREA = context.getClass().toString();

		// Current time:
		long nowTime = System.currentTimeMillis();

		SESSION_STARTED = true;

		// Need to get a new sessionId?
		if (SESSION_ID == null
				|| (SESSION_END_TIME != 0 && nowTime > SESSION_END_TIME)) {
			// Set up unique session id
			SESSION_ID = getSessionId();
			GALog.i("Starting new session");

			// Send off model and OS version
			sendOffUserStats();
		}
	}

	/**
	 * Call this method in every activity's onPause() method to ensure correct
	 * session logging.
	 * 
	 * @param CONTEXT
	 *            the calling activity
	 */
	public static void stopSession() {
		// sessionTimeOut is some time after now
		SESSION_END_TIME = System.currentTimeMillis() + SESSION_TIME_OUT;
		SESSION_STARTED = false;
	}

	/**
	 * Add a new design event to the event stack. This will be sent off in a
	 * batched array after the time interval set using setTimeInterval().
	 * 
	 * @param eventId
	 *            use colons to denote subtypes, e.g. 'PickedUpAmmo:Shotgun'
	 * @param value
	 *            numeric value associated with event e.g. number of shells
	 * @param area
	 *            area/level associated with the event
	 * @param x
	 *            position on x-axis
	 * @param y
	 *            position on y-axis
	 * @param z
	 *            position on z-axis
	 */
	public static void newDesignEvent(String eventId, float value, String area,
			float x, float y, float z) {
		if (ready()) {
			GALog.i("New design event: " + eventId + ", value: " + value
					+ ", area: " + area + ", pos: (" + x + ", " + y + ", " + z
					+ ")");
			// Ensure we have a BatchThread ready to receive events
			startThreadIfReq();

			// Add design event to batch stack
			EVENT_DATABASE.addDesignEvent(USER_ID, SESSION_ID, BUILD, eventId,
					area, x, y, z, value);
		}
	}

	/**
	 * Add a new design event to the event stack. This will be sent off in a
	 * batched array after the time interval set using setTimeInterval(). The
	 * current activity will be used as the 'area' value for the event.
	 * 
	 * @param eventId
	 *            use colons to denote subtypes, e.g. 'PickedUpAmmo:Shotgun'
	 * @param value
	 *            numeric value associated with event e.g. number of shells
	 */
	public static void newDesignEvent(String eventId, float value) {
		newDesignEvent(eventId, value, AREA, 0, 0, 0);
	}

	/**
	 * Add a new quality event to the event stack. This will be sent off in a
	 * batched array after the time interval set using setTimeInterval().
	 * 
	 * @param eventId
	 *            use colons to denote subtypes, e.g.
	 *            'Exception:NullPointerException'
	 * @param message
	 *            message associated with event e.g. the stack trace
	 * @param area
	 *            area/level associated with the event
	 * @param x
	 *            position on x-axis
	 * @param y
	 *            position on y-axis
	 * @param z
	 *            position on z-axis
	 */
	public static void newQualityEvent(String eventId, String message,
			String area, float x, float y, float z) {
		if (ready()) {
			GALog.i("New quality event: " + eventId + ", message: " + message
					+ ", area: " + area + ", pos: (" + x + ", " + y + ", " + z
					+ ")");
			// Ensure we have a BatchThread ready to receive events
			startThreadIfReq();

			// Add quality event to batch stack
			EVENT_DATABASE.addQualityEvent(USER_ID, SESSION_ID, BUILD, eventId,
					area, x, y, z, message);
		}
	}

	/**
	 * Add a new quality event to the event stack. This will be sent off in a
	 * batched array after the time interval set using setTimeInterval(). The
	 * current activity will be used as the 'area' value for the event.
	 * 
	 * @param eventId
	 *            use colons to denote subtypes, e.g.
	 *            'Exception:NullPointerException'
	 * @param message
	 *            message associated with event e.g. the stack trace
	 */
	public static void newQualityEvent(String eventId, String message) {
		newQualityEvent(eventId, message, AREA, 0, 0, 0);
	}

	// This is the privately accessible method to send all user event data.
	// Developers should use the following publically available ones:
	// 1. setUserInfo(char gender, int birthYear, int friendCount)
	// 2. setReferralInfo(String installPublisher, String installSite, String
	// installCampaign, String installAd, String installKeyword)
	private static void newUserEvent(String eventId, char gender,
			int birthYear, int friendCount, String area, float x, float y,
			float z, String platform, String device, String osMajor,
			String osMinor, String sdkVersion, String installPublisher,
			String installSite, String installCampaign, String installAd,
			String installKeyword) {
		if (ready()) {
			GALog.i("New user event: " + eventId + ", gender: " + gender
					+ ", birthYear: " + birthYear + ", friendCount: "
					+ friendCount + ", area: " + area + ", pos: (" + x + ", "
					+ y + ", " + z + ", " + platform + ", " + device + ", "
					+ osMajor + ", " + osMinor + ", " + sdkVersion + ", "
					+ installPublisher + ", " + installSite + ", "
					+ installCampaign + ", " + installAd + ", "
					+ installKeyword + ")");
			// Ensure we have a BatchThread ready to receive events
			startThreadIfReq();

			// Add user event to batch stack
			EVENT_DATABASE.addUserEvent(USER_ID, SESSION_ID, BUILD, eventId,
					area, x, y, z, gender, birthYear, friendCount, platform,
					device, osMajor, osMinor, sdkVersion, installPublisher,
					installSite, installCampaign, installAd, installKeyword);
		}
	}

	/**
	 * Add a new user event to the event stack. This will be sent off in a
	 * batched array after the time interval set using setTimeInterval(). The
	 * current activity will be used as the 'area' value for the event.
	 * 
	 * @param eventId
	 *            use colons to denote subtypes
	 * @param gender
	 *            user gender, use 'm' for male, 'f' for female
	 * @param birthYear
	 *            four digit birth year
	 * @param friendCount
	 *            number of friends
	 * 
	 * @deprecated use {@link setUserInfo()} instead.
	 */
	public static void sendUserEvent(String eventId, char gender,
			int birthYear, int friendCount) {
		newUserEvent(eventId, gender, birthYear, friendCount, AREA, 0, 0, 0,
				null, null, null, null, null, null, null, null, null, null);
	}

	/**
	 * Add a new user event to the event stack. This will be sent off in a
	 * batched array after the time interval set using setTimeInterval(). The
	 * current activity will be used as the 'area' value for the event.
	 * 
	 * @param eventId
	 *            use colons to denote subtypes
	 * @param gender
	 *            user gender, use 'm' for male, 'f' for female
	 * @param birthYear
	 *            four digit birth year
	 * @param friendCount
	 *            number of friends
	 * @param area
	 *            area/level associated with the event
	 * @param x
	 *            position on x-axis
	 * @param y
	 *            position on y-axis
	 * @param z
	 *            position on z-axis
	 * 
	 * @deprecated use {@link setUserInfo()} instead.
	 */
	public static void sendUserEvent(String eventId, char gender,
			int birthYear, int friendCount, String area, float x, float y,
			float z) {
		newUserEvent(eventId, gender, birthYear, friendCount, area, x, y, z,
				null, null, null, null, null, null, null, null, null, null);
	}

	/**
	 * Send user info to the Game Analytics server.
	 * 
	 * @param gender
	 *            user gender, use 'm' for male, 'f' for female
	 * @param birthYear
	 *            four digit birth year
	 * @param friendCount
	 *            number of friends
	 */
	public static void setUserInfo(char gender, int birthYear, int friendCount) {
		newUserEvent(USER_INFO_EVENT_NAME, gender, birthYear, friendCount,
				AREA, 0, 0, 0, null, null, null, null, null, null, null, null,
				null, null);
	}

	/**
	 * Manually send referral info to the Game Analytics server.
	 * 
	 * For automatic referrals, extend ReferralReceiver class set up as
	 * broadcast receiver in Android Manifest.
	 * 
	 * @param installPublisher
	 *            e.g. FB, Chartboost, Google Adwords, Organic
	 * @param installSite
	 *            e.g. FB.com, FBApp, AppId
	 * @param installCampaign
	 *            e.g. Launch, EasterBoost, ChrismasSpecial
	 * @param installAd
	 *            e.g. Add#239823, KnutsShinyAd
	 * @param installKeyword
	 *            e.g. rts mobile game
	 */
	public static void setReferralInfo(String installPublisher,
			String installSite, String installCampaign, String installAd,
			String installKeyword) {
		newUserEvent(USER_INFO_EVENT_NAME, 'n', 0, 0, AREA, 0, 0, 0, null,
				null, null, null, null, installPublisher, installSite,
				installCampaign, installAd, installKeyword);
	}

	/**
	 * Add a new business event to the event stack. This will be sent off in a
	 * batched array after the time interval set using setTimeInterval().
	 * 
	 * @param eventId
	 *            use colons to denote subtypes, e.g. 'PurchaseWeapon:Shotgun'
	 * @param currency
	 *            3 digit code for currency e.g. 'USD'
	 * @param amount
	 *            value of transaction
	 * @param area
	 *            area/level associated with the event
	 * @param x
	 *            position on x-axis
	 * @param y
	 *            position on y-axis
	 * @param z
	 *            position on z-axis
	 */
	public static void newBusinessEvent(String eventId, String currency,
			int amount, String area, float x, float y, float z) {
		if (ready()) {
			GALog.i("New business event: " + eventId + ", currency: "
					+ currency + ", amount: " + amount + ", area: " + area
					+ ", pos: (" + x + ", " + y + ", " + z + ")");
			// Ensure we have a BatchThread ready to receive events
			startThreadIfReq();

			// Add business event to batch stack
			EVENT_DATABASE.addBusinessEvent(USER_ID, SESSION_ID, BUILD,
					eventId, area, x, y, z, currency, amount);

		}
	}

	/**
	 * Add a new business event to the event stack. This will be sent off in a
	 * batched array after the time interval set using setTimeInterval(). The
	 * current activity will be used as the 'area' value for the event.
	 * 
	 * @param eventId
	 *            use colons to denote subtypes, e.g. 'PurchaseWeapon:Shotgun'
	 * @param currency
	 *            3 digit code for currency e.g. 'USD'
	 * @param amount
	 *            value of transaction
	 */
	public static void newBusinessEvent(String eventId, String currency,
			int amount) {
		newBusinessEvent(eventId, currency, amount, AREA, 0, 0, 0);
	}

	/**
	 * Set the amount of time, in milliseconds, between each batch of events
	 * being sent. The default is 20 seconds.
	 * 
	 * @param millis
	 *            interval in milliseconds
	 */
	public static void setSendEventsInterval(int millis) {
		SEND_EVENT_INTERVAL = millis;
	}

	/**
	 * If a network is not available GameAnalytics will poll the connection and
	 * send the events once it is restored. Set the amount of time, in
	 * milliseconds, between polls. The default is 60 seconds.
	 * 
	 * @param millis
	 *            interval in milliseconds
	 */
	public static void setNetworkPollInterval(int millis) {
		NETWORK_POLL_INTERVAL = millis;
	}

	/**
	 * Set the amount of time, in milliseconds, for a session to timeout so that
	 * a new one is started when the application is restarted. being sent. The
	 * default is 20 seconds.
	 * 
	 * @param millis
	 *            interval in milliseconds
	 */
	public static void setSessionTimeOut(int millis) {
		SESSION_TIME_OUT = millis;
	}

	/**
	 * Place somewhere inside your draw loop to log FPS. If you are using openGL
	 * then that will be inside your Renderer class' onDrawFrame() method. You
	 * must then call stopLoggingFPS() at some point to collate the data and
	 * send it to GameAnalytics. You can either do this intermittently e.g.
	 * every 1000 frames, or over an entire gameplay session e.g. in the
	 * activity's onPause() method. Either way, the average FPS will be logged.
	 */
	public static void logFPS() {
		// Have we already started logging FPS?
		if (START_FPS_TIME == 0) {
			// No, start logging
			START_FPS_TIME = System.currentTimeMillis();
		} else {
			// Increment number of frames
			FPS_FRAMES++;
		}
	}

	/**
	 * Call this method when you want to collate you FPS and send it to the
	 * server. You can either do this intermittently e.g. every 1000 frames, or
	 * over an entire gameplay session e.g. in the activity's onPause() method.
	 * Either way, the average FPS will be logged. The parameters are optional.
	 * If left out, the name of the current activity will be logged as the area
	 * parameter.
	 * 
	 * @param area
	 *            (optional - use to log the FPS in a specific level/area)
	 * @param x
	 *            (optional)
	 * @param y
	 *            (optional)
	 * @param z
	 *            (optional)
	 */
	public static void stopLoggingFPS(String area, float x, float y, float z) {
		if (ready()) {
			// Ensure we are logging FPS?
			if (START_FPS_TIME != 0) {
				// Get elapsed time
				long elapsed = System.currentTimeMillis() - START_FPS_TIME;

				if (elapsed != 0) {
					// Work out average FPS and send
					int fps = (int) (FPS_FRAMES * 1000 / elapsed);
					newDesignEvent(FPS_EVENT_NAME, fps, area, x, y, z);
					START_FPS_TIME = 0;
				} else {
					GALog.w("Warning: No time elapsed between starting and stopping FPS logging.");
				}
			} else {
				GALog.w("Warning: stopLoggingFPS() was called before startLoggingFPS().");
			}
		}
	}

	/**
	 * Call this method when you want to collate you FPS and send it to the
	 * server. You can either do this intermittently e.g. every 1000 frames, or
	 * over an entire gameplay session e.g. in the activity's onPause() method.
	 * Either way, the average FPS will be logged. Area, x, y and z parameters
	 * can be added. If omitted, the current activity will be logged as the area
	 * parameter.
	 */
	public static void stopLoggingFPS() {
		stopLoggingFPS(AREA, 0, 0, 0);
	}

	/**
	 * Call this method at the same time as initialise() to automatically log
	 * any unhandled exceptions.
	 */
	public static void logUnhandledExceptions() {
		Thread.currentThread().setUncaughtExceptionHandler(
				new ExceptionLogger());
	}

	/**
	 * Set a custom userId string to be attached to all subsequent events. By
	 * default, the user ID is generated from the unique Android device ID.
	 * 
	 * @param userId
	 *            Custom unique user ID
	 */
	public static void setUserId(String userId) {
		USER_ID = userId;
	}

	/**
	 * Set debug log level. Use GameAnalytics.VERBOSE while you are developing
	 * to see when every event is created and batched to server. Set to
	 * GameAnalytics.RELEASE (default) when you release your application so only
	 * warning and event logs are made.
	 * 
	 * @param level
	 *            Set to either GameAnalytics.VERBOSE or GameAnalytics.RELEASE
	 */
	public static void setDebugLogLevel(int level) {
		if (level == VERBOSE || level == RELEASE) {
			LOGGING = level;
		} else {
			GALog.w("Warning: You should pass in GameAnalytics.VERBOSE or GameAnalytics.RELEASE into GameAnalytics.setLoggingLevel()");
		}
	}

	/**
	 * Enable/disable local caching. By default (true) events are cached locally
	 * so that even if an internet connection is not available, they will be
	 * sent to the GA server when it is restored. If disabled (false) events
	 * will be discarded if a connection is unavailable.
	 * 
	 * @param value
	 *            true = enabled; false = disabled
	 */
	public static void setLocalCaching(boolean value) {
		CACHE_LOCALLY = value;
	}

	/**
	 * Enable/disable automatic batching. By default (true) events are sent off
	 * to the GA server after a time interval set using setSendEventsInterval().
	 * If disabled (false) then you will need to use manualBatch() to send the
	 * events to the server.
	 * 
	 * @param value
	 *            true = enabled; false = disabled
	 */
	public static void setAutoBatch(boolean value) {
		AUTO_BATCH = false;
	}

	/**
	 * Set maximum number of events that are stored locally. Additional events
	 * will be discarded. Set to 0 for unlimited (default).
	 * 
	 * @param max
	 *            maximum number of events that can be stored
	 */
	public static void setMaximumEventStorage(int max) {
		EVENT_DATABASE.setMaximumEventStorage(max);
	}

	/**
	 * 	Create a special BatchThread just to send events. This event will not
	 *	wait for the sendEventInterval nor will it poll the internet
	 *	connection. If there is no connection it will simply return.
	 */
	public static void manualBatch() {
		BatchThread sendEventThread = new BatchThread(CONTEXT, CLIENT,
				EVENT_DATABASE, GAME_KEY, SECRET_KEY, SEND_EVENT_INTERVAL,
				NETWORK_POLL_INTERVAL, CACHE_LOCALLY);
		sendEventThread.manualBatch();
	}

	private static boolean ready() {
		if (INITIALISED) {
			if (SESSION_STARTED) {
				return true;
			} else {
				GALog.w("Warning: GameAnalytics session has not started. Call GameAnalytics.startSession(Context context) in onResume().");
			}
		} else {
			GALog.w("Warning: GameAnalytics has not been initialised. Call GameAnalytics.initialise(Context context, String secretKey, String gameKey) first");
		}
		return false;
	}

	// Generates session id from md5 hash of current time
	private static String getSessionId() {
		Time time = new Time();
		time.setToNow();
		return md5(USER_ID + time.toString());
	}

	// Generates MD5 hash string from String, returns null on error
	protected static String md5(String s) {
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("MD5");
			digest.update(s.getBytes("ISO-8859-1"), 0, s.length());
			byte[] byteArray = digest.digest();
			/*
			 * Convert byte array to hex string: .add(...) adds one to the
			 * beginning of the BigInteger representation of our byte array.
			 * Then .substring(1) removes it. This stops leading zeros being
			 * dropped when converting from BigInteger to hex string.
			 */
			String hash = (new BigInteger(1, byteArray).add(BigInteger.ONE
					.shiftLeft(8 * byteArray.length))).toString(16)
					.substring(1);
			return hash;
		} catch (NoSuchAlgorithmException e) {
			GALog.e("NoSuchAlgorithmException when making authorization hash.",
					e);
			return null;
		} catch (UnsupportedEncodingException e) {
			GALog.e("UnsupportedEncodingException when making authorization hash.",
					e);
			return null;
		}
	}

	private static void startThreadIfReq() {
		// Only start new thread if current thread is null or already sending
		// events
		// AND auto-batch is switched on.
		if ((CURRENT_THREAD == null || CURRENT_THREAD.shouldStartNewThread())
				&& AUTO_BATCH) {
			CURRENT_THREAD = new BatchThread(CONTEXT, CLIENT, EVENT_DATABASE,
					GAME_KEY, SECRET_KEY, SEND_EVENT_INTERVAL,
					NETWORK_POLL_INTERVAL, CACHE_LOCALLY);
			CURRENT_THREAD.start();
		}
	}

	private static void sendOffUserStats() {
		// For developer
		int memory = Math.round(Runtime.getRuntime().maxMemory() / 1000000);
		newQualityEvent("Model:" + android.os.Build.MODEL, "Max memory = "
				+ memory + " mb");
		newQualityEvent("AndroidVersion:" + android.os.Build.VERSION.RELEASE,
				"");

		// For GA
		newUserEvent(USER_INFO_EVENT_NAME, 'n', 0, 0, AREA, 0, 0, 0, ANDROID,
				android.os.Build.MODEL, null, android.os.Build.VERSION.RELEASE,
				SDK_VERSION, null, null, null, null, null);
	}

	// CALL BACK INTERFACE
	protected final static AsyncHttpResponseHandler postResponseHandler = new AsyncHttpResponseHandler() {
		@Override
		public void onStart() {
			GALog.i("onStart");
		}

		@Override
		public void onFinish() {
			GALog.i("onFinish");
		}

		@Override
		public void onSuccess(int statusCode, String content) {
			// Print response to log
			GALog.i("Succesful response: " + content);
		}

		@Override
		public void onFailure(Throwable error, String content) {
			// Try convert error content into JSON
			Gson gson = new Gson();
			ErrorResponse errorResponse = null;
			try {
				errorResponse = gson.fromJson(content, ErrorResponse.class);
			} catch (Exception e) {
				GALog.e("Error converting failure response from json: "
						+ content, e);
			}

			if (errorResponse != null) {
				// Give advice based on error code
				String errorDescription = null;
				switch (errorResponse.code) {
				case 400:
					if (errorResponse.message.equals(BAD_REQUEST)) {
						errorDescription = BAD_REQUEST_DESC;
					} else if (errorResponse.message.equals(NO_GAME)) {
						errorDescription = NO_GAME_DESC;
					} else if (errorResponse.message.equals(DATA_NOT_FOUND)) {
						errorDescription = DATA_NOT_FOUND_DESC;
					}
					break;
				case 401:
					if (errorResponse.message.equals(UNAUTHORIZED)) {
						errorDescription = UNAUTHORIZED_DESC;
					} else if (errorResponse.message.equals(SIG_NOT_FOUND)) {
						errorDescription = SIG_NOT_FOUND_DESC;
					}
					break;
				case 403:
					errorDescription = FORBIDDEN_DESC;
					break;
				case 404:
					if (errorResponse.message.equals(GAME_NOT_FOUND)) {
						errorDescription = GAME_NOT_FOUND_DESC;
					} else if (errorResponse.message
							.equals(METHOD_NOT_SUPPORTED)) {
						errorDescription = METHOD_NOT_SUPPORTED_DESC;
					}
					break;
				case 500:
					errorDescription = INTERNAL_SERVER_ERROR_DESC;
					break;
				case 501:
					errorDescription = NOT_IMPLEMENTED_DESC;
					break;
				}
				if (errorDescription != null) {
					GALog.e("Error response code: " + errorResponse.code
							+ System.getProperty("line.separator")
							+ errorDescription);
				} else {
					GALog.i("Code: " + errorResponse.code);
					GALog.i("Message: " + errorResponse.message);
					GALog.e("Unrecognised response code: " + error.toString(),
							error);
				}
			} else {
				GALog.e("Error: " + error.toString(), error);
			}
		}
	};
}