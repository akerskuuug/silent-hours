package com.silenthoursnew.utility;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;

import com.silenthoursnew.alarm.ScheduleSilenceReceiver;
import com.silenthoursnew.core.Alarm;
import com.silenthoursnew.database.AlarmDbAdapter;

import java.util.ArrayList;
import java.util.Calendar;

public class Tools {

	public static Typeface getTypefaceRobotoLight(final Context context) {
		return Typeface.createFromAsset(context.getAssets(), "fonts/Roboto-Light.ttf");
	}

	public static Typeface getTypefaceRobotoRegular(final Context context) {
		return Typeface.createFromAsset(context.getAssets(), "fonts/Roboto-Regular.ttf");
	}

	public static Typeface getTypefaceRobotoThin(final Context context) {
		return Typeface.createFromAsset(context.getAssets(), "fonts/Roboto-Thin.ttf");
	}
    public static boolean isLollipopOrLater(){
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }
    public static boolean isKitKatOrLater(){
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
    }

	@SuppressLint("NewApi")
	public static boolean isTablet(final Context context) {

		int sdkVersion = android.os.Build.VERSION.SDK_INT;

		if (sdkVersion >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			// Compute screen size
			int dm = context.getResources().getConfiguration().smallestScreenWidthDp;

			return dm >= 720;
		} else if (sdkVersion >= android.os.Build.VERSION_CODES.HONEYCOMB) {
			// All Honeycomb devices are tablets
			return true;
		} else {
			// No pre-Honeycomb devices are tablets as far as this is concerned
			return false;
		}

	}

	/**
	 * This method fixes the formatting in the time for hour and minute values less than 10.
	 * 
	 * @param hours
	 *            self explanatory
	 * @param minutes
	 *            self explanatory
	 * @return the complete string, in the format HH:MM
	 */
	public static String fixTimeFormatting(final int hours, final int minutes) {
		String hr, min;

		// Fix formatting for values less than 10
		hr = hours < 10 ? "0" : "";
		min = minutes < 10 ? "0" : "";

		return hr + hours + ":" + min + minutes;

	}

	/**
	 * Takes an integer as parameter and returns a string that is the integer padded with a zero if the value is below 10
	 * 
	 * @param input
	 *            the integer to examine
	 * @return a padded string
	 */
	public static String getPaddedString(final int input) {

		return input < 10 ? "0" + input : "" + input;

	}

	public static PendingIntent getDistinctPendingIntent(final Context context, final Intent intent, final int requestId) {
		PendingIntent pi = PendingIntent.getBroadcast(context, requestId, intent, PendingIntent.FLAG_CANCEL_CURRENT);

		return pi;
	}

	public static void setAllAlarms(Context context) {

		AlarmDbAdapter dbAdapter = new AlarmDbAdapter(context);
		dbAdapter.open();

		ArrayList<Alarm> alarm_data = dbAdapter.fetchAllAlarms();

		ArrayList<Integer> alarmIDs = dbAdapter.fetchAllAlarmIDs();

		for (int i = 0; i < alarm_data.size(); i++) {
			Alarm alarm = alarm_data.get(i);
			setAlarm(context, dbAdapter, alarm, alarmIDs.get(i));
		}

		dbAdapter.close();
	}

