package com.weblink.mexapp.net;

import java.sql.Date;
import java.util.ArrayList;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Log;

import com.weblink.mexapp.pojo.ContactHolder;
import com.weblink.mexapp.pojo.FolderHolder;
import com.weblink.mexapp.pojo.QueueHolder;
import com.weblink.mexapp.pojo.User;
import com.weblink.mexapp.pojo.WCContactHolder;
import com.weblink.mexapp.utility.Constants;
import com.weblink.mexapp.utility.IntegerNameValuePair;
import com.weblink.mexapp.utility.Task;
import com.weblink.mexapp.utility.Tools;

/**
 * Wrapper class for communication using JSON objects
 * 
 * @author Viktor Åkerskog
 * 
 */
public class JSONResources {

	private static final String TAG = "JSONResources";

	public static JSONObject parseJSONObject(final String message) {
		JSONObject temp = null;

		try {
			temp = new JSONObject(message);
		} catch (Exception e) {
			try {
				temp = new JSONArray(message).getJSONObject(0);

			} catch (Exception e1) {
				Log.e("JSONResources.parseJSONObject()", "An error occured: " + e1.getLocalizedMessage());
				return null;
			}

		}

		return temp;

	}

	public static JSONArray parseJSONArray(final String message) {
		JSONArray temp = null;

		try {
			temp = new JSONArray(message);

		} catch (Exception e1) {
			Log.e("JSONResources.parseJSONArray()", "An error occured: " + e1.getLocalizedMessage());
			return new JSONArray();
		}

		return temp;

	}

	public static String getUserSettings(final User user) {

		try {
			JSONArray array = new JSONArray();

			JSONObject followMeJSON = new JSONObject();
			followMeJSON.put("g", "followMe");

			JSONObject userSettingJSON = new JSONObject();
			userSettingJSON.put("g", "settings");

			JSONObject dndJSON = new JSONObject();
			dndJSON.put("g", "dnd");

			JSONObject currentOutboundJSON = new JSONObject();
			currentOutboundJSON.put("g", "outboundcid");

			JSONObject possibleOutboundJSON = new JSONObject();
			possibleOutboundJSON.put("g", "mexoutboundcid");

			JSONObject queueJSON = new JSONObject();
			queueJSON.put("g", "queues");
			try {
				JSONArray temp = new JSONArray();
				temp.put("i");
				queueJSON.put("v", temp);
			} catch (JSONException e) {
			}

			JSONObject forwardingJSON = new JSONObject();
			forwardingJSON.put("g", "cf");

			JSONObject forwardingUnavJSON = new JSONObject();
			forwardingUnavJSON.put("g", "cfu");

			JSONObject forwardingBusyJSON = new JSONObject();
			forwardingBusyJSON.put("g", "cfb");

			JSONObject voicemailJSON = new JSONObject();
			voicemailJSON.put("g", "voicemails");

			JSONObject possibleMsgJSON = new JSONObject();
			possibleMsgJSON.put("g", "msgs");

			JSONObject currentMsgJSON = new JSONObject();
			currentMsgJSON.put("g", "msg");

			array.put(followMeJSON);
			array.put(userSettingJSON);
			array.put(dndJSON);
			array.put(currentOutboundJSON);
			array.put(possibleOutboundJSON);
			array.put(queueJSON);
			array.put(forwardingJSON);
			array.put(forwardingUnavJSON);
			array.put(forwardingBusyJSON);
			array.put(voicemailJSON);
			array.put(possibleMsgJSON);
			array.put(currentMsgJSON);

			return HTTPResources.performAction(user, array);
		} catch (Exception e) {
			Log.e("JSONResources.getFollowMe()", e.getLocalizedMessage());
		}

		return "";
	}

	public static String getMexLoggedIn(final User user) {
		ArrayList<NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("g", "mexloggedin"));

		String response = HTTPResources.performAction(user, nvps);

