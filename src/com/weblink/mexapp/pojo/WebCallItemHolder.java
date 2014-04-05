package com.weblink.mexapp.pojo;

/**
 * Interface used to enable displaying both contacts and folders in WebCall contact list
 *
 */
public interface WebCallItemHolder {

	public static int ITEM_TYPE_FOLDER = 0;
	public static int ITEM_TYPE_CONTACT = 1;

	/**
	 * 
	 * @return the title to show in list
	 */
	public String getTitle();

	/**
	 * 
	 * @return the subtitle to show in list
	 */
	public String getSubTitle();

	/**
	 * 
	 * @return the item type (folder, contact), used to determine what to do with a click
	 */
	public int getItemType();

	/**
	 * 
	 * @return the ID of the item
	 */
	public int getItemId();

	/**
	 * 
	 * @return whether the item is already visible
	 */
	public boolean isVisibleAlready();

	public void setIsVisible(boolean makeVisible);

	public int getItemLevel();

}
