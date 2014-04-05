package com.weblink.mexapp.db;

import java.util.ArrayList;
import java.util.Calendar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.provider.ContactsContract.PhoneLookup;
import android.util.Log;

import com.weblink.mexapp.R;
import com.weblink.mexapp.pojo.CallHolder;
import com.weblink.mexapp.pojo.ContactHolder;
import com.weblink.mexapp.pojo.FolderHolder;
import com.weblink.mexapp.pojo.PastCallHolder;
import com.weblink.mexapp.pojo.QueueHolder;
import com.weblink.mexapp.pojo.WCContactHolder;
import com.weblink.mexapp.pojo.WebCallItemHolder;

public class MexDbAdapter {

	// Keys
	public static final String KEY_ROWID = "_id";
	public static final String KEY_STATUS = "_status";
	public static final String KEY_STATUS_TEXT = "_status_text";
	public static final String KEY_PEER_ID = "_peerid";

	// Extension keys
	public static final String KEY_NAME = "_name";
	public static final String KEY_EXTENSION = "_extension";
	public static final String KEY_NUMBER = "_number";

	// Call keys
	public static final String KEY_ID2 = "_id2";
	public static final String KEY_REMOTE_NUMBER = "_remote_number";
	public static final String KEY_CONTACT_ID = "_contact_id";
	public static final String KEY_DIRECTION = "_direction";
	public static final String KEY_CONTACT_NAME = "_contact_name";

	// Call history extra keys (uses many from "call")
	public static final String KEY_CALL_DATE = "_call_date";
	public static final String KEY_CALL_DISPOSITION = "_disposition";

	// WebCall folder keys
	public static final String KEY_FOLDER_TITLE = "_folder_title";
	public static final String KEY_FOLDER_PARENT = "_parent";
	public static final String KEY_FOLDER_TYPE = "_type";
	public static final String KEY_FOLDER_LEFT = "_left";
	public static final String KEY_FOLDER_RIGHT = "_right";

	// WebCall contact keys
	public static final String KEY_FIRST_NAME = "_first_name";
	public static final String KEY_LAST_NAME = "_last_name";
	public static final String KEY_WC_CONTACT_TYPE = "_wc_contact_type";
	public static final String KEY_NUMBER_HOME = "_home_number";
	public static final String KEY_NUMBER_CELL = "_cell_number";
	public static final String KEY_NUMBER_WORK = "_work_number";
	public static final String KEY_EMAIL_ADDRESS = "_email_address";
	public static final String KEY_PRIMARY_NUMBER_ID = "_primary_number_id";
	public static final String KEY_FOLDER_ID = "_folder_id";

	private static final String TAG = "MexDbAdapter";
	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;

	// Database properties
	private static final String DATABASE_NAME = "mex_db";
	private static final String DATABASE_TABLE_EXTENSIONS = "Extensions";
	private static final String DATABASE_TABLE_CALLS = "Calls";
	private static final String DATABASE_TABLE_CALL_HISTORY = "CallsHistory";
	private static final String DATABASE_TABLE_QUEUES = "Queues";
	private static final String DATABASE_TABLE_FOLDERS = "Folders";
	private static final String DATABASE_TABLE_WC_CONTACTS = "WebCallContacts";

	private static final int DATABASE_VERSION = 17;

	// Tables
	private static final String CREATE_TABLE_EXTENSIONS = "create table " + DATABASE_TABLE_EXTENSIONS + " (" + KEY_ROWID + " INTEGER PRIMARY KEY, " + KEY_NAME + " TEXT, " + KEY_NUMBER
			+ " TEXT NOT NULL, " + KEY_EXTENSION + " TEXT, " + KEY_STATUS + " INTEGER NOT NULL, " + KEY_STATUS_TEXT + " TEXT);";

	private static final String CREATE_TABLE_CALLS = "create table " + DATABASE_TABLE_CALLS + " (" + KEY_ROWID + " TEXT PRIMARY KEY, " + KEY_ID2 + " TEXT, " + KEY_REMOTE_NUMBER + " TEXT NOT NULL, "
			+ KEY_CONTACT_ID + " INTEGER NOT NULL, " + KEY_STATUS + " TEXT, " + KEY_DIRECTION + " TEXT, " + KEY_CONTACT_NAME + " TEXT);";

	private static final String CREATE_TABLE_CALL_HISTORY = "create table " + DATABASE_TABLE_CALL_HISTORY + " (" + KEY_ROWID + " TEXT PRIMARY KEY, " + KEY_REMOTE_NUMBER + " TEXT NOT NULL, "
			+ KEY_CONTACT_ID + " INTEGER NOT NULL, " + KEY_CALL_DISPOSITION + " TEXT, " + KEY_DIRECTION + " TEXT, " + KEY_CALL_DATE + " DATE, " + KEY_CONTACT_NAME + " TEXT);";

	private static final String CREATE_TABLE_QUEUES = "create table " + DATABASE_TABLE_QUEUES + " (" + KEY_ROWID + " INTEGER PRIMARY KEY, " + KEY_NAME + " TEXT NOT NULL, " + KEY_STATUS
			+ " INTEGER NOT NULL);";

