package com.weblink.mexapp.utility;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ListView;

import com.weblink.mexapp.R;

class MultiselectionDialogPreference extends DialogPreference {
	ListView listView;
	boolean[] checkItems;
	String[] itemNames;

	public MultiselectionDialogPreference(final Context context, final AttributeSet attrs) {
		super(context, attrs);

		setDialogLayoutResource(R.layout.multiselect_dialog_preference);

		itemNames = getContext().getResources().getStringArray(attrs.getAttributeResourceValue("http://schemas.android.com/apk/res/android", "entries", R.array.misc_item_texts));

	}

	@Override
	protected void onBindDialogView(final View view) {
		super.onBindDialogView(view);

		// Initialize ListView, allow multiple choice and set list adapter to fill list
		listView = (ListView) view.findViewById(R.id.multiselect_dialog_list);
		listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_multiple_choice, itemNames);
		checkItems = new boolean[itemNames.length];

		listView.setAdapter(adapter);
		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(final AdapterView<?> parent, final View view, final int id, final long pos) {

				checkItems[(int) pos] = ((CheckedTextView) view).isChecked();
			}

		});

		String rawString = getPersistedString("");

		if (rawString.length() > 0) {
			String[] checkedItemIds = rawString.split(",");

			for (String itemId : checkedItemIds) {
				try {
					int id = Integer.parseInt(itemId);

					listView.setItemChecked(id, true);
					checkItems[id] = true;
				} catch (NumberFormatException e) {
					Log.e("An error occured", e.getLocalizedMessage());
				}
			}
		}
	}

	@Override
	protected void onDialogClosed(final boolean positiveResult) {
		super.onDialogClosed(positiveResult);

		if (positiveResult) {

			StringBuilder builder = new StringBuilder();

			for (int i = 0; i < checkItems.length; i++) {
				if (checkItems[i]) {
					builder.append(i);
					builder.append(",");
				}
			}

			String resultString = builder.toString();

			// Remove last comma
			if (resultString.length() > 1) {
				persistString(resultString.substring(0, resultString.lastIndexOf(',')));
			} else {
				persistString("");
			}

		}
	}
}