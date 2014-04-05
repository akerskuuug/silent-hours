package com.weblink.mexapp.fragment;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.weblink.mexapp.R;
import com.weblink.mexapp.activity.MainActivity;
import com.weblink.mexapp.activity.SettingsActivity;
import com.weblink.mexapp.utility.Constants;

public class MiscFragment extends ListFragment implements OnItemClickListener {
	private MainActivity myActivity;
	private final ArrayList<MiscItemHolder> miscList;

	private LinearLayout headerContainer;
	private TextView tv;

	public MiscFragment() {
		miscList = new ArrayList<MiscItemHolder>();
	}

	@Override
	public void onAttach(final Activity myActivity) {
		super.onAttach(myActivity);
		this.myActivity = (MainActivity) myActivity;

	}

	@Override
	public void onActivityCreated(final Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		Bundle args = getArguments();

		TypedArray iconIdArray = getResources().obtainTypedArray(args.getInt(Constants.MISC_ITEM_ICONS));
		String[] textArray = getResources().getStringArray(args.getInt(Constants.MISC_ITEM_STRINGS));

		ListView list = getListView();
		list.setOnItemClickListener(this);
		list.setBackgroundResource(R.drawable.abs__ab_solid_shadow_holo);

		if (miscList.isEmpty()) {
			for (int i = 0, length = textArray.length; i < length; i++) {
				miscList.add(new MiscItemHolder(iconIdArray.getResourceId(i, 0), textArray[i]));
			}
		}

		iconIdArray.recycle();
	}

	@Override
	public void onResume() {

		super.onResume();

		MiscListAdapter adapter = new MiscListAdapter(miscList);

		if (headerContainer == null) {
			headerContainer = (LinearLayout) getLayoutInflater(getArguments()).inflate(R.layout.list_header, null).findViewById(R.id.header_container);
			tv = (TextView) headerContainer.findViewById(R.id.header_text);
			tv.setText(R.string.no_internet_long);
			tv.setTypeface(myActivity.getTypefaceRobotoLight());
		}

		if (myActivity.isNetworkAvailable()) {

			tv.setVisibility(View.GONE);
		} else {
			ListView lv = getListView();

			lv.setAdapter(null);
			tv.setVisibility(View.VISIBLE);
			if (lv.getHeaderViewsCount() == 0) {
				lv.addHeaderView(headerContainer);
			}

		}

		setListAdapter(adapter);

	}

	@Override
	public void onDestroy() {
		super.onDestroy();

	}

	@Override
	public void onItemClick(final AdapterView<?> parent, final View view, final int id, final long position) {

		switch ((int) position) {
		case Constants.MISC_ITEM_SETTINGS:
			Intent intent = new Intent(myActivity, SettingsActivity.class);
			myActivity.startActivity(intent);
			break;
		case Constants.MISC_ITEM_ABOUT:
			myActivity.showAboutDialog();
			break;
		case Constants.MISC_ITEM_HELP:
			myActivity.showHelpDialog();

			break;
		case Constants.MISC_ITEM_FOLLOWME:

			myActivity.showFollowMeDialog();
			break;
		case Constants.MISC_ITEM_USER:

			myActivity.showUserDialog();

			break;
		case Constants.MISC_ITEM_FORWARDING:

			myActivity.showForwardingDialog();

			break;
		case Constants.MISC_ITEM_CALLS:

			myActivity.showCallSettingDialog();

			break;
		case Constants.MISC_ITEM_LOGOFF:

			myActivity.showLogoffDialog();
			break;
		}

	}

	public class MiscListAdapter extends BaseAdapter {

		MiscItemHolder[] items;

		public MiscListAdapter(final ArrayList<MiscItemHolder> items) {
			this.items = new MiscItemHolder[items.size()];
			items.toArray(this.items);

		}

		@Override
		public View getView(final int position, View convertView, final ViewGroup parent) {
			MiscItemHolder tempHolder = items[position];

			if (convertView == null) {
				convertView = LayoutInflater.from(getActivity()).inflate(R.layout.list_item_misc, null);
			}

			TextView txt = (TextView) convertView.findViewById(R.id.misc_item_text);
			txt.setText(tempHolder.getText());
			txt.setTypeface(myActivity.getTypefaceRobotoLight());

			if (!myActivity.isNetworkAvailable() && isNetworkDependent(position)) {
				if (myActivity.getAppTheme() == Constants.THEME_LIGHT) {
					txt.setTextColor(getResources().getColor(android.R.color.secondary_text_dark));
				} else {
					txt.setTextColor(getResources().getColor(android.R.color.secondary_text_light));
				}
			}

			ImageView view = (ImageView) convertView.findViewById(R.id.misc_item_icon);
			view.setImageResource(tempHolder.drawableResId);

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
		public boolean isEnabled(final int position) {
			if (!myActivity.isNetworkAvailable() && isNetworkDependent(position)) {
				return false;
			}
			return true;
		}

		public boolean isNetworkDependent(final int position) {
			return position == Constants.MISC_ITEM_USER || position == Constants.MISC_ITEM_FOLLOWME || position == Constants.MISC_ITEM_FORWARDING || position == Constants.MISC_ITEM_CALLS;
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
