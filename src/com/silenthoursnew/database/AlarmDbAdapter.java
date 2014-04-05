package com.silenthoursnew.database;

import java.util.ArrayList;
import java.util.Calendar;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.silenthoursnew.core.Alarm;

public class AlarmDbAdapter {

	// Keys
	public static final String KEY_ROWID = "_id";

	// Alarm related keys
	public static final String KEY_FROM = "timefrom";
	public static final String KEY_TO = "timeto";
	public static final String KEY_SUN = "sunday";
	public static final String KEY_MON = "monday";
	public static final String KEY_TUE = "tuesday";
	public static final String KEY_WED = "wednesday";
	public static final String KEY_THU = "thursday";
	public static final String KEY_FRI = "friday";
	public static final String KEY_SAT = "saturday";
	public static final String KEY_VIBRATION = "vibration";
	public static final String KEY_MEDIA = "media";
	private static final String KEY_LOCK = "lock";
	private static final String KEY_UNMUTE_ON_CALL = "unmute";
	private static final String KEY_BRIGHTNESS = "brightness";
	private static final String KEY_DISABLE_NOTIFICATION_LIGHT = "notilight";

	private static final String TAG = "AlarmDbAdapter";
	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;

	// Database properties
	private static final String DATABASE_NAME = "silent_hours_db";
	private static final String DATABASE_TABLE_ALARMS = "Alarms";
	private static final int DATABASE_VERSION = 4;

	// Tables
	private static final String CREATE_TABLE_ALARMS = "create table " + DATABASE_TABLE_ALARMS + " (" + KEY_ROWID + " INTEGER PRIMARY KEY, " + KEY_FROM
			+ " INTEGER NOT NULL, " + KEY_TO + " INTEGER NOT NULL, " + KEY_SUN + " BOOLEAN NOT NULL, " + KEY_MON + " BOOLEAN NOT NULL, " + KEY_TUE
			+ " BOOLEAN NOT NULL, " + KEY_WED + " BOOLEAN NOT NULL, " + KEY_THU + " BOOLEAN NOT NULL, " + KEY_FRI + " BOOLEAN NOT NULL, " + KEY_SAT
			+ " BOOLEAN NOT NULL, " + KEY_VIBRATION + " BOOLEAN NOT NULL, " + KEY_MEDIA + " BOOLEAN NOT NULL, " + KEY_LOCK + " BOOLEAN NOT NULL, "
			+ KEY_UNMUTE_ON_CALL + " BOOLEAN NOT NULL, " + KEY_DISABLE_NOTIFICATION_LIGHT + " BOOLEAN NOT NULL, " + KEY_BRIGHTNESS + " INTEGER NOT NULL);";

	private final Context mCtx;

