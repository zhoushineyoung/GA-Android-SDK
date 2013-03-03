package com.gameanalytics.android.example;

import android.app.Activity;
import com.gameanalytics.android.GameAnalytics;

public abstract class GameAnalyticsActivity extends Activity {

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
