package com.weblink.mexapp.utility;

import org.apache.http.NameValuePair;

public class IntegerNameValuePair implements NameValuePair {

	String name;
	String value;

	public IntegerNameValuePair(final String name, final int value) {
		this.name = name;
		this.value = value + "";
	}

	@Override
	public String getName() {
		return name;
	}

	/** Do not use this, use getIntValue() instead!
	 * 
	 */
	@Override
	public String getValue() {
		return value;
	}

	public int getIntValue() {
		return Integer.parseInt(value);
	}
}
