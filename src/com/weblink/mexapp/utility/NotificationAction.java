package com.weblink.mexapp.utility;

import android.app.PendingIntent;

public class NotificationAction {

	private int iconResId;
	private CharSequence message;
	private PendingIntent pi;

	public NotificationAction(final int iconResId, final CharSequence message, final PendingIntent pi) {
		super();
		this.iconResId = iconResId;
		this.message = message;
		this.pi = pi;
	}
 
	public int getIconResId() {
		return iconResId;
	}

	public void setIconResId(final int iconResId) {
		this.iconResId = iconResId;
	}

	public CharSequence getMessage() {
		return message;
	}

	public void setMessage(final CharSequence message) {
		this.message = message;
	}

	public PendingIntent getPi() {
		return pi;
	}

	public void setPi(final PendingIntent pi) {
		this.pi = pi;
	}

}
