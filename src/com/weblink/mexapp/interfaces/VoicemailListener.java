package com.weblink.mexapp.interfaces;

import android.content.Context;

public interface VoicemailListener {

	public void onReceiveVoicemail(boolean hasVoicemail);

	public Context getContext();
}
