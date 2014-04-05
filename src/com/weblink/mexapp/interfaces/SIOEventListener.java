package com.weblink.mexapp.interfaces;

import android.app.Activity;

import com.weblink.mexapp.db.MexDbAdapter;
import com.weblink.mexapp.pojo.ContactHolder;

public interface SIOEventListener {
	public MexDbAdapter getDbAdapter();

	public Activity getMainActivity();

	public void onReceiveCall(boolean success);

	public void onReceiveStatus(ContactHolder holder);

}
