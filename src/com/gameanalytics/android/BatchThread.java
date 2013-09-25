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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.http.Header;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import com.google.gson.Gson;
import com.loopj.twicecircled.android.http.AsyncHttpClient;

public class BatchThread extends Thread {

	// BATCH THREAD
	// A new batch thread is started when one does not already exist and a new
	// event has been created. After the following two conditions have been met:
	// - Specific time interval passed
	// - Data connection available
	//
	// ... the BatchThread locks the database (so no more events can be added)
	// until it has grabbed all the current events and wiped the database.
	// Next it batches them into JSON arrays and sends them to the GameAnalytics
	// server.
	//
	// When another event is logged a new BatchThread is started and the process
	// is repeated.

	// Once a BatchThread has started it is capable of running independently
	// from the application. Hence all non-final static fields are passed into
	// the BatchThread in its constructor:
	private String defaultGameKey;
	private String secretKey;
	private int sendEventInterval;
	private int networkPollInterval;
	private Context context;
	private AsyncHttpClient client;
	private EventDatabase eventDatabase;
	private boolean cacheLocally;
	private boolean pollNetwork = true;

	protected BatchThread(Context context, AsyncHttpClient client,
			EventDatabase eventDatabase, String gameKey, String secretKey,
			int sendEventInterval, int networkPollInterval, boolean cacheLocally) {
		super();
		this.context = context;
		this.client = client;
		this.eventDatabase = eventDatabase;
		this.defaultGameKey = gameKey;
		this.secretKey = secretKey;
		this.sendEventInterval = sendEventInterval;
		this.networkPollInterval = networkPollInterval;
		this.cacheLocally = cacheLocally;
	}

	@Override
	public void run() {
		GALog.i("BatchThread started");
		// Before we send off the events two conditions have to be met:
		// - Specific time interval passed
		// - Data connection available

		// First wait for time interval
		long endTime = System.currentTimeMillis() + sendEventInterval;
		while (System.currentTimeMillis() < endTime) {
			try {
				sleep(endTime - System.currentTimeMillis());
			} catch (Exception e) {
				GALog.e("Error: " + e.getMessage(), e);
			}
		}
		if (sendEventInterval > 0) {
			GALog.i("Time interval passed");
		} // Otherwise there is no interval so thread continues immediately

		// Is cache locally enabled?
		if (!cacheLocally && !isNetworkConnected()) {
			// Wipe database
			GALog.i("No network available and cache locally is disabled, clearing events");
			eventDatabase.clear();
			// Don't bother polling network
			GameAnalytics.canStartNewThread();
			return;
		}

		// Is network polling disabled
		if (!pollNetwork && !isNetworkConnected()) {
			GALog.i("No network available");
			GameAnalytics.canStartNewThread();
			return;
		}

		// Do we have data connection?
		while (!isNetworkConnected()) {
			// Intermittently poll until network is reconnected
			GALog.i("Polling network...");
			try {
				sleep(networkPollInterval);
			} catch (Exception e) {
				GALog.e("Error: " + e.getMessage(), e);
			}
		}

		GALog.i("Network is connected, sending events");

		sendEvents();
	}

	@SuppressWarnings("unchecked")
	private void sendEvents() {
		// Get events from database
		Object[] eventLists = eventDatabase.getEvents();
		HashMap<String, EventList<DesignEvent>> designEvents = (HashMap<String, EventList<DesignEvent>>) eventLists[0];
		HashMap<String, EventList<BusinessEvent>> businessEvents = (HashMap<String, EventList<BusinessEvent>>) eventLists[1];
		HashMap<String, EventList<UserEvent>> userEvents = (HashMap<String, EventList<UserEvent>>) eventLists[2];
		HashMap<String, EventList<QualityEvent>> qualityEvents = (HashMap<String, EventList<QualityEvent>>) eventLists[3];

		// For each game id and event array
		String eventGameKey;
		EventList<?> eventList;
		// Convert event lists to json using GSON
		Gson gson = new Gson();
		// Send events if the list is not empty
		for (Entry<String, EventList<DesignEvent>> e : designEvents.entrySet()) {
			eventGameKey = e.getKey();
			eventList = e.getValue();
			if (!eventList.isEmpty()) {
				GALog.i("Sending " + eventList.size() + " design events.");
				sendEventSet(gson.toJson(eventList), GameAnalytics.DESIGN,
						eventGameKey, eventList.getEventIdList());
			} else
				GALog.i("No design events to send.");
		}
		for (Entry<String, EventList<BusinessEvent>> e : businessEvents
				.entrySet()) {
			eventGameKey = e.getKey();
			eventList = e.getValue();
			if (!eventList.isEmpty()) {
				GALog.i("Sending " + businessEvents.size()
						+ " business events.");
				sendEventSet(gson.toJson(eventList), GameAnalytics.BUSINESS,
						eventGameKey, eventList.getEventIdList());
			} else
				GALog.i("No business events to send.");
		}
		for (Entry<String, EventList<QualityEvent>> e : qualityEvents
				.entrySet()) {
			eventGameKey = e.getKey();
			eventList = e.getValue();
			if (!eventList.isEmpty()) {
				GALog.i("Sending " + qualityEvents.size() + " quality events.");
				sendEventSet(gson.toJson(eventList), GameAnalytics.QUALITY,
						eventGameKey, eventList.getEventIdList());
			} else
				GALog.i("No quality events to send.");
		}
		for (Entry<String, EventList<UserEvent>> e : userEvents.entrySet()) {
			eventGameKey = e.getKey();
			eventList = e.getValue();
			if (!eventList.isEmpty()) {
				GALog.i("Sending " + userEvents.size() + " user events.");
				sendEventSet(gson.toJson(eventList), GameAnalytics.USER,
						eventGameKey, eventList.getEventIdList());
			} else
				GALog.i("No user events to send.");
		}
	}

	private void sendEventSet(String json, String category,
			String eventGameKey, ArrayList<Integer> eventsToDelete) {
		// Game key for these events
		if (eventGameKey == EventDatabase.DEFAULT_GAME_KEY) {
			eventGameKey = this.defaultGameKey;
		}

		// Print response if in VERBOSE mode
		GALog.i("Raw JSON for " + category + ", game key = " + eventGameKey
				+ ", events being sent to GA server: " + json);

		// Add auth header
		Header[] headers = new Header[1];
		headers[0] = new BasicHeader(GameAnalytics.AUTHORIZATION,
				getAuthorizationString(json));

		// POST request to server
		StringEntity jsonEntity = null;
		try {
			jsonEntity = new StringEntity(json);
		} catch (UnsupportedEncodingException e) {
			GALog.e("Error converting json String into StringEntity: "
					+ e.toString(), e);
		}
		
		client.post(
				context,
				GameAnalytics.API_URL + eventGameKey + category,
				jsonEntity,
				GameAnalytics.CONTENT_TYPE_JSON,
				headers,
				new PostResponseHandler(eventsToDelete, eventDatabase, category));
	}

	private String getAuthorizationString(String json) {
		return GameAnalytics.md5(json + secretKey);
	}

	private boolean isNetworkConnected() {
		ConnectivityManager connectivityManager = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
		return (networkInfo != null && networkInfo.isConnected());
	}

	protected void manualBatch() {
		// The manual batch is simply a normal BatchThread but it doesn't wait
		// to send the events and doesn't poll the internet.
		sendEventInterval = 0;
		pollNetwork = false;
		start();
	}
}
