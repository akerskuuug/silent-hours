package com.weblink.mexapp.fragment;

import java.util.ArrayList;
import java.util.Calendar;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.weblink.mexapp.R;
import com.weblink.mexapp.activity.MainActivity;
import com.weblink.mexapp.activity.SettingsActivity;
import com.weblink.mexapp.db.MexDbAdapter;
import com.weblink.mexapp.interfaces.CallListener;
import com.weblink.mexapp.interfaces.MexStatusListener;
import com.weblink.mexapp.interfaces.VoicemailListener;
import com.weblink.mexapp.interfaces.WCStatusListener;
import com.weblink.mexapp.pojo.CallHolder;
import com.weblink.mexapp.pojo.ContactHolder;
import com.weblink.mexapp.pojo.PastCallHolder;
import com.weblink.mexapp.pojo.User;
import com.weblink.mexapp.receiver.ScheduleMexStatusReceiver;
import com.weblink.mexapp.receiver.SchedulePresenceReceiver;
import com.weblink.mexapp.receiver.VoicemailCallReceiver;
import com.weblink.mexapp.receiver.VoicemailIgnoreReceiver;
import com.weblink.mexapp.utility.Constants;
import com.weblink.mexapp.utility.Task;
import com.weblink.mexapp.utility.Task.CallActionTask;
import com.weblink.mexapp.utility.Tools;

public class MainFragment extends Fragment implements OnCheckedChangeListener, View.OnClickListener, View.OnLongClickListener, CallListener, VoicemailListener, WCStatusListener, MexStatusListener {
	// Used for Honeycomb or newer APIs
	private static final float ALPHA_HALF_VISIBLE_FLOAT = 0.3f;
	// Used for older API versions
	private static final int ALPHA_HALF_VISIBLE_INT = 120;

	private ToggleButton mexToggleButton;
	private ListView callList;
	private ArrayList<CallHolder> calls;
	private ArrayList<PastCallHolder> callsHistory;
	private MainActivity myActivity;

	private User user;
	private SharedPreferences sharedPrefs;
	private AlarmManager alarmManager;
	private Editor editor;
	private TextView callListHeader;
	private MexDbAdapter dbAdapter;
	private LinearLayout headerContainer, shortcutContainer;
	private TextView tv;
	private Button statusButton;
	private Button voicemailButton;
	private View voicemailBackground;
	private Builder voicemailDialog;

	// TEMP + UGLY
	private boolean IS_ALINGSAS = false;

	// Holds all possible status messages, updated upon each startup
	private String[] possibleWCMessages;

	private int[] possibleWCMsgIDs;
	private ListView callHistoryList;
	private Button callHistoryHeader;

	@Override
	public void onAttach(final Activity activity) {
		super.onAttach(activity);

		myActivity = (MainActivity) activity;
		setRetainInstance(false);

		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(myActivity);
		editor = sharedPrefs.edit();

		String companyName = sharedPrefs.getString(Constants.LOGIN_COMPANY, "");
		if (companyName.equalsIgnoreCase("AHK")) {
			IS_ALINGSAS = true;
		}

		// Get the saved user info
		user = new User(companyName, sharedPrefs.getString(Constants.LOGIN_PASSWORD, ""), sharedPrefs.getInt(Constants.LOGIN_EXTENSION, 0));

		// Get the saved status names
		possibleWCMessages = sharedPrefs.getString(Constants.POSS_WC_MESSAGES, "").split("//");

		// Get the saved status IDs and parse to integers
		String[] tempArray = sharedPrefs.getString(Constants.POSS_WC_MESSAGE_IDS, "").split("//");

		possibleWCMsgIDs = new int[tempArray.length];
		for (int i = 0, length = tempArray.length; i < length; i++) {
			if (!tempArray[i].equals("")) {
				possibleWCMsgIDs[i] = Integer.parseInt(tempArray[i].replace("/", ""));
			}
		}

	}

	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	@Override
	public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {

		View parent = inflater.inflate(R.layout.fragment_main, null);
		Typeface robotoLight = myActivity.getTypefaceRobotoLight();

		mexToggleButton = (ToggleButton) parent.findViewById(R.id.availability_toggle_button);
		mexToggleButton.setTypeface(robotoLight);
		mexToggleButton.setLongClickable(true);
		mexToggleButton.setOnLongClickListener(this);

		callList = (ListView) parent.findViewById(R.id.call_list);
		callHistoryList = (ListView) parent.findViewById(R.id.call_list_history);
		callHistoryList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(final AdapterView<?> parent, final View view, final int id, final long position) {
				final PastCallHolder call = callsHistory.get((int) position);
				// When a history list item is clicked, open a dialog asking whether to call back or add contact to list
				AlertDialog.Builder optionsDialog = Tools.getSimpleDialog(myActivity, getString(R.string.call_options) + " " + call.getRemoteNumber(), null);

				optionsDialog.setPositiveButton(R.string.call_options_add, new OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog, final int which) {
						final Bundle args = new Bundle();

						String contactName = call.getContactName();

						// Check if the contact name is the phone number. In that case, don't set phone number as name
						args.putString(MexDbAdapter.KEY_CONTACT_NAME, Tools.isNumeric(contactName) ? "" : call.getContactName());
						args.putString(MexDbAdapter.KEY_REMOTE_NUMBER, call.getRemoteNumber());

						AlertDialog.Builder newContactDialog = Tools.getSimpleDialog(myActivity, R.string.call_options_add, R.string.call_options_add_exp);

						// Only allow this option if an internet connection is available
						if (myActivity.isNetworkAvailable()) {
							newContactDialog.setPositiveButton(R.string.contacts_webcall2, new OnClickListener() {
								// Add contact as a WebCall contact
								@Override
								public void onClick(final DialogInterface dialog, final int which) {
									myActivity.getAddContactDialog(args).show();
								}
							});
						}

						newContactDialog.setNeutralButton(R.string.contacts_local2, new OnClickListener() {
							// Launch default contacts app to add contact locally
							@Override
							public void onClick(final DialogInterface dialog, final int which) {
								Intent intent = new Intent(ContactsContract.Intents.SHOW_OR_CREATE_CONTACT, Uri.parse("tel:" + call.getRemoteNumber()));
								intent.putExtra(ContactsContract.Intents.SHOW_OR_CREATE_CONTACT, true);
								startActivity(intent);
							}
						});

						newContactDialog.show();

					}
				});

