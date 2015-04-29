package com.emc.mongoose.persist;
//
import org.apache.logging.log4j.Marker;

import java.util.Date;

/**
 * Created by olga on 28.04.15.
 */
public final class PersistEvent {

	private Date eventTstamp;
	private String eventLevel;
	private String eventThread;
	private String eventClass;
	private String eventMessage;
	private final Marker marker;
	private final String runName;
	//
	public PersistEvent(final Marker marker, final String runName){
		this.marker = marker;
		this.runName = runName;
	}
	//
	public final Date getEventTstamp() {
		return eventTstamp;
	}
	public final void setEventTstamp(final Date eventTstamp) {
		this.eventTstamp = eventTstamp;
	}
	public final String getEventLevel() {
		return eventLevel;
	}
	public final void setEventLevel(final String eventLevel) {
		this.eventLevel = eventLevel;
	}
	public final String getEventThread() {
		return eventThread;
	}
	public final void setEventThread(final String eventThread) {
		this.eventThread = eventThread;
	}
	public final String getEventClass() {
		return eventClass;
	}
	public final void setEventClass(final String eventClass) {
		this.eventClass = eventClass;
	}
	public final String getEventMessage() {
		return eventMessage;
	}
	public final void setEventMessage(final String eventMessage) {
		this.eventMessage = eventMessage;
	}
	public final String getRunName() {
		return runName;
	}
	public Marker getMarker() {
		return marker;
	}
}
