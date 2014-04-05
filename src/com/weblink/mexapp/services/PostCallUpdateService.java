package com.weblink.mexapp.services;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.weblink.mexapp.db.MexDbAdapter;
import com.weblink.mexapp.interfaces.CallListener;
import com.weblink.mexapp.pojo.User;
import com.weblink.mexapp.receiver.ScheduleMessageUpdateReceiver;
import com.weblink.mexapp.utility.Constants;
import com.weblink.mexapp.utility.Task;
import com.weblink.mexapp.utility.Tools;

public class PostCallUpdateService extends Service implements CallListener {

	private MexDbAdapter dbAdapter;

	@SuppressLint("NewApi")
	@Override
	public int onStartCommand(final Intent intent, final int flags, final int startId) {
		super.onStartCommand(intent, flags, startId);

		new VMStatusTask(this).execute();

		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		User user = new User(sharedPrefs.getString(Constants.LOGIN_COMPANY, ""), sharedPrefs.getString(Constants.LOGIN_PASSWORD, ""), sharedPrefs.getInt(Constants.LOGIN_EXTENSION, 0));

		dbAdapter = new MexDbAdapter(this);
		dbAdapter.open();

		Task.GetCallTask task = new Task.GetCallTask(user, this);
		if (Tools.isHoneycombOrLater()) {
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		} else {
			task.execute();
		}

		return START_STICKY;
	}

	@Override
	public IBinder onBind(final Intent intent) {
		Log.d("onBind()", "in onBind()");
		return null;
	}

	public static class VMStatusTask extends AsyncTask<Void, Void, Void> {

		private final Context context;

		public VMStatusTask(final Context context) {
			super();
			this.context = context;
		}

		@Override
		protected Void doInBackground(final Void... params) {

			try {
				// Wait 20 seconds to let the remote peer say their message
				Thread.sleep(20000);
			} catch (InterruptedException e) {
				Log.e("SystemCallReceiver sleep interrupted", e.getLocalizedMessage());
			}

			// See if any new voicemail are available
			ScheduleMessageUpdateReceiver.update(context);

			return null;
		}

	}

	@Override
	public void onReceiveEvent(final boolean success) {

		boolean showNotification = Tools.getIntPreference(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()), Constants.SETTING_CALL_WINDOW, Constants.CALL_WINDOW_NONE) == Constants.CALL_WINDOW_NONE;
		if (showNotification) {
			if (dbAdapter.fetchAllCalls().isEmpty()) {
				Tools.Notifications.dismissCallNotification(this);
			} else {
				Tools.Notifications.showCallNotification(this);
			}
		}

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
