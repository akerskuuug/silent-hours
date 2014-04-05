package com.weblink.mexapp.utility;

import java.util.ArrayList;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.util.Log;
import android.widget.Toast;

import com.weblink.mexapp.R;
import com.weblink.mexapp.activity.MainActivity;
import com.weblink.mexapp.db.MexDbAdapter;
import com.weblink.mexapp.fragment.MiscFragment;
import com.weblink.mexapp.interfaces.AsyncTaskCompleteListener;
import com.weblink.mexapp.interfaces.CallListener;
import com.weblink.mexapp.interfaces.MexStatusListener;
import com.weblink.mexapp.interfaces.VoicemailListener;
import com.weblink.mexapp.interfaces.WCStatusListener;
import com.weblink.mexapp.net.JSONResources;
import com.weblink.mexapp.pojo.ContactHolder;
import com.weblink.mexapp.pojo.FolderHolder;
import com.weblink.mexapp.pojo.QueueHolder;
import com.weblink.mexapp.pojo.User;
import com.weblink.mexapp.pojo.WCContactHolder;

/**
 * Wrapper class for most of the more generic AsyncTasks.
 * 
 * @author Viktor Åkerskog
 * 
 */
public class Task {

	public static class GetContactImagesTask extends AsyncTask<Void, Void, ArrayList<Bitmap>> {
		private final ContentResolver resolver;
		private final AsyncTaskCompleteListener<ArrayList<Bitmap>> callback;

		public GetContactImagesTask(final ContentResolver resolver, final AsyncTaskCompleteListener<ArrayList<Bitmap>> callback) {
			this.resolver = resolver;

			this.callback = callback;
		}

		@Override
		protected ArrayList<Bitmap> doInBackground(final Void... params) {

			ArrayList<Bitmap> contactImageBitmaps = new ArrayList<Bitmap>();

			Cursor contactCursor = resolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, new String[] { Phone.DISPLAY_NAME, Phone.NUMBER, Phone.CONTACT_ID }, null, null,
					Phone.DISPLAY_NAME + " ASC");

			if (contactCursor != null && contactCursor.moveToFirst()) {
				while (!contactCursor.isAfterLast()) {
					int nameFieldColumnIndex = contactCursor.getColumnIndex(Phone.DISPLAY_NAME);
					if (nameFieldColumnIndex != -1) {
						final int contactID;

						int contactIDColumnIndex = contactCursor.getColumnIndex(Phone.CONTACT_ID);
						if (contactIDColumnIndex != -1) {
							contactID = contactCursor.getInt(contactIDColumnIndex);
						} else {
							contactID = 0;
						}

						contactImageBitmaps.add(Tools.loadContactPhoto(resolver, contactID));
					}
					contactCursor.moveToNext();
				}
			}

			contactCursor.close();

			return contactImageBitmaps;

		}

