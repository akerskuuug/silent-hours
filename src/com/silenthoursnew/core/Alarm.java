package com.silenthoursnew.core;

public class Alarm {

	public int from, to;

	public int brightness;

	public boolean enableVibration, muteMedia, lockVolume, unmuteOnCall, disableNotiLight;

	public boolean monday, tuesday, wednesday, thursday, friday, saturday, sunday;

	/**
	 * 
	 * @param from
	 * @param to
	 * @param enableVibration
	 * @param muteMedia 
	 * @param lockVolume
	 * @param unmuteOnCall
	 * @param weekdays
	 *            days of the week beginning with Sunday
	 */
	public Alarm(final int from, final int to, final boolean enableVibration, final boolean muteMedia, final boolean lockVolume, final boolean unmuteOnCall, final boolean disableNotificationLight,
			final int brightness, final boolean[] weekdays) {

		this.from = from;
		this.to = to;

		this.brightness = brightness;

		this.enableVibration = enableVibration;
		this.muteMedia = muteMedia;
		this.lockVolume = lockVolume;
		this.unmuteOnCall = unmuteOnCall;
		disableNotiLight = disableNotificationLight;

		sunday = weekdays[0];
		monday = weekdays[1];
		tuesday = weekdays[2];
		wednesday = weekdays[3];
		thursday = weekdays[4];
		friday = weekdays[5];
		saturday = weekdays[6];

	}
}
