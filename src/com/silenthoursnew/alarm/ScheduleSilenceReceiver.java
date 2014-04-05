package com.silenthoursnew.alarm;

import java.util.Calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;

import com.silenthoursnew.core.Alarm;
import com.silenthoursnew.database.AlarmDbAdapter;
import com.silenthoursnew.utility.Constants;
import com.silenthoursnew.utility.Tools;

public class ScheduleSilenceReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(final Context context, final Intent intent) {
		SharedPreferences settings = context.getSharedPreferences(Constants.SHARED_PREFS_NAME, Context.MODE_PRIVATE);
		Bundle extras = intent.getExtras();

		AlarmDbAdapter dbAdapter = new AlarmDbAdapter(context);
		dbAdapter.open();

		// Put all of the relevant extras into variables
		int alarmId = extras.getInt(Constants.SCHEDULER_ALARM_ID, 0);
		boolean enableSound = extras.getBoolean(Constants.SCHEDULER_SET_SOUND_ENABLED, false);
		boolean enableVibration = extras.getBoolean(Constants.SCHEDULER_SET_VIBRATION_ENABLED, false);
		boolean muteMedia = extras.getBoolean(Constants.SCHEDULER_SET_MEDIA_DISABLED, false);
		boolean lockVolume = extras.getBoolean(Constants.SCHEDULER_VOLUME_LOCK, false);
		boolean unmuteOnCall = extras.getBoolean(Constants.SCHEDULER_UNMUTE_ON_CALL, false);
		boolean disableNotificationLight = extras.getBoolean(Constants.SCHEDULER_DISABLE_NOTI_LIGHT, false);
		int brightness = extras.getInt(Constants.SCHEDULER_BRIGHTNESS, -1);

		// Get the Notification manager to be able to set alarms
		AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

		Intent newIntent = new Intent(context, ScheduleSilenceReceiver.class);
		newIntent.putExtras(extras);

		PendingIntent sender = Tools.getDistinctPendingIntent(context, newIntent, alarmId);

		// Set the alarm for the same time on the next enabled day
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, dbAdapter.daysUntilNextEnabled(alarmId));

		// Set the alarm
		am.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), sender);

		Tools.setSilent(context, settings, enableSound, enableVibration,
				muteMedia, lockVolume, unmuteOnCall, disableNotificationLight,
				brightness);

	}
}
