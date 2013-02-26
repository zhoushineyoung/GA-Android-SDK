package com.twicecircled.gameanalytics.example;

import com.twicecircled.gameanalytics.sdk.ExceptionLogger;
import com.twicecircled.gameanalytics.sdk.GameAnalytics;
import com.twicecircled.gameanalyticssdk.R;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.View;
import android.widget.SeekBar;

public class MainActivity extends Activity {

	private final String GAME_KEY = "59ac59a0afeaf44c424b24974b3f21c2";
	private final String SECRET_KEY = "45bce17e069a51169146371bef627d45a2d2905e";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Set UncaughtExceptionManager - move to static method inside
		// gameAnalytics
		Thread.currentThread().setUncaughtExceptionHandler(
				new ExceptionLogger());

		// Set display
		setContentView(R.layout.activity_main);

		// Set-up game analytics
		GameAnalytics.initialise(this, SECRET_KEY, GAME_KEY);
	}

	public void onDesign(View v) {
		SeekBar seekBar = (SeekBar) findViewById(R.id.seekBar);
		GameAnalytics.newDesignEvent("test", seekBar.getProgress(),
				"on startup", 0f, 5f, 0f);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

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
