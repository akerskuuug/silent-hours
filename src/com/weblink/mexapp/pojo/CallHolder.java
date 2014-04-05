package com.weblink.mexapp.pojo;

public class CallHolder {
	private ContactHolder contact;
	private String callId1, callId2;
	private String contactName;
	private String callStatus, callDirection;

	public CallHolder(final ContactHolder contact, final String contactName, final String callId1, final String callId2, final String callStatus, final String callDirection) {
		super();
		this.contact = contact;
		this.callId1 = callId1;
		this.callId2 = callId1;
		this.callStatus = callStatus;
		this.callDirection = callDirection;

		// Check if the contact name is numeric (the server may send the phone number as contact name)
		// This method might not be the best performance-wise, but since users will have <10 calls active it should be okay
		try {
			// Try parsing the contact name as a number. If it does not fail, the contact name should be fetched from the contact since phone numbers don't make good names
			Integer.parseInt(contactName);
			this.contactName = contact.getName();
		} catch (NumberFormatException e) {
			// Parsing failed, use contactName
			this.contactName = contactName;
		}

	}

	public ContactHolder getContact() {
		return contact;
	}

	public void setContact(final ContactHolder contact) {
		this.contact = contact;
	}

	public String getCallId1() {
		return callId1;
	}

	public void setCallId1(final String callId1) {
		this.callId1 = callId1;
	}

	public String getCallId2() {
		return callId2;
	}

	public void setCallId2(final String callId2) {
		this.callId2 = callId2;
	}

	public String getCallStatus() {
		return callStatus;
	}

	public void setCallStatus(final String callStatus) {
		this.callStatus = callStatus;
	}

	public String getCallDirection() {
		return callDirection;
	}

	public void setCallDirection(final String callDirection) {
		this.callDirection = callDirection;
	}

	@Override
	public String toString() {
		return "CallHolder [contact=" + contact.toString() + ", callId1=" + callId1 + ", callId2=" + callId2 + ", callStatus=" + callStatus + ", callDirection=" + callDirection + "]";
	}

	public String getContactName() {
		return contactName;
	}

}
