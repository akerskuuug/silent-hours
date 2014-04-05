package com.weblink.mexapp.receiver;

import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import com.weblink.mexapp.utility.Constants;
import com.weblink.mexapp.utility.Tools;

public class VoicemailCallReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(final Context context, final Intent intent) {

		Tools.Notifications.dismissVoicemailNotification(context);

		Intent newIntent = new Intent(Intent.ACTION_CALL);
		newIntent.setData(Uri.parse("tel:" + PreferenceManager.getDefaultSharedPreferences(context).getString(Constants.SETTING_VOICEMAIL_NUMBER, Constants.DEFAULT_VOICEMAIL_NUMBER)));
		PendingIntent pi = PendingIntent.getActivity(context, 0, newIntent, Intent.FLAG_ACTIVITY_NEW_TASK);

		try {
			pi.send();
		} catch (CanceledException e) {
			Log.e("CallVoicemailReceiver", e.getLocalizedMessage());
		}

	}

}