	private static final String CREATE_TABLE_FOLDERS = "create table " + DATABASE_TABLE_FOLDERS + " (" + KEY_ROWID + " INTEGER PRIMARY KEY, " + KEY_FOLDER_TITLE + " TEXT NOT NULL, " + KEY_FOLDER_TYPE
			+ " TEXT NOT NULL, " + KEY_FOLDER_PARENT + " INTEGER, " + KEY_FOLDER_LEFT + " INTEGER NOT NULL, " + KEY_FOLDER_RIGHT + " INTEGER NOT NULL);";

	private static final String CREATE_TABLE_WC_CONTACTS = "create table " + DATABASE_TABLE_WC_CONTACTS + " (" + KEY_ROWID + " INTEGER PRIMARY KEY, " + KEY_FIRST_NAME + " TEXT NOT NULL, "
			+ KEY_LAST_NAME + " TEXT NOT NULL, " + KEY_WC_CONTACT_TYPE + " TEXT NOT NULL, " + KEY_NUMBER_HOME + " TEXT NOT NULL, " + KEY_NUMBER_CELL + " TEXT NOT NULL, " + KEY_NUMBER_WORK
			+ " TEXT NOT NULL, " + KEY_PRIMARY_NUMBER_ID + " INTEGER NOT NULL, " + KEY_EMAIL_ADDRESS + " TEXT NOT NULL, " + KEY_FOLDER_ID + " INTEGER NOT NULL);";

	private final Context mCtx;

