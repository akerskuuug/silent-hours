package com.weblink.mexapp.pojo;

public class WCContactHolder implements WebCallItemHolder {
	private String firstName, lastName, homeNumber, workNumber, cellNumber, type, emailAddress;
	private int id, folderID;
	private final int primaryNumber;
	private final int level;
	private boolean isDisplayed;

	public static final int PRIMARY_NUMBER_WORK = 0, PRIMARY_NUMBER_HOME = 2, PRIMARY_NUMBER_CELL = 1;

	public WCContactHolder(final int id, final String firstName, final String lastName, final String homeNumber, final String workNumber, final String cellNumber, final String type,
			final String emailAddress, final int folderID, final int primaryNumber) {
		this(id, firstName, lastName, homeNumber, workNumber, cellNumber, type, emailAddress, folderID, primaryNumber, 0, false);

	}

	public WCContactHolder(final int id, final String firstName, final String lastName, final String homeNumber, final String workNumber, final String cellNumber, final String type,
			final String emailAddress, final int folderID, final int primaryNumber, final int level) {
		this(id, firstName, lastName, homeNumber, workNumber, cellNumber, type, emailAddress, folderID, primaryNumber, level, false);

	}

	public WCContactHolder(final int id, final String firstName, final String lastName, final String homeNumber, final String workNumber, final String cellNumber, final String type,
			final String emailAddress, final int folderID, final int primaryNumber, final int level, final boolean isDisplayed) {
		super();
		this.id = id;
		this.firstName = firstName;
		this.lastName = lastName;
		this.homeNumber = homeNumber;
		this.workNumber = workNumber;
		this.cellNumber = cellNumber;
		this.primaryNumber = primaryNumber;
		this.emailAddress = emailAddress;

		this.folderID = folderID;
		this.type = type;
		this.level = level;

		this.isDisplayed = isDisplayed;
	}

	public boolean isDisplayed() {
		return isDisplayed;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(final String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	/**
	 * 
	 * @return the contacts's full name [firstname lastname]
	 */
	public String getFullName() {
		return firstName + " " + lastName;
	}

	public String getDefaultNumber() {
		String number = homeNumber;

		switch (primaryNumber) {
		case PRIMARY_NUMBER_CELL:
			number = cellNumber;
			break;
		case PRIMARY_NUMBER_WORK:
			number = workNumber;
			break;
		case PRIMARY_NUMBER_HOME:
			number = homeNumber;
			break;
		}

		// If primary number is empty, return first non-empty number
		if (number.length() == 0) {
			if (cellNumber.length() > 0) {
				return cellNumber;
			} else if (workNumber.length() > 0) {
				return workNumber;
			} else if (homeNumber.length() > 0) {
				return homeNumber;
			}

		}

		return number;
	}

	public void setLastName(final String lastName) {
		this.lastName = lastName;
	}

	public String getHomeNumber() {
		return homeNumber;
	}

	public void setHomeNumber(final String homeNumber) {
		this.homeNumber = homeNumber;
	}

	public String getWorkNumber() {
		return workNumber;
	}

	public void setWorkNumber(final String workNumber) {
		this.workNumber = workNumber;
	}

	public String getCellNumber() {
		return cellNumber;
	}

	public void setCellNumber(final String cellNumber) {
		this.cellNumber = cellNumber;
	}

	public String getType() {
		return type;
	}

	public void setType(final String type) {
		this.type = type;
	}

	public int getFolderID() {
		return folderID;
	}

	public void setFolderID(final int folderID) {
		this.folderID = folderID;
	}

	public int getPrimaryNumber() {
		return primaryNumber;
	}

	public int getId() {
		return id;
	}

	public void setId(final int id) {
		this.id = id;
	}

	@Override
	public String getTitle() {
		return getFullName();
	}

	@Override
	public String getSubTitle() {
		return getDefaultNumber();
	}

	@Override
	public int getItemType() {
		return WebCallItemHolder.ITEM_TYPE_CONTACT;
	}

	@Override
	public int getItemId() {
		return id;
	}

	@Override
	public boolean isVisibleAlready() {
		return isDisplayed;
	}

	@Override
	public void setIsVisible(final boolean makeVisible) {
		isDisplayed = makeVisible;

	}

	@Override
	public int getItemLevel() {
		return level;
	}

	public String getEmailAddress() {
		return emailAddress;
	}

	public void setEmailAdress(final String emailAdress) {
		emailAddress = emailAdress;
	}

}
