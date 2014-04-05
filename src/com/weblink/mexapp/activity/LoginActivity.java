package com.weblink.mexapp.activity;

import java.util.ArrayList;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.weblink.mexapp.R;
import com.weblink.mexapp.net.HTTPResources;
import com.weblink.mexapp.pojo.User;
import com.weblink.mexapp.utility.Constants;
import com.weblink.mexapp.utility.Tools;

/**
 * Activity which displays a login screen to the user, offering registration as
 * well.
 */
public class LoginActivity extends SherlockActivity {

	/**
	 * Keep track of the login task to ensure we can cancel it if requested.
	 */
	private UserLoginTask mAuthTask = null;

	// Values for email and password at the time of the login attempt.
	private String company;
	private String password;

	// UI references.
	private EditText companyView;
	private EditText passwordView;
	private View mLoginFormView;
	private View mLoginStatusView;
	private TextView mLoginStatusMessageView;

	private SharedPreferences sharedPrefs;
	private Editor editor;

	private EditText extensionView;

	private int extension;

	private int APP_THEME;

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		// Get saved settings from previous login
		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

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

		final int actionBarTitle = Resources.getSystem().getIdentifier("action_bar_title", "id", "android");
		final TextView title = (TextView) getWindow().findViewById(actionBarTitle);
		if (title != null) {
			Typeface typeface = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Light.ttf");
			title.setTypeface(typeface);
		}

		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_login);

		editor = sharedPrefs.edit();

		if (sharedPrefs.getBoolean(Constants.SIGNED_IN, false)) {

			Intent intent = new Intent(LoginActivity.this, MainActivity.class);
			intent.putExtra(Constants.SETTING_THEME, Tools.getIntPreference(sharedPrefs, Constants.SETTING_THEME, Constants.THEME_LIGHT));

			startActivity(intent);
			finish();

		}

		// Set up the login form.
		companyView = (EditText) findViewById(R.id.company_text);
		companyView.setText(sharedPrefs.getString(Constants.LOGIN_COMPANY, ""));

		int extension = sharedPrefs.getInt(Constants.LOGIN_EXTENSION, 0);
		extensionView = (EditText) findViewById(R.id.extension_text);
		extensionView.setText(extension == 0 ? "" : extension + "");

		passwordView = (EditText) findViewById(R.id.password);
		passwordView.setText(sharedPrefs.getString(Constants.LOGIN_PASSWORD, ""));

		passwordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(final TextView textView, final int id, final KeyEvent keyEvent) {
				if (id == R.id.login || id == EditorInfo.IME_NULL) {
					attemptLogin();
					return true;
				}
				return false;
			}
		});

		mLoginFormView = findViewById(R.id.login_form);
		mLoginStatusView = findViewById(R.id.login_status);
		mLoginStatusMessageView = (TextView) findViewById(R.id.login_status_message);

		findViewById(R.id.sign_in_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View view) {

				if (isNetworkAvailable()) {
					attemptLogin();
				} else {
					Toast.makeText(LoginActivity.this, R.string.no_internet, Toast.LENGTH_LONG).show();
				}
			}
		});

		ImageView logo = (ImageView) findViewById(R.id.login_logo);
		if (APP_THEME != Constants.THEME_LIGHT) {
			logo.setImageResource(R.drawable.logo_white);
			if (APP_THEME == Constants.THEME_DARK) {

				if (Tools.isHoneycombOrLater()) {
					logo.setAlpha(0.6f);
				} else {
					logo.setAlpha(153);
				}
			}
		}

	}

	private boolean isNetworkAvailable() {
		ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
		return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
	}

	/**
	 * Attempts to sign in or register the account specified by the login form.
	 * If there are form errors (invalid email, missing fields, etc.), the
	 * errors are presented and no actual login attempt is made.
	 */
	public void attemptLogin() {
		if (mAuthTask != null) {
			return;
		}

		// Reset errors.
		companyView.setError(null);
		extensionView.setError(null);
		passwordView.setError(null);

		String extensionString = extensionView.getText().toString();

		// Store values at the time of the login attempt.
		company = Tools.replaceSwedishCharacters(companyView.getText().toString());
		password = Tools.replaceSwedishCharacters(passwordView.getText().toString());

		Log.d(company, password);

		// Safety check to avoid NumberFormatException for invalid int ""
		if (!extensionString.equals("")) {
			extension = Integer.parseInt(extensionString);
		} else {
			extension = 0;
		}

		boolean cancel = false;
		View focusView = null;

		// Check for a valid password.
		if (TextUtils.isEmpty(password)) {
			passwordView.setError(getString(R.string.error_field_required));
			focusView = passwordView;
			cancel = true;
		} else if (password.length() < 1) {
			passwordView.setError(getString(R.string.error_invalid_password));
			focusView = passwordView;
			cancel = true;
		}

		if (TextUtils.isEmpty(extensionString)) {
			extensionView.setError(getString(R.string.error_field_required));
			focusView = extensionView;
			cancel = true;
		}

		// Check for a valid email address.
		if (TextUtils.isEmpty(company)) {
			companyView.setError(getString(R.string.error_field_required));
			focusView = companyView;
			cancel = true;
		}

		if (cancel) {
			// There was an error; don't attempt login and focus the first
			// form field with an error.
			focusView.requestFocus();
		} else {
			// Show a progress spinner, and kick off a background task to
			// perform the user login attempt.
			mLoginStatusMessageView.setText(R.string.login_progress_signing_in);
			showProgress(true);
			mAuthTask = new UserLoginTask(new User(company, password, extension));
			mAuthTask.execute();
		}
	}

	/**
	 * Shows the progress UI and hides the login form.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
	private void showProgress(final boolean show) {
		// On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
		// for very easy animations. If available, use these APIs to fade-in
		// the progress spinner.
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
			int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

			mLoginStatusView.setVisibility(View.VISIBLE);
			mLoginStatusView.animate().setDuration(shortAnimTime).alpha(show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationEnd(final Animator animation) {
					mLoginStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
				}
			});

			mLoginFormView.setVisibility(View.VISIBLE);
			mLoginFormView.animate().setDuration(shortAnimTime).alpha(show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationEnd(final Animator animation) {
					mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
				}
			});
		} else {
			// The ViewPropertyAnimator APIs are not available, so simply show
			// and hide the relevant UI components.
			mLoginStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
			mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
		}
	}

	public class GetHashTask extends AsyncTask<Void, Void, Boolean> {
		private final User user;

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
			return isSuccessfulLogin(response);
		}

		@Override
		protected void onPostExecute(final Boolean success) {

			finish();

		}

	}

	/**
	 * Represents an asynchronous login/registration task used to authenticate
	 * the user.
	 */
	public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {
		private final User user;

		public UserLoginTask(final User user) {
			super();
			this.user = user;
		}

		@Override
		protected Boolean doInBackground(final Void... params) {

			ArrayList<NameValuePair> nvps = new ArrayList<NameValuePair>();
			nvps.add(new BasicNameValuePair("g", "hash"));

			JSONObject temp = Tools.createJSONObject(nvps);
			String response = HTTPResources.performAction(user, temp);
			return isSuccessfulLogin(response);
		}

		@Override
		protected void onPostExecute(final Boolean success) {
			mAuthTask = null;
			showProgress(false);

			if (success) {
				launchMainActivity();

				editor.putBoolean(Constants.SIGNED_IN, true);
				editor.putString(Constants.LOGIN_COMPANY, company);
				editor.putString(Constants.LOGIN_PASSWORD, password);
				editor.putInt(Constants.LOGIN_EXTENSION, extension);

				// TODO Change to real name
				editor.putString(Constants.LOGIN_NAME, "");

				editor.commit();

				finish();
			} else {

				companyView.setError(getString(R.string.error_incorrect_company));
				extensionView.setError(getString(R.string.error_incorrect_extension));
				passwordView.setError(getString(R.string.error_incorrect_password));

				passwordView.requestFocus();

			}
		}

		private void launchMainActivity() {
			Intent intent = new Intent(LoginActivity.this, MainActivity.class);
			startActivity(intent);
		}

		@Override
		protected void onCancelled() {
			mAuthTask = null;
			showProgress(false);
		}

		/*
		 * private boolean parseJSONObject(final String response) { JSONObject
		 * responseObj; try { responseObj = new JSONObject(response);
		 * 
		 * if (responseObj.has("s") && responseObj.get("s").equals("error")) {
		 * return false; } } catch (JSONException e) { // Parsing to JSONObject
		 * failed, try parsing as JSONArray // instead return
		 * parseJSONArray(response); }
		 * 
		 * return false; }
		 */

	}

	private boolean isSuccessfulLogin(final String response) {
		return parseLoginJSON(response);
	}

	private boolean parseLoginJSON(final String response) {
		if (response != null) {
			try {

				JSONArray json = new JSONArray(response);

				for (int i = 0; i < json.length(); i++) {
					JSONObject obj = (JSONObject) json.get(i);

					boolean isSuccessful = isLoginSuccessful(obj);

					return isSuccessful;
				}

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
