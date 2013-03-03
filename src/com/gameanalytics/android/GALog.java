package com.gameanalytics.android;

import android.util.Log;

public class GALog {
	// Logging class for GameAnalytics: allows user to set the logging level for
	// GameAnalytics

	private static final String TAG = "GameAnalytics";

	protected static void i(String message) {
		if (GameAnalytics.LOGGING == GameAnalytics.VERBOSE)
			Log.i(TAG, message);
	}

	protected static void w(String message) {
		Log.w(TAG, message);
	}

	protected static void e(String message) {
		Log.e(TAG, message);
	}

	protected static void e(String message, Throwable e) {
		Log.e(TAG, message, e);
	}

}
