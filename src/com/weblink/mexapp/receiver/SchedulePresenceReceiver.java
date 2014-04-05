package com.weblink.mexapp.receiver;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import com.weblink.mexapp.interfaces.WCStatusListener;
import com.weblink.mexapp.pojo.User;
import com.weblink.mexapp.utility.Constants;
import com.weblink.mexapp.utility.Task;
import com.weblink.mexapp.utility.Tools;

public class SchedulePresenceReceiver extends BroadcastReceiver implements WCStatusListener {

	private Editor editor;
	private Context context;

	@SuppressLint("NewApi")
	@Override
	public void onReceive(final Context context, final Intent intent) {

		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		editor = sharedPrefs.edit();
		this.context = context;

		User user = new User(sharedPrefs.getString(Constants.LOGIN_COMPANY, ""), sharedPrefs.getString(Constants.LOGIN_PASSWORD, ""), sharedPrefs.getInt(Constants.LOGIN_EXTENSION, 0));
		int defaultStatusID = sharedPrefs.getInt(Constants.DEFAULT_PRESENCE_ID, 0);

		// Initialize and launch AsyncTasks to reset presence and DND
		Task.SetWCStatusTask task = new Task.SetWCStatusTask(user, defaultStatusID, 0, null, this);
		Task.SetDNDTask dndTask = new Task.SetDNDTask(user, false, context, sharedPrefs.edit());
		if (Tools.isHoneycombOrLater()) {
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			dndTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		} else {
			task.execute();
			dndTask.execute();
		}

		// Set to -1 to avoid running at device startup again
		editor.putLong(Constants.SCHEDULE_PRESENCE_MILLIS, -1);
		editor.commit();

	}

	@Override
	public void onReceiveWCStatus(final String[] possibleMessages, final int[] possibleMessageIDs) {
		// DO NOTHING
	}

	@Override
	public Editor getPrefEditor() {
		return editor;
	}

	@Override
	public Context getContext() {
		return context;
	}
}
