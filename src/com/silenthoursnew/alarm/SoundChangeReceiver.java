package com.silenthoursnew.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.AudioManager;

import com.silenthoursnew.utility.Constants;

public class SoundChangeReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(final Context context, final Intent intent) {

		SharedPreferences settings = context.getSharedPreferences(Constants.SHARED_PREFS_NAME, Context.MODE_PRIVATE);
		Editor editor = settings.edit();

		AudioManager audioMgr = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

		boolean enabled = settings.getBoolean(Constants.SCHEDULER_ENABLED, false);
		boolean muted = settings.getBoolean(Constants.SCHEDULER_SOUND_MUTED, false);
		boolean lock = settings.getBoolean(Constants.SCHEDULER_VOLUME_LOCK, false);

		if (enabled && muted && lock) {

			if (settings.getBoolean(Constants.SCHEDULER_SET_VIBRATION_ENABLED, false)) {
				// Mute sound (with vibration)
				audioMgr.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
			} else {
				// Mute sound (without vibration)
				audioMgr.setRingerMode(AudioManager.RINGER_MODE_SILENT);
			}

		} else if (enabled) {
			if (audioMgr.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE || audioMgr.getRingerMode() == AudioManager.RINGER_MODE_SILENT) {
				editor.putBoolean(Constants.SCHEDULER_SOUND_MUTED, true);
			}
			editor.commit();
		} else {
			if (audioMgr.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {
				editor.putBoolean(Constants.SCHEDULER_SOUND_MUTED, false);
			}
			editor.commit();

		}

	}
}
