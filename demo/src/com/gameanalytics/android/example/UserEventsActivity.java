package com.gameanalytics.android.example;

import com.gameanalytics.android.GameAnalytics;
import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

/*	EVENTS
 *  This activity sends user events...
 */

public class UserEventsActivity extends GameAnalyticsActivity {

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Set display
		setContentView(R.layout.activity_user);

		// Configure DatePicker
		DatePicker picker = (DatePicker) findViewById(R.id.datePicker);
		picker.updateDate(1988, 0, 1);
		int sdk = android.os.Build.VERSION.SDK_INT;
		if (sdk >= android.os.Build.VERSION_CODES.HONEYCOMB) {
			picker.setCalendarViewShown(false);
		}
	}

	public void onSend(View v) {
		// GENDER
		int maleFemale = ((RadioGroup) findViewById(R.id.maleFemaleRadioGroup))
				.getCheckedRadioButtonId();
		char gender;

		switch (maleFemale) {
		case R.id.male:
			gender = 'm';
			break;
		case R.id.female:
			gender = 'f';
			break;
		default:
			Toast.makeText(this, "Please choose a gender.", Toast.LENGTH_SHORT)
					.show();
			return;
		}

		// BIRTHYEAR
		DatePicker picker = (DatePicker) findViewById(R.id.datePicker);
		int birthYear = picker.getYear();

		// FRIEND COUNT
		EditText friendText = (EditText) findViewById(R.id.friendText);
		String friendTextString = friendText.getText().toString();
		int friendCount;
		try {
			friendCount = Integer.valueOf(friendTextString);
		} catch (Exception e) {
			Toast.makeText(
					this,
					"Enter the number of friends you have in digits in the box indicated.",
					Toast.LENGTH_SHORT).show();
			return;
		}

		// Log user event
		/** DEPRECATED: **/
		// GameAnalytics.newUserEvent("Submit", gender, birthYear, friendCount);
		GameAnalytics.setUserInfo(gender, birthYear, friendCount);

		// Go back
		finish();

	}
}
