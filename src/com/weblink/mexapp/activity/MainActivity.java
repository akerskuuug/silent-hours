package com.weblink.mexapp.activity;

import io.socket.SocketIO;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.ActionBar.TabListener;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.weblink.mexapp.R;
import com.weblink.mexapp.R.array;
import com.weblink.mexapp.R.id;
import com.weblink.mexapp.R.layout;
import com.weblink.mexapp.R.string;
import com.weblink.mexapp.db.MexDbAdapter;
import com.weblink.mexapp.fragment.ContactListFragment;
import com.weblink.mexapp.fragment.MainFragment;
import com.weblink.mexapp.fragment.MiscFragment;
import com.weblink.mexapp.interfaces.AsyncTaskCompleteListener;
import com.weblink.mexapp.interfaces.CallListener;
import com.weblink.mexapp.interfaces.SIOEventListener;
import com.weblink.mexapp.interfaces.VoicemailListener;
import com.weblink.mexapp.interfaces.WCStatusListener;
import com.weblink.mexapp.net.JSONResources;
import com.weblink.mexapp.net.ServerCallback;
import com.weblink.mexapp.pojo.CallHolder;
import com.weblink.mexapp.pojo.ContactHolder;
import com.weblink.mexapp.pojo.FolderHolder;
import com.weblink.mexapp.pojo.QueueHolder;
import com.weblink.mexapp.pojo.User;
import com.weblink.mexapp.pojo.WCContactHolder;
import com.weblink.mexapp.utility.Constants;
import com.weblink.mexapp.utility.Task;
import com.weblink.mexapp.utility.Task.AddFolderTask;
import com.weblink.mexapp.utility.Task.DeleteContactTask;
import com.weblink.mexapp.utility.Task.DeleteFolderTask;
import com.weblink.mexapp.utility.Task.GetCallHistoryTask;
import com.weblink.mexapp.utility.Tools;

