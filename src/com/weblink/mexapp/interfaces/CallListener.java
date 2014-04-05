package com.weblink.mexapp.interfaces;

import android.content.Context;

import com.weblink.mexapp.db.MexDbAdapter;

public interface CallListener {
	public void onReceiveEvent(boolean success);

	public MexDbAdapter getDbAdapter();

	public Context getContext();
}
