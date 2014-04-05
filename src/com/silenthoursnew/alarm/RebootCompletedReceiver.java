package com.silenthoursnew.alarm;

import java.util.ArrayList;
import java.util.Calendar;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.silenthoursnew.core.Alarm;
import com.silenthoursnew.database.AlarmDbAdapter;
import com.silenthoursnew.utility.Constants;
import com.silenthoursnew.utility.Tools;

/**
 * This class is used to start up relevant services when phone is started.
 * 
 */
public class RebootCompletedReceiver extends BroadcastReceiver {

	AlarmManager am;
	SharedPreferences settings;

	@Override
	public void onReceive(final Context context, final Intent intent) {

	
		Tools.setAllAlarms(context);
		
	}
}
