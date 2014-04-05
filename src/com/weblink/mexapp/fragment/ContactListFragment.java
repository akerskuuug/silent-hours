package com.weblink.mexapp.fragment;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.support.v4.app.ListFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.weblink.mexapp.R;
import com.weblink.mexapp.R.string;
import com.weblink.mexapp.activity.MainActivity;
import com.weblink.mexapp.db.MexDbAdapter;
import com.weblink.mexapp.interfaces.AsyncTaskCompleteListener;
import com.weblink.mexapp.interfaces.CallListener;
import com.weblink.mexapp.pojo.ContactHolder;
import com.weblink.mexapp.pojo.FolderHolder;
import com.weblink.mexapp.pojo.User;
import com.weblink.mexapp.pojo.WCContactHolder;
import com.weblink.mexapp.pojo.WebCallItemHolder;
import com.weblink.mexapp.utility.Constants;
import com.weblink.mexapp.utility.ExpandAnimation;
import com.weblink.mexapp.utility.ExpandRelativeLayoutAnimation;
import com.weblink.mexapp.utility.IndentAnimation;
import com.weblink.mexapp.utility.Task;
import com.weblink.mexapp.utility.Task.DeleteFolderTask;
import com.weblink.mexapp.utility.Tools;

public class ContactListFragment extends ListFragment implements AsyncTaskCompleteListener<ArrayList<Bitmap>>, OnItemLongClickListener {
	/**
	 * The fragment argument representing the section number for this fragment.
	 */
	// private MainActivity activity;
	public static final String ARG_SECTION_NUMBER = "section_number";
	private ArrayList<ContactHolder> contactList, localContactList;
	private ArrayList<WebCallItemHolder> webCallItems;

	private ArrayList<Integer> openFolders;
	public User user;
	private SharedPreferences sharedPrefs;
	private Editor editor;

	private MainActivity myActivity;
	public MexDbAdapter dbAdapter;
	private LinearLayout headerContainer;
	private TextView tv;

	public ProgressDialog contactProgressDialog;
	private ArrayList<Button> headerButtons;
	private int isShowingContactType;
	private ArrayList<Bitmap> contactImageBitmaps;

	private LinearLayout webCallFooter;

	@Override
	public void onAttach(final Activity activity) {
		super.onAttach(activity);

		myActivity = (MainActivity) activity;

		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(myActivity);
		editor = sharedPrefs.edit();

		user = new User(sharedPrefs.getString(Constants.LOGIN_COMPANY, ""), sharedPrefs.getString(Constants.LOGIN_PASSWORD, ""), sharedPrefs.getInt(Constants.LOGIN_EXTENSION, 0));

		isShowingContactType = Constants.CONTACT_TYPE_EXTENSION;
		localContactList = getLocalContactList();

		// Load contact images in the background
		new Task.GetContactImagesTask(myActivity.getContentResolver(), this).execute();
	}

	@Override
	public void onActivityCreated(final Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		dbAdapter = myActivity.getDbAdapter();

		contactList = dbAdapter.fetchAllExtensions();

		if (contactList.isEmpty()) {
			contactProgressDialog = new ProgressDialog(myActivity);
			contactProgressDialog.setMessage(myActivity.getString(R.string.contacts_loading));
			contactProgressDialog.setCancelable(true);
			contactProgressDialog.setCanceledOnTouchOutside(false);
			contactProgressDialog.setIndeterminate(true);

			contactProgressDialog.show();
		}

		ListView listView = getListView();
		listView.setOnItemLongClickListener(this);
		listView.setFastScrollEnabled(true);
		listView.setTextFilterEnabled(true);
		listView.setHeaderDividersEnabled(false);
		listView.setBackgroundResource(R.drawable.abs__ab_solid_shadow_holo);

	}

	
	@Override
	public void onResume() {
		super.onResume();
		ListView lv = getListView();

		// Initialize header and footer, add to listview
		initializeFooter();
		initializeHeader();
		
		setListAdapter(null);
		if (lv.getHeaderViewsCount() == 0) {
			if(headerContainer != null){
				lv.addHeaderView(headerContainer);
			}
			if(webCallFooter != null){
				lv.addFooterView(webCallFooter);
			}
		}
		
		openFolders = new ArrayList<Integer>();

		Tools.dismissCallPopup(myActivity);

		setContactLists();

	}