		@Override
		protected void onPostExecute(final ArrayList<Bitmap> result) {
			// Update the images in the fragment
			callback.finished(result);
		}

	}

	public static class GetExtensionTask extends AsyncTask<Void, Void, Boolean> {
		private final User user;
		private final AsyncTaskCompleteListener<Boolean> callback;
		private final MexDbAdapter dbAdapter;
		private final Context context;

		public GetExtensionTask(final User user, final AsyncTaskCompleteListener<Boolean> callback, final MexDbAdapter dbAdapter, final Context context) {
			super();
			this.user = user;
			this.callback = callback;
			this.dbAdapter = dbAdapter;

			this.context = context;
		}

		@Override
		protected Boolean doInBackground(final Void... params) {
			ArrayList<ContactHolder> contacts = JSONResources.getExtensions(user, context);

			if (contacts != null && !contacts.isEmpty()) {
				if (dbAdapter != null) {

					boolean success = true;

					for (ContactHolder holder : contacts) {
						if (!isCancelled()) {
							if (!dbAdapter.updateExtension(holder)) {
								if (!dbAdapter.createExtension(holder)) {
									success = false;
								}
							}
						}
					}

					return success;

				} else {

					return false;
				}
			} else {

				return false;
			}
		}

		@Override
		protected void onPostExecute(final Boolean result) {
			// Update the list in the fragment
			callback.finished(result);
		}

	}

	public static class AddFolderTask extends AsyncTask<Void, Void, Boolean> {
		private final User user;
		private final AsyncTaskCompleteListener<Boolean> callback;
		private final MexDbAdapter dbAdapter;
		private final FolderHolder folder;

		public AddFolderTask(final User user, final AsyncTaskCompleteListener<Boolean> callback, final MexDbAdapter dbAdapter, final FolderHolder folder) {
			super();
			this.user = user;
			this.callback = callback;
			this.dbAdapter = dbAdapter;
			this.folder = folder;
		}

		@Override
		protected Boolean doInBackground(final Void... arg0) {
			boolean success = JSONResources.setFolder(user, folder);
			if (success && dbAdapter != null) {
				if (!dbAdapter.updateFolder(folder)) {
					dbAdapter.createFolder(folder);
				}
			} else {
				return false;
			}
			return success;
		}

		@SuppressLint("NewApi")
		@Override
		protected void onPostExecute(final Boolean result) {

			GetFolderTask folderTask = new GetFolderTask(user, callback, dbAdapter);

			// Launch AsyncTasks to get WebCall info
			if (Tools.isHoneycombOrLater()) {
				folderTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			} else {
				folderTask.execute();
			}

			// Update the list in the fragment
			callback.finished(result);
		}

	}

	/**
	 * Adds or alters a Web Call contact. If a valid id (!=0) is given to the contact object, it will be updated. Otherwise, a new one will be added 
	 *
	 */
	public static class SetContactTask extends AsyncTask<Void, Void, Boolean> {
		private final User user;
		private final AsyncTaskCompleteListener<Boolean> callback;
		private final MexDbAdapter dbAdapter;
		private final WCContactHolder contact;

		public SetContactTask(final User user, final AsyncTaskCompleteListener<Boolean> callback, final MexDbAdapter dbAdapter, final WCContactHolder contact) {
			super();
			this.user = user;
			this.callback = callback;
			this.dbAdapter = dbAdapter;
			this.contact = contact;
		}

		@Override
		protected Boolean doInBackground(final Void... arg0) {
			// Create a temporary contact, in order to replace Swedish characters in the sent item but not the one put into the list
			WCContactHolder tempContact = contact;
			tempContact.setFirstName(Tools.replaceSwedishCharacters(tempContact.getFirstName()));
			tempContact.setLastName(Tools.replaceSwedishCharacters(tempContact.getLastName()));

			boolean success = JSONResources.setWebCallContact(user, tempContact);
			if (success && dbAdapter != null) {
				if (!dbAdapter.updateWCContact(contact)) {
					dbAdapter.createWCContact(contact);
				}
			} else {
				return false;
			}
			return success;
		}

		@SuppressLint("NewApi")
		@Override
		protected void onPostExecute(final Boolean result) {
			GetWCCOntactTask contactTask = new GetWCCOntactTask(user, callback, dbAdapter);

			// Launch AsyncTasks to get WebCall info
			if (Tools.isHoneycombOrLater()) {
				contactTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			} else {
				contactTask.execute();
			}

			// Update the list in the fragment
			callback.finished(result);
		}

	}

	public static class DeleteContactTask extends AsyncTask<Void, Void, Boolean> {
		private final User user;
		private final AsyncTaskCompleteListener<Boolean> callback;
		private final MexDbAdapter dbAdapter;
		private final WCContactHolder contact;

		public DeleteContactTask(final User user, final AsyncTaskCompleteListener<Boolean> callback, final MexDbAdapter dbAdapter, final WCContactHolder contact) {
			super();
			this.user = user;
			this.callback = callback;
			this.dbAdapter = dbAdapter;
			this.contact = contact;
		}

		@Override
		protected Boolean doInBackground(final Void... arg0) {
			// Create a temporary contact, in order to replace Swedish characters in the sent item but not the one put into the list

			boolean success = JSONResources.deleteContact(user, contact.getId());
			if (success && dbAdapter != null) {
				dbAdapter.deleteWCContact(contact.getId());
			} else {
				return false;
			}
			return success;
		}

		@SuppressLint("NewApi")
		@Override
		protected void onPostExecute(final Boolean result) {
			GetWCCOntactTask contactTask = new GetWCCOntactTask(user, callback, dbAdapter);

			// Launch AsyncTasks to get WebCall info
			if (Tools.isHoneycombOrLater()) {
				contactTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			} else {
				contactTask.execute();
			}

			// Update the list in the fragment
			callback.finished(result);
		}
	}

	public static class DeleteFolderTask extends AsyncTask<Void, Void, Boolean> {
		private final User user;
		private final AsyncTaskCompleteListener<Boolean> callback;
		private final MexDbAdapter dbAdapter;
		private final FolderHolder folder;

		public DeleteFolderTask(final User user, final AsyncTaskCompleteListener<Boolean> callback, final MexDbAdapter dbAdapter, final FolderHolder folder) {
			super();
			this.user = user;
			this.callback = callback;
			this.dbAdapter = dbAdapter;
			this.folder = folder;
		}

		@Override
		protected Boolean doInBackground(final Void... arg0) {
			boolean success = JSONResources.deleteFolder(user, folder.getId());
			if (success && dbAdapter != null) {
				dbAdapter.deleteFolder(folder.getId());
			} else {
				return false;
			}
			return success;
		}

		@SuppressLint("NewApi")
		@Override
		protected void onPostExecute(final Boolean result) {

			GetFolderTask folderTask = new GetFolderTask(user, callback, dbAdapter);

			// Launch AsyncTasks to get WebCall info
			if (Tools.isHoneycombOrLater()) {
				folderTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			} else {
				folderTask.execute();
			}

			// Update the list in the fragment
			callback.finished(result);
		}

	}

	public static class GetWCCOntactTask extends AsyncTask<Void, Void, Boolean> {
		private final User user;
		private final AsyncTaskCompleteListener<Boolean> callback;
		private final MexDbAdapter dbAdapter;

		public GetWCCOntactTask(final User user, final AsyncTaskCompleteListener<Boolean> callback, final MexDbAdapter dbAdapter) {
			super();
			this.user = user;
			this.callback = callback;
			this.dbAdapter = dbAdapter;

		}

		@Override
		protected Boolean doInBackground(final Void... params) {

			ArrayList<WCContactHolder> contacts = JSONResources.getWebCallContacts(user);

			if (contacts != null && !contacts.isEmpty()) {
				if (dbAdapter != null) {
					dbAdapter.deleteAllWCContacts();

					boolean success = true;

					for (WCContactHolder holder : contacts) {
						if (!isCancelled()) {

							if (!dbAdapter.updateWCContact(holder)) {
								if (!dbAdapter.createWCContact(holder)) {
									success = false;
								}
							}
						}
					}

					return success;

				} else {

					return false;
				}
			} else {

				return false;
			}

		}

		@Override
		protected void onPostExecute(final Boolean result) {
			// Update the list in the fragment
			callback.finished(result);
		}

	}

	public static class GetFolderTask extends AsyncTask<Void, Void, Boolean> {
		private final User user;
		private final AsyncTaskCompleteListener<Boolean> callback;
		private final MexDbAdapter dbAdapter;

		public GetFolderTask(final User user, final AsyncTaskCompleteListener<Boolean> callback, final MexDbAdapter dbAdapter) {
			super();
			this.user = user;
			this.callback = callback;
			this.dbAdapter = dbAdapter;

		}

		@Override
		protected Boolean doInBackground(final Void... params) {
			ArrayList<FolderHolder> folders = JSONResources.getFolders(user);

			if (folders != null && !folders.isEmpty()) {
				if (dbAdapter != null) {
					dbAdapter.deleteAllFolders();

					boolean success = true;

					for (FolderHolder holder : folders) {
						if (!isCancelled()) {

							if (!dbAdapter.updateFolder(holder)) {
								if (!dbAdapter.createFolder(holder)) {
									success = false;
								}
							}
						}
					}

					return success;

				} else {

					return false;
				}
			} else {

				return false;
			}
		}

		@Override
		protected void onPostExecute(final Boolean result) {
			// Update the list in the fragment
			callback.finished(result);
		}

	}

	public static class GetCallTaskDelayed extends AsyncTask<Void, Void, Void> implements CallListener {
		private final User user;
		private final CallListener callback;
		private final MexDbAdapter dbAdapter;
		private int numberOfRetries = 0;
		private final int MAX_RETRIES = 3;

		public GetCallTaskDelayed(final User user, final CallListener callback) {
			super();
			this.user = user;
			this.callback = callback;

			dbAdapter = callback.getDbAdapter();

		}

		@SuppressLint("NewApi")
		@Override
		protected Void doInBackground(final Void... params) {

			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				Log.e("Sleep interrupted", e.getLocalizedMessage());
			}

			Task.GetCallTask task = new Task.GetCallTask(user, callback);

			if (Tools.isHoneycombOrLater()) {
				task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			} else {
				task.execute();
			}

			return null;
		}

		@SuppressLint("NewApi")
		@Override
		public void onReceiveEvent(final boolean success) {

			if (dbAdapter.fetchAllCalls().isEmpty() && numberOfRetries < MAX_RETRIES) {

				if (Tools.isHoneycombOrLater()) {
					executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				} else {
					this.execute();
				}

				numberOfRetries++;

			} else {
				callback.onReceiveEvent(success);

			}

		}

		@Override
		public MexDbAdapter getDbAdapter() {
			return callback.getDbAdapter();
		}

		@Override
		public Context getContext() {
			return callback.getContext();
		}

	}

	public static class GetCallTask extends AsyncTask<Void, Void, Boolean> {
		private final User user;
		private final CallListener callback;
		private final MexDbAdapter dbAdapter;

		public GetCallTask(final User user, final CallListener callback) {
			super();
			this.user = user;
			this.callback = callback;

			dbAdapter = callback.getDbAdapter();

		}

		@Override
		protected Boolean doInBackground(final Void... params) {

			try {
				JSONArray array = new JSONArray(JSONResources.getCalls(user));
				if (array != null && array.length() > 0) {
					JSONArray message = array.getJSONObject(0).getJSONArray("v");
					if (!isCancelled()) {
						boolean success = dbAdapter.createCallsFromResponse(message, callback.getContext());

						return success;

					} else {
						return false;
					}

				} else {
					dbAdapter.deleteAllCalls();
					return true;
				}
			} catch (Exception e) {
				dbAdapter.deleteAllCalls();
				return true;
			}
		}

		@Override
		protected void onPostExecute(final Boolean result) {

			if (callback != null) {
				// Update the list in the fragment
				callback.onReceiveEvent(result);
			}
		}

	}

	// TODO
	public static class GetCallHistoryTask extends AsyncTask<Void, Void, Boolean> {
		private final User user;
		private final CallListener callback;
		private final MexDbAdapter dbAdapter;

		public GetCallHistoryTask(final User user, final CallListener callback) {
			super();
			this.user = user;
			this.callback = callback;

			dbAdapter = callback.getDbAdapter();

		}

		@Override
		protected Boolean doInBackground(final Void... params) {

			try {
				JSONArray array = new JSONArray(JSONResources.getCallHistory(user));
				if (array != null && array.length() > 0) {
					JSONArray message = array.getJSONObject(0).getJSONArray("v");
					if (!isCancelled()) {

						boolean success = dbAdapter.createPastCallsFromResponse(message, callback.getContext());

						return success;

					} else {
						return false;
					}

				} else {
					dbAdapter.deleteAllCalls();
					return true;
				}
			} catch (Exception e) {
				dbAdapter.deleteAllCalls();
				return true;
			}
		}

		@Override
		protected void onPostExecute(final Boolean result) {

			if (callback != null) {
				// Update the list in the fragment
				callback.onReceiveEvent(result);
			}
		}

	}

	public static class GetVoicemailTask extends AsyncTask<Void, Void, Boolean> {
		private final User user;
		private final Editor editor;
		private final SharedPreferences sharedPrefs;
		private final Context context;
		private final VoicemailListener callback;

		public GetVoicemailTask(final User user, final VoicemailListener callback) {
			super();
			this.user = user;

			this.callback = callback;
			context = callback.getContext();

			sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
			editor = sharedPrefs.edit();
		}

		@Override
		protected Boolean doInBackground(final Void... params) {

			String result = JSONResources.getVoiceMail(user);

			if (JSONResources.getBooleanValue(result)) {
				JSONObject temp = null;

				temp = JSONResources.parseJSONObject(result);

				JSONArray vmArray;
				try {
					vmArray = temp.getJSONArray("v");
					int numberOfVoicemail = vmArray.length();

					if (numberOfVoicemail > 0) {
						String latestAcked = sharedPrefs.getString(Constants.LATEST_ACKED_VOICEMAIL, "");
						String latest = vmArray.getJSONObject(0).getString("dt");

						if (!latestAcked.equals(latest)) {
							Tools.Notifications.showVoicemailNotification(context, numberOfVoicemail);

							editor.putString(Constants.LATEST_VOICEMAIL, vmArray.getJSONObject(0).getString("dt"));
							editor.putInt(Constants.NUMBER_OF_VOICEMAIL, numberOfVoicemail);
							editor.putBoolean(Constants.NEW_VOICEMAIL, true);
							editor.commit();
							return true;
						} else {
							Tools.Notifications.dismissVoicemailNotification(context);

							editor.putBoolean(Constants.NEW_VOICEMAIL, false);
							editor.putInt(Constants.NUMBER_OF_VOICEMAIL, 0);
							editor.commit();

							return false;
						}

					} else {
						return false;
					}

				} catch (Exception e) {
					Log.e("GetVoicemailTask", e.getLocalizedMessage());
					return false;
				}

			} else {
				Tools.Notifications.dismissVoicemailNotification(context);
				return false;
			}

		}

		@Override
		protected void onPostExecute(final Boolean result) {

			callback.onReceiveVoicemail(result);

		}
	}

	public static class GetUserInfoTask extends AsyncTask<Void, Void, Void> {
		private final User user;
		private final Editor editor;
		private final MexDbAdapter dbAdapter;
		private final VoicemailListener voicemailCallback;
		private final WCStatusListener wcStatusCallback;

		private String[] possibleMessages;
		private int[] possibleMessageIDs;
		private boolean hasNewVoicemail = false;

		public GetUserInfoTask(final User user, final Editor editor, final MexDbAdapter dbAdapter, final VoicemailListener voicemailCallback, final WCStatusListener wcStatusCallback) {
			super();
			this.user = user;

			this.dbAdapter = dbAdapter;
			this.editor = editor;
			this.wcStatusCallback = wcStatusCallback;
			this.voicemailCallback = voicemailCallback;
		}

		@Override
		protected Void doInBackground(final Void... params) {

			getAllUserSettings();

			return null;
		}

		@Override
		protected void onPostExecute(final Void params) {

			// Because views are being updated, this must be run on main thread
			wcStatusCallback.onReceiveWCStatus(possibleMessages, possibleMessageIDs);
			voicemailCallback.onReceiveVoicemail(hasNewVoicemail);
		}

		private void getAllUserSettings() {
			String response = JSONResources.getUserSettings(user);

			JSONArray settingsArray = JSONResources.parseJSONArray(response);
			if (settingsArray.length() > 8) {
				try {

					getFollowMe(settingsArray.getJSONObject(0));
					getUserSettings(settingsArray.getJSONObject(1));
					getDND(settingsArray.getJSONObject(2));
					getOutboundNumber(settingsArray.getJSONObject(3), settingsArray.getJSONObject(4));
					getQueues(settingsArray.getJSONObject(5));
					getForwardingNumbers(settingsArray.getJSONObject(6), settingsArray.getJSONObject(7), settingsArray.getJSONObject(8));
					getVoicemail(settingsArray.getJSONObject(9));
					getWCStatus(settingsArray.getJSONObject(10), settingsArray.getJSONObject(11));
				} catch (Exception e) {
					Log.e("Task.InitialTask.doInBackGround()", e.getLocalizedMessage());
				}
			}

			// Get Mex status
			String result = JSONResources.getMexLoggedIn(user);

			if (JSONResources.isActionSuccessful(result)) {
				editor.putBoolean(Constants.USER_HAS_MEX, true);
				editor.putBoolean(Constants.MEX_LOGGED_IN, JSONResources.getBooleanValue(result));
			} else {
				editor.putBoolean(Constants.MEX_LOGGED_IN, false);
				editor.putBoolean(Constants.USER_HAS_MEX, false);
			}
			editor.commit();

		}

		private void getVoicemail(final JSONObject voicemailJSON) {
			Context context = wcStatusCallback.getContext();
			SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

			if (JSONResources.getBooleanValue(voicemailJSON.toString())) {

				JSONArray vmArray;
				try {
					vmArray = voicemailJSON.getJSONArray("v");
					int numberOfVoicemail = vmArray.length();

					if (numberOfVoicemail > 0) {
						String latestAcked = sharedPrefs.getString(Constants.LATEST_ACKED_VOICEMAIL, "");
						String latest = vmArray.getJSONObject(0).getString("dt");

						if (!latestAcked.equals(latest)) {
							Tools.Notifications.showVoicemailNotification(context, numberOfVoicemail);

							editor.putString(Constants.LATEST_VOICEMAIL, vmArray.getJSONObject(0).getString("dt"));
							editor.putInt(Constants.NUMBER_OF_VOICEMAIL, numberOfVoicemail);
							editor.putBoolean(Constants.NEW_VOICEMAIL, true);
							editor.commit();
							hasNewVoicemail = true;
						} else {
							editor.putBoolean(Constants.NEW_VOICEMAIL, false);
							editor.putInt(Constants.NUMBER_OF_VOICEMAIL, 0);
							editor.commit();
							voicemailCallback.onReceiveVoicemail(false);
							Tools.Notifications.dismissVoicemailNotification(context);
							hasNewVoicemail = false;
						}

					}

				} catch (Exception e) {
					Log.e("GetVoicemailTask", e.getLocalizedMessage());
				}

			} else {
				Tools.Notifications.dismissVoicemailNotification(context);
				hasNewVoicemail = false;
			}

		}

		private void getWCStatus(final JSONObject possibleMessageJSON, final JSONObject currentMessageJSON) {

			boolean usesDatatal = false;
			int status = -1;
			String statusText = "";

			try {

				// Get the inner array containing all possible WC status
				// messages
				JSONArray possibleMsgsArray = possibleMessageJSON.getJSONArray("v");

				// Initialize a String array of sufficient size
				possibleMessages = new String[possibleMsgsArray.length()];
				possibleMessageIDs = new int[possibleMsgsArray.length()];

				// Add all status strings to the array
				for (int i = 0, length = possibleMsgsArray.length(); i < length; i++) {
					JSONObject obj = possibleMsgsArray.getJSONObject(i);

					possibleMessages[i] = obj.has("text") ? obj.getString("text") : "";
					possibleMessageIDs[i] = obj.has("v") ? obj.getInt("v") : 0;

				}

				JSONObject obj = currentMessageJSON.getJSONObject("v");
				if (obj != null) {
					usesDatatal = currentMessageJSON.has("t");

					// If there is no status, use the default (-1 when using datatal, 1 when not)
					status = obj.has("v") ? obj.getInt("v") : usesDatatal ? -1 : 1;

					if (!usesDatatal && status == 3) {
						statusText = obj.has("text") ? obj.getString("text") : "";
					}
				}

				for (int i = 0, length = possibleMessageIDs.length; i < length; i++) {
					if (possibleMessageIDs[i] == status) {
						editor.putInt(Constants.CURRENT_STATUS, i);
						break;
					}

				}

				if (status == 3) {// !usesDatatal && status == 3) {
					editor.putString(Constants.CURRENT_STATUS_TEXT, statusText);
				}
				editor.putBoolean(Constants.USES_DATATAL, usesDatatal);

				// Save the statuses for the next application startup
				StringBuilder statusBuilder = new StringBuilder();

				// Save the status IDs for next application startup
				StringBuilder idBuilder = new StringBuilder();

				if (possibleMessages != null && possibleMessages.length > 0) {

					editor.putString(Constants.DEFAULT_PRESENCE, possibleMessages[0]);
					editor.putInt(Constants.DEFAULT_PRESENCE_ID, possibleMessageIDs[0]);

					for (int i = 0; i < possibleMessages.length; i++) {
						String statusString = possibleMessages[i];
						int statusID = possibleMessageIDs[i];

						statusBuilder.append(statusString);
						statusBuilder.append("//");

						idBuilder.append(statusID);
						idBuilder.append("//");

					}

					String accumulatedStatusString = statusBuilder.toString();
					editor.putString(Constants.POSS_WC_MESSAGES, accumulatedStatusString.substring(0, accumulatedStatusString.length() - 2));

					String accumulatedIDString = idBuilder.toString();
					editor.putString(Constants.POSS_WC_MESSAGE_IDS, accumulatedIDString.substring(0, accumulatedIDString.length() - 2));

				}

				editor.commit();

			} catch (Exception e) {
				if (e != null && e.getLocalizedMessage() != null) {
					Log.e("Task.GetWCStatusTask", e.getLocalizedMessage());
				} else {
					Log.e("Task.GetWCStatusTask", "Error message was null ");
				}
			}

		}

		private void getForwardingNumbers(final JSONObject forwardingJSON, final JSONObject forwardingUnavJSON, final JSONObject forwardingBusyJSON) {
			String forwardingNumber = forwardingJSON.toString();
			boolean forward = JSONResources.getBooleanValue(forwardingNumber);
			editor.putBoolean(Constants.ENABLE_CALL_FORWARDING_ALWAYS, forward);
			editor.putString(Constants.FORWARDING_NUMBER_ALWAYS, JSONResources.getStringValue(forwardingNumber));

			String forwardingNumberNA = forwardingUnavJSON.toString();
			boolean forwardNA = JSONResources.getBooleanValue(forwardingNumberNA);
			editor.putBoolean(Constants.ENABLE_CALL_FORWARDING_NO_ANSWER, forwardNA);
			editor.putString(Constants.FORWARDING_NUMBER_NOT_AVAIL, JSONResources.getStringValue(forwardingNumberNA));

			String forwardingNumberBusy = forwardingBusyJSON.toString();
			boolean forwardBusy = JSONResources.getBooleanValue(forwardingNumberBusy);
			editor.putBoolean(Constants.ENABLE_CALL_FORWARDING_BUSY, forwardBusy);
			editor.putString(Constants.FORWARDING_NUMBER_BUSY, JSONResources.getStringValue(forwardingNumberBusy));

			editor.commit();

		}

		private void getQueues(final JSONObject jsonObject) {
			if (JSONResources.getBooleanValue(jsonObject.toString())) {

				try {
					JSONObject obj = jsonObject.getJSONObject("v");

					ArrayList<QueueHolder> queues = new ArrayList<QueueHolder>();

					Iterator keys = obj.keys();

					while (keys.hasNext()) {

						try {
							int key = Integer.parseInt(keys.next().toString());

							JSONObject queueObject = obj.getJSONObject(key + "").getJSONObject("i");

							String name = queueObject.has("Name") ? queueObject.getString("Name") : "";
							boolean loggedIn = queueObject.has("in") ? queueObject.getInt("in") == 1 : false;

							queues.add(new QueueHolder(key, name, loggedIn));
						} catch (Exception e1) {
							Log.e("Task.getQueues() Error", e1.getLocalizedMessage());
						}

					}

					QueueHolder[] temp = new QueueHolder[obj.length()];

					temp = queues.toArray(temp);
					dbAdapter.createQueues(temp);
				} catch (Exception e1) {
					Log.e("GetVoicemailTask", e1.getLocalizedMessage());
				}

			}
		}

		private void getOutboundNumber(final JSONObject currentNumberObject, final JSONObject possibleNumberObject) {

			if (JSONResources.getBooleanValue(currentNumberObject.toString()) && JSONResources.getBooleanValue(possibleNumberObject.toString())) {
				try {
					JSONArray newArray = new JSONArray(possibleNumberObject.getString("v"));
					String pre = newArray.toString();

					editor.putString(Constants.POSS_OUTGOING_NUMBERS, pre.replace("[", "").replace("]", "").replace("\"", ""));
					editor.commit();
				} catch (Exception e) {
					Log.e("Task.InitialTask.getOutboundNumber()", e.getLocalizedMessage());
				}

				try {
					editor.putString(Constants.CURRENT_OUTGOING_NUMBER, currentNumberObject.getString("v").replace("\"", ""));
					editor.commit();

				} catch (Exception e) {
					Log.e("Task.InitialTask.getOutboundNumber()", e.getLocalizedMessage());
				}
			}
		}

		private void getDND(final JSONObject obj) {
			if (JSONResources.getBooleanValue(obj.toString())) {
				// DND settings
				editor.putBoolean(Constants.DO_NOT_DISTURB, JSONResources.getBooleanValue(obj.toString()));
				editor.commit();
			}
		}

		private void getFollowMe(final JSONObject input) {
			if (JSONResources.getBooleanValue(input.toString())) {
				int active = 0;
				int pre = 0;
				String list = "";
				int time = 0;
				int confirm = 0;

				JSONObject obj = null;
				try {
					if (input.getString("s").equals("ok") && input.has("v")) {
						obj = new JSONObject(input.getString("v"));

						active = obj.has("a") ? obj.getInt("a") : 0;
						pre = obj.has("p") ? obj.getInt("p") : 0;
						list = obj.has("l") ? obj.getString("l") : "";
						time = obj.has("t") ? obj.getInt("t") : 0;
						confirm = obj.has("c") ? obj.getInt("c") : 0;

						list = list.replace("[", "");
						list = list.replace("]", "");
						list = list.replace("\"", "");
						list = list.replace("#", "");
						list = list.replace(",", "\n");

						editor.putBoolean(Constants.FOLLOWME_ENABLE, active == 1);
						editor.putInt(Constants.FOLLOWME_MAIN_DURATION, pre);
						editor.putString(Constants.FOLLOWME_LIST, list);
						editor.putInt(Constants.FOLLOWME_LIST_DURATION, time);
						editor.putBoolean(Constants.FOLLOWME_CONFIRMATION, confirm == 1);

						editor.commit();

					}

				} catch (Exception e) {
					Log.e("MiscFragment.InitialTask.doInBackground()", "An error occured: " + e.getLocalizedMessage());
				}
			}
		}

		private void getUserSettings(JSONObject obj) {
			if (JSONResources.getBooleanValue(obj.toString())) {

				String firstname = "";
				String surname = "";
				String email = "";
				String cellNumber = "";
				String homeNumber = "";
				String pass = "";
				String cfRingtime = "";

				int ringtime = 0;
				try {
					if (obj.getString("s").equals("ok") && obj.has("v")) {
						obj = new JSONObject(obj.getString("v"));

						firstname = getString(obj, "n");
						surname = getString(obj, "l");
						email = getString(obj, "e");

						cellNumber = getString(obj, "nr_cell");
						homeNumber = getString(obj, "nr_home");

						pass = getString(obj, "pass");

						ringtime = obj.has("t") ? obj.getInt("t") : 30;
						cfRingtime = obj.has("tcf") ? obj.getString("tcf") : "30";

						editor.putString(Constants.USER_FIRST_NAME, firstname);
						editor.putString(Constants.USER_LAST_NAME, surname);
						editor.putString(Constants.USER_EMAIL, email);
						editor.putString(Constants.USER_MOBILE_PHONE, cellNumber);
						editor.putString(Constants.USER_HOME_PHONE, homeNumber);
						editor.putString(Constants.USER_PASSWORD, pass);

						editor.putInt(Constants.USER_RINGTIME, ringtime);
						editor.putString(Constants.USER_FORW_RINGTIME, cfRingtime);

						editor.commit();

					}

				} catch (Exception e) {
					Log.e("MiscFragment.InitialTask.doInBackground()", "An error occured: " + e.getLocalizedMessage());
				}

			}
		}
	}

	public static class SetWCStatusTask extends AsyncTask<Void, Void, Boolean> {
		private final User user;
		private final Context context;
		private final Editor editor;

		private final int statusRemoteIndex, statusListIndex;
		private final String statusText;
		private final WCStatusListener callback;

		public SetWCStatusTask(final User user, final int statusRemoteIndex, final int statusListIndex, final String statusText, final WCStatusListener callback) {
			super();
			this.user = user;
			context = callback.getContext();
			editor = PreferenceManager.getDefaultSharedPreferences(context).edit();

			this.statusRemoteIndex = statusRemoteIndex;
			this.statusText = statusText;
			this.statusListIndex = statusListIndex;

			this.callback = callback;
		}

		@Override
		protected Boolean doInBackground(final Void... params) {

			return JSONResources.getBooleanValue(JSONResources.setWCStatus(user, statusRemoteIndex, statusText));

		}

		@Override
		protected void onPostExecute(final Boolean result) {
			if (!result) {
				Toast.makeText(context, R.string.could_not_set, Toast.LENGTH_LONG).show();
			} else {
				editor.putInt(Constants.CURRENT_STATUS, statusListIndex);
				editor.putString(Constants.CURRENT_STATUS_TEXT, statusText);
				editor.commit();

				callback.onReceiveWCStatus(null, null);
			}

		}
	}

	public static class SetQueuesTask extends AsyncTask<Void, Void, Boolean> {
		private final User user;
		private final MexDbAdapter dbAdapter;
		private final Context context;
		private final QueueHolder[] queues;

		public SetQueuesTask(final User user, final Context context, final QueueHolder[] queues, final MexDbAdapter dbAdapter) {
			super();
			this.user = user;
			this.dbAdapter = dbAdapter;
			this.context = context;

			this.queues = queues;

		}

		@Override
		protected Boolean doInBackground(final Void... params) {

			return JSONResources.setLoggedInQueues(user, queues);

		}

		@Override
		protected void onPostExecute(final Boolean result) {
			if (!result) {
				Toast.makeText(context, R.string.could_not_set, Toast.LENGTH_LONG).show();
			} else {
				dbAdapter.updateQueues(queues);
			}
		}
	}

	public static class SetDNDTask extends AsyncTask<Void, Void, Boolean> {
		private final User user;
		private final boolean dndStatus;
		private final Context context;
		private final Editor editor;

		public SetDNDTask(final User user, final boolean dndStatus, final Context context, final Editor editor) {
			super();
			this.user = user;
			this.dndStatus = dndStatus;
			this.editor = editor;

			this.context = context;
		}

		@Override
		protected Boolean doInBackground(final Void... params) {

			return JSONResources.setDNDEnabled(user, dndStatus);

		}

		@Override
		protected void onPostExecute(final Boolean result) {
			if (!result) {
				Toast.makeText(context, R.string.could_not_set, Toast.LENGTH_LONG).show();
			} else {
				editor.putInt(Constants.USER_AVAILABILITY, dndStatus ? Constants.AVAILABILITY_BUSY : Constants.AVAILABILITY_AVAILABLE);
				editor.putBoolean(Constants.DO_NOT_DISTURB, dndStatus);
				editor.commit();
			}
		}
	}

	public static class SetCurrOutgoingNumberTask extends AsyncTask<Void, Void, Boolean> {
		private final User user;
		private final String number;
		private final Context context;
		private final Editor editor;

		public SetCurrOutgoingNumberTask(final User user, final String number, final Context context, final Editor editor) {
			super();
			this.user = user;
			this.editor = editor;

			this.number = number;

			this.context = context;
		}

		@Override
		protected Boolean doInBackground(final Void... params) {

			return JSONResources.setCurrentOutboundNumber(user, number);

		}

		@Override
		protected void onPostExecute(final Boolean result) {
			if (!result) {
				Toast.makeText(context, R.string.could_not_set, Toast.LENGTH_LONG).show();
			} else {
				editor.putString(Constants.CURRENT_OUTGOING_NUMBER, number);
				editor.commit();
			}
		}
	}

	public static class SetFollowMeTask extends AsyncTask<Void, Void, Boolean> {
		private final User user;
		private final String list;
		private final int preTime, listTime;
		private final boolean fmStatus, confirm;
		private final Context context;
		private final Editor editor;

		public SetFollowMeTask(final User user, final boolean fmStatus, final String numberList, final int preTime, final int listTime, final boolean confirm, final Context context,
				final Editor editor) {
			super();

			this.user = user;
			this.fmStatus = fmStatus;

			list = numberList;
			this.preTime = preTime;
			this.listTime = listTime;
			this.confirm = confirm;

			this.context = context;
			this.editor = editor;

		}

		@Override
		protected Boolean doInBackground(final Void... params) {

			return JSONResources.getBooleanValue(JSONResources.setFollowMe(user, fmStatus, list.split("\n"), preTime, listTime, confirm));

		}

		@Override
		protected void onPostExecute(final Boolean result) {
			if (!result) {
				Toast.makeText(context, R.string.could_not_set, Toast.LENGTH_LONG).show();
			} else {
				editor.putBoolean(Constants.FOLLOWME_ENABLE, fmStatus);

				editor.putInt(Constants.FOLLOWME_MAIN_DURATION, preTime);
				editor.putInt(Constants.FOLLOWME_LIST_DURATION, listTime);

				editor.putString(Constants.FOLLOWME_LIST, list);

				editor.putBoolean(Constants.FOLLOWME_CONFIRMATION, confirm);

				editor.commit();
			}
		}

	}

	public static class SetUserInfoTask extends AsyncTask<Void, Void, Boolean> {
		private final String firstname, surname, email, cellNumber, homeNumber, userPassword;
		private final User user;

		private final int ringTime, cfRingTime;
		private final MiscFragment fragment;
		private final Context context;

		public SetUserInfoTask(final User user, final String firstname, final String surname, final String email, final String cellNumber, final String homeNumber, final int ringTime,
				final int cfRingTime, final String userPassword, final Context context, final MiscFragment fragment) {
			super();
			this.user = user;
			this.context = context;

			this.firstname = Tools.replaceSwedishCharacters(firstname);
			this.surname = Tools.replaceSwedishCharacters(surname);
			this.email = email;
			this.cellNumber = cellNumber;
			this.homeNumber = homeNumber;
			this.userPassword = userPassword;

			this.ringTime = ringTime;
			this.cfRingTime = cfRingTime;

			this.fragment = fragment;
		}

		@Override
		protected Boolean doInBackground(final Void... params) {

			return JSONResources.getBooleanValue(JSONResources.setUserInfo(user, firstname, surname, email, cellNumber, homeNumber, ringTime, cfRingTime, userPassword));

		}

		@Override
		protected void onPostExecute(final Boolean result) {
			if (!result) {
				Toast.makeText(context, R.string.could_not_set, Toast.LENGTH_LONG).show();
			} else {
				((MainActivity) fragment.getActivity()).logOff();
			}
		}

	}

	public static class SetMexStatusTask extends AsyncTask<Void, Void, String> {
		private final User user;
		private final Editor editor;
		private final boolean setLoggedIn;
		private final MexStatusListener callback;

		public SetMexStatusTask(final User user, final boolean setLoggedIn, final MexStatusListener callback) {
			super();
			this.user = user;
			this.setLoggedIn = setLoggedIn;

			this.callback = callback;

			editor = PreferenceManager.getDefaultSharedPreferences(callback.getContext()).edit();
		}

		@Override
		protected String doInBackground(final Void... params) {

			return setLoggedIn ? JSONResources.logInMex(user) : JSONResources.logOutMex(user);
		}

		@Override
		protected void onPostExecute(final String result) {
			if (JSONResources.isActionSuccessful(result)) {
				editor.putBoolean(Constants.MEX_LOGGED_IN, setLoggedIn);
				editor.commit();
			}

			callback.onReceiveMexStatus(setLoggedIn);

		}
	}

	public static class CallActionTask extends AsyncTask<Void, Void, Boolean> {
		private final User user;
		private final String callId;
		private final CallListener callback;
		private final int action;

		public static final int CALL_ANSWER = 0;
		public static final int CALL_HOLD = 1;
		public static final int CALL_RESUME = 2;
		public static final int CALL_HANGUP = 3;

		public CallActionTask(final User user, final int action, final String callId, final CallListener callback) {
			super();
			this.user = user;
			this.callId = callId;
			this.callback = callback;

			this.action = action;
		}

		@Override
		protected Boolean doInBackground(final Void... params) {

			int i = 0;
			boolean success = false;
			while (i++ < 2 && !success) {

				success = performCallAction();
			}

			return success;
		}

		@Override
		protected void onPostExecute(final Boolean result) {
			if (!result) {
				new Task.GetCallTask(user, callback).execute();
				Tools.showCallNotSuccessfulToast(callback.getContext());
			}
		}

		protected boolean performCallAction() {
			switch (action) {

			case CALL_ANSWER:
				return JSONResources.answerCall(user, callId);
			case CALL_HOLD:
				return JSONResources.holdCall(user, callId);
			case CALL_RESUME:
				return JSONResources.resumeCall(user, callId);
			case CALL_HANGUP:
				return JSONResources.endCall(user, callId);
			default:
				return false;
			}

		}

	}

	public static class CallTask extends AsyncTask<Void, Void, Void> {
		private final String number;
		private final User user;

		public CallTask(final User user, final String number) {
			super();
			this.user = user;

			this.number = number;

		}

		@Override
		protected Void doInBackground(final Void... params) {

			JSONResources.makeCall(user, number);
			return null;
		}

	}

	public static class AttendedTransferTask extends AsyncTask<Void, Void, Boolean> {
		private final String callId, transferNumber;
		private final User user;
		private final CallListener callback;
		public final SharedPreferences sharedPrefs;
		public final static String ATTENDED_TRANSFER_CANCEL = "cancel";
		public final static String ATTENDED_TRANSFER_FINISH = null;

		public AttendedTransferTask(final User user, final String callId, final String transferNumber, final CallListener callback, final SharedPreferences sharedPrefs) {
			super();
			this.user = user;

			this.callId = callId;
			this.transferNumber = transferNumber;
			this.callback = callback;

			this.sharedPrefs = sharedPrefs;
		}

		@Override
		protected Boolean doInBackground(final Void... params) {
			int i = 0;

			boolean success = false;
			// while (i++ < 2 && !success) {

			Log.d("callID", "" + transferNumber);
			success = JSONResources.transferAttended(user, callId, transferNumber, sharedPrefs.edit());
			// }

			return success;

		}

		@Override
		protected void onPostExecute(final Boolean result) {
			if (!result) {
				new Task.GetCallTask(user, callback).execute();
				Tools.showCallNotSuccessfulToast(callback.getContext());
			}
		}

	}

	public static class TransferTask extends AsyncTask<Void, Void, Boolean> {
		private final String callId, transferNumber;
		private final User user;
		private final CallListener callback;

		public TransferTask(final User user, final String callId, final String transferNumber, final CallListener callback) {
			super();
			this.user = user;

			this.callId = callId;
			this.transferNumber = transferNumber;
			this.callback = callback;
		}

		@Override
		protected Boolean doInBackground(final Void... params) {
			int i = 0;

			boolean success = false;
			while (i++ < 2 && !success) {

				success = JSONResources.transferCall(user, callId, transferNumber);
			}

			return success;

		}

		@Override
		protected void onPostExecute(final Boolean result) {
			if (!result) {
				new Task.GetCallTask(user, callback).execute();
				Tools.showCallNotSuccessfulToast(callback.getContext());
			}
		}

	}

	private static int getInt(final JSONObject obj, final String columnName) {
		return Tools.getInt(obj, columnName);
	}

	private static String getString(final JSONObject obj, final String columnName) {
		return Tools.getString(obj, columnName);
	}

}
