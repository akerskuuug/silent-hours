package com.weblink.mexapp.interfaces;

import android.content.Context;
import android.content.SharedPreferences.Editor;

public interface WCStatusListener {
	/**
	 * Called when a WebCall status is received
	 * @param possibleMessages 
	 * @param success
	 */
	public void onReceiveWCStatus(String[] possibleMessages, int[] possibleMessageIDs);

	public Editor getPrefEditor();

	public Context getContext();
}
