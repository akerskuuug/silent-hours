package com.weblink.mexapp.pojo;

public class FolderHolder implements WebCallItemHolder {
	private int id;
	private String type;
	private int left, right, parent;
	private String title;

	private int level;
	private boolean isDisplayed;

	public FolderHolder(final String title, final String type, final int parent) {

		this(0, title, type, parent, 0, 0, 0);
	}

	public FolderHolder(final int id, final String title, final String type, final int parent, final int left, final int right, final int level) {

		this(id, title, type, parent, left, right, level, false);
	}

	public FolderHolder(final int id, final String title, final String type, final int parent, final int left, final int right, final int level, final boolean isDisplayed) {
		super();
		this.id = id;
		this.type = type;
		this.left = left;
		this.right = right;
		this.parent = parent;
		this.title = title;
		this.level = level;
		this.isDisplayed = isDisplayed;
	}

	public int getLeft() {
		return left;
	}

	public void setLeft(final int left) {
		this.left = left;
	}

	public int getRight() {
		return right;
	}

	public void setRight(final int right) {
		this.right = right;
	}

	public int getParent() {
		return parent;
	}

	public void setParent(final int parent) {
		this.parent = parent;
	}

	public int getId() {
		return id;
	}

	public void setId(final int id) {
		this.id = id;
	}

	public String getType() {
		return type;
	}

	public void setType(final String type) {
		this.type = type;
	}

	@Override
	public String getTitle() {
		return title;
	}

	public void setTitle(final String title) {
		this.title = title;
	}

	@Override
	public String getSubTitle() {

		return type;
	}

	@Override
	public int getItemType() {
		return WebCallItemHolder.ITEM_TYPE_FOLDER;

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

	public void setItemLevel(final int level) {
		this.level = level;
	}

	@Override
	public int getItemLevel() {
		return level;
	}

}
