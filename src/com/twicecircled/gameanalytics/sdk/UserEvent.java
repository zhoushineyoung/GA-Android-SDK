package com.twicecircled.gameanalytics.sdk;

public class UserEvent extends Event {
	// GENERAL
	private String user_id;
	private String session_id;
	private String build;
	private String event_id;
	private String area;
	private float x;
	private float y;
	private float z;

	// USER
	private char gender;
	private int birth_year;
	private int friend_count;

	public UserEvent(String user_id, String session_id, String build,
			String event_id, String area, float x, float y, float z,
			char gender, int birth_year, int friend_count) {
		this.user_id = user_id;
		this.session_id = session_id;
		this.build = build;
		this.event_id = event_id;
		this.area = area;
		this.x = x;
		this.y = y;
		this.z = z;

		this.gender = gender;
		this.birth_year = birth_year;
		this.friend_count = friend_count;
	}
}
