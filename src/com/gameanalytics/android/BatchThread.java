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
	private String gameKey;
	private String secretKey;
	private int sendEventInterval;
	private int networkPollInterval;
	private Context context;
	private AsyncHttpClient client;
	private EventDatabase eventDatabase;

	// Once this is set to true, all further events will be send off by the next
	// BatchThread.
	private boolean sendingEvents = false;

	protected BatchThread(Context context, AsyncHttpClient client,
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
		GALog.i("Time interval passed");

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
			GALog.i("Sending " + designEvents.size() + " design events.");
		} else
			GALog.i("No design events to send.");

		if (!businessEvents.isEmpty()) {
			sendEventSet(gson.toJson(businessEvents), GameAnalytics.BUSINESS);
			GALog.i("Sending " + businessEvents.size() + " business events.");
		} else
			GALog.i("No business events to send.");

		if (!qualityEvents.isEmpty()) {
			sendEventSet(gson.toJson(qualityEvents), GameAnalytics.QUALITY);
			GALog.i("Sending " + qualityEvents.size() + " quality events.");
		} else
			GALog.i("No quality events to send.");

		if (!userEvents.isEmpty()) {
			sendEventSet(gson.toJson(userEvents), GameAnalytics.USER);
			GALog.i("Sending " + userEvents.size() + " user events.");
		} else
			GALog.i("No user events to send.");
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
			GALog.e("Error converting json String into StringEntity: "
					+ e.toString(), e);
		}
		client.post(context, GameAnalytics.API_URL + gameKey + category,
				jsonEntity, GameAnalytics.CONTENT_TYPE_JSON, headers,
				GameAnalytics.postResponseHandler);
	}

	private String getAuthorizationString(String json) {
		return GameAnalytics.md5(json + secretKey);
	}

	protected boolean isSendingEvents() {
		return sendingEvents;
	}

	private boolean isNetworkConnected() {
		ConnectivityManager connectivityManager = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
		return (networkInfo != null && networkInfo.isConnected());
	}
}