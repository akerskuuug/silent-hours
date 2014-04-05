package com.weblink.mexapp.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

import com.weblink.mexapp.utility.Constants;
import com.weblink.mexapp.utility.Tools;

public class VoicemailIgnoreReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(final Context context, final Intent intent) {

		Tools.Notifications.dismissVoicemailNotification(context);
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		Editor editor = sharedPrefs.edit();

		editor.putString(Constants.LATEST_ACKED_VOICEMAIL, sharedPrefs.getString(Constants.LATEST_VOICEMAIL, ""));
		editor.commit();

	}

}
