package com.gameanalytics.android.example;

import java.util.concurrent.CopyOnWriteArrayList;

import javax.microedition.khronos.opengles.GL10;

import com.gameanalytics.android.GameAnalytics;
import com.twicecircled.spritebatcher.Drawer;
import com.twicecircled.spritebatcher.SpriteBatcher;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

/*	NOTE ON GRAPHICS:
 *  The OpenGL graphics are implemented using SpriteBatcher, a library to allow easy
 *  drawing of 2D sprites with OpenGL in Android.
 * 
 *  This project uses android-sprite-batcher-v1.1.jar which can be downloaded from
 *  https://github.com/twicecircled/Android-Sprite-Batcher
 */

/*	EVENTS:
 *  This activity sends two types of design events:
 *  1. It sends an event each time the user presses the button using newDesignEvent(..)
 *  2. It logs the average FPS (OpenGL not UI) across the activity's lifecycle using logFPS()
 *  and stopLoggingFPS().
 */

public class DesignEventsActivity extends GameAnalyticsActivity implements
		Drawer {

	private GLSurfaceView surface;
	private CopyOnWriteArrayList<GAIcon> icons = new CopyOnWriteArrayList<GAIcon>();

	// For onscreen FPS counter
	private long lastTime;
	private long timer;
	private int frameCounter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Set display
		setContentView(R.layout.activity_design);

		// Set-up SpriteBatcher which will handle all our 2d sprite drawing
		int[] bitmapIds = new int[] { R.drawable.icon };
		surface = (GLSurfaceView) findViewById(R.id.glSurfaceView);
		surface.setRenderer(new SpriteBatcher(getResources(), bitmapIds, this));
	}

	public void onButton(View v) {
		// Add icon to centre of screen with random direction
		icons.add(new GAIcon());

		// Log event with the number of icons created. This way we can see how
		// many of our users keep created icons until the engine chugs at 2fps!
		GameAnalytics.newDesignEvent("IconCreated", (float) icons.size());
	}

	// Gets called every time a frame is to be drawn on GLSurfaceView:
	@Override
	public void onDrawFrame(GL10 gl, SpriteBatcher sb) {
		// Turn on automatic logging of unhandled exceptions for this thread
		GameAnalytics.logUnhandledExceptions();

		// Log Frames per Second (FPS)
		GameAnalytics.logFPS();

		// For screen indicator
		long nowTime = System.currentTimeMillis();
		if (lastTime == 0) {
			lastTime = nowTime;
		} else {
			timer += nowTime - lastTime;
			lastTime = nowTime;
			frameCounter++;
			// Every second
			if (timer > 1000) {
				// Update on screen indicator
				runOnUiThread(new Runnable() {
					public void run() {
						((TextView) findViewById(R.id.fpsCounter))
								.setText("FPS: " + frameCounter);
						timer = 0;
						frameCounter = 0;
					}
				});
			}
		}

		// Update our entities' positions
		updateEntities(sb);
		/**
		 * NB IN A REAL GAME DO NOT DO THIS IN THE DRAW LOOP!! Update entity
		 * positions in a seperate thread called the update loop
		 */

		// Draw the entities
		drawEntities(gl, sb);

	}

	private void updateEntities(SpriteBatcher sb) {
		for (GAIcon i : icons) {
			i.move(sb);
		}
	}

	private void drawEntities(GL10 gl, SpriteBatcher sb) {
		for (GAIcon i : icons) {
			i.draw(gl, sb);
		}
	}

	@Override
	public void onResume() {
		// Pause our surface view so it stops drawing
		if (surface != null) {
			surface.onResume();
		}
		super.onResume();
	}

	@Override
	public void onPause() {
		// Pause our surface view so it stops drawing
		surface.onPause();

		// Stop logging fps, collate information and send to server:
		GameAnalytics.stopLoggingFPS();

		// No need to call GameAnalytics.stopSession() as the superclass
		// GameAnalyticsActivity already does that for us.
		super.onPause();

		/**
		 * Be careful to log any events in the onPause() method BEFORE calling
		 * stopSession() (either via a super class or explicitly. Otherwise the
		 * events won't be logged. Look out for warnings in the log file.
		 */
	}

}
