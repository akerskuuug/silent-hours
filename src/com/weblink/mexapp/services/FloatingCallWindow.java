package com.weblink.mexapp.services;

import java.util.ArrayList;
import java.util.Locale;

import wei.mark.standout.StandOutWindow;
import wei.mark.standout.constants.StandOutFlags;
import wei.mark.standout.ui.Window;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract.PhoneLookup;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.weblink.mexapp.R;
import com.weblink.mexapp.activity.MainActivity;
import com.weblink.mexapp.db.MexDbAdapter;
import com.weblink.mexapp.interfaces.CallListener;
import com.weblink.mexapp.pojo.CallHolder;
import com.weblink.mexapp.pojo.User;
import com.weblink.mexapp.utility.Constants;
import com.weblink.mexapp.utility.Task;
import com.weblink.mexapp.utility.Task.CallActionTask;
import com.weblink.mexapp.utility.Tools;

public class FloatingCallWindow extends StandOutWindow implements CallListener {

	public static final int DATA_CALL_STATUS_UPDATED = 0;
	private SharedPreferences sharedPrefs;
	private Editor editor;
	private String callID = "";
	private CallHolder call;
	private MexDbAdapter dbAdapter;
	private TextView extensionText;
	private TextView contactNameText;

	@Override
	public String getAppName() {
		return getString(R.string.app_name);
	}

	@Override
	public int getAppIcon() {
		return R.drawable.ic_launcher;
	}

	@Override
	public void createAndAttachView(final int id, final FrameLayout frame) {
		final Context applicationContext = getApplicationContext();

		// Get shared preferences and editor
		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(applicationContext);
		editor = sharedPrefs.edit();

		dbAdapter = new MexDbAdapter(getApplicationContext());
		dbAdapter.open();

		initializeViews(id, frame, applicationContext);

	}

	@SuppressLint("NewApi")
	private void initializeViews(final int id, final FrameLayout frame, final Context applicationContext) {

		if (dbAdapter != null && dbAdapter.isOpen()) {

			ArrayList<CallHolder> calls = dbAdapter.fetchAllCalls();
			if (calls == null || calls.size() <= 0) {

				LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
				final View view = inflater.inflate(R.layout.floating_window_call, frame, true);

				// If the call list is empty, dismiss this popup
				dismiss();
			} else {
				// Disallow showing of new windows
				editor.putBoolean(Constants.CALL_WINDOW_DISPLAYED, true).commit();

				call = calls.get(0);
				callID = call.getCallId1();

				final User user = new User(sharedPrefs.getString(Constants.LOGIN_COMPANY, ""), sharedPrefs.getString(Constants.LOGIN_PASSWORD, ""), sharedPrefs.getInt(Constants.LOGIN_EXTENSION, 0));

				// create a new layout from floating_window_call.xml
				LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
				final View view = inflater.inflate(R.layout.floating_window_call, frame, true);

				ImageView callImage = (ImageView) view.findViewById(R.id.call_image);

				// Get content resolver to query for contact image
				ContentResolver resolver = getContentResolver();

				Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(call.getContact().getExtension()));
				Cursor contactCursor = resolver.query(uri, new String[] { PhoneLookup._ID }, null, null, null);
				if (contactCursor != null && contactCursor.moveToFirst()) {
					long contactId = contactCursor.getLong(0);
					Bitmap contactImage = Tools.loadContactPhoto(resolver, contactId);
					callImage.setImageBitmap(contactImage);
				} else {

					callImage.setImageBitmap(Tools.getDefaultContactPhoto(applicationContext));
				}
				contactCursor.close();

				contactNameText = (TextView) view.findViewById(R.id.contact_name);

				contactNameText.setText(call == null ? "" : call.getContact().getName());

				extensionText = (TextView) view.findViewById(R.id.contact_extension);
				extensionText.setText(call == null ? "" : call.getContact().getExtension());

				Button closeButton = (Button) view.findViewById(R.id.close_button);
				closeButton.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(final View v) {
						// Dismiss this popup
						dismiss();
					}
				});

