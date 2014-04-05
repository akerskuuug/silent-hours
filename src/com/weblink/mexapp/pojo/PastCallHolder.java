package com.weblink.mexapp.pojo;


public class PastCallHolder {
	private String id, remoteNumber, callDisposition, contactName, callDate;
	private int contactID, callDirection;

	public static final int CALL_DIRECTION_IN = 0;
	public static final int CALL_DIRECTION_OUT = 1;

	public PastCallHolder(final String id, final String remoteNumber, final String callDisposition, final int callDirection, final String contactName, final int contactID, final String callDate) {
		super();
		this.id = id;
		this.remoteNumber = remoteNumber;
		this.callDisposition = callDisposition;
		this.callDirection = callDirection;
		this.contactName = contactName;
		this.contactID = contactID;
		this.callDate = callDate;
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public String getRemoteNumber() {
		return remoteNumber;
	}

	public void setRemoteNumber(final String remoteNumber) {
		this.remoteNumber = remoteNumber;
	}

	public String getCallDisposition() {
		return callDisposition;
	}

	public void setCallDisposition(final String callDisposition) {
		this.callDisposition = callDisposition;
	}

	public int getCallDirection() {
		return callDirection;
	}

	public void setCallDirection(final int callDirection) {
		this.callDirection = callDirection;
	}

	public String getContactName() {
		return contactName;
	}

	public void setContactName(final String contactName) {
		this.contactName = contactName;
	}

	public int getContactID() {
		return contactID;
	}

	public void setContactID(final int contactID) {
		this.contactID = contactID;
	}

	public String getCallDate() {
		return callDate;
	}

	public void setCallDate(final String callDate) {
		this.callDate = callDate;
	}

}
