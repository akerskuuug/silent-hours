package com.weblink.mexapp.receiver;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.weblink.mexapp.services.VoicemailUpdateService;
import com.weblink.mexapp.utility.Constants;
import com.weblink.mexapp.utility.Tools;

public class ScheduleMessageUpdateReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(final Context context, final Intent intent) {

		update(context);

	}

	public static void update(final Context context) {

		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		int updateFrequency = Tools.getIntPreference(sharedPrefs, Constants.SETTING_UPDATE_FREQUENCY, 30);

		if (updateFrequency != 0) {
			Intent newIntent = new Intent(context, VoicemailUpdateService.class);
			context.startService(newIntent);

			AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
			Tools.setUpdateAlarm(alarmManager, context, updateFrequency);
		}
	}
}
