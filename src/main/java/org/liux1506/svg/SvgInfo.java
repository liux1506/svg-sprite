package org.liux1506.svg;

import java.io.File;

/**
 * description:
 *
 * @author liuxing
 * @date Created on 2022/7/21
 **/
public class SvgInfo {

	/**
	 * prevent css and selector clashed
	 */
	private String uuid;
	/**
	 * title in json to use in web
	 */
	private String title;
	/**
	 * icon width
	 */
	private int width;
	/**
	 * icon height
	 */
	private int height;
	/**
	 * locate x
	 */
	private int x;
	/**
	 * locate y
	 */
	private int y;

	/**
	 * source file
	 */
	private File source;

	public SvgInfo() {
	}
	public SvgInfo(String uuid, String name, int width, int height, File source) {
		this.uuid = uuid;
		this.title = name;
		this.width = width;
		this.height = height;
		this.source = source;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
	}

	public File getSource() {
		return source;
	}

	public void setSource(File source) {
		this.source = source;
	}
}
