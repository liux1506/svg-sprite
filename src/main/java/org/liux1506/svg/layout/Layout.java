package org.liux1506.svg.layout;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.ToIntFunction;
import org.liux1506.svg.SvgInfo;

/**
 * description: layout svg icons by width or height
 *
 * @author liuxing
 * @date Created on 2022/7/21
 **/
public class Layout {

	/**
	 * calc layout
	 *
	 * @param svgs
	 * @return int[]{width, height}
	 */
	public static <T extends SvgInfo> int[] fit(List<T> svgs) {
	    // return {0,0} if svgs is empty
		if (svgs.isEmpty()) {
			return new int[2];
		}
		// sort by width or height asc
		ToIntFunction<T> keyExtractor = item -> Math.max(item.getWidth(), item.getHeight());
		svgs.sort(Comparator.comparingInt(keyExtractor).reversed());

		Root root = new Root(0, 0, svgs.get(0).getWidth(), svgs.get(0).getHeight());

		// the sum width and height
		int width = 0, height = 0;
		for (int i = 0; i < svgs.size(); i++) {
			T svg = svgs.get(i);

			Root node = findNode(root, svg.getWidth(), svg.getHeight());

			Root fit = node != null ? splitNode(node, svg.getWidth(), svg.getHeight())
				: growNode(root, svg.getWidth(), svg.getHeight());

			svg.setX(fit.getX());
			svg.setY(fit.getY());
			width = Math.max(width, svg.getX() + svg.getWidth());
			height = Math.max(height, svg.getY() + svg.getHeight());
		}
		return new int[]{width, height};
	}

	private static Root growNode(Root root, int width, int height) {
		boolean canGrowBottom = width <= root.getWidth(),
			canGrowRight = height <= root.getHeight(),
			shouldGrowRight = canGrowRight && (root.getHeight() >= (root.getWidth() + width)),
			shouldGrowBottom = canGrowBottom && (root.getWidth() >= (root.getHeight()) + height);

		if (shouldGrowRight) {
			return growRight(root, width, height);
		}
		if (shouldGrowBottom) {
			return growBottom(root, width, height);
		}
		if (canGrowRight) {
			return growRight(root, width, height);
		}
		if (canGrowBottom) {
			return growBottom(root, width, height);
		}
		return null;
	}

	private static Root growBottom(Root root, int width, int height) {
		Root oldRoot = new Root(root);
		root.setUsed(true).setX(0).setY(0).setWidth(oldRoot.getWidth())
			.setHeight(oldRoot.getHeight() + height).setRight(oldRoot)
			.setDown(new Root(0, oldRoot.getHeight(), oldRoot.getWidth(), height));

		Root node = findNode(root, width, height);
		if (null != node) {
			return splitNode(node, width, height);
		}
		return null;
	}

	private static Root growRight(Root root, int width, int height) {
		Root oldRoot = new Root(root);
		root.setUsed(true).setX(0).setY(0).setWidth(oldRoot.getWidth() + width)
			.setHeight(oldRoot.getHeight()).setDown(oldRoot)
			.setRight(new Root(oldRoot.getWidth(), 0, width, oldRoot.getHeight()));

		Root node = findNode(root, width, height);
		if (null != node) {
			return splitNode(node, width, height);
		}
		return null;
	}

	private static Root splitNode(Root node, int width, int height) {
		node.setUsed(true);
		node.setDown(new Root(node.getX(), node.getY() + height, node.getWidth(),
			node.getHeight() - height));
		node.setRight(new Root(node.getX() + width, node.getY(), node.getWidth() - width, height));
		return node;
	}

	private static Root findNode(Root root, int width, int height) {
		if (root.isUsed()) {
			return Optional.ofNullable(findNode(root.getRight(), width, height))
				.orElse(findNode(root.getDown(), width, height));
		}
		if (width <= root.getWidth() && height <= root.getHeight()) {
			return root;
		}
		return null;
	}

}

class Root {

	private boolean used;
	private int x;
	private int y;
	private int width;
	private int height;
	private Root right;
	private Root down;

	public Root(int x, int y, int width, int height) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}

	public Root(Root root) {
		this.used = root.isUsed();
		this.x = root.getX();
		this.y = root.getY();
		this.width = root.getWidth();
		this.height = root.getHeight();
		this.right = root.getRight();
		this.down = root.getDown();
	}

	public boolean isUsed() {
		return used;
	}

	public Root setUsed(boolean used) {
		this.used = used;
		return this;
	}

	public int getX() {
		return x;
	}

	public Root setX(int x) {
		this.x = x;
		return this;
	}

	public int getY() {
		return y;
	}

	public Root setY(int y) {
		this.y = y;
		return this;
	}

	public int getWidth() {
		return width;
	}

	public Root setWidth(int width) {
		this.width = width;
		return this;
	}

	public int getHeight() {
		return height;
	}

	public Root setHeight(int height) {
		this.height = height;
		return this;
	}

	public Root getRight() {
		return right;
	}

	public Root setRight(Root right) {
		this.right = right;
		return this;
	}

	public Root getDown() {
		return down;
	}

	public Root setDown(Root down) {
		this.down = down;
		return this;
	}
}
