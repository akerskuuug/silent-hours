package com.weblink.mexapp.receiver;

import java.util.Calendar;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.weblink.mexapp.utility.Constants;

public class SystemRebootReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(final Context context, final Intent intent) {
		ScheduleMessageUpdateReceiver.update(context);

		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		long scheduledTime = sharedPrefs.getLong(Constants.SCHEDULE_PRESENCE_MILLIS, -1);

		if (scheduledTime != -1) {
			Calendar cal = Calendar.getInstance();

			cal.setTimeInMillis(scheduledTime);

			// Initialize pending intent for alarms
			Intent newIntent = new Intent(context, SchedulePresenceReceiver.class);
			PendingIntent pi = PendingIntent.getBroadcast(context, Constants.ALARM_ID_PRESENCE, newIntent, PendingIntent.FLAG_CANCEL_CURRENT);
			((AlarmManager) context.getSystemService(Activity.ALARM_SERVICE)).set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);

		}

	}
}
