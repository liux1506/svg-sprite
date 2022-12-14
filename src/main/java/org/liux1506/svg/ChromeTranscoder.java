package org.liux1506.svg;

import com.ruiyun.jvppeteer.core.Puppeteer;
import com.ruiyun.jvppeteer.core.browser.Browser;
import com.ruiyun.jvppeteer.core.page.Page;
import com.ruiyun.jvppeteer.options.Clip;
import com.ruiyun.jvppeteer.options.LaunchOptions;
import com.ruiyun.jvppeteer.options.LaunchOptionsBuilder;
import com.ruiyun.jvppeteer.options.PageNavigateOptions;
import com.ruiyun.jvppeteer.options.ScreenshotOptions;
import com.ruiyun.jvppeteer.options.Viewport;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import org.apache.commons.io.FileUtils;
import org.dom4j.Document;
import org.slf4j.LoggerFactory;

/**
 * description:
 * chrome svg to png
 * @author liuxing
 * @date Created on 2022/11/22
 **/
public class ChromeTranscoder extends SVGTranscoder{

	private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(SVGTranscoder.class);
	private static final String TEMP_HTML_NAME = "convert-svg-png.html";

	@Override
	protected void transcoderImage(Document doc, Path targetPath, int[] containerWH) throws IOException {
		LOG.info("use chrome screenshot transcoder");
		Browser browser = null;
		Page page;
		try {
			browser = browser();
			page = browser.newPage();
			String path = writeTempFile(doc);
			LOG.info("load html page");
			page.goTo("file://" + path,
				new PageNavigateOptions(null, 30 * 60000, null));
			Viewport viewport = new Viewport();
			viewport.setWidth(containerWH[0]);
			viewport.setHeight(containerWH[1]);
			page.setViewport(viewport);
			ScreenshotOptions options = new ScreenshotOptions();
			options.setPath(targetPath.toString());
			options.setClip(new Clip(0,0,containerWH[0],containerWH[1]));
			options.setOmitBackground(true);
			options.setType("png");
			LOG.info("screenshot start");
			page.screenshot(options);
		} catch (InterruptedException e) {
			throw new RuntimeException("page goto interrupted");
		} catch (ExecutionException e) {
			throw new RuntimeException(e);
		} finally {
			LOG.info("close browser");
			if (browser != null) {
				browser.close();
			}
		}
	}

	private String writeTempFile(Document doc) throws IOException {
		String svgXml = doc.asXML();
		int start = svgXml.indexOf("<svg");
		String html = "<!DOCTYPE html>\n"
			+ "<base href=\"${options.baseUrl}\">"
			+ "<style>\n"
			+ "* { margin: 0; padding: 0; }\n"
			+ "</style>\n"
			+ "%s\n"
			+ "</html>";
		if (start >= 0) {
			html = String.format(html, svgXml);
		} else {
			throw new RuntimeException("SVG element open tag not found in input. Check the SVG input");
		}

		Path path = Paths.get(System.getProperty("java.io.tmpdir"),"tmp-svg-html", TEMP_HTML_NAME);
		FileUtils.writeStringToFile(path.toFile(), html, "UTF-8");
		return path.toString();
	}

	/**
	 * ???????????????
	 * @return
	 * @throws IOException
	 */
	private Browser browser() throws IOException, InterruptedException, ExecutionException {
		ArrayList<String> arrayList = new ArrayList<>();
		// ???????????????????????????????????????????????????????????????????????????
		arrayList.add("--no-first-run");
		//??????????????????
		arrayList.add("--no-sandbox");
		// ??????uid??????
		arrayList.add("--disable-setuid-sandbox");
		//????????????????????????????????????????????????
		BrowserFetcher.downloadIfNotExist("1073354");
		LaunchOptions options = new LaunchOptionsBuilder().withArgs(arrayList).withHeadless(true).build();
		options.setIgnoreHTTPSErrors(true);
		return Puppeteer.launch(options);
	}
}
