package com.gameanalytics.android;

import java.util.ArrayList;
import java.util.List;

public class EventList<E> extends ArrayList<E> {

	/**
	 * GENERATED ID JUST IN CASE SERIELISATION IS NEEDED IN FUTURE
	 */
	private static final long serialVersionUID = 6853023502311564615L;

	// Event ID list maintained alongside the main list of events
	private ArrayList<Integer> eventIdList = new ArrayList<Integer>();

	public ArrayList<Integer> getEventIdList() {
		return eventIdList;
	}

	public boolean addEvent(E event, int id) {
		eventIdList.add(id);
		return super.add(event);
	}
}