	private static class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper(final Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(final SQLiteDatabase db) {

			db.execSQL(CREATE_TABLE_EXTENSIONS);
			db.execSQL(CREATE_TABLE_CALLS);
			db.execSQL(CREATE_TABLE_CALL_HISTORY);
			db.execSQL(CREATE_TABLE_QUEUES);
			db.execSQL(CREATE_TABLE_FOLDERS);
			db.execSQL(CREATE_TABLE_WC_CONTACTS);

		}

		@Override
		public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
			Log.d(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_EXTENSIONS);
			db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_CALLS);
			db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_CALL_HISTORY);
			db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_QUEUES);
			db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_FOLDERS);
			db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_WC_CONTACTS);
			onCreate(db);

		}
	}

	public MexDbAdapter(final Context ctx) {
		mCtx = ctx;
	}

	/**
	 * Opens the database
	 * 
	 * @return
	 * @throws SQLException
	 */
	public MexDbAdapter open() throws SQLException {
		if (!isOpen()) {
			mDbHelper = new DatabaseHelper(mCtx);
			try {
				mDb = mDbHelper.getWritableDatabase();
			} catch (SQLException e) {
				// If the database is already open/locked, don't do work on it
				mDb = null;
				Log.e("MexDbAdapter.open()", "Database already open/locked");
			}
		}
		return this;
	}

	public boolean isOpen() {
		return mDb != null && mDb.isOpen();
	}

	/**
	 * Closes the database.
	 */
	public void close() {
		if (isOpen()) {
			mDbHelper.close();
		}
	}

	// CREATE

	public boolean createQueues(final QueueHolder[] queueArray) {
		boolean success = true;

		deleteAllQueues();

		for (QueueHolder queue : queueArray) {
			if (!createQueue(queue)) {
				success = false;
			}
		}

		return success;
	}

	public boolean createQueue(final QueueHolder queue) {
		return createQueue(queue.getId(), queue.getName(), queue.isLoggedIn());
	}

	public boolean createQueue(final int id, final String name, final boolean loggedIn) {
		final ContentValues args = new ContentValues();
		args.put(KEY_ROWID, id);
		args.put(KEY_NAME, name == null ? "" : name);
		args.put(KEY_STATUS, loggedIn);

		if (args != null && isOpen()) {
			return mDb.insert(DATABASE_TABLE_QUEUES, null, args) != -1;
		} else {
			return false;
		}
	}

	public boolean createExtension(final ContactHolder extension) {
		return createExtension(extension.getPeerId(), extension.getName(), extension.getNumber(), extension.getExtension(), extension.getAvailability(), extension.getStatusLine());
	}

	public boolean createExtension(final int id, final String name, final String number, final String extension, final int status, final String statusLine) {

		final ContentValues args = getExtensionCV(id, name, number, extension, status, statusLine);

		if (args != null && isOpen()) {
			return mDb.insert(DATABASE_TABLE_EXTENSIONS, null, args) != -1;
		} else {
			return false;
		}
	}

	public boolean createCall(final CallHolder call) {
		return createCall(call.getCallId1(), call.getCallId2(), call.getContact().getPeerId(), call.getContactName(), call.getContact().getNumber(), call.getCallStatus(), call.getCallDirection());
	}

	public boolean createCall(final String id, final String id2, final int contactId, final String contactName, final String remoteNumber, final String status, final String direction) {

		final ContentValues args = getCallCV(id, id2, contactId, contactName, remoteNumber, status, direction);

		if (isOpen()) {

			long success = mDb.insert(DATABASE_TABLE_CALLS, null, args);
			return success != -1;
		} else {
			return false;
		}
	}

	public boolean createCalls(final ArrayList<CallHolder> calls) {
		boolean success = true;
		deleteAllCalls();
		for (CallHolder holder : calls) {

			if (!updateCall(holder)) {
				if (!createCall(holder)) {
					success = false;
				}
			}
		}

		return success;
	}

	public boolean createPastCalls(final ArrayList<PastCallHolder> calls) {
		boolean success = true;

		deleteAllCallHistory();
		for (PastCallHolder call : calls) {
			if (!updatePastCall(call)) {
				if (!createPastCall(call)) {
					success = false;
				}
			}
		}
		return success;
	}

	public boolean createPastCall(final PastCallHolder call) {

		final ContentValues args = getPastCallCV(call.getId(), call.getRemoteNumber(), call.getContactID(), call.getCallDisposition(), call.getCallDirection(), call.getCallDate(),
				call.getContactName());
		if (isOpen()) {
			long success = mDb.insert(DATABASE_TABLE_CALL_HISTORY, null, args);
			return success != -1;
		} else {
			return false;
		}
	}

	public boolean createWCContact(final WCContactHolder contact) {
		return createWCContact(contact.getId(), contact.getFirstName(), contact.getLastName(), contact.getHomeNumber(), contact.getWorkNumber(), contact.getCellNumber(), contact.getType(),
				contact.getEmailAddress(), contact.getFolderID(), contact.getPrimaryNumber());
	}

	public boolean createWCContact(final int id, final String firstName, final String lastName, final String homeNumber, final String workNumber, final String cellNumber, final String type,
			final String emailAdress, final int folderID, final int primaryNumberID) {
		final ContentValues args = getWCContactCV(id, firstName, lastName, homeNumber, workNumber, cellNumber, type, emailAdress, folderID, primaryNumberID);

		if (isOpen()) {
			long success = mDb.insert(DATABASE_TABLE_WC_CONTACTS, null, args);
			return success != -1;
		} else {
			return false;
		}
	}

	public boolean createFolder(final FolderHolder folder) {
		return createFolder(folder.getId(), folder.getTitle(), folder.getType(), folder.getParent(), folder.getRight(), folder.getLeft());
	}

	public boolean createFolder(final int id, final String title, final String type, final int parent, final int left, final int right) {
		final ContentValues args = getFolderCV(id, title, type, parent, left, right);

		if (isOpen()) {
			long success = mDb.insert(DATABASE_TABLE_FOLDERS, null, args);
			return success != -1;
		} else {
			return false;
		}
	}

	public boolean updateExtension(final ContactHolder extension) {
		return updateExtension(extension.getPeerId(), extension.getName(), extension.getNumber(), extension.getExtension(), extension.getAvailability(), extension.getStatusLine());
	}

	public boolean updateExtension(final int id, final String name, final String number, final String extension, final int status, final String statusLine) {
		final ContentValues args = getExtensionCV(id, name, number, extension, status, statusLine);

		if (isOpen()) {

			return mDb.update(DATABASE_TABLE_EXTENSIONS, args, KEY_ROWID + "=" + id, null) > 0;
		} else {
			return false;
		}
	}

	public boolean updateCall(final CallHolder call) {
		return updateCall(call.getCallId1(), call.getCallId2(), call.getContact().getPeerId(), call.getContactName(), call.getContact().getNumber(), call.getCallStatus(), call.getCallDirection());
	}

	public boolean updateCall(final String id, final String id2, final int contactId, final String contactName, final String remoteNumber, final String status, final String direction) {
		final ContentValues args = getCallCV(id, id2, contactId, contactName, remoteNumber, status, direction);

		if (isOpen()) {
			int success = mDb.update(DATABASE_TABLE_CALLS, args, KEY_ROWID + "=" + id, null);
			return success > 0;
		} else {
			return false;
		}
	}

	public boolean updatePastCall(final PastCallHolder call) {

		final ContentValues args = getPastCallCV(call.getId(), call.getRemoteNumber(), call.getContactID(), call.getCallDisposition(), call.getCallDirection(), call.getCallDate(),
				call.getContactName());
		if (isOpen()) {
			long success = mDb.update(DATABASE_TABLE_CALL_HISTORY, args, KEY_ROWID + "=" + call.getId(), null);
			return success > 0;
		} else {
			return false;
		}
	}

	public boolean updateFolder(final FolderHolder folder) {
		return updateFolder(folder.getId(), folder.getTitle(), folder.getType(), folder.getParent(), folder.getRight(), folder.getLeft());
	}

	public boolean updateFolder(final int id, final String title, final String type, final int parent, final int left, final int right) {
		final ContentValues args = getFolderCV(id, title, type, parent, left, right);

		if (isOpen()) {
			long success = mDb.update(DATABASE_TABLE_FOLDERS, args, KEY_ROWID + "=" + id, null);
			return success > 0;
		} else {
			return false;
		}
	}

	public boolean updateWCContact(final WCContactHolder contact) {
		return updateWCContact(contact.getId(), contact.getFirstName(), contact.getLastName(), contact.getHomeNumber(), contact.getWorkNumber(), contact.getCellNumber(), contact.getType(),
				contact.getEmailAddress(), contact.getFolderID(), contact.getPrimaryNumber());
	}

	public boolean updateWCContact(final int id, final String firstName, final String lastName, final String homeNumber, final String workNumber, final String cellNumber, final String type,
			final String emailAdress, final int folderID, final int primaryNumberID) {
		final ContentValues args = getWCContactCV(id, firstName, lastName, homeNumber, workNumber, cellNumber, type, emailAdress, folderID, primaryNumberID);

		if (isOpen()) {
			long success = mDb.update(DATABASE_TABLE_WC_CONTACTS, args, KEY_ROWID + "=" + id, null);
			return success > 0;
		} else {
			return false;
		}
	}

	public boolean updateQueues(final QueueHolder[] queueArray) {
		deleteAllQueues();

		// Insert all queues into database
		return createQueues(queueArray);
	}

	public boolean updateQueues(final ArrayList<QueueHolder> queueList) {
		// Copy the list into an array and insert into database
		QueueHolder[] temp = new QueueHolder[queueList.size()];
		queueList.toArray(temp);
		return updateQueues(temp);
	}

	private ContentValues getExtensionCV(final int id, final String name, final String number, final String extension, final int status, final String statusLine) {
		final ContentValues args = new ContentValues();
		args.put(KEY_ROWID, id);
		args.put(KEY_NAME, name == null ? "" : name);
		args.put(KEY_NUMBER, number == null ? "" : number);
		args.put(KEY_EXTENSION, extension == null ? "" : extension);
		args.put(KEY_STATUS, status);
		args.put(KEY_STATUS_TEXT, statusLine == null ? "" : statusLine);

		return args;
	}

	private ContentValues getPastCallCV(final String id, final String remoteNumber, final int contactID, final String callDisposition, final int callDirection, final String callDate,
			final String contactName) {
		final ContentValues args = new ContentValues();

		args.put(KEY_ROWID, id);
		args.put(KEY_REMOTE_NUMBER, remoteNumber);
		args.put(KEY_CONTACT_ID, contactID);
		args.put(KEY_CONTACT_NAME, contactName);
		args.put(KEY_DIRECTION, callDirection);
		args.put(KEY_CALL_DISPOSITION, callDisposition);
		args.put(KEY_CALL_DATE, callDate.toString());

		return args;
	}

	private ContentValues getCallCV(final String id, final String id2, final int contactId, final String contactName, final String remoteNumber, final String status, final String direction) {
		final ContentValues args = new ContentValues();
		args.put(KEY_ROWID, id);
		args.put(KEY_ID2, id2);
		args.put(KEY_CONTACT_ID, contactId);
		args.put(KEY_CONTACT_NAME, contactName);
		args.put(KEY_REMOTE_NUMBER, remoteNumber);
		args.put(KEY_STATUS, status);
		args.put(KEY_DIRECTION, direction);

		return args;
	}

	private ContentValues getFolderCV(final int id, final String title, final String type, final int parent, final int left, final int right) {
		final ContentValues args = new ContentValues();
		args.put(KEY_ROWID, id);
		args.put(KEY_FOLDER_TITLE, title);
		args.put(KEY_FOLDER_TYPE, type);
		args.put(KEY_FOLDER_PARENT, parent);
		args.put(KEY_FOLDER_LEFT, left);
		args.put(KEY_FOLDER_RIGHT, right);

		return args;
	}

	private ContentValues getWCContactCV(final int id, final String firstName, final String lastName, final String homeNumber, final String workNumber, final String cellNumber, final String type,
			final String emailAdress, final int folderID, final int primaryNumberID) {
		final ContentValues args = new ContentValues();
		args.put(KEY_ROWID, id);
		args.put(KEY_WC_CONTACT_TYPE, type);
		args.put(KEY_FIRST_NAME, firstName);
		args.put(KEY_LAST_NAME, lastName);
		args.put(KEY_NUMBER_HOME, homeNumber);
		args.put(KEY_NUMBER_WORK, workNumber);
		args.put(KEY_NUMBER_CELL, cellNumber);
		args.put(KEY_EMAIL_ADDRESS, emailAdress);
		args.put(KEY_PRIMARY_NUMBER_ID, primaryNumberID);
		args.put(KEY_FOLDER_ID, folderID);

		return args;

	}

	public ContactHolder fetchExtension(final int id) {
		if (isOpen()) {
			Cursor onlineCursor = mDb.query(DATABASE_TABLE_EXTENSIONS, new String[] { KEY_ROWID, KEY_NAME, KEY_NUMBER, KEY_EXTENSION, KEY_STATUS, KEY_STATUS_TEXT }, KEY_ROWID + " == " + id, null,
					null, null, null);

			if (onlineCursor != null && onlineCursor.moveToFirst()) {
				ContactHolder temp = new ContactHolder(onlineCursor.getString(1), onlineCursor.getString(2), onlineCursor.getString(3), onlineCursor.getInt(4), onlineCursor.getInt(0),
						onlineCursor.getString(5));

				onlineCursor.close();
				return temp;

			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	public CallHolder fetchCall(final String id) {

		if (isOpen()) {
			Cursor callCursor = mDb.query(DATABASE_TABLE_CALLS, new String[] { KEY_ROWID, KEY_ID2, KEY_CONTACT_ID, KEY_REMOTE_NUMBER, KEY_STATUS, KEY_DIRECTION, KEY_CONTACT_NAME }, KEY_ROWID + " = "
					+ id, null, null, null, null);

			if (isOpen() && callCursor != null && callCursor.moveToFirst()) {
				ContactHolder extension = fetchExtension(callCursor.getInt(2));
				if (extension == null) {
					String number = "0" + callCursor.getString(3);
					extension = new ContactHolder(callCursor.getString(2), number, number, 0, 0, "");
				}

				final String contactName = callCursor.getString(6);
				final String callId1 = callCursor.getString(0);
				final String callId2 = callCursor.getString(1);
				final String callStatus = callCursor.getString(4);
				final String callDirection = callCursor.getString(5);

				CallHolder holder = new CallHolder(extension, contactName, callId1, callId2, callStatus, callDirection);

				callCursor.close();
				return holder;
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	public ContactHolder fetchExtensionByExt(final int extension) {
		if (isOpen()) {
			Cursor onlineCursor = mDb.query(DATABASE_TABLE_EXTENSIONS, new String[] { KEY_ROWID, KEY_NAME, KEY_NUMBER, KEY_EXTENSION, KEY_STATUS, KEY_STATUS_TEXT },
					KEY_EXTENSION + " == " + extension, null, null, null, null);

			if (onlineCursor != null && onlineCursor.moveToFirst()) {
				ContactHolder temp = new ContactHolder(onlineCursor.getString(1), onlineCursor.getString(2), onlineCursor.getString(3), onlineCursor.getInt(4), onlineCursor.getInt(0),
						onlineCursor.getString(5));

				onlineCursor.close();
				return temp;

			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	public ArrayList<ContactHolder> fetchAllExtensions() {
		ArrayList<ContactHolder> data = new ArrayList<ContactHolder>();

		if (isOpen()) {
			Cursor onlineCursor = mDb.query(DATABASE_TABLE_EXTENSIONS, new String[] { KEY_ROWID, KEY_NAME, KEY_NUMBER, KEY_EXTENSION, KEY_STATUS, KEY_STATUS_TEXT }, KEY_STATUS + " != 0 AND "
					+ KEY_ROWID + " != 0", null, null, null, KEY_NAME);
			Cursor offlineCursor = mDb.query(DATABASE_TABLE_EXTENSIONS, new String[] { KEY_ROWID, KEY_NAME, KEY_NUMBER, KEY_EXTENSION, KEY_STATUS, KEY_STATUS_TEXT }, KEY_STATUS + " = 0 AND "
					+ KEY_ROWID + " != 0", null, null, null, KEY_NAME);

			addContactsToList(data, onlineCursor, offlineCursor);

			onlineCursor.close();
			offlineCursor.close();
		}
		return data;
	}

	public ArrayList<WebCallItemHolder> fetchTopLevelItems() {

		return fetchItemsInFolder(-1, -1);
	}

	public ArrayList<WebCallItemHolder> fetchItemsInFolder(final int folderID, final int folderLevel) {

		ArrayList<WebCallItemHolder> items = new ArrayList<WebCallItemHolder>();

		items.addAll(fetchWCContactsInFolder(folderID, folderLevel));
		items.addAll(fetchChildFolders(folderID, folderLevel));

		return items;
	}

	/**
	 * Returns all WebCall contacts in the database
	 */
	public ArrayList<WCContactHolder> fetchAllWCContacts() {
		ArrayList<WCContactHolder> data = new ArrayList<WCContactHolder>();
		if (isOpen()) {
			Cursor contactCursor = mDb.query(DATABASE_TABLE_WC_CONTACTS, new String[] { KEY_ROWID, KEY_FIRST_NAME, KEY_LAST_NAME, KEY_WC_CONTACT_TYPE, KEY_NUMBER_HOME, KEY_NUMBER_WORK,
					KEY_NUMBER_CELL, KEY_PRIMARY_NUMBER_ID, KEY_FOLDER_ID, KEY_EMAIL_ADDRESS }, null, null, null, null, KEY_ROWID + " ASC");

			if (contactCursor != null && contactCursor.moveToFirst()) {
				while (!contactCursor.isAfterLast()) {
					data.add(new WCContactHolder(contactCursor.getInt(0), contactCursor.getString(1), contactCursor.getString(2), contactCursor.getString(3), contactCursor.getString(4), contactCursor
							.getString(5), contactCursor.getString(6), contactCursor.getString(9), contactCursor.getInt(7), contactCursor.getInt(8)));

					contactCursor.moveToNext();
				}
			}

			contactCursor.close();
		}

		return data;
	}

	public ArrayList<WCContactHolder> fetchRootWCContacts() {
		return fetchWCContactsInFolder(-1, -1);
	}

	public ArrayList<WCContactHolder> fetchWCContactsInFolder(final int folderID, final int folderLevel) {
		ArrayList<WCContactHolder> data = new ArrayList<WCContactHolder>();
		if (isOpen()) {
			Cursor contactCursor = mDb.query(DATABASE_TABLE_WC_CONTACTS, new String[] { KEY_ROWID, KEY_FIRST_NAME, KEY_LAST_NAME, KEY_NUMBER_HOME, KEY_NUMBER_WORK, KEY_NUMBER_CELL,
					KEY_WC_CONTACT_TYPE, KEY_FOLDER_ID, KEY_PRIMARY_NUMBER_ID, KEY_EMAIL_ADDRESS }, KEY_FOLDER_ID + " = " + folderID, null, null, null, KEY_ROWID + " ASC");

			if (contactCursor != null && contactCursor.moveToFirst()) {
				while (!contactCursor.isAfterLast()) {
					data.add(new WCContactHolder(contactCursor.getInt(0), contactCursor.getString(1), contactCursor.getString(2), contactCursor.getString(3), contactCursor.getString(4), contactCursor
							.getString(5), contactCursor.getString(6), contactCursor.getString(9), contactCursor.getInt(7), contactCursor.getInt(8), folderLevel + 1));

					contactCursor.moveToNext();
				}
			}

			contactCursor.close();
		}

		return data;
	}

	/**
	 * Returns all folders in the database
	 */
	public ArrayList<FolderHolder> fetchAllFolders() {
		ArrayList<FolderHolder> data = new ArrayList<FolderHolder>();
		if (isOpen()) {
			Cursor folderCursor = mDb.query(DATABASE_TABLE_FOLDERS, new String[] { KEY_ROWID, KEY_FOLDER_TITLE, KEY_FOLDER_TYPE, KEY_FOLDER_PARENT, KEY_FOLDER_LEFT, KEY_FOLDER_RIGHT }, null, null,
					null, null, KEY_ROWID + " ASC");

			if (folderCursor != null && folderCursor.moveToFirst()) {
				while (!folderCursor.isAfterLast()) {
					data.add(new FolderHolder(folderCursor.getInt(0), folderCursor.getString(1), folderCursor.getString(2), folderCursor.getInt(3), folderCursor.getInt(4), folderCursor.getInt(5), 0));

					folderCursor.moveToNext();
				}
			}

			folderCursor.close();
		}

		return data;
	}

	/**
	 * Returns the top level folders contained in the db.
	 * 
	 */
	public ArrayList<FolderHolder> fetchTopLevelFolders() {
		return fetchFoldersWithParent(-1, -1);
	}

	/**
	 * Returns the folders in the db with parent ID corresponding to the given parent folder's ID, and level set in relation to parent level. 
	 * 
	 * @param parent The parent folder to find children for
	 * @param parentLevel The (indentation) level of the parent. Used to paint the GUI correctly 
	 * 
	 */
	public ArrayList<FolderHolder> fetchChildFolders(final FolderHolder parent, final int parentLevel) {
		return fetchChildFolders(parent.getId(), parentLevel);
	}

	public ArrayList<FolderHolder> fetchChildFolders(final int parentID, final int parentLevel) {
		return fetchFoldersWithParent(parentID, parentLevel);
	}

	/**
	 * Method used by wrapper methods to get all folders with a given parent ID. A parent ID of -1 gets all root folders.
	 * 
	 * @param parentId
	 * @param parentLevel
	 * @return
	 */
	private ArrayList<FolderHolder> fetchFoldersWithParent(final int parentId, final int parentLevel) {
		ArrayList<FolderHolder> data = new ArrayList<FolderHolder>();
		if (isOpen()) {
			Cursor folderCursor = mDb.query(DATABASE_TABLE_FOLDERS, new String[] { KEY_ROWID, KEY_FOLDER_TITLE, KEY_FOLDER_TYPE, KEY_FOLDER_PARENT, KEY_FOLDER_LEFT, KEY_FOLDER_RIGHT },
					KEY_FOLDER_PARENT + " = " + parentId, null, null, null, KEY_ROWID + " ASC");

			if (folderCursor != null && folderCursor.moveToFirst()) {
				while (!folderCursor.isAfterLast()) {
					data.add(new FolderHolder(folderCursor.getInt(0), folderCursor.getString(1), folderCursor.getString(2), folderCursor.getInt(3), folderCursor.getInt(4), folderCursor.getInt(5),
							parentLevel + 1));

					folderCursor.moveToNext();
				}
			}

			folderCursor.close();
		}

		return data;
	}

	public QueueHolder[] fetchAllQueues() {
		ArrayList<QueueHolder> data = new ArrayList<QueueHolder>();

		if (isOpen()) {
			Cursor queueCursor = mDb.query(DATABASE_TABLE_QUEUES, new String[] { KEY_ROWID, KEY_NAME, KEY_STATUS }, null, null, null, null, KEY_ROWID + " ASC");

			if (queueCursor != null && queueCursor.moveToFirst()) {
				while (!queueCursor.isAfterLast()) {

					data.add(new QueueHolder(queueCursor.getInt(0), queueCursor.getString(1), queueCursor.getInt(2) == 1));

					queueCursor.moveToNext();
				}
				queueCursor.close();
			}

		}
		QueueHolder[] holders = new QueueHolder[data.size()];

		return data.toArray(holders);
	}

	public ArrayList<CallHolder> fetchAllCalls() {

		ArrayList<CallHolder> data = new ArrayList<CallHolder>();

		if (isOpen()) {
			Cursor callCursor = mDb.query(DATABASE_TABLE_CALLS, new String[] { KEY_ROWID, KEY_ID2, KEY_CONTACT_ID, KEY_REMOTE_NUMBER, KEY_STATUS, KEY_DIRECTION, KEY_CONTACT_NAME }, null, null, null,
					null, KEY_ROWID);

			if (isOpen() && callCursor != null && callCursor.moveToFirst()) {
				while (!callCursor.isAfterLast() && isOpen()) {
					ContactHolder extension = fetchExtension(callCursor.getInt(2));
					if (extension == null) {
						String s = "0" + callCursor.getString(2);
						extension = new ContactHolder(callCursor.getString(3), s, s, 0, 0, "");
					}

					final String contactName = callCursor.getString(6);
					final String callId1 = callCursor.getString(0);
					final String callId2 = callCursor.getString(1);
					final String callStatus = callCursor.getString(4);
					final String callDirection = callCursor.getString(5);

					CallHolder holder = new CallHolder(extension, contactName, callId1, callId2, callStatus, callDirection);

					data.add(holder);

					callCursor.moveToNext();
				}

			}
			callCursor.close();
		}
		return data;

	}

	public ArrayList<PastCallHolder> fetchAllCallHistory() {

		ArrayList<PastCallHolder> data = new ArrayList<PastCallHolder>();

		if (isOpen()) {
			Cursor callCursor = mDb.query(DATABASE_TABLE_CALL_HISTORY, new String[] { KEY_ROWID, KEY_REMOTE_NUMBER, KEY_CONTACT_ID, KEY_CONTACT_NAME, KEY_DIRECTION, KEY_CALL_DISPOSITION,
					KEY_CALL_DATE }, null, null, null, null, KEY_CALL_DATE + " DESC");

			if (isOpen() && callCursor != null && callCursor.moveToFirst()) {
				while (!callCursor.isAfterLast() && isOpen()) {

					final String callId1 = callCursor.getString(0);
					final String remoteNumber = callCursor.getString(1);
					final String contactName = callCursor.getString(3);
					final String callDisposition = callCursor.getString(5);
					final String callDate = callCursor.getString(6);

					final int contactID = callCursor.getInt(2);
					final int callDirection = callCursor.getInt(4);

					PastCallHolder holder = new PastCallHolder(callId1, remoteNumber, callDisposition, callDirection, contactName, contactID, callDate);

					data.add(holder);

					callCursor.moveToNext();
				}

			}
			callCursor.close();
		}
		return data;
	}

	private void addContactsToList(final ArrayList<ContactHolder> data, final Cursor onlineCursor, final Cursor offlineCursor) {
		// First, add all online contacts to list
		if (onlineCursor != null && onlineCursor.moveToFirst()) {
			while (!onlineCursor.isAfterLast()) {
				data.add(new ContactHolder(onlineCursor.getString(1), onlineCursor.getString(2), onlineCursor.getString(3), onlineCursor.getInt(4), onlineCursor.getInt(0), onlineCursor.getString(5)));
				onlineCursor.moveToNext();
			}
		}
		// Add all offline contacts to list
		if (offlineCursor != null && offlineCursor.moveToFirst()) {
			while (!offlineCursor.isAfterLast()) {

				data.add(new ContactHolder(offlineCursor.getString(1), offlineCursor.getString(2), offlineCursor.getString(3), offlineCursor.getInt(4), offlineCursor.getInt(0), offlineCursor
						.getString(5)));
				offlineCursor.moveToNext();
			}
		}
	}

	// DELETE

	public void deleteAllContacts() {
		if (isOpen()) {
			mDb.delete(DATABASE_TABLE_EXTENSIONS, null, null);
		}
	}

	public void deleteAllCalls() {
		if (isOpen()) {
			mDb.delete(DATABASE_TABLE_CALLS, null, null);
		}
	}

	public void deleteAllCallHistory() {
		if (isOpen()) {
			mDb.delete(DATABASE_TABLE_CALL_HISTORY, null, null);
		}
	}

	public void deleteAllQueues() {
		if (isOpen()) {
			mDb.delete(DATABASE_TABLE_QUEUES, null, null);
		}
	}

	public void deleteFolder(final int folderId) {
		if (isOpen()) {
			mDb.delete(DATABASE_TABLE_FOLDERS, KEY_ROWID + " = '" + folderId + "'", null);
		}
	}

	public void deleteAllFolders() {
		if (isOpen()) {
			mDb.delete(DATABASE_TABLE_FOLDERS, null, null);
		}
	}

	public void deleteWCContact(final int contactId) {
		if (isOpen()) {
			mDb.delete(DATABASE_TABLE_WC_CONTACTS, KEY_ROWID + " = '" + contactId + "'", null);
		}
	}

	public void deleteAllWCContacts() {
		if (isOpen()) {
			mDb.delete(DATABASE_TABLE_WC_CONTACTS, null, null);
		}
	}

	public boolean deleteCall(final String callId) {
		if (isOpen()) {
			return mDb.delete(DATABASE_TABLE_CALLS, KEY_ROWID + " = '" + callId + "'", null) != 0;
		} else {
			return false;
		}

	}

	public boolean createPastCallsFromResponse(final JSONArray message, final Context context) throws NullPointerException {
		ArrayList<PastCallHolder> calls = new ArrayList<PastCallHolder>();
		if (isOpen()) {

			if (message != null) {
				for (int i = 0, length = message.length(); i < length; i++) {

					JSONObject temp = null;

					try {
						temp = message.getJSONObject(i);
						String callID = "";
						String disposition = "";
						String contactName = "";
						String extension = "";
						int contactID = 0;
						int direction = PastCallHolder.CALL_DIRECTION_IN;
						String callDate = "";

						callID = temp.has("id") ? temp.getString("id") : "";
						disposition = temp.has("disposition") ? temp.getString("disposition") : "";
						contactID = Integer.parseInt(temp.has("peer_id") ? temp.getString("peer_id") : "0");
						callDate = temp.has("calldate") ? temp.getString("calldate") : Calendar.getInstance().toString();
						// Remove seconds from call date
						callDate = callDate.substring(0, callDate.lastIndexOf(':'));

						String directionString = temp.has("direction") ? temp.getString("direction") : "";
						if (directionString.equalsIgnoreCase("IN")) {
							direction = PastCallHolder.CALL_DIRECTION_IN;
						} else if (directionString.equalsIgnoreCase("OUT")) {
							direction = PastCallHolder.CALL_DIRECTION_OUT;
						}

						String contactInfo = temp.has("clid") ? temp.getString("clid") : "";
						if (contactInfo.length() > 0) {
							if(contactInfo.contains("\"")){
								contactName = contactInfo.substring(contactInfo.indexOf('\"') + 1, contactInfo.lastIndexOf('\"'));
							}else{
								contactName = contactInfo;
							}
							if(contactInfo.contains("<")){
								
								extension = contactInfo.substring(contactInfo.indexOf('<') + 1, contactInfo.indexOf('>'));
							}else{
								extension = contactInfo;
							}
						}

						// Look for contact in phone contacts
						Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(extension));
						ContentResolver resolver = context.getContentResolver();

						Cursor lookupCursor = resolver.query(uri, new String[] { PhoneLookup.DISPLAY_NAME }, null, null, null);

						// If contact was found, set new contact name
						if (lookupCursor != null && lookupCursor.moveToFirst()) {
							contactName = lookupCursor.getString(0);
						}
						lookupCursor.close();

						calls.add(new PastCallHolder(callID, extension, disposition, direction, contactName, contactID, callDate));

					} catch (JSONException e) {

						Log.e("MainFragment.onReceive()", "An error occured: " + e.getLocalizedMessage());

					}

				}
			}

			return createPastCalls(calls);
		} else {
			return false;
		}

	}

	public boolean createCallsFromResponse(final JSONArray message, final Context context) throws NullPointerException {

		ArrayList<CallHolder> calls = new ArrayList<CallHolder>();
		if (isOpen()) {

			if (message != null) {
				for (int i = 0, length = message.length(); i < length; i++) {

					int id = -1;
					String extension = "";
					JSONObject temp = null;

					try {
						temp = message.getJSONObject(i);

						id = temp.has("p") ? temp.getInt("p") : -1;
						extension = temp.has("nr2") ? temp.getString("nr2") : "";

					} catch (JSONException e) {

						Log.e("MainFragment.onReceive()", "An error occured: " + e.getLocalizedMessage());

					}
					String callID1 = "";
					String callID2 = "";
					String dir = "";
					String status = "";
					String contactName = "";

					try {
						callID1 = temp.has("id1") ? temp.getString("id1") : "";
						callID2 = temp.has("id2") ? temp.getString("id2") : "";
						dir = temp.has("dir") ? temp.getString("dir") : "";
						status = temp.has("s") ? temp.getString("s") : "R";
						contactName = temp.has("name2") ? temp.getString("name2") : "";
					} catch (JSONException e) {

						Log.e("MainFragment.onReceive()", "An error occured: " + e.getLocalizedMessage());
					}

					if (!extension.equals("")) {

						ContactHolder contact = fetchExtensionByExt(Integer.parseInt(extension));

						if (contact != null) {

							// We have found the correct contact, add call to list with the correct status
							if (!status.equals("")) {

								calls.add(new CallHolder(contact, contactName, callID1, callID2, status, dir));
							} else {
								calls.add(new CallHolder(contact, contactName, callID1, callID2, "R", dir));
							}
						} else {
							// Look for contact in phone contacts
							Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(extension));
							ContentResolver resolver = context.getContentResolver();

							Cursor lookupCursor = resolver.query(uri, new String[] { PhoneLookup.DISPLAY_NAME }, null, null, null);

							if (lookupCursor != null && lookupCursor.moveToFirst()) {
								calls.add(new CallHolder(new ContactHolder(lookupCursor.getString(0), lookupCursor.getString(0), extension, 0, Integer.parseInt(extension), ""), contactName, callID1,
										callID2, status, dir));
							} else {
								calls.add(new CallHolder(new ContactHolder(extension, extension, extension, 0, 0, ""), contactName, callID1, callID2, status, dir));
							}
							lookupCursor.close();
						}
					} else {

						try {
							// If ID is -1, this is a new, outgoing call
							calls.add(new CallHolder(new ContactHolder(context.getString(R.string.new_call), context.getString(R.string.call_direction_out), "", 0, 0, ""), contactName, temp
									.getString("id1"), temp.has("id2") ? temp.getString("id2") : "", "R", "OUT"));
						} catch (JSONException e) {

							Log.e("MainFragment.onReceive()", "An error occured: " + e.getLocalizedMessage());
						}

					}
				}
			}

			return createCalls(calls);
		} else {
			return false;
		}
	}

}
