package com.gameanalytics.android;

import java.io.IOException;

import android.content.Context;
import android.os.AsyncTask;
import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.ads.identifier.AdvertisingIdClient.Info;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;

public class GetGoogleAIDAsync extends AsyncTask<Void, Void, Void> {

	private Context context;

	public GetGoogleAIDAsync(Context context) {
		this.context = context;
	}

	@Override
	protected Void doInBackground(Void... params) {
		GALog.i("Getting Google AID.");
		Info adInfo = null;
		try {
			adInfo = AdvertisingIdClient.getAdvertisingIdInfo(context);
			if (adInfo.isLimitAdTrackingEnabled()) {
				GALog.i("Google AID is available but user has opted out. Disabling analytics.");
				GameAnalytics.disableAnalytics();
			} else {
				GALog.i("Google AID is available.");
				if (!GameAnalytics.TYPE_UUID.equals(GameAnalytics.getIdType())) {
					GALog.i("Use it.");
					GameAnalytics.setGoogleAID(adInfo.getId());
				} else {
					GALog.i("Previously used UUID. Disabling analytics.");
					GameAnalytics.disableAnalytics();
				}
			}
		} catch (IOException e) {
			// Unrecoverable error connecting to Google Play services (e.g.,
			// the old version of the service doesn't support getting
			// AdvertisingId).
			GALog.i("Google AID is unavailable on this version of Google Play Services.");
			if (!GameAnalytics.TYPE_GOOGLE_AID.equals(GameAnalytics.getIdType())) {
				GALog.i("Falling back to UUID.");
				GameAnalytics.setUserIdToUUID(true);
			} else {
				GALog.i("Previously used Google AID, cannot use UUID. Disabling analytics.");
				GameAnalytics.disableAnalytics();
			}
		} catch (GooglePlayServicesNotAvailableException e) {
			// Google Play services is not available entirely.
			GALog.i("Google AID is entirely unavailable on this device.");
			if (!GameAnalytics.TYPE_GOOGLE_AID.equals(GameAnalytics.getIdType())) {
				GALog.i("Falling back to UUID.");
				GameAnalytics.setUserIdToUUID(true);
			} else {
				GALog.i("Previously used Google AID, cannot use UUID. Disabling analytics.");
				GameAnalytics.disableAnalytics();
			}
		} catch (IllegalStateException e) {
			// Call was made on main thread - will not happen
			GALog.e("Error retrieving Google AID, have you placed the necessary metatag in AndroidManifest.xml? See error:",
					e);
			if (!GameAnalytics.TYPE_GOOGLE_AID.equals(GameAnalytics.getIdType())) {
				GALog.i("Falling back to UUID.");
				GameAnalytics.setUserIdToUUID(true);
			} else {
				GALog.i("Previously used Google AID, cannot use UUID. Disabling analytics.");
				GameAnalytics.disableAnalytics();
			}
		} catch (GooglePlayServicesRepairableException e) {
			// Repairable exception but we don't want to force user to update
			// just to get android ID, return null
			GALog.i("Google Play Services is disabled or requires an update.");
			if (!GameAnalytics.TYPE_GOOGLE_AID.equals(GameAnalytics.getIdType())) {
				GALog.i("Falling back to UUID.");
				GameAnalytics.setUserIdToUUID(true);
			} else {
				GALog.i("Previously used Google AID, cannot use UUID. Disabling analytics.");
				GameAnalytics.disableAnalytics();
			}
		}
		return null;
	}
}
