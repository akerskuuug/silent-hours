package com.weblink.mexapp.pojo;

public class User {

	private String company, password;
	private int extension;

	public User(final String company, final String password, final int extension) {
		super();
		this.company = company;
		this.password = password;
		this.extension = extension;
	}

	public String getCompany() {
		return company;
	}

	public void setCompany(final String company) {
		this.company = company;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(final String password) {
		this.password = password;
	}

	public int getExtension() {
		return extension;
	}

	public void setExtension(final int extension) {
		this.extension = extension;
	}

}
