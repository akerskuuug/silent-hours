package com.weblink.mexapp.services;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.weblink.mexapp.interfaces.VoicemailListener;
import com.weblink.mexapp.pojo.User;
import com.weblink.mexapp.utility.Constants;
import com.weblink.mexapp.utility.Task;
import com.weblink.mexapp.utility.Tools;

public class VoicemailUpdateService extends Service implements VoicemailListener {

	private static final String TAG = "VoicemailUpdateService";
	SharedPreferences sharedPrefs;

	@Override
	public void onCreate() {
		super.onCreate();

	}

	@SuppressLint("NewApi")
	@Override
	public int onStartCommand(final Intent intent, final int flags, final int startId) {
		super.onStartCommand(intent, flags, startId);

		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		final User user = new User(sharedPrefs.getString(Constants.LOGIN_COMPANY, ""), sharedPrefs.getString(Constants.LOGIN_PASSWORD, ""),
				sharedPrefs.getInt(Constants.LOGIN_EXTENSION, 0));

		if (isNetworkAvailable()) {
			Task.GetVoicemailTask task = new Task.GetVoicemailTask(user, this);
			if (Tools.isHoneycombOrLater()) {
				task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			} else {

				task.execute();
			}

		}
		return START_STICKY;
	}

	public boolean isNetworkAvailable() {
		ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
		return activeNetworkInfo != null && activeNetworkInfo.isConnected();
	}

	@Override
	public IBinder onBind(final Intent arg0) {
		Log.d(TAG, "in onBind()");
		return null;
	}

	@Override
	public void onReceiveVoicemail(final boolean hasVM) {
		// Do nothing
	}

	@Override
	public Context getContext() {
		return this;
	}

}
