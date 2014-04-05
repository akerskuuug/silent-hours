package com.weblink.mexapp.interfaces;

import android.content.Context;

public interface MexStatusListener {

	public void onReceiveMexStatus(boolean mexStatus);

	public Context getContext();

}