public class MainActivity extends SherlockFragmentActivity implements TabListener, OnCheckedChangeListener, SIOEventListener, AsyncTaskCompleteListener<Boolean>, CallListener, VoicemailListener,
		WCStatusListener {

	/**
	 * The {@link android.support.v4.view.PagerAdapter} that will provide
	 * fragments for each of the sections. We use a
	 * {@link android.support.v4.app.FragmentPagerAdapter} derivative, which
	 * will keep every loaded fragment in memory. If this becomes too memory
	 * intensive, it may be best to switch to a
	 * {@link android.support.v4.app.FragmentStatePagerAdapter}.
	 */
	SectionsPagerAdapter mSectionsPagerAdapter;

	public static final int COLOR_FOREGROUND_DARK = 0xFF181818;// 323232;
	public static final int COLOR_FOREGROUND_BLUE = 0xFF224062;

	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	ViewPager mViewPager;
	private SharedPreferences sharedPrefs;
	private Editor editor;

	private EditText alwaysText, notAvailText, busyText;
	private CheckBox callWaitingCheck, dndCheck, forwardingAlwaysCheck, forwardingBusyCheck, forwardingNACheck;

	private User user;

	private ServerCallback callback;
	private SocketIO socket;

	private MexDbAdapter dbAdapter;
	private int APP_THEME = Constants.THEME_LIGHT;

	private Typeface robotoLight;
	private Typeface robotoMedium;

	private AsyncTask<Void, Void, Boolean> extensionTask;
	private AsyncTask<Void, Void, Boolean> callListTask;
	private AsyncTask<Void, Void, Boolean> folderTask;
	private AsyncTask<Void, Void, Boolean> wcContactTask;
	private AsyncTask<Void, Void, Void> initialTask;

	private int currentTab;
	private com.actionbarsherlock.app.ActionBar actionBar;

	private AsyncTask<Void, Void, String> queueTask;

	private Bundle extras;

	public MainFragment mainFragment;
	public ContactListFragment clFragment;
	public MiscFragment miscFragment;

	private GetCallHistoryTask callHistoryTask;

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		if (!sharedPrefs.getBoolean(Constants.SIGNED_IN, false)) {
			Intent intent = new Intent(MainActivity.this, LoginActivity.class);
			startActivity(intent);
			finish();
		}

		APP_THEME = Tools.getIntPreference(sharedPrefs, Constants.SETTING_THEME, Constants.THEME_LIGHT);

		switch (APP_THEME) {
		case Constants.THEME_LIGHT:
			setTheme(R.style.CustomTheme);
			break;
		case Constants.THEME_BLUE:
			setTheme(R.style.CustomThemeDark);
			break;
		case Constants.THEME_DARK:
			setTheme(R.style.CustomThemeBlack);
			break;
		}

		// Fetch Roboto typeface
		robotoLight = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Light.ttf");
		robotoMedium = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Medium.ttf");

		// Set title typeface to Roboto
		final int actionBarTitle = Resources.getSystem().getIdentifier("action_bar_title", "id", "android");
		final TextView title = (TextView) getWindow().findViewById(actionBarTitle);
		if (title != null) {
			title.setTypeface(robotoLight);
			title.setTextSize(22);
		}

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Fetch and open the database adapter
		dbAdapter = new MexDbAdapter(MainActivity.this);
		dbAdapter.open();

		// Set up the action bar.
		actionBar = getSupportActionBar();
		actionBar.setDisplayShowHomeEnabled(true);
		actionBar.setDisplayShowTitleEnabled(true);
		actionBar.setNavigationMode(com.actionbarsherlock.app.ActionBar.NAVIGATION_MODE_TABS);

		actionBar.setHomeButtonEnabled(true);

		editor = sharedPrefs.edit();

		// Get the width of the application window
		// Uses old method for older Android versions (urghhh...)
		Point size = new Point();
		if (Tools.isHoneycombOrLater()) {
			getWindowManager().getDefaultDisplay().getSize(size);
			Log.e("Window Size", size.x + " ddasadadss");
			editor.putInt(Constants.WINDOW_WIDTH, size.x).commit();
			editor.putInt(Constants.WINDOW_HEIGHT, size.y).commit();
		} else {
			editor.putInt(Constants.WINDOW_WIDTH, getWindowManager().getDefaultDisplay().getWidth()).commit();
			editor.putInt(Constants.WINDOW_HEIGHT, getWindowManager().getDefaultDisplay().getHeight()).commit();
		}

		// Create the adapter that will return a fragment for each of the three
		// primary sections of the app.
		mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);

		if (APP_THEME == Constants.THEME_DARK) {

			// Set title bar background color
			actionBar.setBackgroundDrawable(new ColorDrawable(COLOR_FOREGROUND_DARK));

			// Set tab bar background color
			actionBar.setStackedBackgroundDrawable(new ColorDrawable(Color.rgb(28, 28, 28)));

			// Set bottom button background color
			actionBar.setSplitBackgroundDrawable(new ColorDrawable(COLOR_FOREGROUND_DARK));

		} else if (APP_THEME == Constants.THEME_BLUE) {
			// Set title bar background color
			actionBar.setBackgroundDrawable(new ColorDrawable(COLOR_FOREGROUND_BLUE));

			// Set tab bar background color
			actionBar.setStackedBackgroundDrawable(new ColorDrawable(Color.rgb(30, 60, 93)));

			// Set bottom button background color
			actionBar.setSplitBackgroundDrawable(new ColorDrawable(COLOR_FOREGROUND_BLUE));
		}

		// When swiping between different sections, select the corresponding
		// tab. We can also use ActionBar.Tab#select() to do this if we have
		// a reference to the Tab.
		mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(final int position) {
				actionBar.setSelectedNavigationItem(position);
			}

		});

		user = new User(sharedPrefs.getString(Constants.LOGIN_COMPANY, ""), sharedPrefs.getString(Constants.LOGIN_PASSWORD, ""), sharedPrefs.getInt(Constants.LOGIN_EXTENSION, 0));

		int startupTab = Tools.getIntPreference(sharedPrefs, Constants.SETTING_STARTUP_TAB, 1);

		// For each of the sections in the app, add a tab to the action bar.
		for (int i = 0, length = mSectionsPagerAdapter.getCount(); i < length; i++) {
			// Create a tab with text corresponding to the page title defined by
			// the adapter. Also specify this Activity object, which implements
			// the
			// TabListener interface, as the callback (listener) for when this
			// tab is selected.
			actionBar.addTab(actionBar.newTab().setText(mSectionsPagerAdapter.getPageTitle(i)).setTabListener(this), i == startupTab);

		}

	}

	@Override
	public void onResume() {
		super.onResume();

		if (APP_THEME != Tools.getIntPreference(sharedPrefs, Constants.SETTING_THEME, Constants.THEME_LIGHT)) {
			Intent newIntent = new Intent(this, MainActivity.class);
			startActivity(newIntent);
			finish();

		}

		extras = getIntent().getExtras();
		if (extras != null && extras.getBoolean(Constants.VOICEMAIL_SHOW_DIALOG)) {
			actionBar.setSelectedNavigationItem(1);
		}

		// Clear intent extra to avoid dialog being shown several times
		getIntent().removeExtra(Constants.VOICEMAIL_SHOW_DIALOG);

		editor.putBoolean(Constants.CALL_WINDOW_DISPLAYED, false).commit();
		// Dismiss floating window, not needed when app is open
		Tools.dismissCallPopup(this);

		this.registerReceiver(mConnReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

	}

	@Override
	public void onPause() {
		super.onPause();

		unregisterReceiver(mConnReceiver);

		if (callback != null) {
			callback.unregisterObserver(this);
		}
		disconnectFromSIO();
	}

	@Override
	public void onStop() {
		super.onStop();

		if (callback != null) {
			callback.unregisterObserver(this);
		}
	}

	public Bundle getExtras() {
		return extras;
	}

	private void connectToSIO() {

		try {
			socket = new SocketIO(Constants.SOCKETIO_SERVER_URL);
			callback = new ServerCallback(socket, user, sharedPrefs);
			socket.connect(callback);
			callback.registerObserver(MainActivity.this);

		} catch (Exception e) {
			Log.e("Cannot resolve host", "Is Internet connection active? " + e.getLocalizedMessage());
		}

	}

	public void setTabTitle(final int showContactType) {

		int tabTextID;

		switch (showContactType) {
		case Constants.CONTACT_TYPE_LOCAL:
			tabTextID = R.string.contacts_local;
			break;
		case Constants.CONTACT_TYPE_WEBCALL:
			tabTextID = R.string.contacts_webcall;
			break;
		default:
			tabTextID = R.string.contacts_remote;
			break;
		}

		getSupportActionBar().getTabAt(0).setText(tabTextID);

	}

	private void disconnectFromSIO() {
		if (socket != null) {
			while (socket.isConnected()) {
				socket.disconnect();
			}
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		cancelAsyncTasks();

		dbAdapter.close();
	}

	public ServerCallback getSIOInstance() {
		return callback;
	}

	public Typeface getTypefaceRobotoLight() {
		return robotoLight;
	}

	public Typeface getTypefaceRobotoMedium() {
		return robotoMedium;
	}

	@Override
	public MexDbAdapter getDbAdapter() {
		return dbAdapter;
	}

	// Checks if an internet connection is available
	public boolean isNetworkAvailable() {
		ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
		return activeNetworkInfo != null && activeNetworkInfo.isConnected();
	}

	public int getAppTheme() {
		return APP_THEME;
	}

	@Override
	public void onTabSelected(final Tab tab, final android.support.v4.app.FragmentTransaction ft) {
		// When the given tab is selected, switch to the corresponding page in
		// the ViewPager.
		mViewPager.setCurrentItem(tab.getPosition());
		currentTab = tab.getPosition();

		if (currentTab == 0) {
			tab.setText(sharedPrefs.getInt(Constants.SHOW_CONTACT_TYPE, Constants.CONTACT_TYPE_EXTENSION) == Constants.CONTACT_TYPE_LOCAL ? R.string.contacts_local : R.string.contacts_remote);

			int tabTextID;
			switch (sharedPrefs.getInt(Constants.SHOW_CONTACT_TYPE, Constants.CONTACT_TYPE_EXTENSION)) {
			case Constants.CONTACT_TYPE_LOCAL:
				tabTextID = R.string.contacts_local_short;
				break;
			case Constants.CONTACT_TYPE_WEBCALL:
				tabTextID = R.string.contacts_webcall;
				break;
			default:
				tabTextID = R.string.contacts_remote;
				break;
			}
			tab.setText(getString(tabTextID));

			tab.setIcon(APP_THEME == Constants.THEME_LIGHT ? R.drawable.social_group : R.drawable.social_group_dark);

			if (clFragment != null && clFragment.isResumed()) {
				Tools.smoothScrollListIfTop(clFragment.getListView(), 1);
			}
		}

	}

	@Override
	public void onTabUnselected(final Tab tab, final android.support.v4.app.FragmentTransaction ft) {
		if (tab.getPosition() == 0) {
			tab.setIcon(null);
			tab.setText(R.string.contacts);
		}
	}

	@Override
	public void onTabReselected(final Tab tab, final android.support.v4.app.FragmentTransaction ft) {

		if (tab.getPosition() == 0 && currentTab == 0) {

			Tools.smoothScrollListView(clFragment.getListView(), 0);

		}
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.menu_main, menu);

		if (APP_THEME != Constants.THEME_LIGHT) {
			menu.findItem(R.id.ab_call_button).setIcon(R.drawable.ic_action_call_dark);
			menu.findItem(R.id.queueueueue_button).setIcon(R.drawable.ic_action_cc_bcc_dark);
		}

		return true;
	}

	@SuppressLint("NewApi")
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		int itemId = item.getItemId();

		if (itemId == R.id.user_button) {
			if (isNetworkAvailable()) {

				showForwardingDialog();

			} else {
				Tools.showNoInternetToast(this);
			}

		} else if (itemId == R.id.ab_call_button) {

			showCallDialog();

		} else if (itemId == R.id.queueueueue_button) {
			if (isNetworkAvailable()) {
				showQueueDialog();
			} else {
				Tools.showNoInternetToast(this);
			}
		} else if (itemId == android.R.id.home) {

			launchAsyncTasks();

			disconnectFromSIO();
			connectToSIO();

			Toast.makeText(this, R.string.updating, Toast.LENGTH_LONG).show();

		}
		return super.onOptionsItemSelected(item);

	}

	private void showQueueDialog() {
		AlertDialog.Builder builder = Tools.getSimpleDialog(this, R.string.queueueueueueues_manage, -1);

		final QueueHolder[] queues = dbAdapter.fetchAllQueues();
		String[] queueNames = Tools.getQueueNames(queues);
		boolean[] enabledQueues = Tools.getEnabledQueues(queues);

		builder.setMultiChoiceItems(queueNames, enabledQueues, new DialogInterface.OnMultiChoiceClickListener() {

			@Override
			public void onClick(final DialogInterface dialog, final int which, final boolean isChecked) {
				queues[which].setLoggedIn(isChecked);

			}
		});

		builder.setPositiveButton(R.string.ok, new OnClickListener() {

			@SuppressLint("NewApi")
			@Override
			public void onClick(final DialogInterface dialog, final int which) {
				// Save to DB
				Task.SetQueuesTask task = new Task.SetQueuesTask(user, MainActivity.this, queues, dbAdapter);

				if (Tools.isHoneycombOrLater()) {
					task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				} else {
					task.execute();
				}
			}
		});
		if (queues.length > 0) {
			builder.show();
		} else {
			Toast.makeText(this, R.string.no_queues, Toast.LENGTH_LONG).show();
		}

	}

	private void showCallDialog() {

		AlertDialog.Builder builder = Tools.getSimpleDialog(this, R.string.properties, -1);

		final EditText etx = new EditText(this);
		etx.setInputType(InputType.TYPE_CLASS_PHONE);

		builder.setView(etx);
		if (!isNetworkAvailable()) {
			builder.setMessage(R.string.calling_directly);
		}
		builder.setTitle(R.string.call_to);

		// If OK is pressed, transfer the call.
		builder.setPositiveButton(R.string.ok, new OnClickListener() {
			@Override
			public void onClick(final DialogInterface dialog, final int which) {

				makeCall(null, etx.getText().toString().trim());
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

	public void showForwardingDialog() {

		AlertDialog.Builder builder = Tools.getSimpleDialog(this, R.string.properties, -1);
		// Inflate and show the forwarding popup view
		LayoutInflater inflater = getLayoutInflater();
		View v = inflater.inflate(R.layout.popup_forwarding, null);
		builder.setView(v);

		// Initialize all number fields and get old values
		initializeForwardingDialog(v);

		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(final DialogInterface dialog, final int which) {

				// Send numbers to server
				new SubmitForwardingTask(user).execute();

			}

		});

		builder.show();

	}

	private void initializeForwardingDialog(final View v) {

		OnEditorActionListener editorListener = new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(final TextView v, final int actionId, final KeyEvent event) {

				if (actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_NULL) {
					v.setText(Tools.formatPhoneNumber(v.getText().toString()));
				}
				return false;
			}
		};

		alwaysText = (EditText) v.findViewById(R.id.forwarding_input_always);
		alwaysText.setText(sharedPrefs.getString(Constants.FORWARDING_NUMBER_ALWAYS, ""));
		alwaysText.setOnEditorActionListener(editorListener);

		notAvailText = (EditText) v.findViewById(R.id.forwarding_input_na);
		notAvailText.setText(sharedPrefs.getString(Constants.FORWARDING_NUMBER_NOT_AVAIL, ""));
		notAvailText.setOnEditorActionListener(editorListener);

		busyText = (EditText) v.findViewById(R.id.forwarding_input_busy);
		busyText.setText(sharedPrefs.getString(Constants.FORWARDING_NUMBER_BUSY, ""));
		busyText.setOnEditorActionListener(editorListener);

		forwardingNACheck = (CheckBox) v.findViewById(R.id.forward_na_check);
		forwardingNACheck.setOnCheckedChangeListener(this);
		forwardingBusyCheck = (CheckBox) v.findViewById(R.id.forward_busy_check);
		forwardingBusyCheck.setOnCheckedChangeListener(this);
		forwardingAlwaysCheck = (CheckBox) v.findViewById(R.id.forward_always_check);
		forwardingAlwaysCheck.setOnCheckedChangeListener(this);

		boolean enabled = sharedPrefs.getBoolean(Constants.ENABLE_CALL_FORWARDING_NO_ANSWER, false);
		forwardingNACheck.setChecked(enabled);
		notAvailText.setEnabled(enabled);

		enabled = sharedPrefs.getBoolean(Constants.ENABLE_CALL_FORWARDING_BUSY, false);
		forwardingBusyCheck.setChecked(enabled);
		busyText.setEnabled(enabled);

		enabled = sharedPrefs.getBoolean(Constants.ENABLE_CALL_FORWARDING_ALWAYS, false);
		forwardingAlwaysCheck.setChecked(enabled);
		alwaysText.setEnabled(enabled);

		View callWaitingLabel = v.findViewById(R.id.call_waiting_label);

		callWaitingCheck = (CheckBox) v.findViewById(R.id.call_waiting_check);
		callWaitingCheck.setChecked(sharedPrefs.getBoolean(Constants.ALLOW_CALL_WAITING, false));

		dndCheck = (CheckBox) v.findViewById(R.id.block_incoming_check);
		dndCheck.setChecked(sharedPrefs.getBoolean(Constants.DO_NOT_DISTURB, false));

	}

	@Override
	public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
		int id = buttonView.getId();

		if (id == R.id.forward_always_check) {
			alwaysText.setEnabled(isChecked);

			forwardingNACheck.setEnabled(!isChecked);
			notAvailText.setEnabled(!isChecked && forwardingNACheck.isChecked());
			forwardingBusyCheck.setEnabled(!isChecked);
			busyText.setEnabled(!isChecked && forwardingBusyCheck.isChecked());

		} else if (id == R.id.forward_na_check) {
			notAvailText.setEnabled(isChecked);

		} else if (id == R.id.forward_busy_check) {
			busyText.setEnabled(isChecked);
		}

	}

	@Override
	public Activity getMainActivity() {
		return this;
	}

	@Override
	public void onReceiveCall(final boolean success) {
		if (mainFragment != null) {
			mainFragment.onReceiveEvent(success);
		}
	}

	@Override
	public void onReceiveStatus(final ContactHolder holder) {
		if (clFragment != null) {
			clFragment.onReceive(holder);
		}
	}

	@Override
	public void finished(final Boolean result) {
		if (result && clFragment != null) {

			editor.putBoolean(Constants.SHOW_LOCAL_CONTACTS, !sharedPrefs.getBoolean(Constants.SHOW_LOCAL_CONTACTS, false)).commit();

			clFragment.setContactLists();
			if (clFragment.contactProgressDialog != null) {
				clFragment.contactProgressDialog.dismiss();
			}

		}

		if (mainFragment != null && mainFragment.isResumed()) {
			mainFragment.saveLoginName();
		}
	}

	@Override
	public void onReceiveEvent(final boolean success) {
		if (mainFragment != null) {
			mainFragment.onReceiveEvent(success);
		}

	}

	public void resumeMainFragment() {
		if (mainFragment != null && mainFragment.isResumed()) {
			mainFragment.onResume();
		}
	}

	public void resumeCLFragment() {
		if (clFragment != null && clFragment.isResumed()) {
			clFragment.onResume();
		}
	}

	public void resumeMiscFragment() {
		if (miscFragment != null && miscFragment.isResumed()) {
			miscFragment.onResume();
		}
	}

	/**
	 * 
	 * @param name
	 *            the name of the contact to call (may be null)
	 * @param phoneNumber
	 *            the number to the contact (not null)
	 */
	@SuppressLint("NewApi")
	public void makeCall(final String name, final String phoneNumber) {
		if (phoneNumber != null && phoneNumber.length() > 0) {

			int callWindowPreferenceValue = Tools.getIntPreference(sharedPrefs, Constants.SETTING_CALL_WINDOW, Constants.CALL_WINDOW_TOP);
			if (callWindowPreferenceValue == Constants.CALL_WINDOW_NONE) {
				Tools.Notifications.showCallNotification(MainActivity.this);
			}

			if (isNetworkAvailable() && Tools.getIntPreference(sharedPrefs, Constants.SETTING_CALL_LOCAL, 1) == 0) {

				Task.CallTask task = new Task.CallTask(user, phoneNumber);
				if (Tools.isHoneycombOrLater()) {
					task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				} else {
					task.execute();
				}
			} else {
				Intent intent = new Intent(Intent.ACTION_CALL);

				intent.setData(Uri.parse("tel:" + phoneNumber));
				startActivity(intent);
			}

			if (name != null) {
				Toast.makeText(this, getString(R.string.calling) + " " + name + " (" + phoneNumber + ")", Toast.LENGTH_LONG).show();
			} else {
				Toast.makeText(this, getString(R.string.calling) + " " + phoneNumber, Toast.LENGTH_LONG).show();
			}
		} else {
			Toast.makeText(this, getString(R.string.supply_number), Toast.LENGTH_LONG).show();
		}
	}

	@SuppressLint("NewApi")
	private void launchAsyncTasks() {

		callListTask = new Task.GetCallTask(user, this);
		callHistoryTask = new Task.GetCallHistoryTask(user, this);
		extensionTask = new Task.GetExtensionTask(user, this, dbAdapter, this);
		folderTask = new Task.GetFolderTask(user, this, dbAdapter);
		wcContactTask = new Task.GetWCCOntactTask(user, this, dbAdapter);
		initialTask = new Task.GetUserInfoTask(user, editor, dbAdapter, this, this);

		// Launch AsyncTasks
		if (Tools.isHoneycombOrLater()) {
			callListTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			callHistoryTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			extensionTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			folderTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			wcContactTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			initialTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		} else {
			callListTask.execute();
			callHistoryTask.execute();
			extensionTask.execute();
			folderTask.execute();
			wcContactTask.execute();
			initialTask.execute();
		}

	}

	/**
	 * Cancels all running (non-null) AsyncTasks.
	 */
	private void cancelAsyncTasks() {
		if (extensionTask != null) {
			extensionTask.cancel(true);
		}
		if (callListTask != null) {
			callListTask.cancel(true);
		}
		if (callHistoryTask != null) {
			callHistoryTask.cancel(true);
		}
		if (folderTask != null) {
			folderTask.cancel(true);
		}
		if (wcContactTask != null) {
			wcContactTask.cancel(true);
		}
		if (initialTask != null) {
			initialTask.cancel(true);
		}
		if (queueTask != null) {
			queueTask.cancel(true);
		}
	}

	public class SubmitForwardingTask extends AsyncTask<Void, Void, Boolean> {
		private final User user;

		public SubmitForwardingTask(final User user) {
			super();
			this.user = user;

		}

		@Override
		protected Boolean doInBackground(final Void... params) {

			// Format the numbers correctly
			String alwaysNumber = Tools.formatPhoneNumber(alwaysText.getText().toString());
			String notAvailNumber = Tools.formatPhoneNumber(notAvailText.getText().toString());
			String busyNumber = Tools.formatPhoneNumber(busyText.getText().toString());

			boolean enableForwardingAlways = forwardingAlwaysCheck.isChecked();
			boolean enableForwardingNA = forwardingNACheck.isChecked();
			boolean enableForwardingBusy = forwardingBusyCheck.isChecked();
			boolean dndEnabled = dndCheck.isChecked();

			if (!JSONResources.setForwarding(user, enableForwardingAlways ? alwaysNumber : "", enableForwardingBusy ? busyNumber : "", enableForwardingNA ? notAvailNumber : "", dndEnabled)) {

				return false;
			} else {

				editor.putString(Constants.FORWARDING_NUMBER_ALWAYS, alwaysNumber);
				editor.putBoolean(Constants.ENABLE_CALL_FORWARDING_ALWAYS, enableForwardingAlways);

				editor.putString(Constants.FORWARDING_NUMBER_NOT_AVAIL, notAvailNumber);
				editor.putBoolean(Constants.ENABLE_CALL_FORWARDING_NO_ANSWER, enableForwardingNA);

				editor.putString(Constants.FORWARDING_NUMBER_BUSY, busyNumber);
				editor.putBoolean(Constants.ENABLE_CALL_FORWARDING_BUSY, enableForwardingBusy);

				editor.putBoolean(Constants.DO_NOT_DISTURB, dndEnabled);

				// Save the numbers for the next time this dialog is opened
				editor.commit();
				return true;
			}

		}

		@Override
		protected void onPostExecute(final Boolean result) {

			if (!result) {
				Tools.showSetNotSuccessfulToast(MainActivity.this);
			}

			if (mainFragment != null) {
				// Show DND status to the user through the color of the bottom bar (not ActionBar) - red for DND active, else grey/blue depending on theme
				mainFragment.setDNDBarColor(sharedPrefs.getBoolean(Constants.DO_NOT_DISTURB, false));
			}

		}
	}

	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
	 * one of the sections/tabs/pages.
	 */
	public class SectionsPagerAdapter extends FragmentPagerAdapter {

		public SectionsPagerAdapter(final FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(final int position) {
			Fragment fragment = null;
			// getItem is called to instantiate the fragment for the given page.
			// Return a DummySectionFragment (defined as a static inner class
			// below) with the page number as its lone argument.
			if (position == 0) {
				fragment = new ContactListFragment();

				clFragment = (ContactListFragment) fragment;
			} else if (position == 1) {

				fragment = new MainFragment();

				mainFragment = (MainFragment) fragment;
			} else if (position == 2) {
				Bundle args = new Bundle();

				fragment = new MiscFragment();

				miscFragment = (MiscFragment) fragment;

				if (Tools.getIntPreference(sharedPrefs, Constants.SETTING_THEME, Constants.THEME_LIGHT) == Constants.THEME_LIGHT) {
					args.putInt(Constants.MISC_ITEM_ICONS, R.array.misc_item_icons);
				} else {
					args.putInt(Constants.MISC_ITEM_ICONS, R.array.misc_item_icons_dark);
				}
				args.putInt(Constants.MISC_ITEM_STRINGS, R.array.misc_item_texts);
				fragment.setArguments(args);
			}
			return fragment;
		}

		@Override
		public int getCount() {
			// Show 3 total pages.
			return 3;
		}

		public void onPageChange() {

		}

		@Override
		public CharSequence getPageTitle(final int position) {
			switch (position) {
			case 0:
				return getString(R.string.title_section1);
			case 1:
				return getString(R.string.title_section2);
			case 2:
				return getString(R.string.title_section3);
			}
			return null;
		}

	}

	/**
	 * Receives an event when connections is found or lost.
	 */
	private final BroadcastReceiver mConnReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {

			NetworkInfo currentNetworkInfo = (NetworkInfo) intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);

			if (currentNetworkInfo.isConnected()) {
				launchAsyncTasks();

				if (socket == null || socket != null && !socket.isConnected()) {
					connectToSIO();
				}
			}

			resumeCLFragment();
			resumeMainFragment();
			resumeMiscFragment();

		}
	};

	private ArrayList<CallHolder> calls;

	@Override
	public Context getContext() {
		return this;
	}

	@Override
	public void onReceiveWCStatus(final String[] possibleMessages, final int[] possibleMessageIDs) {
		if (mainFragment != null) {
			mainFragment.onReceiveWCStatus(possibleMessages, possibleMessageIDs);
		}
	}

	@Override
	public Editor getPrefEditor() {
		return editor;
	}

	@Override
	public void onReceiveVoicemail(final boolean hasVoicemail) {
		if (mainFragment != null) {
			mainFragment.onReceiveVoicemail(hasVoicemail);
		}

	}

	public void showCallSettingDialog() {
		final AlertDialog.Builder bldr = Tools.getSimpleDialog(this, R.string.call_settings, -1);

		final String[] callSettingNames = getResources().getStringArray(R.array.call_setting_texts);

		bldr.setItems(callSettingNames, new OnClickListener() {

			@Override
			public void onClick(final DialogInterface dialog, final int which) {

				AlertDialog.Builder innerBldr = Tools.getSimpleDialog(MainActivity.this, callSettingNames[which], null);
				if (which == 0) {
					// Display number
					final String[] possibleOutgoingNumbers = sharedPrefs.getString(Constants.POSS_OUTGOING_NUMBERS, "").split(",");
					String currentOutgoingNumber = sharedPrefs.getString(Constants.CURRENT_OUTGOING_NUMBER, "");
					int currentIndex = -1;

					// Find which index the current outgoing number has
					for (int i = 0, length = possibleOutgoingNumbers.length; i < length; i++) {
						if (possibleOutgoingNumbers[i].equals(currentOutgoingNumber)) {
							currentIndex = i;
						}
					}

					innerBldr.setSingleChoiceItems(possibleOutgoingNumbers, currentIndex, new OnClickListener() {

						@Override
						public void onClick(final DialogInterface dialog, final int which) {

							// Send the new number to the server
							new Task.SetCurrOutgoingNumberTask(user, possibleOutgoingNumbers[which], MainActivity.this, editor).execute();
							dialog.dismiss();

						}

					});
				}

				innerBldr.setNegativeButton(R.string.cancel, new OnClickListener() {

					@Override
					public void onClick(final DialogInterface dialog, final int which) {
						bldr.show();

					}
				});

				innerBldr.setOnCancelListener(new DialogInterface.OnCancelListener() {

					@Override
					public void onCancel(final DialogInterface dialog) {
						bldr.show();

					}
				});
				innerBldr.show();

			}
		});

		bldr.show();

	}

	public void showUserDialog() {
		AlertDialog.Builder bldr = Tools.getSimpleDialog(this, R.string.user, -1);
		LayoutInflater inflater = getLayoutInflater();
		View v = inflater.inflate(R.layout.popup_user, null);
		bldr.setView(v);

		final EditText fNameInput = (EditText) v.findViewById(R.id.user_fname_input);
		fNameInput.setText(sharedPrefs.getString(Constants.USER_FIRST_NAME, ""));

		final EditText lNameInput = (EditText) v.findViewById(R.id.user_lname_input);
		lNameInput.setText(sharedPrefs.getString(Constants.USER_LAST_NAME, ""));

		final EditText eMailInput = (EditText) v.findViewById(R.id.user_email_input);
		eMailInput.setText(sharedPrefs.getString(Constants.USER_EMAIL, ""));

		final EditText mobileNumberInput = (EditText) v.findViewById(R.id.user_n_mobile_input);
		mobileNumberInput.setText(sharedPrefs.getString(Constants.USER_MOBILE_PHONE, ""));

		final EditText homeNumberInput = (EditText) v.findViewById(R.id.user_n_home_input);
		homeNumberInput.setText(sharedPrefs.getString(Constants.USER_HOME_PHONE, ""));

		final EditText passwordInput = (EditText) v.findViewById(R.id.user_password_input);
		passwordInput.setText(sharedPrefs.getString(Constants.USER_PASSWORD, ""));

		final Spinner rtSpinner = (Spinner) v.findViewById(R.id.user_ringtime_spinner);
		int userRingtime = sharedPrefs.getInt(Constants.USER_RINGTIME, 30);
		rtSpinner.setSelection(userRingtime > 0 && userRingtime <= rtSpinner.getAdapter().getCount() ? userRingtime - 1 : 0);

		final Spinner cfrtSpinner = (Spinner) v.findViewById(R.id.user_cf_ringtime_spinner);

		String cfRingTime = sharedPrefs.getString(Constants.USER_FORW_RINGTIME, "30");
		if (cfRingTime.equals("a") || cfRingTime.equals("")) {
			cfrtSpinner.setSelection(0);
		} else {
			int forwardingRingtime = Integer.parseInt(cfRingTime);
			cfrtSpinner.setSelection(forwardingRingtime >= 0 && forwardingRingtime < cfrtSpinner.getAdapter().getCount() ? forwardingRingtime : 0);
		}

		bldr.setPositiveButton(R.string.cont, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(final DialogInterface dialog, final int which) {

				int cfTimeInt = 0;
				int selectedPosition = cfrtSpinner.getSelectedItemPosition();

				if (selectedPosition == 0) {
					cfTimeInt = -1;
				} else {
					// Get selected item
					String temp = cfrtSpinner.getSelectedItem().toString();

					cfTimeInt = Integer.parseInt(temp.substring(0, temp.indexOf(' ')));
				}

				new Task.SetUserInfoTask(user, fNameInput.getText().toString(), lNameInput.getText().toString(), eMailInput.getText().toString(), mobileNumberInput.getText().toString(),
						homeNumberInput.getText().toString(), rtSpinner.getSelectedItemPosition() + 1, cfTimeInt, passwordInput.getText().toString(), MainActivity.this, miscFragment).execute();

			}

		});

		bldr.show();
	}

	public void showAboutDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		View view = getLayoutInflater().inflate(R.layout.popup_about, null);

		PackageManager manager = getPackageManager();
		PackageInfo appInfo = null;
		try {
			// Get the application info
			appInfo = manager.getPackageInfo(getPackageName(), 0);
		} catch (NameNotFoundException e) {
			Log.e("MiscFragment.showAboutDialog()", e.getLocalizedMessage());
		}

		ImageView logoView = (ImageView) view.findViewById(R.id.about_image);
		if (getAppTheme() != Constants.THEME_LIGHT) {
			logoView.setImageResource(R.drawable.logo_white);
		}

		// Get the copyright TextView
		TextView copyrightText = (TextView) view.findViewById(R.id.about_copyright_text);
		copyrightText.setTypeface(getTypefaceRobotoLight());

		// Get the version code TextView
		TextView versionCodeText = (TextView) view.findViewById(R.id.about_version_code_text);
		versionCodeText.setTypeface(getTypefaceRobotoLight());

		// Get the version name TextView
		TextView versionNameText = (TextView) view.findViewById(R.id.about_version_number_text);
		versionNameText.setTypeface(getTypefaceRobotoLight());

		if (appInfo != null) {
			// Set the TextView texts to the current version names and numbers
			versionCodeText.setText(getString(R.string.about_version_no) + " " + appInfo.versionCode);
			versionNameText.setText(getString(R.string.about_version_name) + appInfo.versionName);
		}

		builder.setView(view);

		builder.setTitle(R.string.about);
		builder.setPositiveButton(R.string.ok, new OnClickListener() {

			@Override
			public void onClick(final DialogInterface dialog, final int which) {
				dialog.dismiss();
			}
		});
		builder.show();

	}

	public void showHelpDialog() {
		final TypedArray iconIdArray = getResources().obtainTypedArray(R.array.help_item_icons);

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		ArrayList<MiscItemHolder> helpItems = new ArrayList<MiscItemHolder>();
		String[] textArray = getResources().getStringArray(R.array.help_item_texts);

		View view = getLayoutInflater().inflate(R.layout.popup_help, null);
		ListView helpList = (ListView) view.findViewById(R.id.help_list);

		for (int i = 0, length = textArray.length; i < length; i++) {
			helpItems.add(new MiscItemHolder(iconIdArray.getResourceId(i, 0), textArray[i]));
		}

		TextView header = new TextView(this);
		header.setText(R.string.help_symbol_explanation);
		header.setTextSize(17);
		header.setTypeface(robotoMedium);
		helpList.addHeaderView(header);

		helpList.setAdapter(new HelpListAdapter(helpItems));

		builder.setView(view);

		builder.setTitle(R.string.help);
		builder.setPositiveButton(R.string.ok, new OnClickListener() {

			@Override
			public void onClick(final DialogInterface dialog, final int which) {
				dialog.dismiss();
			}
		});

		iconIdArray.recycle();
		builder.show();

	}

	public void showLogoffDialog() {
		AlertDialog.Builder builder;
		builder = Tools.getSimpleDialog(this, R.string.log_out, R.string.log_out_prompt);
		builder.setPositiveButton(R.string.cont, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(final DialogInterface dialog, final int which) {

				// Log off user and go to login screen
				logOff();

			}
		});
		builder.show();
	}

	public void showFollowMeDialog() {
		AlertDialog.Builder builder;
		builder = Tools.getSimpleDialog(this, R.string.follow_me, -1);

		// Inflate and show the forwarding popup view
		LayoutInflater inflater = getLayoutInflater();
		View v = inflater.inflate(R.layout.popup_follow_me, null);
		builder.setView(v);

		// Initialize all number fields and get old values

		final CheckBox enableCheck = (CheckBox) v.findViewById(R.id.follow_me_check);
		enableCheck.setChecked(sharedPrefs.getBoolean(Constants.FOLLOWME_ENABLE, false));

		final Spinner mainSpinner = (Spinner) v.findViewById(R.id.fm_main_spinner);
		mainSpinner.setSelection(sharedPrefs.getInt(Constants.FOLLOWME_MAIN_DURATION, 0));

		final Spinner listSpinner = (Spinner) v.findViewById(R.id.fm_list_spinner);
		listSpinner.setSelection(sharedPrefs.getInt(Constants.FOLLOWME_LIST_DURATION, 0));

		final EditText numberInput = (EditText) v.findViewById(R.id.fm_numbers);
		numberInput.setText(sharedPrefs.getString(Constants.FOLLOWME_LIST, ""));

		final CheckBox confirmationCheck = (CheckBox) v.findViewById(R.id.fm_confirmation_check);
		confirmationCheck.setChecked(sharedPrefs.getBoolean(Constants.FOLLOWME_CONFIRMATION, false));

		builder.setPositiveButton(R.string.ok, new OnClickListener() {

			@Override
			public void onClick(final DialogInterface dialog, final int which) {

				// Send the field values to server
				new Task.SetFollowMeTask(user, enableCheck.isChecked(), numberInput.getText().toString(), mainSpinner.getSelectedItemPosition(), listSpinner.getSelectedItemPosition(),
						confirmationCheck.isChecked(), MainActivity.this, editor).execute();
			}
		});

		builder.show();
	}

	public void logOff() {

		// Save application settings before clearing sharedprefs
		String startupTab = sharedPrefs.getString(Constants.SETTING_STARTUP_TAB, "0");
		String callMode = sharedPrefs.getString(Constants.SETTING_CALL_LOCAL, "0");
		String theme = sharedPrefs.getString(Constants.SETTING_THEME, "0");
		String updateFrequency = sharedPrefs.getString(Constants.SETTING_UPDATE_FREQUENCY, "0");
		String shortcuts = sharedPrefs.getString(Constants.SETTING_SHORTCUT, "");
		String showCallWindow = sharedPrefs.getString(Constants.SETTING_CALL_WINDOW, Constants.CALL_WINDOW_NONE + "");
		String voicemailNumber = sharedPrefs.getString(Constants.SETTING_VOICEMAIL_NUMBER, Constants.DEFAULT_VOICEMAIL_NUMBER);

		// Clear shared preferences
		editor.clear();

		// Restore stored values
		editor.putBoolean(Constants.SIGNED_IN, false);
		editor.putString(Constants.LOGIN_NAME, "");
		editor.putString(Constants.LATEST_VOICEMAIL, "");
		editor.putString(Constants.LATEST_ACKED_VOICEMAIL, "");
		editor.putString(Constants.SETTING_STARTUP_TAB, startupTab);
		editor.putString(Constants.SETTING_CALL_LOCAL, callMode);
		editor.putString(Constants.SETTING_THEME, theme);
		editor.putString(Constants.SETTING_UPDATE_FREQUENCY, updateFrequency);
		editor.putString(Constants.SETTING_SHORTCUT, shortcuts);
		editor.putString(Constants.SETTING_CALL_WINDOW, showCallWindow);
		editor.putString(Constants.SETTING_VOICEMAIL_NUMBER, voicemailNumber);
		editor.commit();

		// Remove all cached values from database for security
		dbAdapter.open();
		dbAdapter.deleteAllCalls();
		dbAdapter.deleteAllCallHistory();
		dbAdapter.deleteAllContacts();
		dbAdapter.deleteAllQueues();
		dbAdapter.deleteAllFolders();
		dbAdapter.deleteAllWCContacts();
		dbAdapter.close();

		Intent intent = new Intent(MainActivity.this, LoginActivity.class);
		startActivity(intent);

		finish();
	}

	@SuppressLint("NewApi")
	public void showTransferDialog(final String callExtension) {
		if (isNetworkAvailable()) {

			calls = dbAdapter.fetchAllCalls();
			final AlertDialog.Builder builder;

			if (calls.size() > 0) {
				builder = new AlertDialog.Builder(this);

				CharSequence[] nameArray = new CharSequence[calls.size()];

				// Populate the array which will be displayed in the popup
				for (int i = 0, length = calls.size(); i < length; i++) {
					CallHolder h = calls.get(i);
					nameArray[i] = h.getContact().getName() + "\n" + h.getContact().getExtension();
				}
				if (calls.size() != 1) {
					builder.setTitle(R.string.transfer_select_call);
					builder.setSingleChoiceItems(nameArray, 0, null);
				} else {
					builder.setTitle(R.string.transfer_select_manner);

				}

				// Set the negative button to dismiss the dialog
				builder.setNegativeButton(R.string.cancel, null);

				// Set the neutral (middle) button to transfer the call blindly
				builder.setNeutralButton(R.string.transfer_direct, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(final DialogInterface dialog, final int which) {
						// Get the AlertDialog's internal ListView
						ListView lw = ((AlertDialog) dialog).getListView();
						// Get the selected item in the listview (or 0 if there is only one call)
						int checkedItemPosition = calls.size() == 1 ? 0 : lw.getCheckedItemPosition();

						// Transfer the call to the selected contact and dismiss dialog
						Task.TransferTask task = new Task.TransferTask(user, calls.get(checkedItemPosition).getCallId1(), callExtension, MainActivity.this);
						Toast.makeText(MainActivity.this, R.string.call_transferring, Toast.LENGTH_LONG).show();
						if (Tools.isHoneycombOrLater()) {
							task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
						} else {
							task.execute();
						}
					}
				});

				builder.setPositiveButton(R.string.transfer_att, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(final DialogInterface dialog, final int which) {

						// Get the AlertDialog's internal ListView
						ListView lw = ((AlertDialog) dialog).getListView();
						// Get the selected item in the listview (or 0 if there is only one call)
						int checkedItemPosition = calls.size() == 1 ? 0 : lw.getCheckedItemPosition();
						String callID = calls.get(checkedItemPosition).getCallId1();

						ContactListFragment.startAttendedTransfer(user, callID, callExtension, MainActivity.this, sharedPrefs);

					}
				});

				builder.show();
			} else {
				builder = Tools.getSimpleDialog(MainActivity.this, R.string.no_calls, -1);
				builder.show();
			}

		} else {
			Toast.makeText(MainActivity.this, R.string.no_internet, Toast.LENGTH_LONG).show();
		}
	}

	public AlertDialog.Builder getAddContactDialog(final Bundle args) {
		return getContactDialog(0, args);
	}

	public AlertDialog.Builder getContactDialog(final int contactID, final Bundle args) {
		String fullName = "", phoneNumber = "", firstName = "", lastName = "", cellNumber = "", workNumber = "", emailAddress = "";
		int primaryNumber = WCContactHolder.PRIMARY_NUMBER_CELL;

		if (args != null) {
			fullName = args.getString(MexDbAdapter.KEY_CONTACT_NAME);
			phoneNumber = args.getString(MexDbAdapter.KEY_REMOTE_NUMBER);

			int firstNameEndIndex = fullName.indexOf(" ") == -1 ? fullName.length() : fullName.indexOf(" ");
			firstName = fullName.substring(0, firstNameEndIndex);

			// If there is a last name, set the text field
			if (firstName.length() + 1 < fullName.length()) {
				lastName = fullName.substring(fullName.indexOf(" ") + 1);
			}

			if (phoneNumber.startsWith("07")) {
				cellNumber = phoneNumber;
				primaryNumber = WCContactHolder.PRIMARY_NUMBER_CELL;
			} else {
				workNumber = phoneNumber;
				primaryNumber = WCContactHolder.PRIMARY_NUMBER_WORK;
			}

		}

		return getContactDialog(new WCContactHolder(contactID, firstName, lastName, "", workNumber, cellNumber, getResources().getStringArray(R.array.folder_type_values)[0], emailAddress, 0,
				primaryNumber));
	}

	public AlertDialog.Builder getContactDialog(final WCContactHolder contact) {

		final ArrayList<String> folderTitles = new ArrayList<String>();
		final ArrayList<FolderHolder> tempFolders = dbAdapter.fetchAllFolders();

		int parentFolderIndex = 0;

		// Get dialog and inflate view
		int titleResourceID = contact != null && contact.getId() != 0 ? R.string.contact_update : R.string.contact_add;
		final AlertDialog.Builder contactDialog = Tools.getSimpleDialog(this, titleResourceID, -1);

		final View contentView = getLayoutInflater().inflate(R.layout.popup_add_contact, null);
		contactDialog.setView(contentView);

		final EditText workNumberInput = (EditText) contentView.findViewById(R.id.contact_n_work_input);
		final EditText cellNumberInput = (EditText) contentView.findViewById(R.id.contact_n_mobile_input);
		final EditText homeNumberInput = (EditText) contentView.findViewById(R.id.contact_n_home_input);

		final EditText firstNameInput = (EditText) contentView.findViewById(R.id.contact_fname_input);
		final EditText lastNameInput = (EditText) contentView.findViewById(R.id.contact_lname_input);
		final EditText emailInput = (EditText) contentView.findViewById(R.id.contact_email_input);

		final Spinner primaryNumberSpinner = (Spinner) contentView.findViewById(R.id.contact_primary_number_input);
		final Spinner typeSpinner = (Spinner) contentView.findViewById(R.id.contact_type_input);

		folderTitles.add("-- Root --");
		for (FolderHolder folder : tempFolders) {
			folderTitles.add(folder.getTitle());
			if (contact != null && folder.getId() == contact.getFolderID()) {
				parentFolderIndex = folderTitles.size() - 1;
			}
		}

		final Spinner folderSpinner = (Spinner) contentView.findViewById(R.id.contact_folder_input);
		// Fill the spinner with possible parent folders (all folders)
		folderSpinner.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, folderTitles));

		// If there are arguments, add these to the text fields
		if (contact != null) {

			cellNumberInput.setText(contact.getCellNumber());
			workNumberInput.setText(contact.getWorkNumber());
			homeNumberInput.setText(contact.getHomeNumber());

			firstNameInput.setText(contact.getFirstName());
			lastNameInput.setText(contact.getLastName());

			emailInput.setText(contact.getEmailAddress());

			primaryNumberSpinner.setSelection(contact.getPrimaryNumber());

			String[] contactTypes = getResources().getStringArray(R.array.folder_types);
			int typeIndex = 0;
			// Loop through possible item types, set Spinner selection when current one found
			for (int i = 0; i < contactTypes.length; i++) {
				if (contactTypes[i].equalsIgnoreCase(contact.getType())) {
					typeIndex = i;
					break;
				}
			}

			typeSpinner.setSelection(typeIndex);
			folderSpinner.setSelection(parentFolderIndex);

		}

		contactDialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(final DialogInterface dialog, final int which) {
				String firstName = firstNameInput.getText().toString().trim();
				String lastName = lastNameInput.getText().toString().trim();

				// It is not enough to input either first or last name
				if (firstName.length() > 0 || lastName.length() > 0) {
					// Get selected contact type
					String type = getResources().getStringArray(R.array.folder_type_values)[typeSpinner.getSelectedItemPosition()];

					// Get the entered number values and primary number value
					int primaryNumberID = primaryNumberSpinner.getSelectedItemPosition();
					String homeNumber = homeNumberInput.getText().toString();
					String workNumber = workNumberInput.getText().toString();
					String cellNumber = cellNumberInput.getText().toString();
					String emailAddress = emailInput.getText().toString();

					int selectedItemPosition = folderSpinner.getSelectedItemPosition();
					int folderID;

					// If the folder ID is root, send -1. Otherwise send ID corresponding to selected folder
					if (selectedItemPosition != 0) {
						folderID = tempFolders.get(folderSpinner.getSelectedItemPosition() - 1).getId();
					} else {
						folderID = -1;
					}

					// Try to add contact. Replace Swedish characters
					Task.SetContactTask addContactTask = new Task.SetContactTask(user, MainActivity.this, dbAdapter, new WCContactHolder(contact.getId(), firstName, lastName, homeNumber, workNumber,
							cellNumber, type, emailAddress, folderID, primaryNumberID));

					addContactTask.execute();
				} else {

					// Show error dialog
					AlertDialog.Builder bldr = Tools.getSimpleDialog(MainActivity.this, R.string.error, R.string.error_name);
					bldr.show();
				}
			}
		});

		if (contact.getId() != 0) {
			contactDialog.setNeutralButton(R.string.delete, new OnClickListener() {

				@Override
				public void onClick(final DialogInterface arg0, final int arg1) {
					AlertDialog.Builder deleteDialog = Tools.getSimpleDialog(MainActivity.this, R.string.delete, R.string.delete_contact_confirm);

					deleteDialog.setPositiveButton(R.string.delete, new OnClickListener() {

						@Override
						public void onClick(final DialogInterface arg0, final int arg1) {
							// Try to add contact. Replace Swedish characters
							Task.DeleteContactTask deleteContactTask = new DeleteContactTask(user, MainActivity.this, dbAdapter, contact);

							deleteContactTask.execute();
						}
					});

					deleteDialog.show();
				}
			});
		}

		return contactDialog;
	}

	public AlertDialog.Builder getFolderDialog(final FolderHolder newFolder) {
		final ArrayList<String> folderTitles = new ArrayList<String>();
		final ArrayList<FolderHolder> tempFolders = dbAdapter.fetchAllFolders();
		final LayoutInflater inflater = getLayoutInflater();
		int parentFolderIndex = 0;
		folderTitles.add("-- Root --");
		for (FolderHolder folder : tempFolders) {
			folderTitles.add(folder.getTitle());
			if (newFolder != null && folder.getId() == newFolder.getParent()) {
				parentFolderIndex = folderTitles.size() - 1;
			}
		}

		// Get AlertDialog and inflate view
		Builder builder = Tools.getSimpleDialog(this, newFolder != null && newFolder.getId() == 0 ? string.folder_add : string.folder_update, -1);
		final View contentView = inflater.inflate(layout.popup_add_folder, null);

		builder.setView(contentView);

		final Spinner parentSpinner = (Spinner) contentView.findViewById(id.folder_parent_input);
		final EditText titleText = (EditText) contentView.findViewById(id.folder_title_input);
		final Spinner typeSpinner = (Spinner) contentView.findViewById(id.folder_type_input);

		// Fill the spinner with possible parent folders (all folders)
		parentSpinner.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, folderTitles));

		// If we are updating an existing folder, set fields correspondingly
		if (newFolder != null) {
			parentSpinner.setSelection(parentFolderIndex);
			titleText.setText(newFolder.getTitle());

			String[] folderTypes = getResources().getStringArray(array.folder_types);
			int typeIndex = 0;
			// Loop through possible item types, set Spinner selection when current one found
			for (int i = 0; i < folderTypes.length; i++) {
				if (folderTypes[i].equalsIgnoreCase(newFolder.getType())) {
					typeIndex = i;
					break;
				}
			}

			typeSpinner.setSelection(typeIndex);

		}

		// When clicked, try to create a new folder
		builder.setPositiveButton(string.ok, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(final DialogInterface dialog, final int which) {
				if (titleText.getText().length() > 0) {
					// Get the selected folder type
					String type = getResources().getStringArray(array.folder_type_values)[typeSpinner.getSelectedItemPosition()];

					int selectedItemPosition = parentSpinner.getSelectedItemPosition();
					int parent;

					// If root folder has been selected send -1. Otherwise send correct ID for folder
					if (selectedItemPosition != 0) {
						parent = tempFolders.get(parentSpinner.getSelectedItemPosition() - 1).getId();
					} else {
						parent = -1;
					}

					FolderHolder tempFolder = new FolderHolder(titleText.getText().toString(), type, parent);
					// If we are updating a folder, set correct info
					if (newFolder != null) {
						tempFolder.setId(newFolder.getId());
						tempFolder.setLeft(newFolder.getLeft());
						tempFolder.setRight(newFolder.getRight());
						tempFolder.setItemLevel(newFolder.getItemLevel());
					}

					// Try to add folder
					AddFolderTask addFolderTask = new AddFolderTask(user, MainActivity.this, dbAdapter, tempFolder);
					addFolderTask.execute();
				} else {
					titleText.setError(getString(string.error_field_required));

				}
			}
		});

		if (newFolder != null && newFolder.getId() != 0) {
			builder.setNeutralButton(string.delete, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(final DialogInterface arg0, final int arg1) {
					Builder deleteDialog = Tools.getSimpleDialog(MainActivity.this, string.delete, string.delete_folder_confirm);

					deleteDialog.setPositiveButton(string.delete, new DialogInterface.OnClickListener() {

						@Override
						public void onClick(final DialogInterface arg0, final int arg1) {
							// Try to add contact. Replace Swedish characters
							DeleteFolderTask deleteFolderTask = new DeleteFolderTask(user, MainActivity.this, dbAdapter, newFolder);
							deleteFolderTask.execute();
						}
					});

					deleteDialog.show();
				}
			});
		}

		return builder;
	}

	public class HelpListAdapter extends BaseAdapter {

		MiscItemHolder[] items;
		Bitmap defaultContactImage;

		public HelpListAdapter(final ArrayList<MiscItemHolder> items) {
			this.items = new MiscItemHolder[items.size()];
			items.toArray(this.items);

			defaultContactImage = Tools.getDefaultContactPhoto(MainActivity.this);
		}

		@SuppressLint("NewApi")
		@Override
		public View getView(final int position, View convertView, final ViewGroup parent) {
			MiscItemHolder tempHolder = items[position];

			if (convertView == null) {
				convertView = LayoutInflater.from(MainActivity.this).inflate(R.layout.list_item_help, null);
			}

			TextView txt = (TextView) convertView.findViewById(R.id.misc_item_text);
			txt.setText(tempHolder.getText());

			ImageView view = (ImageView) convertView.findViewById(R.id.misc_item_icon);

			// Since contact availability is handled using tints rather than different drawables
			switch (position) {
			case 0:
				view.setImageBitmap(defaultContactImage);
				view.setColorFilter(Constants.COLOR_TINT_AVAILABLE);
				view.setScaleType(ScaleType.CENTER_CROP);
				break;
			case 1:
				view.setImageBitmap(defaultContactImage);
				view.setColorFilter(Constants.COLOR_TINT_TRANSPARENT);
				view.setScaleType(ScaleType.CENTER_CROP);
				break;
			case 2:
				view.setImageBitmap(defaultContactImage);
				view.setColorFilter(Constants.COLOR_TINT_BUSY);
				view.setScaleType(ScaleType.CENTER_CROP);
				break;
			case 3:
				view.setImageBitmap(defaultContactImage);
				view.setColorFilter(Constants.COLOR_TINT_INCALL);
				view.setScaleType(ScaleType.CENTER_CROP);
				break;
			case 6:
			case 15:
				view.setImageResource(tempHolder.drawableResId);
				view.setColorFilter(Color.RED);
				view.setScaleType(ScaleType.CENTER_INSIDE);
				break;
			case 13:
				view.setImageResource(tempHolder.drawableResId);
				view.setColorFilter(Color.rgb(50, 200, 100));
				view.setScaleType(ScaleType.CENTER_INSIDE);
				break;
			case 14:
				view.setImageResource(tempHolder.drawableResId);
				view.setColorFilter(Color.rgb(50, 150, 180));
				view.setScaleType(ScaleType.CENTER_INSIDE);
				break;
			default:
				view.setImageResource(tempHolder.drawableResId);
				view.setColorFilter(Constants.COLOR_TINT_TRANSPARENT);
				view.setScaleType(ScaleType.CENTER_INSIDE);
				break;
			}

			return convertView;
		}

		@Override
		public long getItemId(final int position) {

			return position;
		}

		@Override
		public Object getItem(final int position) {
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

	public class MiscItemHolder {
		private int drawableResId;
		private String text;

		public MiscItemHolder(final int drawableResId, final int textId) {
			this(drawableResId, getString(textId));
		}

		public MiscItemHolder(final int drawableResId, final String text) {
			this.drawableResId = drawableResId;
			this.text = text;
		}

		public int getDrawableResId() {
			return drawableResId;
		}

		public void setDrawableResId(final int drawableResId) {
			this.drawableResId = drawableResId;
		}

		public String getText() {
			return text;
		}

		public void setText(final String text) {
			this.text = text;
		}

	}

}
