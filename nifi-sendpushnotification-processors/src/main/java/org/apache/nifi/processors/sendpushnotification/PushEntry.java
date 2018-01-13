package org.apache.nifi.processors.sendpushnotification;

public class PushEntry {
	
	private String deviceIdentifier;
	private String content;
	private String status;
	private Integer payload_badge;
	private String payload_alert_body;
	private String payload_alert_title;
	private String payload_sound;
	private boolean payload_content_available;
	private String payload_category;
	private String payload_threadID;

	public String getDeviceIdentifier() {
		return deviceIdentifier;
	}
	public void setDeviceIdentifier(String deviceIdentifier) {
		this.deviceIdentifier = deviceIdentifier;
	}
	
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public Integer getPayload_badge() {
		return payload_badge;
	}
	public void setPayload_badge(Integer payload_badge) {
		this.payload_badge = payload_badge;
	}
	public String getPayload_sound() {
		return payload_sound;
	}
	public void setPayload_sound(String payload_sound) {
		this.payload_sound = payload_sound;
	}
	public boolean isPayload_content_available() {
		return payload_content_available;
	}
	public void setPayload_content_available(boolean payload_content_available) {
		this.payload_content_available = payload_content_available;
	}
	public String getPayload_category() {
		return payload_category;
	}
	public void setPayload_category(String payload_category) {
		this.payload_category = payload_category;
	}
	public String getPayload_threadID() {
		return payload_threadID;
	}
	public void setPayload_threadID(String payload_threadID) {
		this.payload_threadID = payload_threadID;
	}
	public String getPayload_alert_body() {
		return payload_alert_body;
	}
	public void setPayload_alert_body(String payload_alert_body) {
		this.payload_alert_body = payload_alert_body;
	}
	public String getPayload_alert_title() {
		return payload_alert_title;
	}
	public void setPayload_alert_title(String payload_alert_title) {
		this.payload_alert_title = payload_alert_title;
	}

}

