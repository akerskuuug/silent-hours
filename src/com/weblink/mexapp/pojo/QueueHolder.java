package com.weblink.mexapp.pojo;

public class QueueHolder {

	private int id;
	private String name;
	private boolean loggedIn;

	public QueueHolder(final int id, final String name, final boolean loggedIn) {
		super();

		this.id = id;

		this.name = name;
		this.loggedIn = loggedIn;
	}

	public int getId() {
		return id;
	}

	public void setId(final int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public boolean isLoggedIn() {
		return loggedIn;
	}

	public void setLoggedIn(final boolean loggedIn) {
		this.loggedIn = loggedIn;
	}

}
