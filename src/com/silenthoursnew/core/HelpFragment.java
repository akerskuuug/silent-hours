package com.silenthoursnew.core;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.silenthoursnew.R;
import com.silenthoursnew.utility.Tools;

public class HelpFragment extends Fragment {

	public HelpFragment() {
	}

	@Override
	public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_help, container, false);
 
		Typeface typefaceRobotoLight = Tools.getTypefaceRobotoLight(getActivity());

		TextView tip = (TextView) v.findViewById(R.id.reject_tooltip);
		tip.setTypeface(typefaceRobotoLight);

		if (Tools.isTablet(getActivity())) {
			TextView title = (TextView) v.findViewById(android.R.id.title);
			title.setTypeface(typefaceRobotoLight);
		}
		return v;

	}
}
