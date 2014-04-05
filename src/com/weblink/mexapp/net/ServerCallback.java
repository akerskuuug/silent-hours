package com.weblink.mexapp.net;

import io.socket.IOAcknowledge;
import io.socket.IOCallback;
import io.socket.SocketIO;
import io.socket.SocketIOException;

import java.util.ArrayList;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.util.Log;

import com.weblink.mexapp.interfaces.SIOEventListener;
import com.weblink.mexapp.pojo.ContactHolder;
import com.weblink.mexapp.pojo.User;
import com.weblink.mexapp.utility.Constants;
import com.weblink.mexapp.utility.Tools;

public class ServerCallback implements IOCallback {
	private final SocketIO socket;
	private final ArrayList<SIOEventListener> observers;
	private final Editor editor;
	private final User user;
	private final SharedPreferences sharedPrefs;

	public ServerCallback(final SocketIO socket, final User user, final SharedPreferences sharedPrefs) {
		this.socket = socket;
		this.sharedPrefs = sharedPrefs;
		editor = this.sharedPrefs.edit();
		this.user = user;

		observers = new ArrayList<SIOEventListener>();

	}

	public void registerObserver(final SIOEventListener observer) {
		if (!observers.contains(observer)) {
			observers.add(observer);
		}
	}

	public void unregisterObserver(final SIOEventListener observer) {
		if (observers.contains(observer)) {
			observers.remove(observer);
		}
	}

	@Override
	public void onMessage(final JSONObject json, final IOAcknowledge ack) {
		try {
			Log.d("SocketIO callback", "Server said:" + json.toString(2));
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onMessage(final String data, final IOAcknowledge ack) {
		Log.d("SocketIO callback", "Server said: " + data);
	}

	@Override
	public void onError(final SocketIOException socketIOException) {
		Log.e("SocketIO error callback", "An Error occured: " + socketIOException.getLocalizedMessage());
		socketIOException.printStackTrace();
	}

	@Override
	public void onDisconnect() {
		Log.d("SocketIO callback", "Connection terminated.");
	}

	@SuppressLint("NewApi")
	@Override
	public void onConnect() {
		GetHashTask task = new GetHashTask(user);

		if (Tools.isHoneycombOrLater()) {
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		} else {
			task.execute();
		}

	}

	@Override
	public void on(final String event, final IOAcknowledge ack, final Object... args) {
		JSONArray array = null;
		JSONObject obj = null;

		if (args.length > 0) {
			if (args[0] instanceof JSONArray) {
				array = (JSONArray) args[0];
			} else if (args[0] instanceof JSONObject) {
				obj = (JSONObject) args[0];
			}
		} else {
			return;
		}

		if (event.equals("status")) {
			try {
				ContactHolder holder = new ContactHolder("", "", "", obj.getInt("s"), obj.getInt("p"), "");
				for (SIOEventListener observer : observers) {
					observer.onReceiveStatus(holder);
				}
			} catch (JSONException e) {
				Log.d("ServerCallback.on()", "Error: " + e.getLocalizedMessage());
			}

		} else if (event.equals("calls")) {

			SIOEventListener temp = observers.get(0);
			boolean success = temp.getDbAdapter().createCallsFromResponse(array, temp.getMainActivity());

			for (SIOEventListener observer : observers) {
				observer.onReceiveCall(success);
			}

		} else if (event.equals("wcstatus")) {
			try {

				String[] possibleWCMessageIDs = sharedPrefs.getString(Constants.POSS_WC_MESSAGE_IDS, "").split("//");
				String[] possibleWCMessages = sharedPrefs.getString(Constants.POSS_WC_MESSAGES, "").split("//");

				int status = obj.has("v") ? obj.getInt("v") : -1;

				for (int i = 0; i < possibleWCMessageIDs.length; i++) {

					if (status == Integer.parseInt(possibleWCMessageIDs[i])) {
						ContactHolder holder = new ContactHolder("", "", "", -1, obj.getInt("p"), obj.has("text") ? obj.getString("text") : possibleWCMessages[i]);
						for (SIOEventListener observer : observers) {
							observer.onReceiveStatus(holder);
						}
						break;
					}

				}

			} catch (JSONException e) {
				Log.d("ServerCallback.on()", "Error: " + e.getLocalizedMessage());
			}
		}

	}

	public class GetHashTask extends AsyncTask<Void, Void, Boolean> {
		private final User user;
		private String hash;

		public GetHashTask(final User user) {
			super();
			this.user = user;
		}

		@Override
		protected Boolean doInBackground(final Void... params) {

			ArrayList<NameValuePair> nvps = new ArrayList<NameValuePair>();
			nvps.add(new BasicNameValuePair("g", "hash"));

			JSONObject temp = Tools.createJSONObject(nvps);
			String response = HTTPResources.performAction(user, temp);
			isSuccessfulLogin(response);

			try {
				JSONArray arr = Tools.convertToJSONArray(new String[] { "status", "calls", "wcstatus" });

				socket.emit("login", new JSONObject().put("hash", hash).put("events", arr));
			} catch (JSONException ex) {
				Log.e("ServerCallback onConnect Error", ex.getLocalizedMessage());
			}

			return true;
		}

		private boolean isSuccessfulLogin(final String response) {
			if (response != null) {
				try {
					JSONArray json = new JSONArray(response);
					JSONObject obj = (JSONObject) json.get(0);

					return isLoginSuccessful(obj);

				} catch (JSONException e) {
					Log.e("LoginActivity | Parsing to JSONArray failed", e.getLocalizedMessage());
					return false;
				}
			}
			return false;
		}

		private boolean isLoginSuccessful(final JSONObject obj) throws JSONException {
			// Check if the login was successful
			if (obj.has("s") && obj.get("s").equals("ok") && obj.has("hash")) {
				String sHash = obj.get("hash").toString();
				hash = sHash;

				if (sHash.length() == 128) {
					// Save the hash
					editor.putString(Constants.LOGIN_HASH, sHash);
					editor.commit();

					return true;
				} else {
					return false;
				}

			} else {
				return false;
			}
		}
	}
}