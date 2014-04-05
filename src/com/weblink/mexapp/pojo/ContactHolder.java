package com.weblink.mexapp.pojo;

import java.util.ArrayList;

public class ContactHolder {
	private int availability = 0;
	private String name = "";
	private String extension = "";
	private String statusLine = "";
	private int pid = 0;

	private final ArrayList<String> numbers;

	public ContactHolder(final String name, final String number, final String extension, final int availability, final int pid, final String statusLine) {

		this.name = name;
		this.extension = extension;

		this.availability = availability;
		this.pid = pid;
		this.statusLine = statusLine;

		numbers = new ArrayList<String>();
		numbers.add(number);
	}

	public int getAvailability() {
		return availability;
	}

	public void setAvailability(final int availability) {
		this.availability = availability;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public String getNumber() {
		return numbers.get(0);
	}

	public ArrayList<String> getAllNumbers() {
		return numbers;
	}

	public void addNumber(final String number) {
		numbers.add(number);
	}

	public void setFirstNumber(final String number) {
		numbers.set(0, number);
	}

	public String getExtension() {
		return extension;
	}

	public void setExtension(final String extension) {
		this.extension = extension;
	}

	public int getPeerId() {
		return pid;
	}

	public void setPid(final int pid) {
		this.pid = pid;
	}

	public String getStatusLine() {
		return statusLine;
	}

	public void setStatusLine(final String statusLine) {
		this.statusLine = statusLine;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();

		builder.append(getName());
		builder.append(",");

		builder.append(getNumber());
		builder.append(",");

		builder.append(getExtension());
		builder.append(",");

		builder.append(getAvailability());
		builder.append(",");

		builder.append(getPeerId());

		return builder.toString();

	}

}