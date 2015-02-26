package com.silenthoursnew.core;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.silenthoursnew.R;
import com.silenthoursnew.utility.Tools;

public class AlarmAdapter extends ArrayAdapter<Alarm> {

	Context context;
	int layoutResourceId;
	Alarm data[] = null;

	public AlarmAdapter(final Context context, final int layoutResourceId, final Alarm[] data) {
		super(context, layoutResourceId, data);
		this.layoutResourceId = layoutResourceId;
		this.context = context;
		this.data = data;

	}

	@Override
	public View getView(final int position, final View convertView, final ViewGroup parent) {
		View row = convertView;
		AlarmHolder holder = null;

		if (row == null) {

			Typeface robotoLight = Tools.getTypefaceRobotoLight(context);
			Typeface robotoRegular = Tools.getTypefaceRobotoRegular(context);
			Typeface robotoThin = Tools.getTypefaceRobotoThin(context);

			LayoutInflater inflater = ((Activity) context).getLayoutInflater();
			row = inflater.inflate(layoutResourceId, parent, false);

			holder = new AlarmHolder();
			holder.from = (TextView) row.findViewById(R.id.from_label);
			holder.from.setTypeface(robotoThin);

			holder.and = (TextView) row.findViewById(R.id.and_label);
			holder.and.setTypeface(robotoLight);

			holder.to = (TextView) row.findViewById(R.id.to_label);
			holder.to.setTypeface(robotoThin);

			holder.vibration = (TextView) row.findViewById(R.id.disable_vibr_label);
			holder.vibration.setTypeface(robotoLight);
			holder.media = (TextView) row.findViewById(R.id.disable_media_label);
			holder.media.setTypeface(robotoLight);
			holder.lock = (TextView) row.findViewById(R.id.lock_volume_label);
			holder.lock.setTypeface(robotoLight);
			holder.unmuteOnCall = (TextView) row.findViewById(R.id.unmute_label);
			holder.unmuteOnCall.setTypeface(robotoLight);
			holder.brightness = (TextView) row.findViewById(R.id.brightness_label);
			holder.brightness.setTypeface(robotoLight);

			holder.notiLight = (TextView) row.findViewById(R.id.disable_light_label);
			holder.notiLight.setTypeface(robotoLight);

			holder.monday = (TextView) row.findViewById(R.id.monday_label);
			holder.monday.setTypeface(robotoLight);
			holder.tuesday = (TextView) row.findViewById(R.id.tuesday_label);
			holder.tuesday.setTypeface(robotoLight);
			holder.wednesday = (TextView) row.findViewById(R.id.wednesday_label);
			holder.wednesday.setTypeface(robotoLight);
			holder.thursday = (TextView) row.findViewById(R.id.thursday_label);
			holder.thursday.setTypeface(robotoLight);
			holder.friday = (TextView) row.findViewById(R.id.friday_label);
			holder.friday.setTypeface(robotoLight);
			holder.saturday = (TextView) row.findViewById(R.id.saturday_label);
			holder.saturday.setTypeface(robotoLight);
			holder.sunday = (TextView) row.findViewById(R.id.sunday_label);
			holder.sunday.setTypeface(robotoLight);

			row.setTag(holder);
		} else {
			holder = (AlarmHolder) row.getTag();
		}

		Alarm alarm = data[position];

		holder.from.setText(Tools.getPaddedString(alarm.from / 60) + ":" + Tools.getPaddedString(alarm.from % 60));

		holder.to.setText(Tools.getPaddedString(alarm.to / 60) + ":" + Tools.getPaddedString(alarm.to % 60));

		int primColorId = context.getResources().getColor(android.R.color.secondary_text_light);

		if (!alarm.enableVibration) {
			holder.vibration.setTextColor(Color.parseColor("#c0c0c0"));
		} else {
			holder.vibration.setTextColor(primColorId);
		}

		if (!alarm.muteMedia) {
			holder.media.setTextColor(Color.parseColor("#c0c0c0"));
		} else {
			holder.media.setTextColor(primColorId);
		}

		if (!alarm.lockVolume) {
			holder.lock.setTextColor(Color.parseColor("#c0c0c0"));
		} else {
			holder.lock.setTextColor(primColorId);
		}

		if (!alarm.unmuteOnCall) {
			holder.unmuteOnCall.setTextColor(Color.parseColor("#c0c0c0"));
		} else {
			holder.unmuteOnCall.setTextColor(primColorId);
		}

		if (!alarm.disableNotiLight) {
			holder.notiLight.setTextColor(Color.parseColor("#c0c0c0"));
		} else {
			holder.notiLight.setTextColor(primColorId);
		}

		if (alarm.brightness == -1) {
			holder.brightness.setTextColor(Color.parseColor("#c0c0c0"));
		} else {
			holder.brightness.setTextColor(primColorId);
		}

		//

		// Weekdays
		if (!alarm.monday) {
			holder.monday.setTextColor(Color.parseColor("#c0c0c0"));
		} else {
			holder.monday.setTextColor(primColorId);
		}

		if (!alarm.tuesday) {
			holder.tuesday.setTextColor(Color.parseColor("#c0c0c0"));
		} else {
			holder.tuesday.setTextColor(primColorId);
		}

		if (!alarm.wednesday) {
			holder.wednesday.setTextColor(Color.parseColor("#c0c0c0"));
		} else {
			holder.wednesday.setTextColor(primColorId);
		}

		if (!alarm.thursday) {
			holder.thursday.setTextColor(Color.parseColor("#c0c0c0"));
		} else {
			holder.thursday.setTextColor(primColorId);
		}

		if (!alarm.friday) {
			holder.friday.setTextColor(Color.parseColor("#c0c0c0"));
		} else {
			holder.friday.setTextColor(primColorId);
		}

		if (!alarm.saturday) {
			holder.saturday.setTextColor(Color.parseColor("#c0c0c0"));
		} else {
			holder.saturday.setTextColor(primColorId);
		}

		if (!alarm.sunday) {
			holder.sunday.setTextColor(Color.parseColor("#c0c0c0"));
		} else {
			holder.sunday.setTextColor(primColorId);
		}

		return row;
	}

	static class AlarmHolder {

		TextView from, to, and;

		TextView vibration, media, lock, unmuteOnCall, notiLight, brightness;

		TextView monday, tuesday, wednesday, thursday, friday, saturday, sunday;

	}

}
