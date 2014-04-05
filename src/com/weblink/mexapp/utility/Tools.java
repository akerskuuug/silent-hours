package com.weblink.mexapp.utility;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Formatter;

import org.apache.http.NameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import wei.mark.standout.StandOutWindow;
import android.annotation.SuppressLint; 
import android.app.Activity; 
import android.app.AlarmManager; 
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint; 
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.weblink.mexapp.R;
import com.weblink.mexapp.activity.MainActivity;
import com.weblink.mexapp.pojo.QueueHolder;
import com.weblink.mexapp.receiver.ScheduleMessageUpdateReceiver;
import com.weblink.mexapp.receiver.VoicemailCallReceiver;
import com.weblink.mexapp.receiver.VoicemailIgnoreReceiver;
import com.weblink.mexapp.services.FloatingCallWindow;

public class Tools { 
	private final static String TAG = "Tools";

	/** Checks whether given string is numeric, returns true if it is
	 * 
	 * @param s the string to check
	 * @return true if string is numeric
	 */
	public static boolean isNumeric(final String s) { 
		return s.matches("[-+]?\\d*\\.?\\d+");
	}

	public static void showCallPopup(final Context context) {

		if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Constants.CALL_WINDOW_DISPLAYED, false)) {
			StandOutWindow.show(context, FloatingCallWindow.class, StandOutWindow.DEFAULT_ID);
		}
	}

	/**
	 * Convenience method for closing any open call popups and tell the system they are closed
	 * @param context
	 */
	public static void dismissCallPopup(final Context context) {
		// Get preference editor
		Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();

		// Allow showing of new windows
		editor.putBoolean(Constants.CALL_WINDOW_DISPLAYED, false).commit();
		editor.putBoolean(Constants.CALL_HELD, false).commit();

		// Dismiss this popup
		StandOutWindow.closeAll(context, FloatingCallWindow.class);
	}

	/**
	 * Returns the current API version from android.os.Build.VERSION.SDK_INT.
	 * Compare with android.os.Build.VERSION_CODES.*
	 * 
	 */
	public static int getAPIversion() {
		return android.os.Build.VERSION.SDK_INT;
	}

	public static boolean isHoneycombOrLater() {
		return getAPIversion() >= android.os.Build.VERSION_CODES.HONEYCOMB;
	}

	public static Bitmap getDefaultContactPhoto(final Context context) {

		Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_contact_picture);

		return Tools.makeCircularBitmap(bitmap);

	}

	/**
	 * This method fixes the formatting in the time for hour and minute values less than 10.
	 * 
	 * @param hours
	 *            self explanatory
	 * @param minutes
	 *            self explanatory
	 * @return the complete string, in the format HH:MM
	 */
	public static String fixTimeFormatting(final int hours, final int minutes) {
		String hr, min;

		// Fix formatting for values less than 10
		hr = hours < 10 ? "0" : "";
		min = minutes < 10 ? "0" : "";

		return hr + hours + ":" + min + minutes;

	}

	/**
	 * Loads contact photo from the device using ContentResolver
	 * @param cr the ContentResolver to query
	 * @param contactID the ID of the contact
	 * @return the contact photo
	 */
	public static Bitmap loadContactPhoto(final ContentResolver cr, final long contactID) {
		Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactID);
		InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(cr, uri);

		if (input == null) {
			return null;

		}

		Bitmap bitmap = BitmapFactory.decodeStream(input);
		return makeCircularBitmap(bitmap);
	}

	/** 
	 * Takes a square bitmap and makes a circular one
	 * @param source
	 * @return
	 */
	public static Bitmap makeCircularBitmap(final Bitmap bitmap) {

		Bitmap inpBitmap = bitmap;
		int width = 0;
		int height = 0;
		width = inpBitmap.getWidth();
		height = inpBitmap.getHeight();

		if (width <= height) {
			height = width;
		} else {
			width = height;
		}

		Bitmap output = Bitmap.createBitmap(width, height, Config.ARGB_8888);
		Canvas canvas = new Canvas(output);

		final Paint paint = new Paint();
		final Rect rect = new Rect(0, 0, width, height);
		final RectF rectF = new RectF(rect);
		final float roundPx = 100;

		paint.setAntiAlias(true);
		canvas.drawARGB(0, 0, 0, 0);
		// paint.setColor(color);
		canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

		paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
		canvas.drawBitmap(inpBitmap, rect, rect, paint);

		return output;

		/*
		Bitmap circleBitmap = Bitmap.createBitmap(source.getWidth(), source.getHeight(), Bitmap.Config.ARGB_8888);

		BitmapShader shader = new BitmapShader(source, TileMode.CLAMP, TileMode.CLAMP);
		Paint paint = new Paint();
		paint.setShader(shader);

		Canvas c = new Canvas(circleBitmap);
		c.drawCircle(30, 30, 50, paint);

		return circleBitmap;*/
	}

	/**
	 * Replaces all Swedish characters (ÅåÄäÖö) with corresponding Unicodes in a string 
	 */
	public static String replaceSwedishCharacters(final String input) {

		StringBuilder b = new StringBuilder(input.length());
		Formatter f = new Formatter(b);

		// Work through the string and replace special chars
		for (char c : input.toCharArray()) {
			if (c < 128) {
				b.append(c);
			} else {
				f.format("\\u%04x", (int) c);
			}
		}
		f.close();

		return b.toString();
	}

	/**
	 * Converts a String array to a JSONArray and returns this.
	 */
	public static JSONArray convertToJSONArray(final String[] strings) {
		JSONArray arr = new JSONArray();
		for (String s : strings) {
			arr.put(s);
		}
		return arr;
	}

	public static void setUpdateAlarm(final AlarmManager alarmManager, final Context context, final int minutesToAlarm) {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MINUTE, minutesToAlarm);

		Intent intent = new Intent(context, ScheduleMessageUpdateReceiver.class);
		PendingIntent pi = PendingIntent.getBroadcast(context, Constants.ALARM_ID_VOICEMAIL, intent, PendingIntent.FLAG_CANCEL_CURRENT);

		alarmManager.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
	}

	public static void smoothScrollListIfTop(final ListView listView, final int scrollTo) {

		listView.post(new Runnable() {
			@Override
			public void run() {
				if (listView.getFirstVisiblePosition() == 0) {
					smoothScrollListView(listView, scrollTo);
				}
			}
		});
	}

	/**
	 * Smooth scrolls the supplied ListView to the desired position
	 * 
	 * Method used to avoid having NewApi everywhere in the Fragments and Activity
	 * @param listView the list to scroll
	 * @param scrollTo the position to scroll to (from top)
	 */
	@SuppressLint("NewApi")
	public static void smoothScrollListView(final ListView listView, final int scrollTo) {
		final int API_VERSION = getAPIversion();

		if (API_VERSION >= android.os.Build.VERSION_CODES.HONEYCOMB) {

			listView.smoothScrollToPositionFromTop(scrollTo, 0);

		} else if (API_VERSION >= android.os.Build.VERSION_CODES.FROYO) {
			listView.smoothScrollToPosition(scrollTo, 0);
		}

	}

	public static JSONObject createJSONObject(final ArrayList<NameValuePair> objectList) {
		JSONObject obj = new JSONObject();

		for (NameValuePair nvp : objectList) {
			try {
				if (nvp instanceof IntegerNameValuePair) {
					obj.accumulate(nvp.getName(), ((IntegerNameValuePair) nvp).getIntValue());
				} else {
					obj.accumulate(nvp.getName(), nvp.getValue());
				}
			} catch (JSONException e) {
				Log.e("Tools JSonException", e.getLocalizedMessage());
			}
		}

		return obj;
	}

	/**
	 * Retrieves a saved integer preference from the supplied SharedPreferences.
	 * This is necessary because integer values stored in Settings are always
	 * stored as Strings.
	 * 
	 * @param prefs
	 *            The SharedPreferences to fetch the integer from
	 * @param key
	 *            The key to the desired preference
	 * @param defaultValue
	 *            The default value for the preference, if no previous value is
	 *            set
	 * 
	 * @return The preference value if found, default value if preference value
	 *         could not be found. -1 if the found preference was not an
	 *         integer.
	 */
	public static int getIntPreference(final SharedPreferences prefs, final String key, final int defaultValue) {
		int value;

		String tempString = prefs.getString(key, defaultValue + "");
		try {
			value = Integer.parseInt(tempString);
		} catch (NumberFormatException e) {
			Log.e("Illegal conversion attempted", tempString + " is not an integer. Returning -1");
			value = -1;
		}

		return value;
	}

	public static void setImageToStatus(final ImageView view, final String statusIdent, final String directionIdent) {

		if (directionIdent.equals(Constants.CALL_DIRECTION_OUT)) {
			if (statusIdent.equals(Constants.CALL_STATUS_RINGING)) {
				view.setImageResource(R.drawable.call_status_out_r);
			} else if (statusIdent.equals(Constants.CALL_STATUS_CONNECTED)) {
				view.setImageResource(R.drawable.call_status_out_c);
			} else if (statusIdent.equals(Constants.CALL_STATUS_HOLD)) {
				view.setImageResource(R.drawable.call_status_hold);
			}
		} else if (directionIdent.equals(Constants.CALL_DIRECTION_IN)) {
			if (statusIdent.equals(Constants.CALL_STATUS_RINGING)) {
				view.setImageResource(R.drawable.call_status_in_r);
			} else if (statusIdent.equals(Constants.CALL_STATUS_CONNECTED)) {
				view.setImageResource(R.drawable.call_status_in_c);
			} else if (statusIdent.equals(Constants.CALL_STATUS_HOLD)) {
				view.setImageResource(R.drawable.call_status_hold);
			}
		}

	}

	public static void setTextToDirection(final String directionIdent, final TextView view) {

		if (directionIdent.equals(Constants.CALL_DIRECTION_OUT)) {
			view.setText(Constants.CALL_DIRECTION_OUT);
		} else if (directionIdent.equals(Constants.CALL_DIRECTION_IN)) {
			view.setText(Constants.CALL_DIRECTION_IN);
		}

	}

	/**
	 * Gives an AlertDialog.Builder with the supplied title and message. Also
	 * comes with the negative button (Cancel) set to dismiss dialog.
	 * 
	 * Positive button must still be specified.
	 * 
	 * @param activity
	 * @param title
	 *            The title (short text) resource ID for the top of the dialog.
	 *            -1 if no title is wanted
	 * @param message
	 *            The message (longer text) resource ID for the body of the
	 *            dialog. -1 if no message is wanted
	 * @return a dialog
	 */
	public static AlertDialog.Builder getSimpleDialog(final Activity activity, final int titleResId, final int messageResId) {

		return getSimpleDialog(activity, titleResId == -1 ? null : activity.getString(titleResId), messageResId == -1 ? null : activity.getString(messageResId));

	}

	/**
	 * Gives an AlertDialog.Builder with the supplied title and message. Also
	 * comes with the negative button (Cancel) set to dismiss dialog.
	 * 
	 * Positive button must still be specified.
	 * 
	 * @param activity
	 * @param title
	 *            The title (short text) for the top of the dialog. May be null
	 *            if no title is wanted
	 * @param message
	 *            The message (longer text) for the body of the dialog. May be
	 *            null if no message is wanted
	 * @return a dialog
	 */
	public static AlertDialog.Builder getSimpleDialog(final Activity activity, final String title, final String message) {
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);

		if (title != null) {
			builder.setTitle(title);
		}

		if (message != null) {
			builder.setMessage(message);
		}

		if (!isHoneycombOrLater()) {
			// builder.set
		}

		builder.setNegativeButton(R.string.cancel, new OnClickListener() {

			@Override
			public void onClick(final DialogInterface dialog, final int which) {
				dialog.dismiss();

			}
		});

		return builder;
	}

	/**
	 * Formats a phone number and removes any illegal characters (Allowed
	 * characters are 0-9, * and #, as well as + in the beginning
	 * 
	 * @param number
	 * @return
	 */
	public static String formatPhoneNumber(final String number) {
		String newNumber = number;

		if (newNumber.startsWith("+")) {
			newNumber = newNumber.replace("+", "00");
		} else if (newNumber.startsWith("0") && !newNumber.startsWith("00")) {
			newNumber = newNumber.replaceFirst("0", "0046");
		}

		newNumber = newNumber.replaceAll("[^0-9*#]", "");

		return newNumber;
	}

	/**
	 * Displays a Toast saying the function is not yet implemented. Not for
	 * release.
	 * 
	 * @param context
	 */
	public static void showNotImplToast(final Context context) {
		Toast.makeText(context, R.string.not_implemented, Toast.LENGTH_SHORT).show();

	}

	/**
	 * Displays a Toast saying Internet is not available
	 * 
	 * @param context
	 */
	public static void showNoInternetToast(final Context context) {
		if (context != null) {
			Toast.makeText(context, R.string.no_internet, Toast.LENGTH_SHORT).show();
		}

	}

	/**
	 * Displays a Toast saying that a task was not completed successfully
	 * 
	 * @param context
	 */
	public static void showSetNotSuccessfulToast(final Context context) {

		if (context != null) {
			Toast.makeText(context, R.string.could_not_set, Toast.LENGTH_LONG).show();
		}
	}

	/**
	 * Displays a Toast saying that a task was not completed successfully
	 * 
	 * @param context
	 */
	public static void showGetNotSuccessfulToast(final Context context) {

		if (context != null) {
			Toast.makeText(context, R.string.could_not_get, Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * Displays a Toast saying that a call update task was not completed successfully
	 * 
	 * @param context
	 */
	public static void showCallNotSuccessfulToast(final Context context) {

		if (context != null) {
			Toast.makeText(context, R.string.could_not_call, Toast.LENGTH_LONG).show();
		}
	}

	public static String[] getQueueNames(final QueueHolder[] queueArray) {
		String[] queueNames = new String[queueArray.length];
		for (int i = 0, length = queueArray.length; i < length; i++) {
			queueNames[i] = queueArray[i].getId() + "";

			String name = queueArray[i].getName();
			queueNames[i] += name.equals("") ? name : ": " + name;
		}

		return queueNames;
	}

	public static boolean[] getEnabledQueues(final QueueHolder[] queueArray) {
		boolean[] queueStatuses = new boolean[queueArray.length];

		for (int i = 0, length = queueArray.length; i < length; i++) {
			queueStatuses[i] = queueArray[i].isLoggedIn();
		}

		return queueStatuses;
	}

	public static int getInt(final JSONObject obj, final String columnName) {

		try {
			return obj.has(columnName) ? obj.getInt(columnName) : 0;
		} catch (Exception e) {
			Log.e("Tools.getInt()", e.getLocalizedMessage());
			return 0;
		}
	}

	public static String getString(final JSONObject obj, final String columnName) {
		try {
			return obj.has(columnName) ? obj.getString(columnName) : "";
		} catch (Exception e) {
			Log.e("Tools.getString()", e.getLocalizedMessage());
			return "";
		}
	}

	public static class Notifications {

		public static Notification showVoicemailNotification(final Context context, final int number) {
			Notification noti;

			Bundle extras = new Bundle();
			extras.putBoolean(Constants.VOICEMAIL_SHOW_DIALOG, true);
			PendingIntent pIntent = getApplicationPendingIntent(context, extras);

			Intent callIntent = new Intent(context, VoicemailCallReceiver.class);
			PendingIntent callPendingIntent = PendingIntent.getBroadcast(context, 0, callIntent, 0);

			Intent ignoreIntent = new Intent(context, VoicemailIgnoreReceiver.class);
			PendingIntent ignorePendingIntent = PendingIntent.getBroadcast(context, 0, ignoreIntent, 0);

			ArrayList<NotificationAction> actions = new ArrayList<NotificationAction>();
			actions.add(new NotificationAction(R.drawable.ic_menu_call_light, context.getString(R.string.call_voicemail), callPendingIntent));
			actions.add(new NotificationAction(R.drawable.navigation_cancel_dark, context.getString(R.string.ignore_voicemail), ignorePendingIntent));

			noti = getNotification(context, number + " " + context.getString(R.string.new_voicemail), context.getString(R.string.new_voicemail_long), R.drawable.device_access_mic_dark, pIntent,
					actions);

			notifyClear(context, Constants.NOTIFICATION_ID_VOICEMAIL, noti);

			return noti;
		}

		public static Notification showCallNotification(final Context context) {

			Notification noti = getNotification(context, R.string.active_calls, R.string.active_calls_long, R.drawable.ic_launcher, getApplicationPendingIntent(context, null), null);

			notifyNoClear(context, Constants.NOTIFICATION_ID_CALLS, noti);

			return noti;
		}

		private static PendingIntent getApplicationPendingIntent(final Context context, final Bundle extras) {
			// Prepare intent which is triggered if the
			// notification is selected
			Intent intent = new Intent(context, MainActivity.class);
			if (extras != null) {
				intent.putExtras(extras);
			}
			PendingIntent pIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_ONE_SHOT);
			return pIntent;
		}

		private static Notification getNotification(final Context context, final int titleResId, final int messageResId, final int iconResId, final PendingIntent pIntent,
				final ArrayList<NotificationAction> actions) {

			return getNotification(context, context.getString(titleResId), context.getString(messageResId), iconResId, pIntent, actions);

		}

		@SuppressLint("NewApi")
		private static Notification getNotification(final Context context, final String title, final String message, final int iconResId, final PendingIntent pIntent,
				final ArrayList<NotificationAction> actions) {

			Notification noti;
			if (getAPIversion() >= android.os.Build.VERSION_CODES.JELLY_BEAN) {

				// Build notification
				Notification.Builder builder = new Notification.Builder(context).setContentTitle(title).setContentText(message).setSmallIcon(iconResId).setContentIntent(pIntent);

				// Add all actions to notification (ONLY on 4.1 and up)
				if (actions != null) {
					for (NotificationAction action : actions) {
						builder.addAction(action.getIconResId(), action.getMessage(), action.getPi());
					}
				}

				noti = builder.build();

			} else {

				noti = new NotificationCompat.Builder(context).setContentTitle(title).setContentText(message).setSmallIcon(iconResId).setContentIntent(pIntent).getNotification();
			}
			return noti;
		}

		private static void notifyClear(final Context context, final int notificationId, final Notification noti) {

			NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
			// Hide the notification after its selected
			noti.flags |= Notification.FLAG_AUTO_CANCEL | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP;

			mNotificationManager.notify(notificationId, noti);
		}

		private static void notifyNoClear(final Context context, final int notificationId, final Notification noti) {

			NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
			// Hide the notification after its selected
			noti.flags |= Notification.FLAG_NO_CLEAR | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP;

			mNotificationManager.notify(notificationId, noti);
		}

		public static void dismissVoicemailNotification(final Context context) {
			dismissNotification(context, Constants.NOTIFICATION_ID_VOICEMAIL);
		}

		public static void dismissCallNotification(final Context context) {
			dismissNotification(context, Constants.NOTIFICATION_ID_CALLS);
		}

		public static void dismissNotification(final Context context, final int notificationId) {
			((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(notificationId);
		}

	}
}