	public static void setAlarm(Context context, AlarmDbAdapter dbAdapter, Alarm alarm, int alarmId) {

		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

		// Calendars used to set all of the alarms
		Calendar calNow = Calendar.getInstance();
		Calendar calFrom = Calendar.getInstance();
		Calendar calTo = Calendar.getInstance();

		// Get the current day of the week (starting with Sunday as 0)
		int currentDay = calNow.get(Calendar.DAY_OF_WEEK) - 1;

		int fromHours, fromMinutes, toHours, toMinutes, disableId, enableId;

		fromHours = alarm.from / 60;
		fromMinutes = alarm.from % 60;

		toHours = alarm.to / 60;
		toMinutes = alarm.to % 60;

		disableId = alarmId;
		enableId = disableId + 1;

		// Reset calendars for every new alarm
		calFrom = Calendar.getInstance();
		calTo = Calendar.getInstance();

		// Set the correct time for the calendars
		calFrom.set(Calendar.HOUR_OF_DAY, fromHours);
		calFrom.set(Calendar.MINUTE, fromMinutes);
		calFrom.set(Calendar.SECOND, 0);

		calTo.set(Calendar.HOUR_OF_DAY, toHours);
		calTo.set(Calendar.MINUTE, toMinutes);
		calTo.set(Calendar.SECOND, 0);

		// Get the boolean value representing the enabled status for all
		// weekdays
		boolean wdays[] = new boolean[] { alarm.sunday, alarm.monday, alarm.tuesday, alarm.wednesday, alarm.thursday, alarm.friday, alarm.saturday };

		// If the alarm is set to be enabled before it is disabled, set
		// it for 24 hours later
		if (calTo.compareTo(calFrom) <= 0) {
			calTo.add(Calendar.DATE, 1);
		}

		int daysUntil = dbAdapter.daysUntilNextEnabled(disableId);

		// If the alarm is disabled or both of the times have already
		// passed, set the alarms for the next enabled day
		if (!wdays[currentDay] || calFrom.compareTo(calNow) <= 0 && calTo.compareTo(calNow) <= 0) {

			calFrom.add(Calendar.DATE, daysUntil);
			calTo.add(Calendar.DATE, daysUntil);

		}

		// If the alarm is set to be disabled before now, set it for the
		// next enabled day
		if (calFrom.compareTo(calNow) <= 0) {
			SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

			setSilent(context, sharedPrefs, false, alarm.enableVibration, alarm.muteMedia, alarm.lockVolume, alarm.unmuteOnCall, alarm.disableNotiLight, alarm.brightness);
			calFrom.add(Calendar.DATE, daysUntil);
		}

		Intent schedulerIntent = new Intent(context, ScheduleSilenceReceiver.class);

		schedulerIntent.putExtra(Constants.SCHEDULER_ALARM_ID, disableId);
		schedulerIntent.putExtra(Constants.SCHEDULER_SET_SOUND_ENABLED, false);
		schedulerIntent.putExtra(Constants.SCHEDULER_SET_VIBRATION_ENABLED, alarm.enableVibration);
		schedulerIntent.putExtra(Constants.SCHEDULER_SET_MEDIA_DISABLED, alarm.muteMedia);
		schedulerIntent.putExtra(Constants.SCHEDULER_VOLUME_LOCK, alarm.lockVolume);
		schedulerIntent.putExtra(Constants.SCHEDULER_UNMUTE_ON_CALL, alarm.unmuteOnCall);
		schedulerIntent.putExtra(Constants.SCHEDULER_BRIGHTNESS, alarm.brightness);
		schedulerIntent.putExtra(Constants.SCHEDULER_DISABLE_NOTI_LIGHT, alarm.disableNotiLight);

		cancelOldAlarms(context, alarmManager, alarmId);

		// Set the alarm
		alarmManager.set(AlarmManager.RTC_WAKEUP, calFrom.getTimeInMillis(), Tools.getDistinctPendingIntent(context, schedulerIntent, disableId));

		// Change the second alarm to enable connections
		schedulerIntent.putExtra(Constants.SCHEDULER_ALARM_ID, enableId);
		schedulerIntent.putExtra(Constants.SCHEDULER_SET_SOUND_ENABLED, true);

		// Set the alarm
		alarmManager.set(AlarmManager.RTC_WAKEUP, calTo.getTimeInMillis(), Tools.getDistinctPendingIntent(context, schedulerIntent, enableId));
	}

	public static void cancelOldAlarms(Context context, AlarmManager am, final int tempId) {
		// Needed to cancel the alarms properly
		Intent tempIntent = new Intent(context, ScheduleSilenceReceiver.class);

		PendingIntent sender = getDistinctPendingIntent(context, tempIntent, tempId);
		// Cancel ongoing alarm related to scheduled
		// sound disabling
		am.cancel(sender);

		PendingIntent newSender = getDistinctPendingIntent(context, tempIntent, tempId + 1);

		// Cancel ongoing alarm related to scheduled
		// sound enabling
		am.cancel(newSender);
	}

