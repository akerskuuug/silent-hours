package com.silenthoursnew.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;

public class QuickSilenceReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(final Context context, final Intent intent) {

		// Get the sound manager
		AudioManager audioMgr = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

		audioMgr.setRingerMode(AudioManager.RINGER_MODE_NORMAL);

	}
}
