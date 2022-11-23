package org.liux1506.svg;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.dom4j.Document;
import org.liux1506.svg.layout.Layout;
import org.slf4j.LoggerFactory;

/**
 * description: SVGTranscoder
 *
 * @author liuxing
 * @date Created on 2022/11/10
 **/
public abstract class SVGTranscoder {

	private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(SVGTranscoder.class);

	/**
	 * thread num
	 */
	private int threadNum = Runtime.getRuntime().availableProcessors() / 2;

	public void setThreadNum(int threadNum){
		this.threadNum = threadNum;
	}

	public int getThreadNum() {
		return threadNum;
	}

	/**
	 * threshold of multiple threads
	 */
	private int threshold = 5000;

	public int getThreshold() {
		return threshold;
	}

	public void setThreshold(int threshold) {
		this.threshold = threshold;
	}

	/**
	 * svg name in svg file property, as key in json to use
	 * @param svgFiles svg files
	 * @param ratios ratios
	 * @return sprite.png and sprite.json file zip
	 * @throws IOException
	 */
	public byte[] transcoder(List<File> svgFiles,int[] ratios) throws IOException {
		Path targetPath = Paths.get(UUID.randomUUID().toString());
		transcoder(targetPath, svgFiles, ratios);
		// zip file
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		try (
			Stream<Path> pathStream = Files.list(targetPath);
			ZipOutputStream zos = new ZipOutputStream(os)) {
			List<Path> list = pathStream.collect(Collectors.toList());
			for (Path path : list) {
				zos.putNextEntry(new ZipEntry(path.getFileName().toString()));
				zos.write(Files.readAllBytes(path));
			}
			zos.closeEntry();
		} finally {
			LOG.info("delete directory :{}", targetPath);
			FileUtils.deleteDirectory(targetPath.toFile());
		}
		return os.toByteArray();
	}

	/**
	 * transcoder mulit svg to png
	 * @param targetPath save sprite.png and sprite.json directory
	 * @param svgFiles svgs
	 * @param ratios ratios
	 */
	public void transcoder(Path targetPath, List<File> svgFiles, int[] ratios) throws IOException {
		if (!Files.exists(targetPath)) {
			Files.createDirectories(targetPath);
		}
		LOG.info("extract width and height, svg size: {}", svgFiles.size());
		// extract width and height
		List<SvgInfo> svgInfos = new ArrayList<>();
		if (svgFiles.size() > threshold) {
			List<List<File>> partition = CommonUtil.partition(svgFiles,
				(int) Math.ceil(svgFiles.size() / (double) threadNum));

			List<Callable<List<SvgInfo>>> callables = partition.stream().map(files -> {
				Callable<List<SvgInfo>> callable = () -> SvgHandleUtil.extractSvgAttr(files);;
				return callable;
			}).collect(Collectors.toList());

			ExecutorService executorService = Executors.newFixedThreadPool(callables.size());
			try {
				List<Future<List<SvgInfo>>> futures = executorService.invokeAll(callables);
				for (Future<List<SvgInfo>> future : futures) {
					List<SvgInfo> infos = future.get();
					svgInfos.addAll(infos);
				}
			} catch (ExecutionException | InterruptedException e) {
				throw new RuntimeException(e);
			} finally {
				executorService.shutdown();
			}
		} else {
			svgInfos = SvgHandleUtil.extractSvgAttr(svgFiles);
		}
		int[] containerWH = Layout.fit(svgInfos);

		int maxRatio = Arrays.stream(ratios).max().orElse(1);
		Document merge;
		try {
			LOG.info("merge svgs to one svg file, container width {} height {}", containerWH[0], containerWH[1]);
			merge = SvgHandleUtil.merge(svgInfos, containerWH[0], containerWH[1], maxRatio);
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
		String fileName = maxRatio == 1 ? "sprite" : "sprite@" + maxRatio + "x";
		Path maxDestPath = targetPath.resolve(fileName + ".png");
		LOG.info("translate to Image");
		transcoderImage(merge, svgInfos, ratios, maxDestPath, Arrays.stream(containerWH).map(operand -> operand * maxRatio).toArray());
		// write json
		try (OutputStream jsonOs = Files.newOutputStream(targetPath.resolve(fileName + ".json"))){
			String spriteJson = SvgHandleUtil.iconInfoJson(svgInfos, maxRatio);
			IOUtils.write(spriteJson.getBytes(StandardCharsets.UTF_8), jsonOs);
		}
		// 去除最大ratio， 循环缩放小尺寸ratio
		int[] leftRatios = Arrays.stream(ratios).filter(value -> value < maxRatio)
			.toArray();
		for (Integer ratio : leftRatios) {
			LOG.info("reduce image to ratio {}", ratio);
			String otherName = ratio == 1 ? "sprite" : "sprite@" + ratio + "x";
			CommonUtil.reducePng(maxDestPath, targetPath.resolve(otherName + ".png"), ratio / (float)maxRatio);
			String otherSpriteJson = SvgHandleUtil.iconInfoJson(svgInfos, ratio);
			try(FileOutputStream fos = new FileOutputStream(targetPath.resolve(otherName + ".json").toFile())) {
				IOUtils.write(otherSpriteJson.getBytes(StandardCharsets.UTF_8), fos);
			}
		}
	}
	protected abstract void transcoderImage(Document merge, List<SvgInfo> svgInfos, int[] ratios, Path targetPath, int[] containerWH) throws IOException;
}
