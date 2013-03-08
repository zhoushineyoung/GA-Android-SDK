package com.gameanalytics.android.example;

import javax.microedition.khronos.opengles.GL10;

import android.graphics.Rect;

import com.twicecircled.spritebatcher.SpriteBatcher;

public class GAIcon {

	private int randomSpeed = 8;
	private int minSpeed = 5;
	private int drawable = R.drawable.icon;
	private Rect sourceRect = new Rect(0, 0, 128, 128);
	private int x;
	private int y;
	private int speedX;
	private int speedY;
	private int directionX = 1;
	private int directionY = 1;

	public GAIcon() {
		// Start position
		x = 0;
		y = 0;

		// Randomise speed and direction
		speedX = (int) (Math.random() * randomSpeed + minSpeed);
		speedY = (int) (Math.random() * randomSpeed + minSpeed);
		if (Math.random() > 0.5)
			toggleX();
		if (Math.random() > 0.5)
			toggleY();
	}

	public void move(SpriteBatcher sb) {
		int minX = -sb.getViewWidth() / 2;
		int maxX = sb.getViewWidth() / 2 - 100;
		int minY = -sb.getViewHeight() / 2;
		int maxY = sb.getViewHeight() / 2 - 100;

		x += speedX * directionX;
		y += speedY * directionY;

		if (x < minX || x > maxX)
			toggleX();
		if (y < minY || y > maxY)
			toggleY();
	}

	public void draw(GL10 gl, SpriteBatcher sb) {
		Rect destinationRect = new Rect(sourceRect);
		destinationRect.offset(sb.getViewWidth() / 2 + x, sb.getViewHeight()
				/ 2 + y);
		sb.draw(gl, drawable, sourceRect, destinationRect);
	}

	private void toggleX() {
		directionX *= -1;
	}

	private void toggleY() {
		directionY *= -1;
	}
}