		return response;

	}

	public static String logInMex(final User user) {
		ArrayList<NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("s", "mexlogin"));

		String response = HTTPResources.performAction(user, nvps);

		return response;

	}

	public static String logOutMex(final User user) {
		ArrayList<NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("s", "mexlogout"));

		String response = HTTPResources.performAction(user, nvps);

		if (getBooleanValue(response)) {
			return response;
		}

		return "";
	}

	public static String getWCStatus(final User user) {
		try {
			JSONArray array = new JSONArray();

			JSONObject possibleMsgJSON = new JSONObject();
			possibleMsgJSON.put("g", "msgs");

			JSONObject currentMsgJSON = new JSONObject();
			currentMsgJSON.put("g", "msg");

			array.put(possibleMsgJSON);
			array.put(currentMsgJSON);

			return HTTPResources.performAction(user, array);
		} catch (JSONException e) {
			Log.e("JSONResources.getWCStatus()", e.getLocalizedMessage());
		}

		return "";

	}

	public static String setWCStatus(final User user, final int status, final String statusText) {
		ArrayList<NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("s", "msg"));
		nvps.add(new IntegerNameValuePair("v", status));

		// If status is "Custom text", add this text
		if (status == 3) {
			nvps.add(new BasicNameValuePair("text", statusText == null ? "" : Tools.replaceSwedishCharacters(statusText)));
		}

		return HTTPResources.performAction(user, nvps);
	}

	public static ArrayList<ContactHolder> getExtensions(final User user, final Context context) {
		ArrayList<NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("g", "extensions"));

		String response = HTTPResources.performAction(user, nvps);

		return parseExtensionArray(response, context);
	}

	public static String getVoiceMail(final User user) {
		ArrayList<NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("g", "voicemails"));

		String response = HTTPResources.performAction(user, nvps);

		if (getBooleanValue(response)) {

			return response;

		}

		return "";
	}

	public static boolean setLoggedInQueues(final User user, final QueueHolder[] queues) {

		JSONObject obj;
		try {

			JSONObject logOffJSON = new JSONObject();
			logOffJSON.put("m", "logout");

			JSONObject logInJSON = new JSONObject();
			logInJSON.put("m", "login");

			obj = new JSONObject();

			obj.put("s", "queue");
			for (QueueHolder queue : queues) {
				obj.put("" + queue.getId(), queue.isLoggedIn() ? logInJSON : logOffJSON);

			}

			return getBooleanValue(HTTPResources.performAction(user, obj));
		} catch (JSONException e) {
			Log.e("JSONResources.setLoggedInQueues()", e.getLocalizedMessage());
		}

		return false;
	}

	public static boolean setCurrentOutboundNumber(final User user, final String number) {
		ArrayList<NameValuePair> nvps = new ArrayList<NameValuePair>();

		nvps.add(new BasicNameValuePair("s", "outboundcid"));
		nvps.add(new BasicNameValuePair("nr", number));

		return getBooleanValue(HTTPResources.performAction(user, nvps));
	}

	public static String setFollowMe(final User user, final boolean enable, final String[] numberList, final int preTime, final int listTime, final boolean confirm) {
		JSONObject obj = new JSONObject();
		JSONArray arr = Tools.convertToJSONArray(numberList);

		try {
			obj.put("s", "followMe");
			obj.put("a", enable ? 1 : 0);
			obj.put("l", arr);
			obj.put("p", preTime);
			obj.put("t", listTime);
			obj.put("c", confirm ? 1 : 0);
		} catch (JSONException e) {
			Log.e("JSONResources.setFollowMe", "An error occured: " + e.getLocalizedMessage());
		}

		return HTTPResources.performAction(user, obj);
	}

	public static String setUserInfo(final User user, final String firstname, final String surname, final String email, final String cellNumber, final String homeNumber, final int ringTime,
			final int cfRingTime, final String userPassword) {
		ArrayList<NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("s", "settings"));
		nvps.add(new BasicNameValuePair("n", firstname));
		nvps.add(new BasicNameValuePair("l", surname));
		nvps.add(new BasicNameValuePair("e", email));
		nvps.add(new BasicNameValuePair("nr_cell", cellNumber));
		nvps.add(new BasicNameValuePair("nr_home", homeNumber));
		nvps.add(new IntegerNameValuePair("t", ringTime));
		nvps.add(new IntegerNameValuePair("tcf", cfRingTime));
		if (!userPassword.equals("")) {
			nvps.add(new BasicNameValuePair("pass", userPassword));
		}

		return HTTPResources.performAction(user, nvps);
	}

	public static String getCallHistory(final User user) {
		return getCallHistory(user, null, null);
	}

	public static String getCallHistory(final User user, final Date fromDate, final Date toDate) {
		ArrayList<NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("g", "call_history"));
		if (fromDate != null) {
			nvps.add(new BasicNameValuePair("f", fromDate.toString()));
		}
		if (toDate != null) {
			nvps.add(new BasicNameValuePair("t", toDate.toString()));
		}

		return HTTPResources.performAction(user, nvps);
	}

	public static String getCalls(final User user) {
		ArrayList<NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("g", "calls"));

		return HTTPResources.performAction(user, nvps);
	}

	public static boolean makeCall(final User user, final String number) {
		ArrayList<NameValuePair> nvps = new ArrayList<NameValuePair>();

		if (number != null && !number.equals("")) {
			nvps.add(new BasicNameValuePair("s", "call"));
			nvps.add(new BasicNameValuePair("nr", number));

			return isActionSuccessful(HTTPResources.performAction(user, nvps));
		} else {
			return false;
		}

	}

	public static boolean answerCall(final User user, final String id) {
		ArrayList<NameValuePair> nvps = new ArrayList<NameValuePair>();

		if (id != null && !id.equals("")) {
			nvps.add(new BasicNameValuePair("s", "answer"));
			nvps.add(new BasicNameValuePair("id1", id));

			return isActionSuccessful(HTTPResources.performAction(user, nvps));
		} else {
			return false;
		}

	}

	public static boolean holdCall(final User user, final String id) {

		ArrayList<NameValuePair> nvps = new ArrayList<NameValuePair>();

		if (id != null && !id.equals("")) {
			nvps.add(new BasicNameValuePair("s", "hold"));
			nvps.add(new BasicNameValuePair("id1", id));

			return isActionSuccessful(HTTPResources.performAction(user, nvps));
		} else {
			return false;
		}
	}

	public static boolean resumeCall(final User user, final String id) {

		ArrayList<NameValuePair> nvps = new ArrayList<NameValuePair>();

		if (id != null && !id.equals("")) {
			nvps.add(new BasicNameValuePair("s", "resume"));
			nvps.add(new BasicNameValuePair("id1", id));

			return isActionSuccessful(HTTPResources.performAction(user, nvps));
		} else {
			return false;
		}
	}

	public static boolean endCall(final User user, final String id) {

		ArrayList<NameValuePair> nvps = new ArrayList<NameValuePair>();

		if (id != null && !id.equals("")) {
			nvps.add(new BasicNameValuePair("s", "hangup"));
			nvps.add(new BasicNameValuePair("id1", id));

			return isActionSuccessful(HTTPResources.performAction(user, nvps));
		} else {
			return false;
		}
	}

	public static boolean transferCall(final User user, final String id, final String number) {

		ArrayList<NameValuePair> nvps = new ArrayList<NameValuePair>();

		if (id != null && !id.equals("")) {
			nvps.add(new BasicNameValuePair("s", "transfer"));
			nvps.add(new BasicNameValuePair("id1", id));
			nvps.add(new BasicNameValuePair("nr", number));

			return isActionSuccessful(HTTPResources.performAction(user, nvps));
		} else {
			return false;
		}
	}

	/**
	 * Start an attended transfer session
	 * 
	 * @param user The user who makes the transfer
	 * @param callID The ID for the call to transfer
	 * @param remoteNumber The number to transfer to. Put "cancel" or leave empty to cancel and null to finish
	 * @return
	 */
	public static boolean transferAttended(final User user, final String callID, final String remoteNumber, final Editor editor) {

		ArrayList<NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("s", "attendantTransfer"));

		if (remoteNumber != null && !remoteNumber.equals("") && !remoteNumber.equals(Task.AttendedTransferTask.ATTENDED_TRANSFER_CANCEL)) {
			nvps.add(new BasicNameValuePair("id1", callID));
			nvps.add(new BasicNameValuePair("nr", remoteNumber));

		} else if (remoteNumber == null) {
			nvps.add(new BasicNameValuePair("ch1", callID));
		} else if (remoteNumber.equals(Task.AttendedTransferTask.ATTENDED_TRANSFER_CANCEL)) {
			nvps.add(new BasicNameValuePair("ch1", callID));
			nvps.add(new BasicNameValuePair("nr", "cancel"));

		}
		String result = HTTPResources.performAction(user, nvps);
		boolean success = isActionSuccessful(result);

		if (success && callID != null && !callID.equals("")) {
			JSONObject obj = parseJSONObject(result);

			try {
				editor.putString(Constants.TRANSFER_CHANNEL, obj.has("ch1") ? obj.getString("ch1") : "");
				editor.commit();
			} catch (Exception e) {

			}
		}

		return success;

	}

	public static boolean setDNDEnabled(final User user, final boolean enabled) {
		ArrayList<NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("s", "dnd"));
		nvps.add(new IntegerNameValuePair("set", enabled ? 1 : 0));

		return getBooleanValue(HTTPResources.performAction(user, nvps));
	}

	public static String getForwardingNumbers(final User user) {
		JSONArray array = new JSONArray();
		JSONObject forwardingJSON = new JSONObject();
		JSONObject forwardingNAJSON = new JSONObject();
		JSONObject forwardingBusyJSON = new JSONObject();

		try {

			forwardingJSON.put("g", "cf");
			forwardingNAJSON.put("g", "cfu");
			forwardingBusyJSON.put("g", "cfb");

			array.put(forwardingJSON);
			array.put(forwardingNAJSON);
			array.put(forwardingBusyJSON);

			return HTTPResources.performAction(user, array);
		} catch (Exception e) {
			Log.e("JSONResources.getFollowMe()", e.getLocalizedMessage());
		}

		return "";
	}

	public static String getHash(final User user) {
		ArrayList<NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("g", "hash"));

		JSONObject temp = Tools.createJSONObject(nvps);
		return HTTPResources.performAction(user, temp);
	}

	public static boolean setForwarding(final User user, final String alwaysNumber, final String busyNumber, final String unavNumber, final boolean isDndEnabled) {
		JSONArray array = new JSONArray();
		JSONObject forwardingJSON = new JSONObject();
		JSONObject forwardingNAJSON = new JSONObject();
		JSONObject forwardingBusyJSON = new JSONObject();
		JSONObject dndJSON = new JSONObject();

		try {

			forwardingJSON.put("s", "cf");
			forwardingJSON.put("nr", alwaysNumber);

			forwardingNAJSON.put("s", "cfu");
			forwardingNAJSON.put("nr", unavNumber);

			forwardingBusyJSON.put("s", "cfb");
			forwardingBusyJSON.put("nr", busyNumber);

			dndJSON.put("s", "dnd");
			dndJSON.put("set", isDndEnabled ? 1 : 0);

			array.put(forwardingJSON);
			array.put(forwardingNAJSON);
			array.put(forwardingBusyJSON);
			array.put(dndJSON);

			return getBooleanValue(HTTPResources.performAction(user, array));
		} catch (Exception e) {
			Log.e("JSONResources.setForwarding()", e.getLocalizedMessage());
			return false;
		}

	}

	public static boolean setForwardingNumber(final User user, final String number) {
		ArrayList<NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("s", "cf"));
		nvps.add(new BasicNameValuePair("nr", number));

		return getBooleanValue(HTTPResources.performAction(user, nvps));
	}

	public static boolean setForwardingNANumber(final User user, final String number) {
		ArrayList<NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("s", "cfu"));
		nvps.add(new BasicNameValuePair("nr", number));

		return getBooleanValue(HTTPResources.performAction(user, nvps));
	}

	public static boolean setForwardingBusyNumber(final User user, final String number) {
		ArrayList<NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("s", "cfb"));
		nvps.add(new BasicNameValuePair("nr", number));

		return getBooleanValue(HTTPResources.performAction(user, nvps));
	}

	/*
	 * Mappar

	s: folder
	d: folder

	Kontakter:

	s: contact // Set
	d: contact // delete
	 */

	/** 
	 * Tries to add a WebCall contact to the user's account.  
	 * 
	 * @param user the user to add contact to
	 * @param contact the contact to add
	 * @return true if add was successful
	 * 
	 */
	public static boolean setWebCallContact(final User user, final WCContactHolder contact) {
		String cellNumber = contact.getCellNumber(), homeNumber = contact.getHomeNumber(), workNumber = contact.getWorkNumber();
		String firstName = contact.getFirstName(), lastName = contact.getLastName();
		String emailAddress = contact.getEmailAddress();
		int contactID = contact.getId();

		ArrayList<NameValuePair> nvps = new ArrayList<NameValuePair>();

		nvps.add(new BasicNameValuePair("s", "contact"));
		nvps.add(new BasicNameValuePair("t", contact.getType()));

		// Only add names, addresses and phone numbers if they are non-null and nonempty
		if (contactID != 0) {

			nvps.add(new IntegerNameValuePair("id", contactID));
		}

		if (firstName != null && firstName.length() > 0) {
			nvps.add(new BasicNameValuePair("n", firstName));
		}
		if (lastName != null && lastName.length() > 0) {
			nvps.add(new BasicNameValuePair("l", lastName));
		}
		if (emailAddress != null && emailAddress.length() > 0) {
			nvps.add(new BasicNameValuePair("mail", emailAddress));
		}

		if (cellNumber != null && cellNumber.length() > 0) {
			nvps.add(new BasicNameValuePair("nr_cell", cellNumber));
		}
		if (homeNumber != null && homeNumber.length() > 0) {
			nvps.add(new BasicNameValuePair("nr_home", homeNumber));
		}
		if (workNumber != null && workNumber.length() > 0) {
			nvps.add(new BasicNameValuePair("nr_work", workNumber));
		}

		if (contact.getFolderID() != -1) {
			nvps.add(new IntegerNameValuePair("f", contact.getFolderID()));
		} else {
			nvps.add(new BasicNameValuePair("f", "root"));
		}

		String primaryNumberString = "work";
		switch (contact.getPrimaryNumber()) {
		case WCContactHolder.PRIMARY_NUMBER_CELL:
			primaryNumberString = "cell";
			break;
		case WCContactHolder.PRIMARY_NUMBER_HOME:
			primaryNumberString = "home";
			break;
		}
		nvps.add(new BasicNameValuePair("nr_pri", primaryNumberString));

		return getBooleanValue(HTTPResources.performAction(user, nvps));
	}

	/** 
	 * Queries the server for all WebCall contacts
	 * @param user the user to use in query
	 * @return the array with all WebCall contacts (if any)
	 */
	public static ArrayList<WCContactHolder> getWebCallContacts(final User user) {
		ArrayList<WCContactHolder> contacts = new ArrayList<WCContactHolder>();
		String firstName, lastName, homeNumber, workNumber, cellNumber, primaryNumber, type, emailAdress;
		int id, folderID, primaryNumberID;

		ArrayList<NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("g", "contacts"));

		String result = HTTPResources.performAction(user, nvps);

		try {

			JSONArray array = parseJSONArray(result).getJSONObject(0).getJSONArray("v");

			for (int i = 0; i < array.length(); i++) {

				JSONObject obj = array.getJSONObject(i);

				if (obj != null) {

					id = obj.has("id") ? obj.getInt("id") : -1;
					firstName = obj.has("n") ? obj.getString("n").trim() : "";
					lastName = obj.has("l") ? obj.getString("l").trim() : "";
					homeNumber = obj.has("nr_home") ? obj.getString("nr_home").trim() : "";
					cellNumber = obj.has("nr_cell") ? obj.getString("nr_cell").trim() : "";
					workNumber = obj.has("nr_work") ? obj.getString("nr_work").trim() : "";
					emailAdress = obj.has("mail") ? obj.getString("mail").trim() : "";
					type = obj.has("t") ? obj.getString("t") : "";
					folderID = obj.has("f") ? obj.getInt("f") : -1;

					type = obj.has("t") ? obj.getString("t") : "";
					if (type.equals("c") || type.contains("common")) {
						type = "Gemensam";
					} else if (type.equals("p") || type.contains("private")) {
						type = "Privat";
					}

					primaryNumber = obj.has("nr_pri") ? obj.getString("nr_pri") : "";
					if (primaryNumber.equals("work")) {
						primaryNumberID = WCContactHolder.PRIMARY_NUMBER_WORK;
					} else if (primaryNumber.equals("home")) {
						primaryNumberID = WCContactHolder.PRIMARY_NUMBER_HOME;
					} else {
						primaryNumberID = WCContactHolder.PRIMARY_NUMBER_CELL;
					}

					if (id != -1) {
						contacts.add(new WCContactHolder(id, firstName, lastName, homeNumber, workNumber, cellNumber, type, emailAdress, folderID, primaryNumberID));
					}
				}
			}

		} catch (Exception e1) {

			Log.e("JSONResources.getContacts()", e1.getLocalizedMessage());
		}

		return contacts;
	}

	public static ArrayList<FolderHolder> getFolders(final User user) {
		ArrayList<FolderHolder> folders = new ArrayList<FolderHolder>();
		int id = 0, right = 0, left = 0, parent = -1;
		String title = "", type = "";

		ArrayList<NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("g", "folders"));

		try {
			JSONArray array = parseJSONArray(HTTPResources.performAction(user, nvps)).getJSONObject(0).getJSONArray("v");

			for (int i = 0; i < array.length(); i++) {

				JSONObject obj = array.getJSONObject(i);

				if (obj != null) {
					id = obj.has("id") ? obj.getInt("id") : 0;
					title = obj.has("n") ? obj.getString("n") : "";
					type = obj.has("t") ? obj.getString("t") : "";
					left = obj.has("lft") ? obj.getInt("lft") : 0;
					right = obj.has("rgt") ? obj.getInt("rgt") : 0;
					parent = obj.has("f") ? obj.getInt("f") : -1;
					;
					if (type.equals("c") || type.contains("common")) {
						type = "Gemensam";
					} else if (type.equals("p") || type.contains("private")) {
						type = "Privat";
					}

					folders.add(new FolderHolder(id, title, type, parent, left, right, 0));

				}
			}

		} catch (Exception e1) {

			Log.e("JSONResources.getFolders()", e1.getLocalizedMessage());
		}

		return folders;
	}

	public static boolean setFolder(final User user, final FolderHolder folder) {

		ArrayList<NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("s", "folder"));
		if (folder.getId() != 0) {
			nvps.add(new IntegerNameValuePair("id", folder.getId()));
		}

		nvps.add(new BasicNameValuePair("t", folder.getType()));
		nvps.add(new BasicNameValuePair("n", folder.getTitle()));
		if (folder.getParent() != -1) {
			nvps.add(new IntegerNameValuePair("f", folder.getParent()));
		} else {
			nvps.add(new BasicNameValuePair("f", "root"));
		}

		return getBooleanValue(HTTPResources.performAction(user, nvps));
	}

	public static boolean deleteFolder(final User user, final int folderID) {

		ArrayList<NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("d", "folder"));
		nvps.add(new IntegerNameValuePair("id", folderID));

		return getBooleanValue(HTTPResources.performAction(user, nvps));
	}

	public static boolean deleteContact(final User user, final int contactID) {

		ArrayList<NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("d", "contact"));
		nvps.add(new IntegerNameValuePair("id", contactID));

		return getBooleanValue(HTTPResources.performAction(user, nvps));
	}

	private static ArrayList<ContactHolder> parseExtensionArray(final String response, final Context context) {
		ArrayList<ContactHolder> extensions = new ArrayList<ContactHolder>();
		JSONArray tempArray = null;
		try {
			tempArray = new JSONArray(response);
		} catch (Exception e) {
			if (e != null && e.getLocalizedMessage() != null) {
				Log.e("LoginActivity | Parsing to JSONArray failed", e.getLocalizedMessage());
			}

		}
		if (tempArray != null) {

			for (int i = 0; i < tempArray.length(); i++) {
				JSONObject obj = null;
				JSONArray array = null;

				try {
					obj = (JSONObject) tempArray.get(i);
					array = new JSONArray(obj.getString("v"));
				} catch (Exception e) {
					Log.e("LoginActivity | Parsing to JSONObject failed", e.getLocalizedMessage());

				}
				for (int j = 0, length = array.length(); j < length; j++) {
					try {
						obj = (JSONObject) array.get(j);
					} catch (Exception e) {
						Log.e("LoginActivity | Parsing #2 to JSONArray failed", e.getLocalizedMessage());

					}
					String name = "";
					String extension = "";
					String number = "";
					String statusText = "";
					int peerId = 0;
					int status = 0;
					int wcStatus = 1;

					try {
						name = obj.has("n") ? obj.getString("n").trim() : "";
						name = obj.has("l") ? name + " " + obj.getString("l").trim() : name;
						extension = obj.has("ext") ? obj.getString("ext") : "";
						number = obj.has("cid") ? obj.getString("cid") : "";
						peerId = obj.has("p") ? obj.getInt("p") : 0;
						status = obj.has("s") ? obj.getInt("s") : 0;
						wcStatus = obj.has("ws") ? obj.getInt("ws") : 1;
						statusText = obj.has("text") ? obj.getString("text") : "";

					} catch (Exception e) {
						Log.e("LoginActivity | Field empty", e.getLocalizedMessage());
					}

					SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
					String temp = sharedPrefs.getString(Constants.POSS_WC_MESSAGES, "");
					// String tempIDs = sharedPrefs.getString(Constants.POSS_WC_MESSAGE_IDS, "");
					String message = "";

					if (!temp.equals("")) {
						String[] possibleMessages = temp.split("//");
						// String[] possibleIDs = tempIDs.split("//");

						if (wcStatus != 1 && wcStatus - 1 <= possibleMessages.length) {
							message = possibleMessages[wcStatus - 1];
						} else {
							message = "";
						}
					} else {
						message = "";
					}

					extensions.add(new ContactHolder(name, number, extension, status, peerId, statusText.length() > 0 ? statusText : message));

				}

			}
		}

		return extensions;
	}

	public static boolean getBooleanValue(final String jsonString) {
		JSONObject obj = parseJSONObject(jsonString);

		try {
			boolean isOK = obj.getString("s").equals("ok");
			boolean isValueOK = obj.has("v") ? !obj.getString("v").equals("") && !obj.getString("v").equals("0") : true;

			return isOK && isValueOK;

		} catch (JSONException e) {
			if (e != null && e.getLocalizedMessage() != null) {
				Log.e("JSONResources.getBooleanValue() | JSONException", e.getLocalizedMessage());
			}
		} catch (Exception e) {
			if (e != null && e.getLocalizedMessage() != null) {
				Log.e("JSONResources.getBooleanValue() | Exception", e.getLocalizedMessage());
			}
		}

		return false;
	}

	public static String getStringValue(final String jsonString) {
		try {
			// Try parsing as a JSONArray
			JSONArray array = new JSONArray(jsonString);
			JSONObject obj = array.getJSONObject(0);

			return obj.getString("v");

		} catch (Exception e) {
			Log.i("JSONResources.getStringValue()", "Parsing to JSONArray failed. No worries - trying to parse to JSONObject instead");
			try {
				// Parsing to JSONArray failed, try parsing as a JSONObject
				JSONObject obj = new JSONObject(jsonString);
				return obj.getString("v");
			} catch (Exception e1) {
				if (e != null && e.getLocalizedMessage() != null) {
					Log.e("JSONResources.getStringValue()", e.getLocalizedMessage());
				}
			}

			return "";
		}

	}

	public static boolean isActionSuccessful(final String jsonString) {
		JSONArray array = null;
		JSONObject obj = null;
		if (jsonString != null) {
			try {
				array = new JSONArray(jsonString);
				obj = array.getJSONObject(0);

			} catch (Exception e) {

				Log.d("JSONResources.isSuccess() | JSONException", "Could not parse to JSONArray, trying JSONObject instead");
				try {
					obj = new JSONObject(jsonString);
				} catch (JSONException e1) {
					Log.e("JSONResources.isSuccess() | JSONException", e1.getLocalizedMessage());
				}

			}

			if (obj != null) {
				try {
					return obj.getString("s").equals("ok");
				} catch (JSONException e) {
					Log.e("JSONResources.isSuccess() | JSONException", e.getLocalizedMessage());
					return false;
				}
			} else {
				return false;
			}
		} else {
			return false;
		}
	}
}
