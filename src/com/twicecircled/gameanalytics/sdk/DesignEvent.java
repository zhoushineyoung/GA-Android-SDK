package com.twicecircled.gameanalytics.sdk;

public class DesignEvent extends Event {
	// GENERAL
	private String user_id;
	private String session_id;
	private String build;
	private String event_id;
	private String area;
	private float x;
	private float y;
	private float z;

	// DESIGN
	private float value;

	public DesignEvent(String user_id, String session_id, String build,
			String event_id, String area, float x, float y, float z, float value) {
		this.user_id = user_id;
		this.session_id = session_id;
		this.build = build;
		this.event_id = event_id;
		this.area = area;
		this.x = x;
		this.y = y;
		this.z = z;
		this.value = value;
	}
}
