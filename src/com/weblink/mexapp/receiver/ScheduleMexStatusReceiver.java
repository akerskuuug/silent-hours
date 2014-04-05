package com.weblink.mexapp.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.weblink.mexapp.interfaces.MexStatusListener;
import com.weblink.mexapp.pojo.User;
import com.weblink.mexapp.utility.Constants;
import com.weblink.mexapp.utility.Task;

public class ScheduleMexStatusReceiver extends BroadcastReceiver implements MexStatusListener {
	Context context;

	@Override
	public void onReceive(final Context context, final Intent intent) {

		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

		User user = new User(sharedPrefs.getString(Constants.LOGIN_COMPANY, ""), sharedPrefs.getString(Constants.LOGIN_PASSWORD, ""), sharedPrefs.getInt(Constants.LOGIN_EXTENSION, 0));

		this.context = context;

		new Task.SetMexStatusTask(user, true, this).execute();

	}

	@Override
	public void onReceiveMexStatus(final boolean mexStatus) {
		// Do nothing
	}

	@Override
	public Context getContext() {
		return context;
	}

}