				optionsDialog.setNeutralButton(R.string.call_options_call, new OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog, final int which) {
						myActivity.makeCall(call.getContactName(), call.getRemoteNumber());

					}
				});

				optionsDialog.show();

			}
		});

		callListHeader = (TextView) parent.findViewById(R.id.call_header);
		callListHeader.setTypeface(robotoLight);

		callHistoryHeader = (Button) parent.findViewById(R.id.call_history_header);
		callHistoryHeader.setTypeface(myActivity.getTypefaceRobotoLight());
		callHistoryHeader.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View v) {
				// NOTE: !
				boolean shouldShowCallHistory = !sharedPrefs.getBoolean(Constants.SHOW_CALL_HISTORY, true);

				for(PastCallHolder holder : callsHistory){
					Log.d("Call", holder.getContactName());
				}
				
				// Toggle call history list
				callHistoryList.setAdapter(shouldShowCallHistory ? new CallHistoryListAdapter(myActivity, callsHistory, R.id.contact_name) : null);
				if (myActivity.getAppTheme() == Constants.THEME_LIGHT) {
					((Button) v).setCompoundDrawablesWithIntrinsicBounds(null, null, getResources().getDrawable(shouldShowCallHistory ? R.drawable.ic_action_collapse : R.drawable.ic_action_expand),
							null);
				} else {
					((Button) v).setCompoundDrawablesWithIntrinsicBounds(null, null,
							getResources().getDrawable(shouldShowCallHistory ? R.drawable.ic_action_collapse_dark : R.drawable.ic_action_expand_dark), null);
				}

				editor.putBoolean(Constants.SHOW_CALL_HISTORY, shouldShowCallHistory).commit();
			}
		});

		voicemailButton = (Button) parent.findViewById(R.id.voicemail_button);
		voicemailButton.setOnClickListener(this);
		voicemailButton.setTypeface(robotoLight);
		voicemailBackground = parent.findViewById(R.id.voicemail_bg);

		statusButton = (Button) parent.findViewById(R.id.status_button);
		statusButton.setTypeface(robotoLight);
		statusButton.setOnClickListener(this);
		statusButton.setOnLongClickListener(this);

		alarmManager = (AlarmManager) myActivity.getSystemService(Context.ALARM_SERVICE);

		shortcutContainer = (LinearLayout) parent.findViewById(R.id.shortcut_container);

		dbAdapter = myActivity.getDbAdapter();
		dbAdapter.open();

		ImageView img = (ImageView) parent.findViewById(R.id.bg_img);

		// Set the background image to semi-transparent.
		if (Tools.isHoneycombOrLater()) {
			img.setAlpha(ALPHA_HALF_VISIBLE_FLOAT);
		} else {
			img.setAlpha(ALPHA_HALF_VISIBLE_INT);
		}

		setFragmentTheme(parent, img);

		return parent;
	}

	private void setFragmentTheme(final View parent, final ImageView img) {
		// If the user has selected the dark theme, switch background and redraw
		// required visuals
		int theme = Tools.getIntPreference(sharedPrefs, Constants.SETTING_THEME, Constants.THEME_LIGHT);

		if (theme == Constants.THEME_BLUE || theme == Constants.THEME_DARK) {
			// Force redraw of View
			img.invalidate();
			img.setImageResource(R.drawable.bg_dark);

			HorizontalScrollView shortcutScroller = (HorizontalScrollView) parent.findViewById(R.id.shortcut_scroller);
			View v = parent.findViewById(R.id.bottom_buttons);

			if (theme == Constants.THEME_BLUE) {
				v.setBackgroundColor(getResources().getColor(R.color.weblink_blue));
				voicemailBackground.setBackgroundColor(MainActivity.COLOR_FOREGROUND_BLUE);
				shortcutScroller.setBackgroundColor(MainActivity.COLOR_FOREGROUND_BLUE);

				v = parent.findViewById(R.id.divider_vertical);
				v.setBackgroundColor(0xFF555580);
				v = parent.findViewById(R.id.divider_horizontal);
				v.setBackgroundColor(0xFF555580);
				v = parent.findViewById(R.id.divider_horizontal2);
				v.setBackgroundColor(0xFF555580);
				v = parent.findViewById(R.id.divider_horizontal_middle);
				v.setBackgroundColor(0xFF555580);
				v = parent.findViewById(R.id.divider_horizontal_bottom);
				v.setBackgroundColor(0xFF555580);
			} else {
				voicemailBackground.setBackgroundColor(MainActivity.COLOR_FOREGROUND_DARK);
				shortcutScroller.setBackgroundColor(MainActivity.COLOR_FOREGROUND_DARK);

				v = parent.findViewById(R.id.divider_vertical);
				v.setBackgroundColor(0xFF606060);
				v = parent.findViewById(R.id.divider_horizontal);
				v.setBackgroundColor(0xFF606060);
				v = parent.findViewById(R.id.divider_horizontal2);
				v.setBackgroundColor(0xFF606060);
				v = parent.findViewById(R.id.divider_horizontal_middle);
				v.setBackgroundColor(0xFF606060);
				v = parent.findViewById(R.id.divider_horizontal_bottom);
				v.setBackgroundColor(0xFF606060);
			}

			Drawable drawable = myActivity.getResources().getDrawable(R.drawable.device_access_mic_dark);
			voicemailButton.setCompoundDrawablesWithIntrinsicBounds(null, null, null, drawable);
		}
		if (IS_ALINGSAS) {

			img.invalidate();
			img.setImageResource(R.drawable.bg_main_ahk);
		}
	}

	private void determineNotification() {
		int callWindowPreferenceValue = Tools.getIntPreference(sharedPrefs, Constants.SETTING_CALL_WINDOW, Constants.CALL_WINDOW_TOP);
		boolean isEmpty = calls.isEmpty();

		if (calls != null && !isEmpty) {
			if (callWindowPreferenceValue == Constants.CALL_WINDOW_NONE) {
				Tools.Notifications.showCallNotification(myActivity);

			} else {

				// If window should be displayed, hide notification
				Tools.Notifications.dismissCallNotification(myActivity);

			}

		} else if (calls != null && isEmpty) {
			// If window should be displayed or there are no calls, hide notification
			Tools.Notifications.dismissCallNotification(myActivity);

		}

	}

	@SuppressLint("NewApi")
	@Override
	public void onResume() {
		super.onResume();
		calls = dbAdapter.fetchAllCalls();
		callsHistory = dbAdapter.fetchAllCallHistory();
		// Add the call items to the list
		callList.setAdapter(new CallListAdapter(myActivity, calls, R.id.contact_name));

		boolean showCallHistory = sharedPrefs.getBoolean(Constants.SHOW_CALL_HISTORY, true);
		callHistoryList.setAdapter(showCallHistory ? new CallHistoryListAdapter(myActivity, callsHistory, R.id.contact_name) : null);
		if (myActivity.getAppTheme() == Constants.THEME_LIGHT) {
			callHistoryHeader.setCompoundDrawablesWithIntrinsicBounds(null, null, getResources().getDrawable(showCallHistory ? R.drawable.ic_action_expand : R.drawable.ic_action_collapse), null);
		} else {
			callHistoryHeader.setCompoundDrawablesWithIntrinsicBounds(null, null, getResources().getDrawable(showCallHistory ? R.drawable.ic_action_expand_dark : R.drawable.ic_action_collapse_dark),
					null);
		}

		Bundle extras = myActivity.getExtras();
		if (extras != null && extras.getBoolean(Constants.VOICEMAIL_SHOW_DIALOG)) {
			showVoicemailDialog();
		}

		// If there is no new voicemail message, hide the "New voicemail" button
		boolean hasNewVM = sharedPrefs.getBoolean(Constants.NEW_VOICEMAIL, false);
		voicemailButton.setVisibility(hasNewVM ? View.VISIBLE : View.GONE);
		voicemailBackground.setVisibility(hasNewVM ? View.VISIBLE : View.GONE);

		// If there are no calls, hide the "no calls" text. Otherwise, show it
		callListHeader.setText(calls.isEmpty() ? R.string.no_calls : R.string.active_calls);

		// Check if mex is logged in
		boolean mexLoggedIn = sharedPrefs.getBoolean(Constants.MEX_LOGGED_IN, true);
		setToggleButtonCheckedWithoutOCC(mexToggleButton, mexLoggedIn);

		int currentStatus = sharedPrefs.getInt(Constants.CURRENT_STATUS, 1);

		setStatusText(currentStatus);

		// Show DND status to the user through the color of the bottom bar (not ActionBar) - red for DND active, else grey/blue depending on theme
		setDNDBarColor(sharedPrefs.getBoolean(Constants.DO_NOT_DISTURB, false));

		saveLoginName();
		determineHeaders();
		setShortcuts();

		onReceiveEvent(true);
	}

	public void setToggleButtonCheckedWithoutOCC(final ToggleButton toggleButton, final boolean setChecked) {
		toggleButton.setOnCheckedChangeListener(null);

		toggleButton.setChecked(setChecked);
		toggleButton.setBackgroundColor(setChecked ? Color.TRANSPARENT : Color.rgb(255, 115, 115));

		toggleButton.setOnCheckedChangeListener(this);

	}

	private void setShortcuts() {
		String rawString = sharedPrefs.getString(Constants.SETTING_SHORTCUT, "");

		// Clear buttons
		shortcutContainer.removeAllViews();

		if (rawString.length() > 0) {
			String[] activeShortcuts = rawString.split(",");
			// Get application resources
			Resources resources = getResources();
			// Show shortcut label if fewer than 4 shortcuts
			boolean showLabel = activeShortcuts.length < 4;

			final String[] shortcutNames = resources.getStringArray(R.array.shortcut_item_texts);

			TypedArray iconIdArray = resources
					.obtainTypedArray(Tools.getIntPreference(sharedPrefs, Constants.SETTING_THEME, Constants.THEME_LIGHT) == Constants.THEME_LIGHT ? R.array.shortcut_item_icons
							: R.array.shortcut_item_icons_dark);

			if (activeShortcuts.length > 0) {
				shortcutContainer.setVisibility(View.VISIBLE);

				for (String activeShortcut : activeShortcuts) {
					final int shortcutId = Integer.parseInt(activeShortcut);

					Button button = new Button(myActivity);
					button.setBackgroundResource(R.drawable.list_selector_holo_light);
					button.setCompoundDrawablesWithIntrinsicBounds(iconIdArray.getResourceId(shortcutId, R.drawable.navigation_cancel), 0, 0, 0);
					if (showLabel) {
						button.setPadding(15, 0, 15, 0);
						button.setText(shortcutNames[shortcutId]);
						button.setTextSize(17.0f);
						button.setTypeface(myActivity.getTypefaceRobotoLight());
						button.setCompoundDrawablePadding(5);
					} else {
						button.setPadding(33, 0, 0, 0);
					}

					// Show a Toast with the action's name upon long press
					button.setOnLongClickListener(new View.OnLongClickListener() {

						@Override
						public boolean onLongClick(final View v) {
							Toast.makeText(myActivity, shortcutNames[shortcutId], Toast.LENGTH_SHORT).show();
							return false;
						}
					});

					// Determine which button this is and what should be done when it is pressed
					switch (shortcutId) {
					case 0:
					case 1:
						button.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(final View v) {
								// For index 0, set unavailable for 30 minutes. For index 1; 60 minutes
								setUserUnavailableForDuration(30 + 30 * shortcutId);
							}

						});
						break;

					case 2:
						button.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(final View v) {
								Intent settingsIntent = new Intent(myActivity, SettingsActivity.class);
								myActivity.startActivity(settingsIntent);
							}
						});
						break;

					case 3:
						button.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(final View v) {
								myActivity.showUserDialog();
							}
						});
						break;
					case 4:
						button.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(final View v) {
								myActivity.showFollowMeDialog();
							}
						});
						break;
					case 5:
						button.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(final View v) {

								myActivity.showForwardingDialog();
							}
						});
						break;
					case 6:
						button.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(final View v) {
								myActivity.showCallSettingDialog();
							}
						});
						break;
					case 7:
						button.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(final View v) {
								myActivity.showAboutDialog();
							}
						});
						break;
					case 8:
						button.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(final View v) {
								myActivity.showHelpDialog();
							}
						});
						break;
					case 9:
						button.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(final View v) {
								myActivity.showLogoffDialog();
							}
						});
						break;
					}

					shortcutContainer.addView(button);
				}
			} else {
				shortcutContainer.setVisibility(View.GONE);
			}

			iconIdArray.recycle();

		}

	}

	/**
	 * Logs the user out from Mex, sets a custom status and restores everything after the set time.
	 * 
	 * @param duration the duration the user will be unavailable for
	 */
	private void setUserUnavailableForDuration(final int duration) {

		editor.putInt(Constants.USER_AVAILABILITY, Constants.AVAILABILITY_BUSY);
		editor.putBoolean(Constants.MEX_LOGGED_IN, false);
		editor.commit();

		// Set the time
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MINUTE, duration);

		Intent mexIntent = new Intent(myActivity, ScheduleMexStatusReceiver.class);
		Intent presenceIntent = new Intent(myActivity, SchedulePresenceReceiver.class);

		// Set alarm
		PendingIntent mexPendingIntent = PendingIntent.getBroadcast(myActivity, Constants.ALARM_ID_STATUS, mexIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		PendingIntent presencePendingIntent = PendingIntent.getBroadcast(myActivity, Constants.ALARM_ID_PRESENCE, presenceIntent, PendingIntent.FLAG_CANCEL_CURRENT);

		alarmManager.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), mexPendingIntent);
		alarmManager.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), presencePendingIntent);

		String endTime = Tools.fixTimeFormatting(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE));
		// Set custom status
		new Task.SetWCStatusTask(user, possibleWCMsgIDs[2], 2, getString(R.string.unavilable_until) + " " + endTime, myActivity).execute();
		// Log out from Mex
		new Task.SetMexStatusTask(user, false, this).execute();

		Toast.makeText(myActivity, getString(R.string.unavilable_until_toast) + " " + endTime, duration).show();
	}

	private void setStatusText(final int currentStatus) {
		if (isResumed()) {
			// Get the possible statuses and current status
			String[] statusStrings = getResources().getStringArray(R.array.status_strings);
			if (currentStatus != 2) {
				statusButton.setText(statusStrings[currentStatus]);
			} else {
				statusButton.setText(sharedPrefs.getString(Constants.CURRENT_STATUS_TEXT, ""));
			}
		}
	}

	private void determineHeaders() {
		if (headerContainer == null) {
			headerContainer = (LinearLayout) getLayoutInflater(getArguments()).inflate(R.layout.list_header, null).findViewById(R.id.header_container);

			tv = (TextView) headerContainer.findViewById(R.id.header_text);
			tv.setText(R.string.no_internet_long_calls);
			tv.setTypeface(myActivity.getTypefaceRobotoLight());
		}

		if (myActivity.isNetworkAvailable()) {

			tv.setVisibility(View.GONE);

			callListHeader.setText(calls.isEmpty() ? R.string.no_calls : R.string.active_calls);
		} else {

			// Show "No internet" header
			tv.setVisibility(View.VISIBLE);
			callList.setAdapter(null);
			if (callList.getHeaderViewsCount() == 0) {
				callList.addHeaderView(headerContainer);
			}

		}
	}

	@Override
	public void onReceiveMexStatus(final boolean mexStatus) {
		setToggleButtonCheckedWithoutOCC(mexToggleButton, mexStatus);

	}

	@Override
	public void onReceiveEvent(final boolean success) {

		if (success) {
			if (dbAdapter != null && dbAdapter.isOpen()) {
				calls = dbAdapter.fetchAllCalls();
				callsHistory = dbAdapter.fetchAllCallHistory();

				myActivity.runOnUiThread(new Runnable() {
					boolean isEmpty = calls.isEmpty();

					@Override
					public void run() {
						if (isEmpty) {
							editor.putBoolean(Constants.CALL_HELD, false).commit();
						}

						callListHeader.setText(calls.isEmpty() ? R.string.no_calls : R.string.active_calls);

						determineNotification();

						callList.setAdapter(new CallListAdapter(myActivity, calls, R.id.contact_name));

						boolean showCallHistory = sharedPrefs.getBoolean(Constants.SHOW_CALL_HISTORY, true);

						// Set the correct button drawable for the "expand/collapse list" button
						if (myActivity.getAppTheme() == Constants.THEME_LIGHT) {
							callHistoryHeader.setCompoundDrawablesWithIntrinsicBounds(null, null,
									getResources().getDrawable(showCallHistory ? R.drawable.ic_action_collapse : R.drawable.ic_action_expand), null);
						} else {
							callHistoryHeader.setCompoundDrawablesWithIntrinsicBounds(null, null,
									getResources().getDrawable(showCallHistory ? R.drawable.ic_action_collapse_dark : R.drawable.ic_action_expand_dark), null);

						}

						if (showCallHistory) {
							if (callHistoryList.getAdapter() == null) {
								callHistoryList.setAdapter(new CallHistoryListAdapter(myActivity, callsHistory, R.id.contact_name));
							} else {
								((CallHistoryListAdapter) callHistoryList.getAdapter()).refill(callsHistory);
							}
						} else {
							// remove call history list
							callHistoryList.setAdapter(null);
						}
						editor.putString(Constants.FIRST_ACTIVE_CALL_ID, isEmpty ? "" : calls.get(0).getCallId1()).commit();

					}
				});
			}
		}

	}

	@Override
	public void onReceiveWCStatus(final String[] possibleMsgs, final int[] possibleMessageIDs) {

		int currentStatus = sharedPrefs.getInt(Constants.CURRENT_STATUS, 1);

		if (possibleMsgs != null && possibleMsgs.length > 0) {
			possibleWCMessages = possibleMsgs;
		}
		if (possibleMessageIDs != null && possibleMessageIDs.length > 0) {
			possibleWCMsgIDs = possibleMessageIDs;
		}

		setStatusText(currentStatus);

		boolean hasMex = sharedPrefs.getBoolean(Constants.USER_HAS_MEX, true);
		boolean isMexLoggedIn = sharedPrefs.getBoolean(Constants.MEX_LOGGED_IN, false);
		mexToggleButton.setEnabled(hasMex);
		setToggleButtonCheckedWithoutOCC(mexToggleButton, isMexLoggedIn);

		if (!hasMex) {
			mexToggleButton.setText(R.string.no_mex);
		}

		mexToggleButton.setOnCheckedChangeListener(this);
	}

	@SuppressLint("NewApi")
	@Override
	public void onClick(final View view) {
		int viewId = view.getId();

		if (viewId == R.id.status_button) {

			if (myActivity.isNetworkAvailable()) { 
				final AlertDialog.Builder statusDialog = Tools.getSimpleDialog(myActivity, R.string.status_prompt, -1);
				int currentStatus = sharedPrefs.getInt(Constants.CURRENT_STATUS, 1);

				// Populate dialog and set selected (if valid)
				statusDialog.setSingleChoiceItems(possibleWCMessages, currentStatus <= possibleWCMsgIDs.length && currentStatus >= 0 ? currentStatus : 0, new OnClickListener() {

					@Override
					public void onClick(final DialogInterface dialog, final int which) {

						if (which != 0) {
							showCustomStatusDialog(statusDialog, which);
						} else {
							// "Available" status has no time limit and no options, enable immediately

							// Set the status to "Available" and disable DND
							Task.SetWCStatusTask task = new Task.SetWCStatusTask(user, possibleWCMsgIDs[which], which, null, myActivity);
							Task.SetDNDTask dndTask = new Task.SetDNDTask(user, false, myActivity, editor);
							if (Tools.isHoneycombOrLater()) {
								task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
								dndTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
							} else {
								task.execute();
								dndTask.execute();
							}
							// Initialize pending intent for alarm
							Intent intent = new Intent(myActivity, SchedulePresenceReceiver.class);
							PendingIntent pi = PendingIntent.getBroadcast(myActivity, Constants.ALARM_ID_PRESENCE, intent, PendingIntent.FLAG_CANCEL_CURRENT);

							// Cancel the alarm
							alarmManager.cancel(pi);
							editor.putLong(Constants.SCHEDULE_PRESENCE_MILLIS, -1);
							editor.commit();

							// Set the button drawable according to the new status
							// Tools.setUserImageToStatus(myActivity, statusButton, which, possibleWCMessages[which]);
							setDNDBarColor(false);
						}

						dialog.dismiss();

					}

					private void showCustomStatusDialog(final AlertDialog.Builder statusDialog, final int statusIndex) {
						final TextView statusInputTitle, endTimeTitle;
						final EditText statusInput;
						final TimePicker endTimePicker;
						final CheckBox endTimeCheck, statusDndCheck;

						AlertDialog.Builder customStatusDialog = Tools.getSimpleDialog(myActivity, R.string.status_details, -1);

						// Initialize the view to pick
						View statusDetailDialog = myActivity.getLayoutInflater().inflate(R.layout.popup_status_details, null);

						endTimeTitle = (TextView) statusDetailDialog.findViewById(R.id.status_end_time_title);
						statusInputTitle = (TextView) statusDetailDialog.findViewById(R.id.status_own_title);
						statusDndCheck = (CheckBox) statusDetailDialog.findViewById(R.id.status_dnd_check);

						// Initialize EditText
						statusInput = (EditText) statusDetailDialog.findViewById(R.id.status_own_input);
						statusInput.setText(sharedPrefs.getString(Constants.CURRENT_STATUS_TEXT, ""));
						statusInput.setSelection(statusInput.getText().length());

						// Initialize the time picker which determines end time
						endTimePicker = (TimePicker) statusDetailDialog.findViewById(R.id.status_end_time_picker);
						endTimePicker.setIs24HourView(true);
						endTimePicker.setCurrentHour(Calendar.getInstance().get(Calendar.HOUR_OF_DAY) + 1);
						endTimePicker.setCurrentMinute(Calendar.getInstance().get(Calendar.MINUTE));

						endTimeCheck = (CheckBox) statusDetailDialog.findViewById(R.id.status_use_end_time_check);
						endTimeCheck.setOnCheckedChangeListener(new OnCheckedChangeListener() {
							@Override
							public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
								// If checked, show time picker and title. Else hide

								endTimeTitle.setVisibility(isChecked ? View.VISIBLE : View.GONE);
								endTimePicker.setVisibility(isChecked ? View.VISIBLE : View.GONE);

							}
						});

						if (statusIndex == 2) {
							statusInputTitle.setVisibility(View.VISIBLE);
							statusInput.setVisibility(View.VISIBLE);
						}

						customStatusDialog.setView(statusDetailDialog);

						// If the Cancel button is pressed or the dialog
						// is cancelled, show the parent dialog
						customStatusDialog.setNegativeButton(R.string.cancel, new OnClickListener() {

							@Override
							public void onClick(final DialogInterface dialog, final int which) {
								statusDialog.show();

							}
						});

						customStatusDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {

							@Override
							public void onCancel(final DialogInterface dialog) {

								statusDialog.show();

							}
						});

						// If the status is acknowledged, save status
						// text
						customStatusDialog.setPositiveButton(R.string.ok, new OnClickListener() {

							@Override
							public void onClick(final DialogInterface dialog, final int whichButton) {
								// Initialize pending intent for alarms
								Intent intent = new Intent(myActivity, SchedulePresenceReceiver.class);
								PendingIntent pi = PendingIntent.getBroadcast(myActivity, Constants.ALARM_ID_PRESENCE, intent, PendingIntent.FLAG_CANCEL_CURRENT);

								String statusString = "";

								// Save the status text
								if (statusIndex == 2) {
									statusString = statusInput.getText().toString().trim();
								} else {
									statusString = possibleWCMessages[statusIndex];
								}

								editor.putString(Constants.CURRENT_STATUS_TEXT, statusString);
								editor.commit();

								// Set the button drawable according to the new status
								// Tools.setUserImageToStatus(myActivity, statusButton, statusIndex, statusString);

								if (endTimeCheck.isChecked()) {
									Calendar cal = Calendar.getInstance();

									int currentHour = cal.get(Calendar.HOUR_OF_DAY);
									int givenHour = endTimePicker.getCurrentHour();

									int currentMinute = cal.get(Calendar.MINUTE);
									int givenMinute = endTimePicker.getCurrentMinute();

									// If the time in the TimePicker is earlier than the current time, set the alarm for the next day
									if (givenHour < currentHour || givenHour == currentHour && givenMinute < currentMinute) {
										cal.add(Calendar.DATE, 1);
									}

									// Set the alarm time to the given values
									cal.set(Calendar.HOUR_OF_DAY, givenHour);
									cal.set(Calendar.MINUTE, givenMinute);
									cal.set(Calendar.SECOND, 0);

									alarmManager.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);

									editor.putLong(Constants.SCHEDULE_PRESENCE_MILLIS, cal.getTimeInMillis());
									editor.commit();

								} else {

									// Cancel the alarm
									alarmManager.cancel(pi);
									editor.putLong(Constants.SCHEDULE_PRESENCE_MILLIS, -1);
									editor.commit();

								}

								// Initialize AsyncTasks for setting presence and DND
								Task.SetWCStatusTask task = new Task.SetWCStatusTask(user, possibleWCMsgIDs[statusIndex], statusIndex, statusIndex == 2 ? statusString : null, myActivity);
								Task.SetDNDTask dndTask = new Task.SetDNDTask(user, statusDndCheck.isChecked(), myActivity, editor);

								// Execute asynctasks
								if (Tools.isHoneycombOrLater()) {
									task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
									dndTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
								} else {
									task.execute();
									dndTask.execute();
								}

								setDNDBarColor(statusDndCheck.isChecked());

							}

						});

						customStatusDialog.show();
					}

				});

				statusDialog.show();
			} else {
				Tools.showNoInternetToast(myActivity);
			}
		} else if (viewId == R.id.voicemail_button) {
			showVoicemailDialog();
		} else {
			updateRow(view);
		}
	}

	public void showVoicemailDialog() {
		if (voicemailDialog == null) {
			voicemailDialog = Tools.getSimpleDialog(myActivity, sharedPrefs.getInt(Constants.NUMBER_OF_VOICEMAIL, 0) + " " + myActivity.getString(R.string.new_voicemail_lc), null);

			voicemailDialog.setItems(R.array.voicemail_texts, new OnClickListener() {

				@Override
				public void onClick(final DialogInterface dialog, final int which) {
					Intent intent = new Intent();
					if (which == 0) {
						// "Call voicemail" was clicked
						intent = new Intent(myActivity, VoicemailCallReceiver.class);

					} else if (which == 1) {
						// "Ignore" was clicked
						intent = new Intent(myActivity, VoicemailIgnoreReceiver.class);

						editor.putBoolean(Constants.NEW_VOICEMAIL, false);
						editor.commit();

						voicemailButton.setVisibility(View.GONE);
						voicemailBackground.setVisibility(View.GONE);
					}

					PendingIntent pi = PendingIntent.getBroadcast(myActivity, 0, intent, 0);

					try {
						pi.send();
					} catch (CanceledException e) {
						Log.e("MainFragment.onClick()", e.getLocalizedMessage());
					}

				}
			});

		} else {
			voicemailDialog.show().dismiss();
		}
		voicemailDialog.show();
	}

	public void saveLoginName() {
		int extension = sharedPrefs.getInt(Constants.LOGIN_EXTENSION, 0);

		String name = "";
		ContactHolder holder = dbAdapter.fetchExtensionByExt(extension);
		if (holder != null) {
			name = holder.getName();
			editor.putString(Constants.LOGIN_NAME, name);
			editor.commit();
		}

	}

	@SuppressLint("NewApi")
	private void updateRow(final View v) {
		// Possibly not the best way to access the parent, but it is legal since
		// we, in this case, know the parent is a View with a R.id.call_id child
		View parent = (View) v.getParent();
		TextView txt = (TextView) parent.findViewById(R.id.call_id);

		parent.setBackgroundColor(0x66999999);

		int id = v.getId();
		if (id == R.id.resume_button) {
			// User wants to resume a call
			Task.CallActionTask rtask = new CallActionTask(user, CallActionTask.CALL_RESUME, txt.getText().toString(), myActivity);
			hideButtons(parent);
			// Show confirmation Toast
			Toast.makeText(myActivity, R.string.call_resuming, Toast.LENGTH_LONG).show();
			if (Tools.isHoneycombOrLater()) {
				rtask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			} else {
				rtask.execute();
			}
		} else if (id == R.id.answer_button) {
			// User wants to answer a call
			// Show confirmation Toast
			Toast.makeText(myActivity, R.string.call_answering, Toast.LENGTH_LONG).show();
			Task.CallActionTask atask = new CallActionTask(user, CallActionTask.CALL_ANSWER, txt.getText().toString(), myActivity);
			if (Tools.isHoneycombOrLater()) {
				atask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			} else {
				atask.execute();
			}
			hideButtons(parent);
		} else if (id == R.id.hold_button) {
			// User wants to hold a call
			// Show confirmation Toast
			Toast.makeText(myActivity, R.string.call_holding, Toast.LENGTH_LONG).show();
			Task.CallActionTask holdTask = new CallActionTask(user, CallActionTask.CALL_HOLD, txt.getText().toString(), myActivity);

			if (Tools.isHoneycombOrLater()) {
				holdTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			} else {
				holdTask.execute();
			}
			hideButtons(parent);
		} else if (id == R.id.callpad_button) {
			// User wants to transfer a call
			showDialerDialog(txt.getText().toString());
		} else if (id == R.id.reject_button) {
			// User wants to reject a call
			// Show confirmation Toast
			Toast.makeText(myActivity, R.string.call_hanging_up, Toast.LENGTH_LONG).show();
			Task.CallActionTask task = new CallActionTask(user, CallActionTask.CALL_HANGUP, txt.getText().toString(), myActivity);

			// Allow the floating window, if any, to be closed and/or opened.
			editor.putBoolean(Constants.CALL_HELD, false).commit();

			if (Tools.isHoneycombOrLater()) {
				task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			} else {
				task.execute();
			}
			v.setVisibility(View.INVISIBLE);
		}
	}

	private void hideButtons(final View parent) {

		((Button) parent.findViewById(R.id.hold_button)).setVisibility(View.GONE);
		((Button) parent.findViewById(R.id.resume_button)).setVisibility(View.GONE);
		((Button) parent.findViewById(R.id.answer_button)).setVisibility(View.GONE);
	}

	private void showDialerDialog(final String callId) {
		AlertDialog.Builder builder = Tools.getSimpleDialog(myActivity, getString(R.string.transfer_to), null);

		final EditText etx = new EditText(myActivity);
		etx.setInputType(InputType.TYPE_CLASS_PHONE);

		builder.setView(etx);
		builder.setTitle(R.string.transfer_to);

		// If OK is pressed, transfer the call.
		builder.setNeutralButton(R.string.transfer_direct, new OnClickListener() {
			@Override
			public void onClick(final DialogInterface dialog, final int which) {
				new Task.TransferTask(user, callId, etx.getText().toString(), myActivity).execute();
				Toast.makeText(myActivity, R.string.call_transferring, Toast.LENGTH_LONG).show();

			}
		});

		builder.setPositiveButton(R.string.transfer_att, new OnClickListener() {
			@Override
			public void onClick(final DialogInterface dialog, final int which) {
				ContactListFragment.startAttendedTransfer(user, callId, etx.getText().toString(), myActivity, sharedPrefs);
			}
		});

		// Necessary to automatically show the keyboard
		final AlertDialog dialog = builder.create();

		etx.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(final View v, final boolean hasFocus) {
				if (hasFocus) {
					dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
				}
			}
		});

		dialog.show();

		etx.requestFocus();
	}

	@SuppressLint("NewApi")
	@Override
	public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
		int id = buttonView.getId();

		if (id == R.id.availability_toggle_button) {
			if (myActivity.isNetworkAvailable()) {
				editor.putInt(Constants.USER_AVAILABILITY, isChecked ? Constants.AVAILABILITY_AVAILABLE : Constants.AVAILABILITY_BUSY);
				editor.putBoolean(Constants.MEX_LOGGED_IN, isChecked);
				editor.commit();

				if (!isChecked) {
					showDialog(isChecked, buttonView);
				} else {
					Intent intent = new Intent(myActivity, ScheduleMexStatusReceiver.class);

					PendingIntent pi = PendingIntent.getBroadcast(myActivity, Constants.ALARM_ID_STATUS, intent, PendingIntent.FLAG_CANCEL_CURRENT);
					alarmManager.cancel(pi);

					new Task.SetMexStatusTask(user, true, this).execute();

				}

				buttonView.setBackgroundColor(isChecked ? Color.TRANSPARENT : Color.rgb(255, 115, 115));
			} else {
				Tools.showNoInternetToast(myActivity);
				buttonView.setChecked(!isChecked);
			}
		}
	}

	private void showDialog(final boolean isChecked, final CompoundButton buttonView) {
		AlertDialog.Builder builder = new AlertDialog.Builder(myActivity);

		LayoutInflater inflater = myActivity.getLayoutInflater();
		View view = inflater.inflate(R.layout.return_picker, null);

		builder.setTitle(R.string.log_out_mex_prompt);
		builder.setView(view);

		final TextView timeLabel = (TextView) view.findViewById(R.id.time_label);
		final SeekBar seekBar = (SeekBar) view.findViewById(R.id.time_slider);

		seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			final String minutes = getString(R.string.minutes);

			@Override
			public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) {

				timeLabel.setText(progress + " " + minutes);
			}

			@Override
			public void onStopTrackingTouch(final SeekBar seekBar) {
				// Do nothing
			}

			@Override
			public void onStartTrackingTouch(final SeekBar seekBar) {
				// Do nothing
			}
		});

		seekBar.setProgress(30);

		builder.setPositiveButton(android.R.string.ok, new OnClickListener() {

			@Override
			public void onClick(final DialogInterface dialog, final int which) {
				Calendar cal = Calendar.getInstance();
				cal.add(Calendar.MINUTE, seekBar.getProgress());

				Intent intent = new Intent(myActivity, ScheduleMexStatusReceiver.class);

				PendingIntent pi = PendingIntent.getBroadcast(myActivity, Constants.ALARM_ID_STATUS, intent, PendingIntent.FLAG_CANCEL_CURRENT);
				alarmManager.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);

				// Log out from Mex
				new Task.SetMexStatusTask(user, false, MainFragment.this).execute();

			}

		});
		builder.setNeutralButton(R.string.always, new OnClickListener() {

			@Override
			public void onClick(final DialogInterface dialog, final int which) {

				// Log out from Mex
				new Task.SetMexStatusTask(user, false, MainFragment.this).execute();
			}
		});
		// If user presses the cancel button, return the checked state to
		// unchecked
		builder.setNegativeButton(R.string.cancel, new OnClickListener() {

			@Override
			public void onClick(final DialogInterface dialog, final int which) {
				buttonView.setChecked(!isChecked);

			}
		});

		// If user cancels the dialog in another way, return the checked state
		// to unchecked
		builder.setOnCancelListener(new DialogInterface.OnCancelListener() {

			@Override
			public void onCancel(final DialogInterface dialog) {
				buttonView.setChecked(!isChecked);

			}
		});
		builder.show();

	}

	public void setDNDBarColor(final boolean isDNDEnabled) {
		// Show DND status to the user through the color of the bottom bar (not ActionBar) - red for DND active, else grey/blue depending on theme
		int color = 0;

		switch (Tools.getIntPreference(sharedPrefs, Constants.SETTING_THEME, Constants.THEME_LIGHT)) {
		case Constants.THEME_LIGHT:
			color = isDNDEnabled ? Color.rgb(255, 115, 115) : Color.rgb(221, 221, 221);
			break;
		case Constants.THEME_BLUE:
			color = isDNDEnabled ? Color.rgb(255, 115, 115) : MainActivity.COLOR_FOREGROUND_BLUE;
			break;
		case Constants.THEME_DARK:
			color = isDNDEnabled ? Color.rgb(255, 115, 115) : MainActivity.COLOR_FOREGROUND_DARK;
			break;
		}

		LinearLayout layout = (LinearLayout) myActivity.findViewById(R.id.bottom_buttons);
		layout.setBackgroundColor(color);
	}

	@Override
	public MexDbAdapter getDbAdapter() {
		return myActivity.getDbAdapter();
	}

	@Override
	public Context getContext() {
		return getActivity();
	}

	@Override
	public void onReceiveVoicemail(final boolean hasVoicemail) {
		voicemailBackground.setVisibility(hasVoicemail ? View.VISIBLE : View.GONE);
		voicemailButton.setVisibility(hasVoicemail ? View.VISIBLE : View.GONE);

	}

	@Override
	public Editor getPrefEditor() {
		return editor;
	}

	@Override
	public boolean onLongClick(final View v) {
		int viewID = v.getId();
		int textID = 0;

		if (viewID == R.id.availability_toggle_button) {
			textID = R.string.mex_tooltip;
		} else if (viewID == R.id.status_button) {
			textID = R.string.status_prompt;
		} else if (viewID == R.id.answer_button) {
			textID = R.string.call_answer_tip;
		} else if (viewID == R.id.hold_button) {
			textID = R.string.call_hold_tip;
		} else if (viewID == R.id.resume_button) {
			textID = R.string.call_resume_tip;
		} else if (viewID == R.id.reject_button) {
			textID = R.string.call_hang_up_tip;
		} else if (viewID == R.id.callpad_button) {
			textID = R.string.call_transfer_tip;
		}

		if (viewID == R.id.availability_toggle_button) {
		} else if (viewID == R.id.status_button) {
			Toast.makeText(myActivity, R.string.status_prompt, Toast.LENGTH_SHORT).show();
		}

		if (textID != 0) {
			Toast.makeText(myActivity, textID, Toast.LENGTH_SHORT).show();
		}

		return false;
	}

	public class CallListAdapter extends ArrayAdapter<CallHolder> {

		CallHolder[] items;
		private final Drawable holdDrawableLight, callpadDrawableLight, hangupDrawable, resumeDrawableLight;

		public CallListAdapter(final Context context, final ArrayList<CallHolder> contacts, final int resourceID) {
			super(context, resourceID, contacts);
			items = new CallHolder[contacts.size()];
			contacts.toArray(items);

			// Initialize bright drawables for dark theme
			Resources resources = getContext().getResources();

			holdDrawableLight = resources.getDrawable(R.drawable.ic_action_pause_dark);
			callpadDrawableLight = resources.getDrawable(R.drawable.ic_menu_pad_dark);
			resumeDrawableLight = resources.getDrawable(R.drawable.ic_action_call_dark);

			hangupDrawable = resources.getDrawable(R.drawable.ic_action_end_call_dark);
			hangupDrawable.setColorFilter(Color.RED, PorterDuff.Mode.MULTIPLY);

		}

		public void move(final int oldPosition, final int newPosition) {
			CallHolder temp = items[oldPosition];

			// Move the items between positions in the adapter (also works if
			// the two positions are the same - then nothing happens)
			if (oldPosition < newPosition) {
				for (int i = oldPosition; i < newPosition; i++) {
					items[i] = items[i + 1];
				}
				items[newPosition] = temp;
			} else if (oldPosition > newPosition) {
				for (int i = oldPosition; i > newPosition + 1; i--) {
					items[i] = items[i - 1];
				}
				items[newPosition + 1] = temp;
			}

			notifyDataSetChanged();
		}

		@Override
		public View getView(final int position, View convertView, final ViewGroup parent) {

			if (convertView == null) {
				convertView = LayoutInflater.from(myActivity).inflate(R.layout.list_item_call, null);

			}

			bindView(position, convertView);

			return convertView;
		}

		private void bindView(final int position, final View convertView) {
			int theme = Tools.getIntPreference(sharedPrefs, Constants.SETTING_THEME, Constants.THEME_LIGHT);
			boolean useDarkTheme = theme == Constants.THEME_BLUE || theme == Constants.THEME_DARK;

			// Initialize all Views with click listeners
			CallHolder tempHolder = items[position];
			String callStatus = tempHolder.getCallStatus();
			String callDirection = tempHolder.getCallDirection();

			TextView txt = (TextView) convertView.findViewById(R.id.contact_name);
			txt.setText(tempHolder.getContact().getName());
			txt.setTypeface(myActivity.getTypefaceRobotoLight());

			txt = (TextView) convertView.findViewById(R.id.contact_extension);
			txt.setTypeface(myActivity.getTypefaceRobotoLight());
			String extension = tempHolder.getContact().getExtension();
			if (extension.length() > 2) {
				txt.setText(tempHolder.getContact().getExtension());
			}

			txt = (TextView) convertView.findViewById(R.id.call_id);
			txt.setText(tempHolder.getCallId1());

			ImageView view = (ImageView) convertView.findViewById(R.id.call_status);
			Tools.setImageToStatus(view, callStatus, callDirection);

			Button resumeButton = (Button) convertView.findViewById(R.id.resume_button);
			resumeButton.setOnClickListener(MainFragment.this);
			resumeButton.setOnLongClickListener(MainFragment.this);

			Button answerButton = (Button) convertView.findViewById(R.id.answer_button);
			answerButton.setOnClickListener(MainFragment.this);
			answerButton.setOnLongClickListener(MainFragment.this);

			Button holdButton = (Button) convertView.findViewById(R.id.hold_button);
			holdButton.setOnClickListener(MainFragment.this);
			holdButton.setOnLongClickListener(MainFragment.this);

			Button padButton = (Button) convertView.findViewById(R.id.callpad_button);
			padButton.setOnClickListener(MainFragment.this);
			padButton.setOnLongClickListener(MainFragment.this);

			Button rejectButton = (Button) convertView.findViewById(R.id.reject_button);
			rejectButton.setOnClickListener(MainFragment.this);
			rejectButton.setOnLongClickListener(MainFragment.this);
			rejectButton.setCompoundDrawablesWithIntrinsicBounds(null, hangupDrawable, null, null);

			if (useDarkTheme) {
				// Change to bright drawables for dark theme
				holdButton.setCompoundDrawablesWithIntrinsicBounds(null, holdDrawableLight, null, null);
				padButton.setCompoundDrawablesWithIntrinsicBounds(null, callpadDrawableLight, null, null);
				resumeButton.setCompoundDrawablesWithIntrinsicBounds(null, resumeDrawableLight, null, null);
				answerButton.setCompoundDrawablesWithIntrinsicBounds(null, resumeDrawableLight, null, null);

			}

			if (!myActivity.isNetworkAvailable()) {
				holdButton.setVisibility(View.GONE);
				resumeButton.setVisibility(View.GONE);
				answerButton.setVisibility(View.GONE);
				rejectButton.setVisibility(View.GONE);
				padButton.setVisibility(View.GONE);
			}

			if (callStatus.equals(Constants.CALL_STATUS_HOLD)) {
				holdButton.setVisibility(View.GONE);
				answerButton.setVisibility(View.GONE);
			} else if (callStatus.equals(Constants.CALL_STATUS_CONNECTED)) {
				resumeButton.setVisibility(View.GONE);
				answerButton.setVisibility(View.GONE);
			} else if (callStatus.equals(Constants.CALL_STATUS_RINGING) && callDirection.equals(Constants.CALL_DIRECTION_IN)) {
				holdButton.setVisibility(View.GONE);
				resumeButton.setVisibility(View.GONE);
			} else if (callStatus.equals(Constants.CALL_STATUS_RINGING) && callDirection.equals(Constants.CALL_DIRECTION_OUT)) {
				holdButton.setVisibility(View.GONE);
				resumeButton.setVisibility(View.GONE);
				answerButton.setVisibility(View.GONE);

			}

		}

		@Override
		public long getItemId(final int position) {

			return position;
		}

		@Override
		public CallHolder getItem(final int position) {
			return items[position];
		}

		@Override
		public int getCount() {

			if (items == null) {
				return 0;
			}

			return items.length;
		}

	}

	public class CallHistoryListAdapter extends ArrayAdapter<PastCallHolder> {

		PastCallHolder[] items;
		private final Drawable rejectedDrawable, answeredDrawable;

		public CallHistoryListAdapter(final Context context, final ArrayList<PastCallHolder> calls, final int resourceID) {
			super(context, resourceID, calls);
			items = new PastCallHolder[calls.size()];
			calls.toArray(items);

			// Initialize bright drawables for dark theme
			Resources resources = getContext().getResources();

			answeredDrawable = resources.getDrawable(R.drawable.ic_action_call_dark);

			rejectedDrawable = resources.getDrawable(R.drawable.ic_action_end_call_dark);

		}

		public void refill(final ArrayList<PastCallHolder> calls) {
			items = new PastCallHolder[calls.size()];
			calls.toArray(items);

			notifyDataSetChanged();
		}

		public void move(final int oldPosition, final int newPosition) {
			PastCallHolder temp = items[oldPosition];

			// Move the items between positions in the adapter (also works if
			// the two positions are the same - then nothing happens)
			if (oldPosition < newPosition) {
				for (int i = oldPosition; i < newPosition; i++) {
					items[i] = items[i + 1];
				}
				items[newPosition] = temp;
			} else if (oldPosition > newPosition) {
				for (int i = oldPosition; i > newPosition + 1; i--) {
					items[i] = items[i - 1];
				}
				items[newPosition + 1] = temp;
			}

			notifyDataSetChanged();
		}

		@Override
		public View getView(final int position, View convertView, final ViewGroup parent) {

			if (convertView == null) {
				convertView = LayoutInflater.from(myActivity).inflate(R.layout.list_item_call_history, null);

			}

			bindView(position, convertView);

			return convertView;
		}

		private void bindView(final int position, final View convertView) {
			Typeface robotoLight = myActivity.getTypefaceRobotoLight();

			// Initialize all Views with click listeners
			final PastCallHolder callHolder = items[position];

			TextView nameText = (TextView) convertView.findViewById(R.id.contact_name);
			nameText.setText(callHolder.getContactName());
			nameText.setTypeface(robotoLight);

			TextView extensionText = (TextView) convertView.findViewById(R.id.contact_extension);
			extensionText.setText(callHolder.getRemoteNumber());
			extensionText.setTypeface(robotoLight);

			TextView dateText = (TextView) convertView.findViewById(R.id.call_date);
			dateText.setText(callHolder.getCallDate());
			dateText.setTypeface(robotoLight);

			ImageView statusImage = (ImageView) convertView.findViewById(R.id.call_status);

			Resources resources = getContext().getResources();
			// Set drawable depending on whether call was answered
			if (callHolder.getCallDisposition().equalsIgnoreCase("ANSWERED")) {
				statusImage.setImageDrawable(resources.getDrawable(R.drawable.ic_action_call_dark));
				if (callHolder.getCallDirection() == PastCallHolder.CALL_DIRECTION_IN) {
					statusImage.setColorFilter(Color.rgb(50, 200, 100), PorterDuff.Mode.MULTIPLY);
				} else {
					statusImage.setColorFilter(Color.rgb(50, 150, 180), PorterDuff.Mode.MULTIPLY);
				}
			} else if (callHolder.getCallDisposition().equalsIgnoreCase("NO ANSWER")) {
				statusImage.setImageDrawable(rejectedDrawable);
				statusImage.setColorFilter(Color.RED, PorterDuff.Mode.MULTIPLY);
			} else {
				statusImage.setColorFilter(Color.TRANSPARENT);
			}

		}

		@Override
		public long getItemId(final int position) {

			return position;
		}

		@Override
		public PastCallHolder getItem(final int position) {
			return items[position];
		}

		@Override
		public int getCount() {

			if (items == null) {
				return 0;
			}

			return items.length;
		}

	}

}
