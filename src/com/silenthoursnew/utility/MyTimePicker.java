package com.silenthoursnew.utility;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.silenthoursnew.R;

public class MyTimePicker extends LinearLayout {

	private final ListView hourList;
	private final LayoutInflater layoutInflater;
	private int selectedHourPos;
	private final ListView minuteList;
	private int selectedMinutePos;
	private final View mainView;
	private final String[] hours;
	private final String[] minutes;
	private int hour, minute;

	public MyTimePicker(final Context context, final AttributeSet attrs) {
		super(context, attrs);

		layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mainView = layoutInflater.inflate(R.layout.my_time_picker, this);

		hourList = (ListView) findViewById(R.id.hour_list);
		hourList.setAdapter(new ArrayAdapter<String>(context, R.layout.spinner_item, context.getResources().getStringArray(
				R.array.scheduler_hour_array)));

		hours = context.getResources().getStringArray(R.array.scheduler_hour_array);
		minutes = context.getResources().getStringArray(R.array.scheduler_minute_array);

		Button hourAddButton = (Button) findViewById(R.id.hour_add_button);
		hourAddButton.setOnClickListener(new OnClickListener() {

			public void onClick(final View v) {

				if (selectedHourPos < hourList.getCount() - 1) {
					hourList.setSelection(selectedHourPos + 1);
				} else {
					hourList.setSelection(0);
				}
			}
		});

		Button hourSubtractButton = (Button) findViewById(R.id.hour_subtract_button);
		hourSubtractButton.setOnClickListener(new OnClickListener() {

			public void onClick(final View v) {

				if (selectedHourPos > 0) {
					hourList.setSelection(selectedHourPos - 1);
				} else {

					hourList.setSelection(hourList.getCount() - 1);
				}
			}
		});

		minuteList = (ListView) findViewById(R.id.minute_list);
		minuteList.setAdapter(new ArrayAdapter<String>(context, R.layout.spinner_item, context.getResources().getStringArray(
				R.array.scheduler_minute_array)));

		Button minuteAddButton = (Button) findViewById(R.id.minute_add_button);
		minuteAddButton.setOnClickListener(new OnClickListener() {

			public void onClick(final View v) {

				if (selectedMinutePos < minuteList.getCount() - 1) {
					minuteList.setSelection(selectedMinutePos + 1);
				} else {
					minuteList.setSelection(0);
				}

			}
		});

		Button minuteSubtractButton = (Button) findViewById(R.id.minute_subtract_button);
		minuteSubtractButton.setOnClickListener(new OnClickListener() {

			public void onClick(final View v) {
				if (selectedMinutePos > 0) {
					minuteList.setSelection(selectedMinutePos - 1);
				} else {
					minuteList.setSelection(minuteList.getCount() - 1);
				}
			}
		});

	}

	@Override
	public boolean onInterceptTouchEvent(final MotionEvent event) {
		super.onInterceptTouchEvent(event);

		selectedHourPos = hourList.getFirstVisiblePosition();

		hourList.setSelection(selectedHourPos);

		selectedMinutePos = minuteList.getFirstVisiblePosition();

		minuteList.setSelection(selectedMinutePos);

		return false;

	}

	/**
	 * Getter for the hour value
	 * 
	 * @return the current hour shown by this picker
	 */
	public int getHour() {
		return Integer.parseInt(hourList.getItemAtPosition(hourList.getFirstVisiblePosition()).toString());
	}

	/**
	 * Getter for the minute value
	 * 
	 * @return the current minute shown by this picker
	 */
	public int getMinute() {
		return Integer.parseInt(minuteList.getItemAtPosition(minuteList.getFirstVisiblePosition()).toString());
	}

	/**
	 * Sets the hour selection to the item corresponding to the parameter value
	 * 
	 * @param value
	 *            the desired hour
	 * 
	 * @throws IndexOutOfBoundsException
	 *             if an invalid hour value is supplied
	 */
	public void setHour(final int value) {
		if (value < 24 && value >= 0) {
			for (int i = 0; i < hours.length; i++) {
				if (value == Integer.parseInt(hours[i])) {
					hourList.setSelection(i);
					break;
				}
			}
		} else {
			throw new IndexOutOfBoundsException("Incorrect hour number, the hours of the day are numbered 0 through 23");
		}
	}

	/**
	 * Sets the minute selection to the item corresponding to the parameter value
	 * 
	 * @param value
	 *            the desired minute
	 * 
	 * @throws IndexOutOfBoundsException
	 *             if an invalid minute value is supplied
	 */
	public void setMinute(final int value) {
		if (value < 60 && value >= 0) {
			for (int i = 0; i < minutes.length; i++) {
				if (value == Integer.parseInt(minutes[i])) {
					minuteList.setSelection(i);
					break;
				}
			}
		} else {
			throw new IndexOutOfBoundsException("Incorrect minute number, the minutes are numbered 0 through 59");
		}
	}

}