				final Button holdButton = (Button) view.findViewById(R.id.hold_button);
				final Button resumeButton = (Button) view.findViewById(R.id.resume_button);
				holdButton.setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(final View v) {
						editor.putBoolean(Constants.CALL_HELD, true).commit();
						CallActionTask holdTask = new Task.CallActionTask(user, CallActionTask.CALL_HOLD, callID, FloatingCallWindow.this);

						if (Tools.isHoneycombOrLater()) {
							holdTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
						} else {
							holdTask.execute();
						}

						resumeButton.setVisibility(View.VISIBLE);
						v.setVisibility(View.GONE);
					}
				});

				resumeButton.setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(final View v) {
						editor.putBoolean(Constants.CALL_HELD, false).commit();
						CallActionTask resumeTask = new Task.CallActionTask(user, CallActionTask.CALL_RESUME, callID, FloatingCallWindow.this);

						if (Tools.isHoneycombOrLater()) {
							resumeTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
						} else {
							resumeTask.execute();
						}

						holdButton.setVisibility(View.VISIBLE);
						v.setVisibility(View.GONE);
					}
				});

				Button transferButton = (Button) view.findViewById(R.id.transfer_button);
				transferButton.setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(final View v) {
						// Launch main application for call transfer
						startActivity(getPersistentNotificationIntent(id));

						// Dismiss the dialog
						dismiss();

					}
				});

				Button hangupButton = (Button) view.findViewById(R.id.reject_button);
				hangupButton.setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(final View v) {
						editor.putBoolean(Constants.CALL_HELD, false).commit();
						CallActionTask hangupTask = new Task.CallActionTask(user, CallActionTask.CALL_HANGUP, callID, FloatingCallWindow.this);

						if (Tools.isHoneycombOrLater()) {
							hangupTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
						} else {
							hangupTask.execute();
						}

					}
				});

			}
		} else {
			// Dismiss dialog
			dismiss();
		}
	}

	// every window is initially same size
	@Override
	public StandOutLayoutParams getParams(final int id, final Window window) {

		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		int windowWidth = sharedPrefs.getInt(Constants.WINDOW_WIDTH, 600);
		int windowHeight = sharedPrefs.getInt(Constants.WINDOW_HEIGHT, 600);

		int windowPositionPreference = Tools.getIntPreference(sharedPrefs, Constants.SETTING_CALL_WINDOW, Constants.CALL_WINDOW_CENTER);

		switch (windowPositionPreference) {
		case Constants.CALL_WINDOW_TOP:
			return new StandOutLayoutParams(id, windowWidth, (int) (windowHeight * 0.2), StandOutLayoutParams.CENTER, StandOutLayoutParams.TOP);
		case Constants.CALL_WINDOW_BOTTOM:
			return new StandOutLayoutParams(id, windowWidth, (int) (windowHeight * 0.2), StandOutLayoutParams.CENTER, StandOutLayoutParams.BOTTOM);
		default:
			return new StandOutLayoutParams(id, (int) (windowWidth * 0.9), (int) (windowHeight * 0.2), StandOutLayoutParams.CENTER, StandOutLayoutParams.CENTER);
		}

	}

	@Override
	public int getFlags(final int id) {
		return StandOutFlags.FLAG_BODY_MOVE_ENABLE | StandOutFlags.FLAG_WINDOW_BRING_TO_FRONT_ON_TAP | StandOutFlags.FLAG_WINDOW_EDGE_LIMITS_ENABLE | StandOutFlags.FLAG_WINDOW_PINCH_RESIZE_ENABLE
				| StandOutFlags.FLAG_WINDOW_ASPECT_RATIO_ENABLE;
	}

	@Override
	public String getPersistentNotificationTitle(final int id) {
		return getString(R.string.active_calls);
	}

	@Override
	public String getPersistentNotificationMessage(final int id) {
		return getString(R.string.active_calls_handle);
	}

	// return an Intent that creates a new MultiWindow
	@Override
	public Intent getPersistentNotificationIntent(final int id) {
		Intent intent = new Intent(FloatingCallWindow.this, MainActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		return intent;
	}

	// return an Intent that restores the MultiWindow
	@Override
	public Intent getHiddenNotificationIntent(final int id) {
		return new Intent(FloatingCallWindow.this, MainActivity.class);
	}

	@Override
	public int getThemeStyle() {
		return android.R.style.Theme_Light;
	}

	@Override
	public Animation getShowAnimation(final int id) {
		if (isExistingId(id)) {
			// restore
			return AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left);
		} else {
			// show
			return super.getShowAnimation(id);
		}
	}

	@Override
	public Animation getHideAnimation(final int id) {
		return AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right);
	}

	@Override
	public void onReceiveEvent(final boolean success) {
		if (success) {
			dismiss();
		}

		dbAdapter.close();
	}

	@Override
	public MexDbAdapter getDbAdapter() {
		// return database adapter
		return dbAdapter;
	}

	@Override
	public Context getContext() {
		return getApplicationContext();
	}

	/**
	 * Dismisses the popup
	 */
	public void dismiss() {

		Tools.dismissCallPopup(getContext());
	}

	@Override
	public void onReceiveData(final int id, final int requestCode, final Bundle data, final Class<? extends StandOutWindow> fromCls, final int fromId) {
		// receive data from WidgetsWindow's button press
		// to show off the data sending framework
		switch (requestCode) {
		case FloatingCallWindow.DATA_CALL_STATUS_UPDATED:
			Window window = getWindow(id);
			if (window == null) {
				String errorText = String.format(Locale.US, "%s received data but Window id: %d is not open.", getAppName(), id);
				Log.e("FloatingCallWindow onReceiveData window is null", errorText);
				return;
			} else if (data == null) {
				Log.e("FloatingCallWindow onReceiveData data is null", "Null data for open window");
				return;
			}

			// Get contact name and extension from data
			String contactName = data.getString(MexDbAdapter.KEY_CONTACT_NAME);
			String extension = data.getString(MexDbAdapter.KEY_EXTENSION);

			// Update contact name and extension in dialog
			contactNameText.setText(contactName);
			extensionText.setText(extension);

			String dataText = String.format(Locale.US, "Data received: %s : %s", contactName, extension);
			Log.d("FloatingCallWindow", dataText);
			break;
		default:
			Log.d("FloatingCallWindow", "Unexpected data received.");
			break;
		}
	}
}
