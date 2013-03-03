package com.gameanalytics.android.example;

import com.gameanalytics.android.GameAnalytics;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

/*	EVENTS
 *  This activity sends business events each time a user makes a purchase.
 *  The amounts are collated so on your dashboard you can get useful data 
 *  about which users are spending and what they are spending it on.
 */

public class BusinessEventsActivity extends GameAnalyticsActivity {

	// ITEMS
	// Remember to use a semicolon in the eventId to denote subtypes
	private static final String BUY = "Buy:";
	private static final String CLAWS = "Claws";
	private static final String WAND = "Wand";
	private static final String INSURANCE = "Insurance";

	// CURRENCIES
	private static final String GBP = "GBP"; // British Sterling Pounds

	private int moneyRemaining = 5000;
	private boolean tenNotSaid = true;
	private boolean hunNotSaid = true;
	private boolean fiveNotSaid = true;
	private boolean thouNotSaid = true;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Set display
		setContentView(R.layout.activity_business);

	}

	public void buyClaws(View v) {
		// Log event business event!
		GameAnalytics.newBusinessEvent(BUY + CLAWS, GBP, 69);
		moneyRemaining -= 69;
		updateMoney();
	}

	public void buyWand(View v) {
		// Log event business event!
		GameAnalytics.newBusinessEvent(BUY + WAND, GBP, 169);
		moneyRemaining -= 169;
		updateMoney();
	}

	public void buyInsurance(View v) {
		// Log event business event!
		GameAnalytics.newBusinessEvent(BUY + INSURANCE, GBP, 1299);
		moneyRemaining -= 1299;
		updateMoney();
	}

	public void updateMoney() {
		// Update money
		TextView money = (TextView) findViewById(R.id.money);
		int pounds = moneyRemaining / 100;
		int pence = Math.abs(moneyRemaining % 100);
		money.setText("£" + pounds + "." + pence);

		// Check for negative
		if (pounds < 0) {
			money.setTextColor(Color.RED);
			if (pounds < -1000 && thouNotSaid) {
				Toast.makeText(this,
						"Wow, persistent... sorry I've run out of jokes.",
						Toast.LENGTH_SHORT).show();
				thouNotSaid = false;
			} else if (pounds < -500 && fiveNotSaid) {
				Toast.makeText(this,
						"The bailiffs are on their way to your home!",
						Toast.LENGTH_SHORT).show();
				fiveNotSaid = false;
			} else if (pounds < -100 && hunNotSaid) {
				Toast.makeText(this,
						"Warning you are getting into serious debt!",
						Toast.LENGTH_SHORT).show();
				hunNotSaid = false;
			} else if (pounds < -10 && tenNotSaid) {
				Toast.makeText(
						this,
						"I should have cut off your credit limit a while ago huh?",
						Toast.LENGTH_SHORT).show();
				tenNotSaid = false;

			}
		}
	}
}
