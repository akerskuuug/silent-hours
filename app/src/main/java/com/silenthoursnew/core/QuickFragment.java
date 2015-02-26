package com.silenthoursnew.core;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.silenthoursnew.R;
import com.silenthoursnew.alarm.QuickSilenceReceiver;
import com.silenthoursnew.utility.Constants;
import com.silenthoursnew.utility.Tools;

import java.util.Calendar;

public class QuickFragment extends Fragment implements OnClickListener {

	private SharedPreferences settings;
	private AudioManager audioMgr;
	private AlarmManager am;
	private Editor editor;
	private SeekBar durationSlider;
	private Button muteButton;
	private CheckBox vibrationCheck;

	public QuickFragment() {
	}

	@Override
	public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_quick, container, false);

		audioMgr = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
		am = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);
		settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
		editor = settings.edit();

		Typeface typefaceRobotoLight = Tools.getTypefaceRobotoLight(getActivity());
		Typeface typefaceRobotoThin = Tools.getTypefaceRobotoThin(getActivity());

		durationSlider = (SeekBar) v.findViewById(R.id.duration_slider);
		muteButton = (Button) v.findViewById(R.id.quick_button);
		muteButton.setTypeface(typefaceRobotoLight);

		vibrationCheck = (CheckBox) v.findViewById(R.id.enable_vibr_check);
		vibrationCheck.setTypeface(typefaceRobotoLight);

		if (Tools.isTablet(getActivity())) {
			TextView title = (TextView) v.findViewById(android.R.id.title);
			title.setTypeface(typefaceRobotoLight);
		}

		final TextView durationTitle = (TextView) v.findViewById(R.id.duration_title);
		durationTitle.setTypeface(typefaceRobotoLight);

		final TextView durationText = (TextView) v.findViewById(R.id.duration_text);

		durationSlider.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) {
                if(progress != 0) {
                    durationText.setText(progress + " " + getString(R.string.minutes));
                }else{
                    durationText.setText(getActivity().getString(R.string.indefinitely));
                }
			}

			public void onStopTrackingTouch(final SeekBar seekBar) {
				editor.putInt(Constants.QUICK_DURATION, seekBar.getProgress());
				editor.commit();
			}

			public void onStartTrackingTouch(final SeekBar seekBar) {
				// Do nothing
			}
		});
		durationText.setTypeface(typefaceRobotoThin);

		muteButton.setOnClickListener(this);

		return v;

	}

	@Override
	public void onResume() {
		super.onResume();

		durationSlider.setProgress(settings.getInt(Constants.QUICK_DURATION, 30));
	}

	@SuppressLint("NewApi")
    public void onClick(final View v) {
		int viewId = v.getId();

		if (viewId == R.id.quick_button) {

			audioMgr.setRingerMode(vibrationCheck.isChecked() ? AudioManager.RINGER_MODE_VIBRATE : AudioManager.RINGER_MODE_SILENT);
            if(Tools.isLollipopOrLater() && !vibrationCheck.isChecked()){
                AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        try {
                            Thread.sleep(200);
                            audioMgr.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        return null;
                    }

                };

                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

            }

			Calendar calendar = Calendar.getInstance();
			calendar.add(Calendar.MINUTE, durationSlider.getProgress());

			Intent intent = new Intent(getActivity(), QuickSilenceReceiver.class);
			PendingIntent pendingIntent = PendingIntent.getBroadcast(getActivity(), (int) (Math.random() * 1000 + 1), intent, 0);

            if(durationSlider.getProgress() != 0) {
                am.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            }
		}
	}

}
