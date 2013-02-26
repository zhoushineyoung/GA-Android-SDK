package com.twicecircled.gameanalytics.sdk;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import org.apache.http.Header;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import com.google.gson.Gson;
import com.loopj.android.http.AsyncHttpClient;

public class BatchThread extends Thread {

	// BATCH THREAD
	// A new batch thread is started when one does not already exist and a new
	// event has been created. It holds ArrayLists of events which it sends off
	// when the following two conditions have been met:
	// - Specific time interval passed
	// - Data connection available
	//
	// Once a BatchThread has started sending events, no more events can be
	// added. isSendingEvents() will return true so new events should be added
	// to a new BatchThread.

	// Once a BatchThread has started it is capable of running independently
	// from the application. Hence all non-final static fields are passed into
	// the BatchThread in its constructor:
	private String gameKey;
	private String secretKey;
	private int sendEventInterval;
	private int networkPollInterval;
	private Context context;
	private AsyncHttpClient client;
	private EventDatabase eventDatabase;

	// Once this is set to true, a new BatchThread is created so no more events
	// are sent to this one.
	private boolean sendingEvents = false;

	public BatchThread(Context context, AsyncHttpClient client,
			EventDatabase eventDatabase, String gameKey, String secretKey,
			int sendEventInterval, int networkPollInterval) {
		super();
		this.context = context;
		this.client = client;
		this.eventDatabase = eventDatabase;
		this.gameKey = gameKey;
		this.secretKey = secretKey;
		this.sendEventInterval = sendEventInterval;
		this.networkPollInterval = networkPollInterval;
	}

	@Override
	public void run() {
		Log.d("GameAnalytics", "BatchThread started");
		// Before we send off the events two conditions have to be met:
		// - Specific time interval passed
		// - Data connection available

		// First wait for time interval
		long endTime = System.currentTimeMillis() + sendEventInterval;
		while (System.currentTimeMillis() < endTime) {
			try {
				sleep(endTime - System.currentTimeMillis());
			} catch (Exception e) {
				Log.e("GameAnalytics", "Error: " + e.getMessage(), e);
			}
		}
		Log.d("GameAnalytics", "Time interval passed");

		// Do we have data connection?
		while (!isNetworkConnected()) {
			// Intermittently poll until network is reconnected
			Log.d("GameAnalytics", "Polling network...");
			try {
				sleep(networkPollInterval);
			} catch (Exception e) {
				Log.e("GameAnalytics", "Error: " + e.getMessage(), e);
			}
		}

		Log.d("GameAnalytics", "Network is connected, sending events");
		sendingEvents = true;
		sendEvents();
	}

	@SuppressWarnings("unchecked")
	private void sendEvents() {
		// Get events from database
		ArrayList<?>[] eventLists = eventDatabase.getEvents();
		ArrayList<DesignEvent> designEvents = (ArrayList<DesignEvent>) eventLists[0];
		ArrayList<BusinessEvent> businessEvents = (ArrayList<BusinessEvent>) eventLists[1];
		ArrayList<UserEvent> userEvents = (ArrayList<UserEvent>) eventLists[2];
		ArrayList<QualityEvent> qualityEvents = (ArrayList<QualityEvent>) eventLists[3];

		// Convert event lists to json using GSON
		Gson gson = new Gson();
		// Send events if the list is not empty
		if (!designEvents.isEmpty()) {
			sendEventSet(gson.toJson(designEvents), GameAnalytics.DESIGN);
			Log.d("GameAnalytics", "Sending " + designEvents.size()
					+ " design events.");
		} else
			Log.d("GameAnalytics", "No design events to send.");

		if (!businessEvents.isEmpty()) {
			sendEventSet(gson.toJson(businessEvents), GameAnalytics.BUSINESS);
			Log.d("GameAnalytics", "Sending " + businessEvents.size()
					+ " business events.");
		} else
			Log.d("GameAnalytics", "No business events to send.");

		if (!qualityEvents.isEmpty()) {
			sendEventSet(gson.toJson(qualityEvents), GameAnalytics.QUALITY);
			Log.d("GameAnalytics", "Sending " + qualityEvents.size()
					+ " quality events.");
		} else
			Log.d("GameAnalytics", "No quality events to send.");

		if (!userEvents.isEmpty()) {
			sendEventSet(gson.toJson(userEvents), GameAnalytics.USER);
			Log.d("GameAnalytics", "Sending " + userEvents.size()
					+ " user events.");
		} else
			Log.d("GameAnalytics", "No user events to send.");
	}

	private void sendEventSet(String json, String category) {
		// Add auth header
		Header[] headers = new Header[1];
		headers[0] = new BasicHeader(GameAnalytics.AUTHORIZATION,
				getAuthorizationString(json));

		// POST request to server
		StringEntity jsonEntity = null;
		try {
			jsonEntity = new StringEntity(json);
		} catch (UnsupportedEncodingException e) {
			Log.e("GameAnalytics",
					"Error converting json String into StringEntity: "
							+ e.toString(), e);
		}
		client.post(context, GameAnalytics.API_URL + gameKey + category,
				jsonEntity, GameAnalytics.CONTENT_TYPE_JSON, headers,
				GameAnalytics.postResponseHandler);
	}

	private String getAuthorizationString(String json) {
		return GameAnalytics.md5(json + secretKey);
	}

	public boolean isSendingEvents() {
		return sendingEvents;
	}

	private boolean isNetworkConnected() {
		ConnectivityManager connectivityManager = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
		return (networkInfo != null && networkInfo.isConnected());
	}
}
