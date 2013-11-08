package com.gameanalytics.android.example;

import com.gameanalytics.android.GameAnalytics;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.View;

public class MainActivity extends Activity {

	// Game key and secret key supplied when registering at
	// www.gameanalytics.com
	protected final static String GAME_KEY = "xxx";
	protected final static String SECRET_KEY = "xxx";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Set display
		setContentView(R.layout.activity_main);

		// Set logging level to verbose so we can see when events are being
		// fired off. NB Do this before initialising to ensure you see all logs
		GameAnalytics.setDebugLogLevel(GameAnalytics.VERBOSE);

		// Set-up game analytics
		GameAnalytics.initialise(this, SECRET_KEY, GAME_KEY);

		// Turn on automatic logging of unhandled exceptions for main/GUI thread
		GameAnalytics.logUnhandledExceptions();

		// Set up non-default intervals (optional)
		GameAnalytics.setNetworkPollInterval(30000);
		GameAnalytics.setSendEventsInterval(10000);
		GameAnalytics.setSessionTimeOut(2000);
	}

	public void onDesign(View v) {
		startActivity(new Intent(this, DesignEventsActivity.class));
	}

	public void onUser(View v) {
		startActivity(new Intent(this, UserEventsActivity.class));
	}

	public void onBusiness(View v) {
		startActivity(new Intent(this, BusinessEventsActivity.class));
	}

	public void onError(View v) {
		// Force an error to demonstrate automatic exception handling
		Log.d("GameAnalyticsDemo",
				"Forcing a java.lang.arithmetic exception...");
		@SuppressWarnings("unused")
		int x = 1 / 0;
	}

	public void onReferral(View v) {
		// All referral terms are optional, use null to leave out
		GameAnalytics.setReferralInfo("Game Analytics Test", "Demo Homepage",
				null, null, "Download button", null);
	}

	/** SESSIONING INFO **/
	// GameAnalytics.startSession() and stopSession() must be called in every
	// activity's onResume() and onPause() method respectively.

	// This can be done explicitly as below or by extending a parent class that
	// automatically calls these methods, see GameAnalyticsActivity class.
	@Override
	public void onResume() {
		super.onResume();
		GameAnalytics.startSession(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		GameAnalytics.stopSession();
	}
}
