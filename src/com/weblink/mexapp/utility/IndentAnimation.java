package com.weblink.mexapp.utility;

import android.annotation.SuppressLint;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;

public class IndentAnimation extends Animation {
	private final View mAnimatedView;
	private final int mPaddingStart, mPaddingEnd;
	private boolean mWasEndedAlready = false;
	private final int bottomPadding, rightPadding, topPadding;

	/**
	 * Initialize the animation
	 * 
	 * @param view
	 *            The layout we want to animate
	 * @param duration
	 *            The duration of the animation, in ms
	 */
	@SuppressLint("NewApi")
	public IndentAnimation(final View view, final int duration, final int parentPadding) {

		setDuration(duration);
		mAnimatedView = view;

		mPaddingStart = parentPadding;
		bottomPadding = view.getPaddingBottom();
		topPadding = view.getPaddingTop();
		rightPadding = view.getPaddingRight();

		mPaddingEnd = mPaddingStart + 30;

		if (Tools.isHoneycombOrLater()) {
			view.setAlpha(0.0f);
		}

	}

	@SuppressLint("NewApi")
	@Override
	protected void applyTransformation(final float interpolatedTime, final Transformation t) {
		super.applyTransformation(interpolatedTime, t);

		if (interpolatedTime < 1.0f) {

			// Calculating the new left padding, and setting it
			mAnimatedView.setPadding(mPaddingStart + (int) ((mPaddingEnd - mPaddingStart) * interpolatedTime), topPadding, rightPadding, bottomPadding);

			// Invalidating the layout, making us seeing the changes we made
			mAnimatedView.requestLayout();

			mWasEndedAlready = true;

			if (Tools.isHoneycombOrLater()) {
				mAnimatedView.setAlpha(1.0f * interpolatedTime);
			}

			// Making sure we didn't run the ending before (it happens!)
		} else if (!mWasEndedAlready) {
			mAnimatedView.setPadding(mPaddingEnd, topPadding, rightPadding, bottomPadding);
			mAnimatedView.requestLayout();

			mWasEndedAlready = true;
		}
	}
}
