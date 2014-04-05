package com.weblink.mexapp.services;

import java.util.ArrayList;

import wei.mark.standout.StandOutWindow;
import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.weblink.mexapp.db.MexDbAdapter;
import com.weblink.mexapp.interfaces.CallListener;
import com.weblink.mexapp.pojo.CallHolder;
import com.weblink.mexapp.pojo.User;
import com.weblink.mexapp.utility.Constants;
import com.weblink.mexapp.utility.Task;
import com.weblink.mexapp.utility.Tools;

public class CallReceiverService extends Service implements CallListener {

	private static final String TAG = "CallReceiverService";
	private MexDbAdapter dbAdapter;
	private SharedPreferences sharedPrefs;

	@Override
	public void onCreate() {
		super.onCreate();

		dbAdapter = new MexDbAdapter(this);
		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
	}

	@SuppressLint("NewApi")
	@Override
	public int onStartCommand(final Intent intent, final int flags, final int startId) {
		super.onStartCommand(intent, flags, startId);

		final User user = new User(sharedPrefs.getString(Constants.LOGIN_COMPANY, ""), sharedPrefs.getString(Constants.LOGIN_PASSWORD, ""), sharedPrefs.getInt(Constants.LOGIN_EXTENSION, 0));

		if (isNetworkAvailable()) {

			dbAdapter.open();
			Task.GetCallTaskDelayed task = new Task.GetCallTaskDelayed(user, CallReceiverService.this);

			if (Tools.isHoneycombOrLater()) {
				task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			} else {
				task.execute();
			}

		}
		return START_STICKY;
	}

	@Override
	public IBinder onBind(final Intent arg0) {
		Log.d(TAG, "in onBind()");
		return null;
	}

	public boolean isNetworkAvailable() {
		ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
		return activeNetworkInfo != null && activeNetworkInfo.isConnected();
	}

	@Override
	public void onReceiveEvent(final boolean success) {
		// Check if window is already displayed
		boolean alreadyDisplayed = sharedPrefs.getBoolean(Constants.CALL_WINDOW_DISPLAYED, false);
		// Check if call is held
		boolean callIsHeld = sharedPrefs.getBoolean(Constants.CALL_HELD, false);

		ArrayList<CallHolder> calls = dbAdapter.fetchAllCalls();
		if (!alreadyDisplayed && !calls.isEmpty() && !callIsHeld) {
			// Show floating window
			StandOutWindow.closeAll(getApplicationContext(), FloatingCallWindow.class);
			StandOutWindow.show(getApplicationContext(), FloatingCallWindow.class, StandOutWindow.DEFAULT_ID);

		} else if (!calls.isEmpty() && !callIsHeld) {
			Bundle data = new Bundle();

			// Get the first call
			CallHolder holder = calls.get(0);

			// Get contact name and extension
			String contactName = holder.getContactName();
			String contactExtension = holder.getContact().getExtension();

			// Put name and extension into data bundle
			data.putString(MexDbAdapter.KEY_CONTACT_NAME, contactName);
			data.putString(MexDbAdapter.KEY_EXTENSION, contactExtension);

			// Send data bundle to open window
			StandOutWindow.sendData(getApplicationContext(), FloatingCallWindow.class, StandOutWindow.DEFAULT_ID, FloatingCallWindow.DATA_CALL_STATUS_UPDATED, data, FloatingCallWindow.class,
					StandOutWindow.DEFAULT_ID);

		}

		// Close database
		dbAdapter.close();
	}

	@Override
	public MexDbAdapter getDbAdapter() {

		return dbAdapter;
	}

	@Override
	public Context getContext() {
		return this;
	}

}
