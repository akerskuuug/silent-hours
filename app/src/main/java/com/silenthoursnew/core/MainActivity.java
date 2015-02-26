package com.silenthoursnew.core;

import android.annotation.SuppressLint;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.widget.TextView;

import com.silenthoursnew.R;
import com.silenthoursnew.utility.Tools;

import java.util.Locale;

public class MainActivity extends FragmentActivity {

	/**
	 * The {@link android.support.v4.view.PagerAdapter} that will provide fragments for each of the sections. We use a
	 * {@link android.support.v4.app.FragmentPagerAdapter} derivative, which will keep every loaded fragment in memory. If this becomes too memory
	 * intensive, it may be best to switch to a {@link android.support.v4.app.FragmentStatePagerAdapter}.
	 */
	SectionsPagerAdapter mSectionsPagerAdapter;

	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	ViewPager mViewPager;

	@SuppressLint("NewApi")
    @Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		if (!Tools.isTablet(this)) {
			// Create the adapter that will return a fragment for each of the three
			// primary sections
			// of the app.
			mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

			// Set up the ViewPager with the sections adapter.
			mViewPager = (ViewPager) findViewById(R.id.pager);
			mViewPager.setAdapter(mSectionsPagerAdapter);

			mViewPager.setCurrentItem(1);
		}

        if(Tools.isLollipopOrLater()){
            getWindow().setStatusBarColor(getResources().getColor(R.color.color_main));
        }else if(Tools.isKitKatOrLater()){
            getWindow().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.color_main)));
        }
		TextView title = (TextView) findViewById(R.id.app_title);
		title.setTypeface(Tools.getTypefaceRobotoThin(this));

	}

	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to one of the primary sections of the app.
	 */
	public class SectionsPagerAdapter extends FragmentPagerAdapter {

		public SectionsPagerAdapter(final FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(final int i) {
			Fragment fragment;
			if (i == 0) {
				fragment = new QuickFragment();
			} else if (i == 1) {
				fragment = new MainFragment();
			} else {
				fragment = new HelpFragment();
			}
			return fragment;

		}

		@Override
		public int getCount() {
			return 3;
		}

		@Override
		public CharSequence getPageTitle(final int position) {
			switch (position) {
			case 0:
				return getString(R.string.title_section1).toUpperCase(new Locale("UTF-8"));
			case 1:
				return getString(R.string.title_section2).toUpperCase(new Locale("UTF-8"));
			case 2:
				return getString(R.string.title_section3).toUpperCase(new Locale("UTF-8"));
			}
			return null;
		}
	}

}