	/**
	 * Initializes the views in the footer for Web Call folders and contacts
	 */
	private void initializeFooter() {
		webCallFooter = (LinearLayout) myActivity.getLayoutInflater().inflate(R.layout.list_footer_webcall, null);

		Button folderButton = (Button) webCallFooter.findViewById(R.id.footer_folder_add_button);
		folderButton.setTypeface(myActivity.getTypefaceRobotoLight());

		folderButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(final View v) {
				myActivity.getFolderDialog(null).show();
			}
		});

		Button contactButton = (Button) webCallFooter.findViewById(R.id.footer_contact_add_button);
		contactButton.setTypeface(myActivity.getTypefaceRobotoLight());
		contactButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(final View v) {
				// Show contact dialog
				myActivity.getAddContactDialog(null).show();

			}
		});

	}

	
	private void initializeHeader() {
		if (headerContainer == null) {
			headerContainer = (LinearLayout) getLayoutInflater(getArguments()).inflate(R.layout.list_header_extensiontab, null).findViewById(R.id.header_container);
			tv = (TextView) headerContainer.findViewById(R.id.header_text);
			tv.setText(R.string.no_internet_long_contacts);
			tv.setTypeface(myActivity.getTypefaceRobotoLight());

			EditText filterText = (EditText) headerContainer.findViewById(R.id.filter_text);
			filterText.setTypeface(myActivity.getTypefaceRobotoLight());
			filterText.addTextChangedListener(new TextWatcher() {

				@Override
				public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {

					// Update text filter
					if (isShowingContactType != Constants.CONTACT_TYPE_WEBCALL) {
						((ContactListAdapter) getListAdapter()).getFilter().filter(s.toString().trim());
					}

				}

				@Override
				public void afterTextChanged(final Editable s) {
				}

				@Override
				public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
				}

			});

			isShowingContactType = sharedPrefs.getInt(Constants.SHOW_CONTACT_TYPE, Constants.CONTACT_TYPE_EXTENSION);

			Button extensionButton = (Button) headerContainer.findViewById(R.id.header_button_extensions);
			Button localButton = (Button) headerContainer.findViewById(R.id.header_button_local);
			Button contactButton = (Button) headerContainer.findViewById(R.id.header_button_company);

			headerButtons = new ArrayList<Button>();
			headerButtons.add(extensionButton);
			headerButtons.add(localButton);
			headerButtons.add(contactButton);

			OnClickListener listener = new OnClickListener() {
				@Override
				public void onClick(final View v) {
					int buttonTag = Integer.parseInt(v.getTag().toString());
					myActivity.setTabTitle(buttonTag);

					for (Button b : headerButtons) {
						b.setClickable(true);
						b.setBackgroundResource(R.drawable.abs__list_selector_holo_light);
					}

					((Button) v).setClickable(false);
					v.setBackgroundColor(Color.rgb(51, 181, 229));

					isShowingContactType = buttonTag;
					editor.putInt(Constants.SHOW_CONTACT_TYPE, isShowingContactType).commit();

					// Hide the "Add folder" footer if we are not viewing WebCall contacts
					webCallFooter.setVisibility(isShowingContactType == Constants.CONTACT_TYPE_WEBCALL ? View.VISIBLE : View.GONE);

					setContactLists();

					if (isResumed()) {
						Tools.smoothScrollListView(getListView(), 1);
					}
				}
			};

			for (Button button : headerButtons) {
				button.setTypeface(myActivity.getTypefaceRobotoMedium());
				button.setOnClickListener(listener);
			}

			switch (isShowingContactType) {
			case Constants.CONTACT_TYPE_LOCAL:
				localButton.performClick();
				break;
			case Constants.CONTACT_TYPE_WEBCALL:
				contactButton.performClick();
				break;
			default:
				extensionButton.performClick();
				break;
			}

		}

		if (myActivity.isNetworkAvailable()) {
			tv.setVisibility(View.GONE);
		} else {

			tv.setVisibility(View.VISIBLE);

		}
		

	}

	@SuppressLint("InlinedApi")
	private ArrayList<ContactHolder> getLocalContactList() {
		ArrayList<ContactHolder> newList = new ArrayList<ContactHolder>();
		contactImageBitmaps = new ArrayList<Bitmap>();
		final ContentResolver resolver = myActivity.getContentResolver();

		Cursor contactCursor = resolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, new String[] { Phone.DISPLAY_NAME, Phone.NUMBER, Phone.CONTACT_ID, }, null, null, Phone.DISPLAY_NAME
				+ " ASC");

		if (contactCursor != null && contactCursor.moveToFirst()) {
			while (!contactCursor.isAfterLast()) {
				int nameFieldColumnIndex = contactCursor.getColumnIndex(Phone.DISPLAY_NAME);
				if (nameFieldColumnIndex != -1) {
					String contactName = contactCursor.getString(nameFieldColumnIndex);
					final int contactID;

					int contactIDColumnIndex = contactCursor.getColumnIndex(Phone.CONTACT_ID);
					if (contactIDColumnIndex != -1) {
						contactID = contactCursor.getInt(contactIDColumnIndex);
					} else {
						contactID = 0;
					}

					int numberFieldColumnIndex = contactCursor.getColumnIndex(Phone.NUMBER);

					if (numberFieldColumnIndex != -1) {

						String number = contactCursor.getString(numberFieldColumnIndex);

						// Remove +46's to make the contact list look better
						number = number.startsWith("+46") ? 0 + number.substring(3) : number;
						number = number.replace(" ", "");

						newList.add(new ContactHolder(contactName, number, number, 1, contactID, ""));
					}

				}
				contactCursor.moveToNext();
			}
		}

		contactCursor.close();

		return newList;
	}

	public void onReceive(final ContactHolder message) {

		if (message != null) {
			for (int i = 0, length = contactList.size(); i < length; i++) {
				final ContactHolder temp = contactList.get(i);

				if (temp.getPeerId() == message.getPeerId()) {

					updateView(message, temp, i, length);
					break;
				}
			}
		}

	}

	public void setContactLists() {
		openFolders = new ArrayList<Integer>();
		contactList = dbAdapter.fetchAllExtensions();
		webCallItems = dbAdapter.fetchTopLevelItems();

		setAdapter();
	}

	private void setAdapter() {
		int showContactType = sharedPrefs.getInt(Constants.SHOW_CONTACT_TYPE, Constants.CONTACT_TYPE_EXTENSION);

		// Extensions and local contacts share adapter type
		if (showContactType == Constants.CONTACT_TYPE_EXTENSION || showContactType == Constants.CONTACT_TYPE_LOCAL) {

			// Show filter input view
			headerContainer.findViewById(R.id.filter_text).setVisibility(View.VISIBLE);

			if (getListAdapter() == null || !(getListAdapter() instanceof ContactListAdapter)) {
				ContactListAdapter adapter = new ContactListAdapter(myActivity.getBaseContext(), showContactType == Constants.CONTACT_TYPE_LOCAL ? localContactList : contactList, R.id.contact_name);
				setListAdapter(adapter);

			} else {
				((ContactListAdapter) getListAdapter()).refill(showContactType == Constants.CONTACT_TYPE_LOCAL ? localContactList : contactList);
			}

		} else if (showContactType == Constants.CONTACT_TYPE_WEBCALL) {
			// Remove filter input view, since filters don't make as much sense for WebCall contacts
			headerContainer.findViewById(R.id.filter_text).setVisibility(View.GONE);

			if (getListAdapter() == null || !(getListAdapter() instanceof WebCallListAdapter)) {
				setListAdapter(new WebCallListAdapter(myActivity, webCallItems, R.id.folder_title));
			} else {
				((WebCallListAdapter) getListAdapter()).refill(webCallItems);
			}

		}

	}

	private void updateView(final ContactHolder newStat, final ContactHolder contact, final int index, final int length) {
		final int newIndex;
		String contactName = contact.getName();

		if (newStat.getAvailability() != 0 && contact.getAvailability() != 0) {
			newIndex = index;
		} else if (newStat.getAvailability() == 0 && contact.getAvailability() != 0 || newStat.getAvailability() != 0 && contact.getAvailability() == 0) {
			// Determine if the user is logging in or out
			boolean isLogin = newStat.getAvailability() != 0 && contact.getAvailability() == 0;

			// Temporary variable to circumvent problems with uninitialized
			// final variables (newIndex) with Runnable
			int j = 0;

			for (int i = 0; i < length; i++) {
				ContactHolder temp = contactList.get(i);

				// If this is a login, the user will be moved to the upper (online) part of the list, otherwise the lower (offline) part
				if ((isLogin || temp.getAvailability() == 0) && contactName.compareTo(temp.getName()) < 0) {
					j = i - 1;

					contactList.remove(index);
					contactList.add(j, contact);

					break;
				}

			}

			newIndex = j;
		} else {
			// Something went wrong
			Log.e("ContactListFragment.updateView()", "An error occured.");
			newIndex = -1;
		}

		Runnable run = new Runnable() {

			@Override
			public void run() {

				if (newIndex != -1) {

					// Update the extension in the db
					if (newStat.getAvailability() != -1) {
						contact.setAvailability(newStat.getAvailability());
					}
					contact.setStatusLine(newStat.getStatusLine());

					dbAdapter.updateExtension(contact);

					if (isShowingContactType == Constants.CONTACT_TYPE_EXTENSION) {
						final ContactListAdapter adapter = (ContactListAdapter) getListAdapter();
						if (adapter != null && !adapter.isUsingFilter()) {

							adapter.move(index, newIndex);

						}
					}
				}

			}

		};

		myActivity.runOnUiThread(run);

	}

	@Override
	public void onListItemClick(final ListView l, final View v, final int id, final long position) {

		if (isShowingContactType == Constants.CONTACT_TYPE_EXTENSION || isShowingContactType == Constants.CONTACT_TYPE_LOCAL) {
			onContactItemClicked(l, v, id, position);
		} else {
			onWebcallItemClicked(l, v, id, position);
		}

	}

	private void onWebcallItemClicked(final ListView l, final View v, final int id, final long position) {

		final WebCallItemHolder temp = webCallItems.get((int) position);
		if (temp.getItemType() == WebCallItemHolder.ITEM_TYPE_FOLDER) {
			if (!openFolders.contains(temp.getItemId())) {
				// List indexes will never surpass INTEGER_MAX
				int intPosition = (int) position + 1;

				ArrayList<WebCallItemHolder> childFolders = dbAdapter.fetchItemsInFolder(temp.getItemId(), temp.getItemLevel());

				// Show all folders
				webCallItems.addAll(intPosition, childFolders);

				setAdapter();

				openFolders.add(temp.getItemId());
			}
		} else if (temp.getItemType() == WebCallItemHolder.ITEM_TYPE_CONTACT) {
			final WCContactHolder contact = (WCContactHolder) temp;

			// Set to GONE because otherwise it interfered with list item clicks
			final View toolbarContent = v.findViewById(R.id.wc_toolbar_content);
			toolbarContent.setVisibility(View.VISIBLE);

			final View toolbar = v.findViewById(R.id.wc_toolbar_scroll);

			// Creating the expand animation for the item
			ExpandRelativeLayoutAnimation expandAni = new ExpandRelativeLayoutAnimation(toolbar, 200);

			toolbar.startAnimation(expandAni);

			Button callPrimaryButton = (Button) v.findViewById(R.id.call_default_button);
			callPrimaryButton.setText(contact.getDefaultNumber());
			callPrimaryButton.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(final View v) {

					myActivity.makeCall(contact.getFullName(), contact.getDefaultNumber());

				}
			});

			Button callOtherButton = (Button) v.findViewById(R.id.call_other_button);
			callOtherButton.setOnClickListener(new OnClickListener() {
				final String contactName = contact.getFullName();
				final String cellNumber = contact.getCellNumber();
				final String workNumber = contact.getWorkNumber();
				final String homeNumber = contact.getHomeNumber();

				@Override
				public void onClick(final View v) {
					AlertDialog.Builder builder = Tools.getSimpleDialog(myActivity, R.string.call_select, -1);

					ArrayList<String> callItems = new ArrayList<String>();
					final ArrayList<String> numbers = new ArrayList<String>();
					if (workNumber.length() > 0) {
						callItems.add(getString(R.string.call_number_work) + " " + workNumber);
						numbers.add(workNumber);
					}
					if (cellNumber.length() > 0) {
						callItems.add(getString(R.string.call_number_cell) + " " + cellNumber);
						numbers.add(cellNumber);
					}
					if (homeNumber.length() > 0) {
						callItems.add(getString(R.string.call_number_home) + " " + homeNumber);
						numbers.add(homeNumber);
					}

					String[] temp = new String[callItems.size()];
					callItems.toArray(temp);
					builder.setItems(temp, new DialogInterface.OnClickListener() {

						@Override
						public void onClick(final DialogInterface dialog, final int position) {
							myActivity.makeCall(contactName, numbers.get(position));
						}

					});

					builder.show();

				}
			});

			Button transferButton = (Button) v.findViewById(R.id.transfer_button);
			transferButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(final View v) {
					myActivity.showTransferDialog(contact.getDefaultNumber());

				}
			});

		}

	}

	private void onContactItemClicked(final ListView l, final View v, final int id, final long position) {

		ArrayList<ContactHolder> contactList = isShowingContactType == Constants.CONTACT_TYPE_LOCAL ? localContactList : this.contactList;

		final LinearLayout toolbar = (LinearLayout) v.findViewById(R.id.toolbar);
		final TextView nameText = (TextView) v.findViewById(R.id.contact_name);
		final TextView extensionText = (TextView) v.findViewById(R.id.contact_extension);

		final String callName = nameText.getText().toString().trim();
		final String callExtension = extensionText.getText().toString().trim();

		// Start the animation on the toolbar
		if (contactList.get((int) position).getAvailability() != 0) {

			// Creating the expand animation for the item
			ExpandAnimation expandAni = new ExpandAnimation(toolbar, 200);

			toolbar.startAnimation(expandAni);
		}

		Button callButton = (Button) toolbar.findViewById(R.id.call_button);
		callButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(final View v) {

				myActivity.makeCall(callName, callExtension);

			}
		});

		Button transferButton = (Button) toolbar.findViewById(R.id.transfer_button);
		transferButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(final View v) {

				myActivity.showTransferDialog(callExtension);
			}
		});
	}

	/**
	 * Starts an attended transfer to the number transferTo
	 * 
	 * @param context
	 * @param user The user who owns the call
	 * @param callID The call to transfer
	 * @param transferTo The number to transfer to
	 * @param callback The callback to call when finished or failed
	 */
	@SuppressLint("NewApi")
	public static void startAttendedTransfer(final User user, final String callID, final String transferTo, final CallListener callback, final SharedPreferences sharedPrefs) {

		Task.AttendedTransferTask task = new Task.AttendedTransferTask(user, callID, transferTo, callback, sharedPrefs);
		if (Tools.isHoneycombOrLater()) {
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		} else {
			task.execute();
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(callback.getContext());

		builder.setTitle(R.string.transfer_att_prompt);
		builder.setCancelable(false);

		builder.setNegativeButton(R.string.transfer_att_cancel, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(final DialogInterface dialog, final int which) {
				Task.AttendedTransferTask task = new Task.AttendedTransferTask(user, sharedPrefs.getString(Constants.TRANSFER_CHANNEL, ""), Task.AttendedTransferTask.ATTENDED_TRANSFER_CANCEL,
						callback, sharedPrefs);
				if (Tools.isHoneycombOrLater()) {
					task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				} else {
					task.execute();
				}
			}
		});

		builder.setPositiveButton(R.string.transfer_att_finish, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(final DialogInterface dialog, final int which) {
				Task.AttendedTransferTask task = new Task.AttendedTransferTask(user, sharedPrefs.getString(Constants.TRANSFER_CHANNEL, ""), null, callback, sharedPrefs);
				if (Tools.isHoneycombOrLater()) {
					task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				} else {
					task.execute();
				}
			}
		});

		builder.show();
	}

	public class WebCallListAdapter extends ArrayAdapter<WebCallItemHolder> {
		WebCallItemHolder[] items;

		private final Drawable folderDrawable;
		private final Bitmap contactImageBitmap;

		public WebCallListAdapter(final Context context, final List<WebCallItemHolder> folders, final int textViewResourceId) {
			super(context, textViewResourceId, folders);

			items = new WebCallItemHolder[folders.size()];
			folders.toArray(items);

			contactImageBitmap = Tools.getDefaultContactPhoto(myActivity);
			folderDrawable = myActivity.getResources().getDrawable(myActivity.getAppTheme() == Constants.THEME_LIGHT ? R.drawable.ic_action_collection : R.drawable.ic_action_collection_dark);

		}

		public void refill(final ArrayList<WebCallItemHolder> folders) {
			items = new WebCallItemHolder[folders.size()];
			folders.toArray(items);

			notifyDataSetChanged();
		}

		@Override
		public View getView(final int position, View convertView, final ViewGroup parent) {
			WebCallItemHolder tempHolder = items[position];
			String workString = getString(R.string.call_number_work) + " ";
			String cellString = getString(R.string.call_number_cell) + " ";
			String homeString = getString(R.string.call_number_home) + " ";

			if (tempHolder.getItemType() == WebCallItemHolder.ITEM_TYPE_FOLDER) {
				if (convertView == null || convertView.findViewById(R.id.folder_title) == null) {
					convertView = LayoutInflater.from(myActivity).inflate(R.layout.list_item_folder, null);
				} else {
					convertView.setPadding(0, 0, 0, 0);
				}

				TextView txt = (TextView) convertView.findViewById(R.id.folder_title);
				txt.setText(tempHolder.getTitle());
				txt.setTypeface(myActivity.getTypefaceRobotoLight());

				txt = (TextView) convertView.findViewById(R.id.folder_type);
				txt.setText(tempHolder.getSubTitle());
				txt.setTypeface(myActivity.getTypefaceRobotoLight());

				ImageView imgV = (ImageView) convertView.findViewById(R.id.wc_item_image);
				imgV.setImageDrawable(folderDrawable);

				if (tempHolder.getItemLevel() != 0) {
					if (!tempHolder.isVisibleAlready()) {
						IndentAnimation indentanim = new IndentAnimation(convertView, 300, 30 * tempHolder.getItemLevel() + 10);
						convertView.startAnimation(indentanim);
					} else {
						convertView.setPadding(30 * tempHolder.getItemLevel() + 40, 0, 0, 0);
					}
				}

				tempHolder.setIsVisible(true);
			} else if (tempHolder.getItemType() == WebCallItemHolder.ITEM_TYPE_CONTACT) {
				WCContactHolder contact = (WCContactHolder) tempHolder;

				if (convertView == null || convertView.findViewById(R.id.contact_name) == null) {
					convertView = LayoutInflater.from(myActivity).inflate(R.layout.list_item_wc_contact, null);
				} else {
					convertView.setPadding(0, 0, 0, 0);
				}

				TextView txt = (TextView) convertView.findViewById(R.id.contact_name);
				txt.setText(contact.getFullName());
				txt.setTypeface(myActivity.getTypefaceRobotoLight());

				txt = (TextView) convertView.findViewById(R.id.contact_prim_number);
				txt.setText(contact.getDefaultNumber());
				txt.setTypeface(myActivity.getTypefaceRobotoLight());

				ImageView imgV = (ImageView) convertView.findViewById(R.id.wc_item_image);
				imgV.setImageBitmap(contactImageBitmap);
				imgV.setScaleType(ScaleType.CENTER_CROP);

				String workNumber = contact.getWorkNumber();
				String cellNumber = contact.getCellNumber();
				String homeNumber = contact.getHomeNumber();

				txt = (TextView) convertView.findViewById(R.id.contact_number_work);
				if (workNumber != null && workNumber.length() > 0) {
					txt.setText(workString + workNumber.trim());
					txt.setTypeface(myActivity.getTypefaceRobotoLight());
					txt.setVisibility(View.VISIBLE);
				} else {
					txt.setVisibility(View.GONE);
				}

				txt = (TextView) convertView.findViewById(R.id.contact_number_cell);
				if (cellNumber != null && cellNumber.length() > 0) {
					txt.setText(cellString + cellNumber.trim());
					txt.setTypeface(myActivity.getTypefaceRobotoLight());
					txt.setVisibility(View.VISIBLE);
				} else {

					txt.setVisibility(View.GONE);
				}

				txt = (TextView) convertView.findViewById(R.id.contact_number_home);
				if (homeNumber != null && homeNumber.length() > 0) {
					txt.setText(homeString + homeNumber.trim());
					txt.setTypeface(myActivity.getTypefaceRobotoLight());
					txt.setVisibility(View.VISIBLE);
				} else {

					txt.setVisibility(View.GONE);
				}

				if (tempHolder.getItemLevel() != 0) {
					if (!tempHolder.isVisibleAlready()) {
						IndentAnimation indentanim = new IndentAnimation(convertView, 300, 30 * tempHolder.getItemLevel() + 10);
						convertView.startAnimation(indentanim);
					} else {
						convertView.setPadding(30 * tempHolder.getItemLevel() + 40, 0, 0, 0);
					}
				}
				tempHolder.setIsVisible(true);

			}

			return convertView;
		}

		@Override
		public long getItemId(final int position) {

			return position;
		}

		@Override
		public WebCallItemHolder getItem(final int position) {
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

	public class ContactListAdapter extends ArrayAdapter<ContactHolder> {

		private ContactHolder[] items;

		private boolean isUsingFilter;

		Bitmap defaultContactImage;

		public ContactListAdapter(final Context context, final ArrayList<ContactHolder> contacts, final int resourceID) {
			super(context, resourceID, contacts);
			items = new ContactHolder[contacts.size()];
			contacts.toArray(items);

			defaultContactImage = Tools.getDefaultContactPhoto(myActivity);
		}

		public void refill(final ArrayList<ContactHolder> contacts) {
			items = new ContactHolder[contacts.size()];
			contacts.toArray(items);

			notifyDataSetChanged();
		}

		public void move(final int oldPosition, final int newPosition) {
			ContactHolder temp = items[oldPosition];

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

		public boolean isUsingFilter() {
			return isUsingFilter;
		}

		@Override
		public Filter getFilter() {
			return new Filter() {

				@Override
				protected void publishResults(final CharSequence constraint, final FilterResults results) {
					if (results != null && results.count >= 0) {
						@SuppressWarnings("unchecked")
						ArrayList<ContactHolder> foundItems = (ArrayList<ContactHolder>) results.values;

						items = new ContactHolder[foundItems.size()];
						foundItems.toArray(items);
						isUsingFilter = true;

					} else {
						ArrayList<ContactHolder> tempList = isShowingContactType == Constants.CONTACT_TYPE_LOCAL ? localContactList : contactList;

						items = new ContactHolder[tempList.size()];
						tempList.toArray(items);

						isUsingFilter = false;

					}

					notifyDataSetInvalidated();
				}

				@Override
				protected FilterResults performFiltering(final CharSequence constraint) {
					FilterResults result = new FilterResults();
					if (constraint.length() > 0) {

						ArrayList<ContactHolder> foundItems = new ArrayList<ContactHolder>();
						for (ContactHolder holder : isShowingContactType == Constants.CONTACT_TYPE_LOCAL ? localContactList : contactList) {
							// If the contact's name contains the filter string, add it to list
							if (holder.getName().contains(constraint)) {
								foundItems.add(holder);
							}
						}

						// search results found return count
						result.count = foundItems.size();
						result.values = foundItems;
					} else {
						result.count = -1;// no search results found
					}

					return result;
				}
			};
		}

		@Override
		public View getView(final int position, View convertView, final ViewGroup parent) {
			ContactHolder tempHolder = items[position];

			if (convertView == null) {
				convertView = LayoutInflater.from(myActivity).inflate(R.layout.list_item_contact, null);
			}

			TextView txt = (TextView) convertView.findViewById(R.id.contact_name);
			String name = tempHolder.getName();
			txt.setText(name);

			txt.setTypeface(myActivity.getTypefaceRobotoLight());

			txt = (TextView) convertView.findViewById(R.id.contact_extension);
			txt.setText(tempHolder.getExtension());
			txt.setTypeface(myActivity.getTypefaceRobotoLight());

			txt = (TextView) convertView.findViewById(R.id.contact_status_line);
			String statusLine = tempHolder.getStatusLine();

			if (tempHolder.getAvailability() != 0 && statusLine != null && !statusLine.equals("")) {
				txt.setText(tempHolder.getStatusLine());
				txt.setVisibility(View.VISIBLE);
			} else {
				txt.setVisibility(View.GONE);
			}

			txt.setTypeface(myActivity.getTypefaceRobotoLight());

			ImageView view = (ImageView) convertView.findViewById(R.id.contact_availability);
			// view.setVisibility(isShowingContactType == Constants.CONTACT_TYPE_LOCAL ? View.GONE : View.VISIBLE);
			if (isShowingContactType == Constants.CONTACT_TYPE_EXTENSION) {

				view.setImageBitmap(defaultContactImage);

				// Set the tint according to contact status
				switch (tempHolder.getAvailability()) {
				case Constants.AVAILABILITY_AVAILABLE:
					view.setColorFilter(Constants.COLOR_TINT_AVAILABLE);
					break;
				case Constants.AVAILABILITY_BUSY:
					view.setColorFilter(Constants.COLOR_TINT_BUSY);
					break;
				case Constants.AVAILABILITY_IN_CALL:
					view.setColorFilter(Constants.COLOR_TINT_INCALL);
					break;
				default:
					view.setColorFilter(Constants.COLOR_TINT_TRANSPARENT);
					break;
				}

			} else {

				if (contactImageBitmaps != null && contactImageBitmaps.size() > position) {
					// This is a local contact, show the image corresponding to the user
					Bitmap bitmap = contactImageBitmaps.get(position);
					if (bitmap != null) {
						view.setImageBitmap(contactImageBitmaps.get(position));
					} else {
						view.setImageBitmap(defaultContactImage);
					}
				} else {
					view.setImageBitmap(defaultContactImage);
				}

				view.setColorFilter(Constants.COLOR_TINT_TRANSPARENT);
			}

			// Resets the toolbar to be closed
			View toolbar = convertView.findViewById(R.id.toolbar);
			((LinearLayout.LayoutParams) toolbar.getLayoutParams()).bottomMargin = -40;
			toolbar.setVisibility(View.GONE);

			return convertView;
		}

		@Override
		public long getItemId(final int position) {

			return position;
		}

		@Override
		public ContactHolder getItem(final int position) {
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

	@Override
	public void finished(final ArrayList<Bitmap> result) {
		if (result != null) {
			contactImageBitmaps = result;
		}

	}

	@Override
	public boolean onItemLongClick(final AdapterView<?> parent, final View view, final int id, final long position) {
		if (isShowingContactType == Constants.CONTACT_TYPE_WEBCALL) {
			final WebCallItemHolder holder = webCallItems.get((int) position);

			if (holder instanceof WCContactHolder) {

				myActivity.getContactDialog((WCContactHolder) holder).show();
			} else if (holder instanceof FolderHolder) {
				// myActivity.getFolderDialog((FolderHolder) holder).show();

				// TODO Temporary, remove when folder editing is functional
				Builder deleteDialog = Tools.getSimpleDialog(myActivity, string.delete, string.delete_folder_confirm);

				deleteDialog.setPositiveButton(string.delete, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(final DialogInterface arg0, final int arg1) {
						// Try to add contact. Replace Swedish characters
						DeleteFolderTask deleteFolderTask = new DeleteFolderTask(user, myActivity, dbAdapter, (FolderHolder) holder);
						deleteFolderTask.execute();
					}
				});

				deleteDialog.show();
			}
		}
		return true;
	}
}
