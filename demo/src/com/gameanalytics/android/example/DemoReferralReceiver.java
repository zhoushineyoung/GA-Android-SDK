package com.gameanalytics.android.example;

import com.gameanalytics.android.GameAnalytics;
import com.gameanalytics.android.ReferralReceiver;

public class DemoReferralReceiver extends ReferralReceiver {

	@Override
	public String getSecretKey() {
		return MainActivity.SECRET_KEY;
	}

	@Override
	public String getGameKey() {
		return MainActivity.GAME_KEY;
	}

	@Override
	public int getDebugMode() {
		return GameAnalytics.VERBOSE;
	}

}
