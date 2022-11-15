package org.liux1506.svg;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * description:
 * common utils
 * @author liuxing
 * @date Created on 2022/11/10
 **/
public class CommonUtil {

	/**
	 * <p>
	 *  sub list by size<br/>
	 *  example:<br/>
	 *  <pre>
	 *  {@code
	 *      List<Integer> list = new ArrayList<>(); // 0 ~ 10
	 *      CommonUtil.partition(list, 3);
	 *      // result:
	 *      // [0,1,2]
	 *      // [3,4,5]
	 *      // [6,7,8]
	 *      // [9]
	 *  }
	 *
	 *  </code>
	 * </p>
	 * @param list source list
	 * @param size partition size
	 * @return a list of sublists
	 * @param <T>
	 */
	protected static <T> List<List<T>> partition(List<T> list, int size) {
		if (list == null) {
			throw new NullPointerException("List must not be null");
		} else if (size <= 0) {
			throw new IllegalArgumentException("Size must be greater than 0");
		}
		int index = (int)Math.ceil(list.size() / (float)size);

		List<List<T>> retList = new ArrayList<>();

		for (int i = 0; i < index; i++) {
			int start = i * size;
			int end = Math.min(start + size, list.size());
			retList.add(list.subList(start, end));
		}
		return retList;
	}

	public static void reducePng(Path srcPath, Path targetPath, Float rate) {
		int[] results = getImgWidthHeight(srcPath.toFile());

		if (results[0] == 0 || results[1] == 0) {
			return;
		}

		//按比例缩放或扩大图片大小，将浮点型转为整型
		int widthDist = (int) (results[0] * rate);
		int heightDist = (int) (results[1] * rate);

		try {
			// 开始读取文件并进行压缩
			Image src = ImageIO.read(srcPath.toFile());
			// 构造一个类型为预定义图像类型之一的 BufferedImage
			BufferedImage bi = new BufferedImage(widthDist, heightDist, BufferedImage.TYPE_INT_ARGB);

			//绘制图像  getScaledInstance表示创建此图像的缩放版本，返回一个新的缩放版本Image,按指定的width,height呈现图像
			//Image.SCALE_SMOOTH,选择图像平滑度比缩放速度具有更高优先级的图像缩放算法。
			bi.getGraphics()
				.drawImage(src.getScaledInstance(widthDist, heightDist, Image.SCALE_SMOOTH), 0, 0,
					null);

			//将图片按PNG压缩，保存到out中
			ImageIO.write(bi, "PNG", targetPath.toFile());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static int[] getImgWidthHeight(File file) {

		try (InputStream is = new FileInputStream(file)) {
			// 从流里将图片写入缓冲图片区
			BufferedImage src = ImageIO.read(is);
			return new int[]{src.getWidth(), src.getHeight()};
		} catch (IOException e) {
			return new int[2];
		}
	}

}
