package com.twicecircled.gameanalytics.sdk;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.http.Header;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.provider.Settings.Secure;
import android.text.format.Time;
import android.util.Log;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

public class GameAnalytics {

	// WEBSERVER
	protected static final String API_URL = "http://api.gameanalytics.com/1/";
	protected static final String AUTHORIZATION = "Authorization";
	protected static final String CONTENT_TYPE = "Content-Type";
	protected static final String CONTENT_TYPE_JSON = "application/json";

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
	private static int SEND_EVENT_INTERVAL = 20000; // Default is 20 secs
	private static int NETWORK_POLL_INTERVAL = 20000; // Default is 20 secs

	// OTHER
	private static AsyncHttpClient client;
	private static BatchThread currentThread;
	private static EventDatabase eventDatabase;
	private static Context context;
	private static boolean initialised = false;
	private static Time sessionEndTime;
	private static int sessionTimeOut = 10000; // Default is 10 secs

	// TODO: Make a set of new event methods which use activity class name as
	// 'area'

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
			Log.w("GameAnalytics",
					"Warning: android:versionName tag is not set correctly in Android Manifest.");
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
		USER_ID = Secure.getString(context.getContentResolver(),
				Secure.ANDROID_ID);
		// Set game and secret keys and build
		SECRET_KEY = secretKey;
		GAME_KEY = gameKey + "/";
		BUILD = build;

		// Initialise other variables
		client = new AsyncHttpClient();
		sessionEndTime = new Time();
		eventDatabase = new EventDatabase(context);

		// Set boolean initialised, newEvent() can only be called after
		// initialise() and startSession()
		initialised = true;
	}

	/**
	 * Call this method in every activity's onResume() method to ensure correct
	 * session logging.
	 * 
	 * @param context
	 *            the calling activity
	 */
	public static void startSession(Context context) {
		// Current time:
		Time now = new Time();
		now.setToNow();

		// Need to get a new sessionId?
		if (SESSION_ID == null || now.after(sessionEndTime)) {
			// Set up unique session id
			SESSION_ID = getSessionId();
			Log.d("GameAnalytics", "Starting new session");
		}

		// Update current context
		GameAnalytics.context = context;
	}

	/**
	 * Call this method in every activity's onPause() method to ensure correct
	 * session logging.
	 * 
	 * @param context
	 *            the calling activity
	 */
	public static void stopSession() {
		// sessionTimeOut is some time after now
		sessionEndTime.setToNow();
		sessionEndTime.set(sessionEndTime.toMillis(false) + sessionTimeOut);
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
		if (initialised) {
			Log.d("GameAnalytics", "New design event: " + eventId + ", value: "
					+ value + ", area: " + area + ", pos: (" + x + ", " + y
					+ ", " + z + ")");
			// Ensure we have a BatchThread ready to receive events
			startThreadIfReq();

			// Add design event to batch stack
			eventDatabase.addDesignEvent(USER_ID, SESSION_ID, BUILD, eventId,
					area, x, y, z, value);
		} else {
			Log.w("GameAnalytics",
					"Warning: GameAnalytics has not been initialised. Call GameAnalytics.initialise(Context context, String secretKey, String gameKey) first");
		}
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
		if (initialised) {
			Log.d("GameAnalytics", "New quality event: " + eventId
					+ ", value: " + message + ", area: " + area + ", pos: ("
					+ x + ", " + y + ", " + z + ")");
			// Ensure we have a BatchThread ready to receive events
			startThreadIfReq();

			// Add quality event to batch stack
			eventDatabase.addQualityEvent(USER_ID, SESSION_ID, BUILD, eventId,
					area, x, y, z, message);
		} else {
			Log.w("GameAnalytics",
					"Warning: GameAnalytics has not been initialised. Call GameAnalytics.initialise(Context context, String secretKey, String gameKey) first");
		}
	}

	/**
	 * Add a new quality event to the event stack. This will be sent off in a
	 * batched array after the time interval set using setTimeInterval().
	 * 
	 * @param eventId
	 *            use colons to denote subtypes, e.g. 'PlaytimeMilestone:5hours'
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
	 */
	public static void newUserEvent(String eventId, char gender, int birthYear,
			int friendCount, String area, float x, float y, float z) {
		if (initialised) {
			Log.d("GameAnalytics", "New user event: " + eventId + ", gender: "
					+ gender + ", birthYear: " + birthYear + ", friendCount: "
					+ friendCount + ", area: " + area + ", pos: (" + x + ", "
					+ y + ", " + z + ")");
			// Ensure we have a BatchThread ready to receive events
			startThreadIfReq();

			// Add user event to batch stack
			eventDatabase.addUserEvent(USER_ID, SESSION_ID, BUILD, eventId,
					area, x, y, z, gender, birthYear, friendCount);
		} else {
			Log.w("GameAnalytics",
					"Warning: GameAnalytics has not been initialised. Call GameAnalytics.initialise(Context context, String secretKey, String gameKey) first");
		}
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
		if (initialised) {
			Log.d("GameAnalytics", "New business event: " + eventId
					+ ", currency: " + currency + ", amount: " + amount
					+ ", area: " + area + ", pos: (" + x + ", " + y + ", " + z
					+ ")");
			// Ensure we have a BatchThread ready to receive events
			startThreadIfReq();

			// Add business event to batch stack
			eventDatabase.addBusinessEvent(USER_ID, SESSION_ID, BUILD, eventId,
					area, x, y, z, currency, amount);
		} else {
			Log.w("GameAnalytics",
					"Warning: GameAnalytics has not been initialised. Call GameAnalytics.initialise(Context context, String secretKey, String gameKey) first");
		}
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
			digest.update(s.getBytes(), 0, s.length());
			String hash = new BigInteger(1, digest.digest()).toString(16);
			return hash;
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}
	}

	private static void startThreadIfReq() {
		if (currentThread == null || currentThread.isSendingEvents()) {
			currentThread = new BatchThread(context, client, eventDatabase,
					GAME_KEY, SECRET_KEY, SEND_EVENT_INTERVAL,
					NETWORK_POLL_INTERVAL);
			currentThread.start();
		}
	}

	// CALL BACK INTERFACE
	protected final static AsyncHttpResponseHandler postResponseHandler = new AsyncHttpResponseHandler() {
		@Override
		public void onStart() {
			Log.d("GameAnalytics", "onStart");
		}

		@Override
		public void onFinish() {
			Log.d("GameAnalytics", "onFinish");
		}

		@Override
		public void onSuccess(int statusCode, String content, Header[] headers) {
			// Print response to log
			Log.d("GameAnalytics", "Succesful response: " + content);
		}

		@Override
		public void onFailure(Throwable error, String content, Header[] headers) {
			// Print response to log
			Log.e("GameAnalytics", "Error: " + error.toString(), error);
			Log.e("GameAnalytics", "Failure response: " + content);

			// http://support.gameanalytics.com/entries/22616366-Error-messages
			// TODO: Maybe write custom messages in warnings to give tips how to
			// fix errors.
		}
	};
}
