package com.weblink.mexapp.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;

import com.weblink.mexapp.services.CallReceiverService;
import com.weblink.mexapp.services.PostCallUpdateService;
import com.weblink.mexapp.utility.Constants;
import com.weblink.mexapp.utility.Tools;

public class SystemCallReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(final Context context, final Intent intent) {
		// Needed to see in what way the phone state changed (New call, hang up
		// etc)
		TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

		boolean showNotification = Tools.getIntPreference(PreferenceManager.getDefaultSharedPreferences(context), Constants.SETTING_CALL_WINDOW, Constants.CALL_WINDOW_NONE) == Constants.CALL_WINDOW_NONE;

		// The callstate (Ringing, hung up etc)
		int callState = tm.getCallState();

		// Incoming call
		if (callState == TelephonyManager.CALL_STATE_RINGING) {

			if (showNotification) {
				Tools.Notifications.showCallNotification(context);
			}

			Intent newIntent = new Intent(context, CallReceiverService.class);

			context.startService(newIntent);

		} else if (callState == TelephonyManager.CALL_STATE_OFFHOOK) {

			if (showNotification) {
				// show notification
				Tools.Notifications.showCallNotification(context);
			}

			Intent newIntent = new Intent(context, CallReceiverService.class);

			context.startService(newIntent);

		} else if (callState == TelephonyManager.CALL_STATE_IDLE) {

			// Start a service containing a "wait" thread. Also checks for active calls
			Intent newIntent = new Intent(context, PostCallUpdateService.class);
			context.startService(newIntent);

			SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
			if (!sharedPrefs.getBoolean(Constants.CALL_HELD, false)) {
				Tools.dismissCallPopup(context);
			}

			Tools.Notifications.dismissCallNotification(context);

		}

	}
}