	@SuppressLint("NewApi")
    public static void setSilent(final Context context, SharedPreferences settings, boolean enableSound, boolean enableVibration, boolean muteMedia, boolean lockVolume, boolean unmuteOnCall,
			boolean disableNotificationLight, int brightness) {

		Editor editor = settings.edit();

		if (enableSound) {
			// Unlock the volume and commit changes to the shared preferences
			editor.putBoolean(Constants.SCHEDULER_SOUND_MUTED, false).commit();
			editor.putBoolean(Constants.SCHEDULER_VOLUME_LOCK, false).commit();
			editor.putBoolean(Constants.SCHEDULER_UNMUTE_ON_CALL, false).commit();
		}

		// Get the sound manager
		final AudioManager audioMgr = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

		if (settings.getBoolean(Constants.SCHEDULER_ENABLED, false)) {
			if (enableSound) {

				// Enable sound
				audioMgr.setRingerMode(AudioManager.RINGER_MODE_NORMAL);

			} else {

				if (enableVibration) {
					// Mute sound (with vibration)
					audioMgr.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
				} else {
					// Mute sound (without vibration)
					audioMgr.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                    if(isLollipopOrLater()){
                        // Mute again to achieve proper mute on Lollipop
                        if(Tools.isLollipopOrLater()){
                            AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
                                @Override
                                protected Void doInBackground(Void... params) {
                                    try {
                                        Thread.sleep(200);
                                        audioMgr.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    return null;
                                }

                            };

                            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                        }

                    }

				}

				editor.putBoolean(Constants.SCHEDULER_SOUND_MUTED, true).commit();
				editor.putBoolean(Constants.SCHEDULER_VOLUME_LOCK, lockVolume).commit();
				editor.putBoolean(Constants.SCHEDULER_UNMUTE_ON_CALL, unmuteOnCall);
				// Commit changes to the shared preferences
				editor.commit();
			}

			if (muteMedia) {
				if (enableSound) {
					// Unmute media, set to the same volume as before
					audioMgr.setStreamVolume(AudioManager.STREAM_MUSIC, settings.getInt(Constants.SCHEDULER_MEDIA_VOLUME, audioMgr.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / 3), 0);
				} else {
					// Save the current media volume
					editor.putInt(Constants.SCHEDULER_MEDIA_VOLUME, audioMgr.getStreamVolume(AudioManager.STREAM_MUSIC));
					// Commit changes to the shared preferences
					editor.commit();

					// Mute media
					audioMgr.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
				}
			}

			if (brightness != -1) {
				ContentResolver cr = context.getContentResolver();

				if (enableSound) {
					// Reset brightness

					boolean cachedAB = settings.getBoolean(Constants.SCHEDULER_BRIGHTNESS_AUTO, false);
					int cachedBrightness = settings.getInt(Constants.SCHEDULER_BRIGHTNESS_CACHED, 125);

					Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS_MODE, cachedAB ? Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC : Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
					Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS, cachedBrightness);

				} else {
					try {

						boolean abEnabled = Settings.System.getInt(cr, Settings.System.SCREEN_BRIGHTNESS_MODE) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
						int prevBrightness = Settings.System.getInt(cr, Settings.System.SCREEN_BRIGHTNESS);

						editor.putBoolean(Constants.SCHEDULER_BRIGHTNESS_AUTO, abEnabled);
						editor.putInt(Constants.SCHEDULER_BRIGHTNESS_CACHED, prevBrightness);
						editor.commit();

						Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
						Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS, brightness);

					} catch (SettingNotFoundException e) {
						Log.e("An error occured", e.getLocalizedMessage());
					}

				}

			}

			if (disableNotificationLight) {
				ContentResolver cr = context.getContentResolver();
				if (enableSound) {
					try {
						// android.app.Notification.
						android.provider.Settings.System.putInt(cr, "notification_light_pulse", settings.getInt(Constants.SCHEDULER_CACHED_NOTI_LIGHT, 0));
					} catch (Exception e) {
						Log.e("An error occured", e.getLocalizedMessage());
					}
				} else {
					editor.putInt(Constants.SCHEDULER_CACHED_NOTI_LIGHT, Settings.System.getInt(cr, "notification_light_pulse", 0));
					editor.commit();
					try {
						Settings.System.putInt(cr, "notification_light_pulse", 0);
					} catch (Exception e) {
						Log.e("An error occured", e.getLocalizedMessage());
					}
				}

			}
		}
	}
}
