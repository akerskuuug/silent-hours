package com.silenthoursnew.core;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.silenthoursnew.R;
import com.silenthoursnew.database.AlarmDbAdapter;
import com.silenthoursnew.utility.Constants;
import com.silenthoursnew.utility.MyTimePicker;
import com.silenthoursnew.utility.Tools;

import java.util.ArrayList;
import java.util.Calendar;

public class MainFragment extends Fragment implements OnClickListener, OnItemClickListener, OnCheckedChangeListener {

	private AlarmManager am;
	private SharedPreferences settings;
	private Editor editor;

	private AlarmDbAdapter dbAdapter;
	private ListView alarmList;
	private CheckBox serviceCheck;

	private ArrayList<Alarm> alarm_data;
	private ArrayList<Integer> alarmIDs;

	private Typeface typefaceRobotoLight;

	@Override
	public void onAttach(final Activity activity) {
		super.onAttach(activity);
	}

	@SuppressLint("NewApi")
	@Override
	public final View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_main, container, false);

		MainActivity activity = (MainActivity) getActivity();

		am = (AlarmManager) activity.getSystemService(Activity.ALARM_SERVICE);

		settings = activity.getSharedPreferences(Constants.SHARED_PREFS_NAME, Context.MODE_PRIVATE);
		editor = settings.edit();

		typefaceRobotoLight = Tools.getTypefaceRobotoLight(activity);
		dbAdapter = new AlarmDbAdapter(activity);
		dbAdapter.open();

		serviceCheck = (CheckBox) v.findViewById(R.id.service_check);
		serviceCheck.setTypeface(typefaceRobotoLight);
		serviceCheck.setOnCheckedChangeListener(this);

		alarmList = (ListView) v.findViewById(R.id.alarm_list);

		if (Tools.isTablet(getActivity())) {
			TextView title = (TextView) v.findViewById(android.R.id.title);
			title.setTypeface(typefaceRobotoLight);
		}

		View footer = getActivity().getLayoutInflater().inflate(R.layout.listview_header_row, null);
		alarmList.addFooterView(footer);

		Button addButton = (Button) footer.findViewById(R.id.add_button);
		addButton.setOnClickListener(this);

		if (settings.getBoolean(Constants.FIRST_START, true)) {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

			builder.setMessage(R.string.swipe_tooltip);

			builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				public void onClick(final DialogInterface dialog, final int which) {
					dialog.dismiss();
					editor.putBoolean(Constants.FIRST_START, false);
					editor.commit();
				}
			});

			builder.setCancelable(false);

			builder.show();
		}

		alarmList.setOnItemClickListener(this);
		alarmList.setOnItemLongClickListener(new OnItemLongClickListener() {

			public boolean onItemLongClick(final AdapterView<?> parent, final View view, final int position, final long id) {

				final int pos = position;

				AlertDialog.Builder aDialog = new AlertDialog.Builder(getActivity());

				aDialog.setCancelable(true);

				TextView tv = new TextView(getActivity());
				tv.setText(R.string.confirm_delete);
				tv.setGravity(Gravity.CENTER);
				tv.setPadding(10, 20, 10, 20);
				tv.setTextSize(17);
				tv.setTypeface(typefaceRobotoLight);

				aDialog.setView(tv);

				aDialog.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

					public void onClick(final DialogInterface dialog, final int which) {
						int tempId = alarmIDs.get(pos);

						dbAdapter.deleteAlarm(tempId);

						Tools.cancelOldAlarms(getActivity(), am, tempId);

						alarm_data.remove(pos);
						alarmIDs.remove(pos);

						Alarm[] alarmArray = new Alarm[alarm_data.size()];

						alarmList.setAdapter(new AlarmAdapter(getActivity(), R.layout.alarm_list_item, alarm_data.toArray(alarmArray)));
						dialog.dismiss();
					}
				}).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {

					public void onClick(final DialogInterface dialog, final int which) {
						dialog.dismiss();

					}
				});

				aDialog.create().show();

				return true;
			}

		});
		return v;

	}

	@Override
	public void onResume() {
		super.onResume();

		// See if this service is enabled. If it is, disable the
		// "enable service" button
		boolean isEnabled = settings.getBoolean(Constants.SCHEDULER_ENABLED, false);

		serviceCheck.setChecked(isEnabled);
		serviceCheck.setText(isEnabled ? R.string.service_running : R.string.service_stopped);

		alarm_data = dbAdapter.fetchAllAlarms();
		alarmIDs = dbAdapter.fetchAllAlarmIDs();

		Alarm[] alarmArray = new Alarm[alarm_data.size()];

		alarmList.setAdapter(new AlarmAdapter(getActivity(), R.layout.alarm_list_item, alarm_data.toArray(alarmArray)));

	}

	@Override
	public void onPause() {
		super.onPause();

		alarmIDs.clear();
		alarm_data.clear();

	}

	public void onClick(final View parent) {

		int viewId = parent.getId();

		if (viewId == R.id.add_button) {
			Calendar cal = Calendar.getInstance();
			int currentTime = cal.getTime().getHours() * 60 + cal.getTime().getMinutes() / 10 * 10 + 5;

			final AlertDialog.Builder dialog = createAlarmDialog(currentTime, currentTime, false, false, false, false, false, -1, new boolean[] { true, true, true, true, true, true, true }, true, 0);

			dialog.show();

		}
	}

	public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {

		int viewId = parent.getId();

		if (viewId == R.id.alarm_list) {

			Alarm alarm = alarm_data.get(position);

			final AlertDialog.Builder dialog = createAlarmDialog(alarm.from, alarm.to, alarm.enableVibration, alarm.muteMedia, alarm.lockVolume, alarm.unmuteOnCall, alarm.disableNotiLight,
					alarm.brightness, new boolean[] { alarm.sunday, alarm.monday, alarm.tuesday, alarm.wednesday, alarm.thursday, alarm.friday, alarm.saturday }, false, alarmIDs.get(position));

			dialog.show();
		}

	}

	/**
	 * 
	 * Creates a new Dialog which contains the necessary items to create an alarm
	 * 
	 * @param from
	 * @param to
	 * @param enableVibration
	 * @param muteMedia
	 * @param lockVolume
	 * @param unmuteOnCall
	 * @param wdays
	 * 
	 * @param newAlarm
	 *            if the alarm is new or an updated one
	 * @param updateAlarmId
	 *            if the alarm is to be updated, this is its ID. This value can be set to anything if the alarm is new
	 * @return the new Dialog
	 */
	private android.app.AlertDialog.Builder createAlarmDialog(final int from, final int to, final boolean enableVibration, final boolean muteMedia, final boolean lockVolume,
			final boolean unmuteOnCall, final boolean disableNotificationLight, final int brightness, final boolean[] wdays, final boolean newAlarm, final int updateAlarmId) {

		int fromHours = from / 60;
		int fromMinutes = from % 60;

		int toHours = to / 60;
		int toMinutes = to % 60;

		final AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());

		View view = getActivity().getLayoutInflater().inflate(R.layout.alarm_popup, null);

		dialog.setView(view);

		/*
		if (newAlarm) {
			dialog.setTitle(R.string.add);
		} else {
			dialog.setTitle(R.string.edit);
		}*/
		dialog.setCancelable(true);

		ViewGroup v = (ViewGroup) view.findViewById(R.id.main_layout);

		// Loop through all views and set their typeface to Roboto Light
		for (int i = 0; i < v.getChildCount(); ++i) {
			View nextChild = v.getChildAt(i);
			if (nextChild instanceof CheckBox) {
				((CheckBox) nextChild).setTypeface(typefaceRobotoLight);
			} else if (nextChild instanceof TextView) {
				((TextView) nextChild).setTypeface(typefaceRobotoLight);
			} else if (nextChild instanceof ViewGroup) {
				for (int j = 0; j < ((ViewGroup) nextChild).getChildCount(); ++j) {
					View temp = ((ViewGroup) nextChild).getChildAt(j);
					if (temp instanceof CheckBox) {
						((CheckBox) temp).setTypeface(typefaceRobotoLight);
					} else if (temp instanceof TextView) {
						((TextView) temp).setTypeface(typefaceRobotoLight);
					}
				}
			}
		}

		final Button fromButton = (Button) view.findViewById(R.id.from_button);
		fromButton.setTypeface(typefaceRobotoLight);
		final Button toButton = (Button) view.findViewById(R.id.to_button);
		toButton.setTypeface(typefaceRobotoLight);

		fromButton.setText(Tools.fixTimeFormatting(fromHours, fromMinutes));
		toButton.setText(Tools.fixTimeFormatting(toHours, toMinutes));

		OnClickListener listener = new OnClickListener() {

			public void onClick(final View parent) {
				final AlertDialog.Builder nDialog = new AlertDialog.Builder(getActivity());

				View main = getActivity().getLayoutInflater().inflate(R.layout.time_dialog, null);

				nDialog.setView(main);
				// nDialog.setTitle(R.string.pick_time);
				nDialog.setCancelable(true);

				final MyTimePicker picker = (MyTimePicker) main.findViewById(R.id.picker);
				TextView tv = (TextView) main.findViewById(R.id.picker_hint);
				tv.setTypeface(typefaceRobotoLight);

				String timeString = ((Button) parent).getText().toString();

				picker.setHour(Integer.parseInt(timeString.substring(0, 2)));
				picker.setMinute(Integer.parseInt(timeString.substring(3)));

				nDialog.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

					public void onClick(final DialogInterface dialog, final int which) {
						((Button) parent).setText(Tools.getPaddedString(picker.getHour()) + ":" + Tools.getPaddedString(picker.getMinute()));

						dialog.dismiss();

					}
				});

				nDialog.show();

			}
		};

		fromButton.setOnClickListener(listener);
		toButton.setOnClickListener(listener);

		final CheckBox vibrationCheck = (CheckBox) view.findViewById(R.id.enable_vibr_check);
		vibrationCheck.setChecked(enableVibration);
		vibrationCheck.setTypeface(typefaceRobotoLight);

		final CheckBox mediaCheck = (CheckBox) view.findViewById(R.id.disable_media_check);
		mediaCheck.setChecked(muteMedia);
		mediaCheck.setTypeface(typefaceRobotoLight);

		final CheckBox lockCheck = (CheckBox) view.findViewById(R.id.lock_check);
		lockCheck.setChecked(lockVolume);
		lockCheck.setTypeface(typefaceRobotoLight);
        if(Tools.isLollipopOrLater()){
            lockCheck.setVisibility(View.GONE);
        }

		final CheckBox unmuteCheck = (CheckBox) view.findViewById(R.id.call_unmute_check);
		unmuteCheck.setChecked(unmuteOnCall);
		unmuteCheck.setTypeface(typefaceRobotoLight);

		final CheckBox notiLightCheck = (CheckBox) view.findViewById(R.id.light_check);
		notiLightCheck.setChecked(disableNotificationLight);
		notiLightCheck.setTypeface(typefaceRobotoLight);

		final CheckBox brightnessCheck = (CheckBox) view.findViewById(R.id.brightness_check);
		brightnessCheck.setChecked(brightness != -1);

		if (brightness == -1) {
			editor.putInt(Constants.SCHEDULER_BRIGHTNESS, -1);
		} else {
			editor.putInt(Constants.SCHEDULER_BRIGHTNESS, brightness);
		}
		editor.commit();

		brightnessCheck.setTypeface(typefaceRobotoLight);
		brightnessCheck.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
				if (isChecked) {
					AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

					final View v = getActivity().getLayoutInflater().inflate(R.layout.brightness_dialog, null);

					final TextView title = (TextView) v.findViewById(R.id.brightness_text);
					final SeekBar bar = (SeekBar) v.findViewById(R.id.brightness_slider);

					builder.setCancelable(false);

					bar.setMax(100);

					bar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

						public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) {
							title.setText(progress + "%");
						}

						public void onStopTrackingTouch(final SeekBar seekBar) {
							// Do nothing
						}

						public void onStartTrackingTouch(final SeekBar seekBar) {
							// Do nothing
						}
					});
					title.setTypeface(Tools.getTypefaceRobotoThin(getActivity()));

					builder.setView(v);

					builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

						public void onClick(final DialogInterface dialog, final int which) {
							editor.putInt(Constants.SCHEDULER_BRIGHTNESS, (int) (bar.getProgress() * 2.55));
							editor.commit();

							dialog.dismiss();
						}
					});

					if (brightness > 0) {
						bar.setProgress((int) (brightness / 2.55));
					} else {
						bar.setProgress(50);
					}

					builder.show();

				} else {
					editor.putInt(Constants.SCHEDULER_BRIGHTNESS, -1);
					editor.commit();
				}
			}

		});

		final CheckBox[] weekdayChecks = { (CheckBox) view.findViewById(R.id.sunday_check), (CheckBox) view.findViewById(R.id.monday_check), (CheckBox) view.findViewById(R.id.tuesday_check),
				(CheckBox) view.findViewById(R.id.wednesday_check), (CheckBox) view.findViewById(R.id.thursday_check), (CheckBox) view.findViewById(R.id.friday_check),
				(CheckBox) view.findViewById(R.id.saturday_check) };

		for (int i = 0; i < 7; i++) {
			weekdayChecks[i].setChecked(wdays[i]);
		}

		dialog.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

			public void onClick(final DialogInterface dialog, final int which) {
				String fromString = fromButton.getText().toString();
				String untilString = toButton.getText().toString();

				int fromHours = Integer.parseInt(fromString.substring(0, 2));
				int fromMinutes = Integer.parseInt(fromString.substring(3));

				int toHours = Integer.parseInt(untilString.substring(0, 2));
				int toMinutes = Integer.parseInt(untilString.substring(3));

				boolean vibChecked = vibrationCheck.isChecked();
				boolean mediaChecked = mediaCheck.isChecked();

				boolean lockChecked = lockCheck.isChecked();
				boolean unmuteChecked = unmuteCheck.isChecked();

				boolean notiLightChecked = notiLightCheck.isChecked();

				boolean[] wdays = new boolean[7];

				for (int i = 0; i < 7; i++) {
					wdays[i] = weekdayChecks[i].isChecked();
				}

				if (!newAlarm) {
					for (int i = 0; i < alarmIDs.size(); i++) {

						if (alarmIDs.get(i) == updateAlarmId) {
							alarmIDs.remove(i);
							alarm_data.remove(i);

							break;
						}

					}

				}

				int brightness = settings.getInt(Constants.SCHEDULER_BRIGHTNESS, 125);

				// Set the alarm.
				setAlarm(fromHours, fromMinutes, toHours, toMinutes, vibChecked, mediaChecked, lockChecked, unmuteChecked, notiLightChecked, brightness, wdays, newAlarm, updateAlarmId);

				// Save the current state of this activity in shared
				// preferences
				serviceCheck.setChecked(true);
				serviceCheck.setText(R.string.service_running);

				editor.putBoolean(Constants.SCHEDULER_ENABLED, true);

				editor.commit();
				dialog.dismiss();
			}
		});

		return dialog;
	}

	/**
	 * Sets an alarm pair and adds it to the list and database
	 * 
	 * @param fromHours
	 * @param fromMinutes
	 * @param toHours
	 * @param toMinutes
	 * @param enableVibration
	 * @param muteMedia
	 * @param wdays
	 * @param newAlarm
	 *            if the alarm is new or an updated one
	 * @param updateAlarmId
	 *            if the alarm is to be updated, this is its ID. This value can be set to anything if the alarm is new
	 */
	private void setAlarm(final int fromHours, final int fromMinutes, final int toHours, final int toMinutes, final boolean enableVibration, final boolean muteMedia, final boolean lockVolume,
			final boolean unmuteOnCall, final boolean disableNotificationLight, final int brightness, final boolean[] wdays, final boolean newAlarm, final int updateAlarmId) {

		int alarmId;
		Alarm alarm = new Alarm(fromHours * 60 + fromMinutes, toHours * 60 + toMinutes, enableVibration, muteMedia, lockVolume, unmuteOnCall, disableNotificationLight, brightness, wdays);
		alarm_data.add(alarm);

		Alarm[] alarmArray = new Alarm[alarm_data.size()];

		alarmList.setAdapter(new AlarmAdapter(getActivity(), R.layout.alarm_list_item, alarm_data.toArray(alarmArray)));

		if (newAlarm) {
			alarmId = settings.getInt(Constants.SCHEDULER_MAX_ALARM_ID, 0) + 2;
			dbAdapter.createAlarm(alarmId, alarm);

			editor.putInt(Constants.SCHEDULER_MAX_ALARM_ID, alarmId);
			editor.commit();

		} else {

			alarmId = updateAlarmId;
			dbAdapter.updateAlarm(alarmId, alarm);
		}

		alarmIDs.add(alarmId);

		Tools.setAlarm(getActivity(), dbAdapter, alarm, alarmId);

	}

	public void onCheckedChanged(final CompoundButton parent, final boolean isChecked) {

		int viewId = parent.getId();

		// If the event is the user starting the service
		if (viewId == R.id.service_check) {

			serviceCheck.setText(isChecked ? R.string.service_running : R.string.service_stopped);

			editor.putBoolean(Constants.SCHEDULER_ENABLED, isChecked);
			editor.commit();

			// If the event is the user stopping the service
			if (!isChecked) {

				// Give the user visual confirmation that the service has been
				// stopped

				editor.putBoolean(Constants.SCHEDULER_SOUND_MUTED, false);
				editor.commit();

			}

		}
	}
}