	private static class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper(final Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(final SQLiteDatabase db) {

			db.execSQL(CREATE_TABLE_ALARMS);

		}

		@Override
		public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_ALARMS);
			onCreate(db);

		}
	}

	public AlarmDbAdapter(final Context ctx) {
		mCtx = ctx;
	}

	/**
	 * Opens the database
	 * 
	 * @return
	 * @throws SQLException
	 */
	public AlarmDbAdapter open() throws SQLException {
		mDbHelper = new DatabaseHelper(mCtx);
		mDb = mDbHelper.getWritableDatabase();

		return this;
	}

	/**
	 * Closes the database.
	 */
	public void close() {
		mDbHelper.close();
	}

	// CREATE

	/**
	 * Creates a new Alarm entry in the database with the values provided.
	 * 
	 * @param from
	 *            the time to disable
	 * @param to
	 *            the time to enable
	 * @param enabledWeekdays
	 *            the days of the week, with their enabled status as a boolean (7 boolean array starting with Sunday)
	 * @return
	 */
	public long createAlarm(final int id, final Alarm alarm) {

		final ContentValues args = getContentValues(id, alarm.from, alarm.to, new boolean[] { alarm.sunday, alarm.monday, alarm.tuesday, alarm.wednesday,
				alarm.thursday, alarm.friday, alarm.saturday }, alarm.enableVibration, alarm.muteMedia, alarm.lockVolume, alarm.unmuteOnCall,
				alarm.disableNotiLight, alarm.brightness);

		return mDb.insert(DATABASE_TABLE_ALARMS, null, args);
	}

	/**
	 * Updates an alarm according to the parameters
	 * 
	 * @param id
	 *            the alarm to update
	 * @param from
	 * @param to
	 * @param enabledWeekdays
	 * @param enableVibration
	 * @param muteMedia
	 * @return the success of the operation
	 */
	public boolean updateAlarm(final int id, final Alarm alarm) {
		final ContentValues args = getContentValues(id, alarm.from, alarm.to, new boolean[] { alarm.sunday, alarm.monday, alarm.tuesday, alarm.wednesday,
				alarm.thursday, alarm.friday, alarm.saturday }, alarm.enableVibration, alarm.muteMedia, alarm.lockVolume, alarm.unmuteOnCall,
				alarm.disableNotiLight, alarm.brightness);

		long derp = mDb.update(DATABASE_TABLE_ALARMS, args, KEY_ROWID + "=" + id, null);

		return derp > 0;
	}

	private ContentValues getContentValues(final int id, final int from, final int to, final boolean[] enabledWeekdays, final boolean enableVibration,
			final boolean muteMedia, final boolean lockVolume, final boolean unmuteOnCall, final boolean disableNotficationLight, final float brightness) {
		final ContentValues args = new ContentValues();
		args.put(KEY_ROWID, id);

		args.put(KEY_FROM, from);
		args.put(KEY_TO, to);

		args.put(KEY_SUN, enabledWeekdays[0]);
		args.put(KEY_MON, enabledWeekdays[1]);
		args.put(KEY_TUE, enabledWeekdays[2]);
		args.put(KEY_WED, enabledWeekdays[3]);
		args.put(KEY_THU, enabledWeekdays[4]);
		args.put(KEY_FRI, enabledWeekdays[5]);
		args.put(KEY_SAT, enabledWeekdays[6]);

		// Log.d("Booleans", "vib " + enableVibration + " media " + muteMedia + " lock " + lockVolume + " unm " + unmuteOnCall);

		args.put(KEY_VIBRATION, enableVibration);
		args.put(KEY_MEDIA, muteMedia);
		args.put(KEY_LOCK, lockVolume);
		args.put(KEY_UNMUTE_ON_CALL, unmuteOnCall);
		args.put(KEY_DISABLE_NOTIFICATION_LIGHT, disableNotficationLight);
		args.put(KEY_BRIGHTNESS, brightness);
		return args;
	}

	// DELETE

	public boolean deleteAlarm(final int alarmId) {
		return mDb.delete(DATABASE_TABLE_ALARMS, KEY_ROWID + "=" + alarmId, null) > 0;
	}

	// FETCH
	public Cursor fetchAlarm(final int alarmId) {

		return mDb.query(DATABASE_TABLE_ALARMS, new String[] { KEY_ROWID, KEY_FROM, KEY_TO, KEY_SUN, KEY_MON, KEY_TUE, KEY_WED, KEY_THU, KEY_FRI, KEY_SAT,
				KEY_VIBRATION, KEY_MEDIA, KEY_LOCK, KEY_UNMUTE_ON_CALL, KEY_DISABLE_NOTIFICATION_LIGHT, KEY_BRIGHTNESS }, KEY_ROWID + "=" + alarmId, null,
				null, null, null);

	}

	// FETCH
	public boolean[] fetchAlarmWeekdays(final int alarmId) {

		Cursor weekdayCursor = mDb.query(DATABASE_TABLE_ALARMS, new String[] { KEY_ROWID, KEY_SUN, KEY_MON, KEY_TUE, KEY_WED, KEY_THU, KEY_FRI, KEY_SAT },
				KEY_ROWID + "=" + alarmId, null, null, null, null);

		if (weekdayCursor != null && weekdayCursor.moveToFirst()) {
			return new boolean[] { weekdayCursor.getInt(1) == 1, weekdayCursor.getInt(2) == 1, weekdayCursor.getInt(3) == 1, weekdayCursor.getInt(4) == 1,
					weekdayCursor.getInt(5) == 1, weekdayCursor.getInt(6) == 1, weekdayCursor.getInt(7) == 1 };
		} else {
			return new boolean[] { false, false, false, false, false, false, false };
		}

	}

	public ArrayList<Alarm> fetchAllAlarms() {

		ArrayList<Alarm> alarm_data = new ArrayList<Alarm>();

		Cursor alarmCursor = mDb.query(DATABASE_TABLE_ALARMS, new String[] { KEY_ROWID, KEY_FROM, KEY_TO, KEY_SUN, KEY_MON, KEY_TUE, KEY_WED, KEY_THU, KEY_FRI,
				KEY_SAT, KEY_VIBRATION, KEY_MEDIA, KEY_LOCK, KEY_UNMUTE_ON_CALL, KEY_DISABLE_NOTIFICATION_LIGHT, KEY_BRIGHTNESS }, null, null, null, null,
				KEY_ROWID);

		addAlarmsToList(alarm_data, alarmCursor);

		return alarm_data;
	}

	public ArrayList<Integer> fetchAllAlarmIDs() {

		ArrayList<Integer> alarm_ids = new ArrayList<Integer>();

		Cursor alarmCursor = mDb.query(DATABASE_TABLE_ALARMS, new String[] { KEY_ROWID }, null, null, null, null, KEY_ROWID);

		addIdsToList(alarm_ids, alarmCursor);

		return alarm_ids;
	}

	private void addAlarmsToList(final ArrayList<Alarm> alarm_data, final Cursor alarmCursor) {
		if (alarmCursor != null && alarmCursor.moveToFirst()) {
			while (!alarmCursor.isAfterLast()) {
				alarm_data.add(new Alarm(alarmCursor.getInt(1), alarmCursor.getInt(2), alarmCursor.getInt(10) == 1, alarmCursor.getInt(11) == 1, alarmCursor
						.getInt(12) == 1, alarmCursor.getInt(13) == 1, alarmCursor.getInt(14) == 1, alarmCursor.getInt(15), new boolean[] {
						alarmCursor.getInt(3) == 1, alarmCursor.getInt(4) == 1, alarmCursor.getInt(5) == 1, alarmCursor.getInt(6) == 1,
						alarmCursor.getInt(7) == 1, alarmCursor.getInt(8) == 1, alarmCursor.getInt(9) == 1 }));
				alarmCursor.moveToNext();
			}

		}
	}

	private void addIdsToList(final ArrayList<Integer> alarm_ids, final Cursor alarmCursor) {
		if (alarmCursor != null && alarmCursor.moveToFirst()) {
			while (!alarmCursor.isAfterLast()) {

				alarm_ids.add(alarmCursor.getInt(0));
				alarmCursor.moveToNext();

			}

		}
	}

	/**
	 * The number of days before the given alarm is enabled again
	 * 
	 * @param alarmId
	 *            the alarm to examine
	 * @return the number of days
	 */
	public int daysUntilNextEnabled(final int alarmId) {
		boolean[] enabledWeekdays;

		// All disable alarms have even IDs and are the only ones present in the
		// db
		boolean disable = alarmId % 2 == 0;

		// There is only one database entry for each (enable/disable) alarm pair
		if (disable) {
			enabledWeekdays = fetchAlarmWeekdays(alarmId);
		} else {
			enabledWeekdays = fetchAlarmWeekdays(alarmId - 1);
		}

		// Get the current day of the week -1 (to make comparisons easier)
		int dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1;

		// Days until the alarm is enabled next time, default is 7 (1 week)
		int daysUntil = 7;

		for (int i = 0; i < 7; i++) {
			if (enabledWeekdays[i]) {
				if (i < dayOfWeek) {
					if (daysUntil == 7) {
						daysUntil = 7 - dayOfWeek + i;
					}
				} else if (i > dayOfWeek) {
					daysUntil = i - dayOfWeek;
					break;
				}
			}
		}

		if (!disable) {
			Cursor alarmCursor = fetchAlarm(alarmId - 1);
			if (alarmCursor.moveToFirst()) {
				if (alarmCursor.getInt(1) >= alarmCursor.getInt(2)) {
					if (enabledWeekdays[dayOfWeek]) {
						daysUntil = 1;
					} else {
						daysUntil++;
					}
				}
			}

		}

		return daysUntil;

	}

}
