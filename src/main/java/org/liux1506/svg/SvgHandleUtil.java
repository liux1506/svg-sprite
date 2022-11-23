package org.liux1506.svg;

import com.steadystate.css.dom.CSSStyleDeclarationImpl;
import com.steadystate.css.dom.Property;
import com.steadystate.css.parser.CSSOMParser;
import com.steadystate.css.parser.SACParserCSS3;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.css.sac.InputSource;
import org.w3c.dom.css.CSSRuleList;
import org.w3c.dom.css.CSSStyleRule;
import org.w3c.dom.css.CSSStyleSheet;

/**
 * description: svg dom operation Util
 *
 * @author liuxing
 * @date Created on 2022/11/10
 **/
public class SvgHandleUtil {

	private static final Logger LOG = LoggerFactory.getLogger(SvgHandleUtil.class);

	private static final Map<String, String> NAME_SPACE_MAP = new HashMap<>();
	static {
		NAME_SPACE_MAP.put("svg", "http://www.w3.org/2000/svg");
		NAME_SPACE_MAP.put("xlink", "http://www.w3.org/1999/xlink");
	}

	public static List<SvgInfo> extractSvgAttr(List<File> svgFiles) {

		SAXReader read = new SAXReader();
		read.getDocumentFactory().setXPathNamespaceURIs(NAME_SPACE_MAP);

		List<SvgInfo> list = new ArrayList<>();

		for (int i = 0; i < svgFiles.size(); i++) {
			File svgFile = svgFiles.get(i);
			Document doc;
			try {
				doc = read.read(svgFile);
			} catch (DocumentException e) {
				LOG.error("read svg file to document error：" + svgFile.getName(), e);
				continue;
			}
			Element svgEle = doc.getRootElement();
			// random uuid prevent svgs clashed
			String id = UUID.randomUUID().toString().replaceAll("-", "");
			svgEle.addAttribute("id", id);

			// get name
			String title = svgEle.elementTextTrim("title");

			Integer width = px2Int(svgEle.attributeValue("width"));
			Integer height = px2Int(svgEle.attributeValue("height"));
			if(width == null || height == null){
				LOG.warn("{} missing attribute height or width", svgFile.getName());
				continue;
			}
			SvgInfo svg = new SvgInfo(id, title, width, height, svgFile);
			list.add(svg);
		}

		return list;
	}

	private static Integer px2Int(String pixel) {
		return Optional.ofNullable(pixel).map(s -> {
			String number = s.replace("px", "");
			try {
				return ((int) Math.ceil(Double.parseDouble(number)));
			} catch (NumberFormatException e) {
				LOG.error("number format exception, pixel is " + pixel, e);
				return null;
			}
		}).orElse(null);
	}


	public static Document merge(List<SvgInfo> svgs, int containerW, int containerH, int ratio)
		throws InterruptedException, ExecutionException {
		Document document = DocumentHelper.createDocument();
		Element svgTag = document.addElement("svg");
		svgTag.addNamespace("", "http://www.w3.org/2000/svg");
		svgTag.addAttribute("xmlns:xlink", "http://www.w3.org/1999/xlink");
		svgTag.addAttribute("width", String.valueOf(containerW * ratio));
		svgTag.addAttribute("height", String.valueOf(containerH * ratio));
		svgTag.addAttribute("viewBox", String.format("0 0 %d %d", containerW, containerH));

		int processors = Runtime.getRuntime().availableProcessors();

		List<List<SvgInfo>> partition = CommonUtil.partition(svgs,
			(int) Math.ceil(svgs.size() / (double) processors));

		List<Callable<List<Element>>> callables = partition.stream().map(svgInfos -> {
			Callable<List<Element>> callable = () -> iconProcess(svgInfos);
			return callable;
		}).collect(Collectors.toList());

		ExecutorService executorService = Executors.newFixedThreadPool(callables.size());

		try {
			List<Future<List<Element>>> futures = executorService.invokeAll(callables);
			for (Future<List<Element>> future : futures) {
				List<Element> elements = future.get();
				for (Element element : elements) {
					svgTag.add(element);
				}
			}
		} finally {
			executorService.shutdown();
		}
		return document;
	}

	/**
	 * process svg to element
	 * @param svgInfos
	 */
	private static List<Element> iconProcess(List<SvgInfo> svgInfos) {
		Document doc;
		List<Element> list = new ArrayList<>();
		for (SvgInfo svgInfo : svgInfos) {
			try {
				SAXReader read = new SAXReader();
				read.getDocumentFactory().setXPathNamespaceURIs(NAME_SPACE_MAP);
				doc = read.read(svgInfo.getSource());
			} catch (DocumentException e) {
				LOG.warn("read {} as document error", svgInfo.getSource().getName());
				continue;
			}
			handleReference(doc, svgInfo);

			Element svgEle = (Element) doc.getRootElement().clone();
			svgEle.addAttribute("x", String.valueOf(svgInfo.getX()))
				.addAttribute("y", String.valueOf(svgInfo.getY()));
			svgEle.addAttribute("width", String.valueOf(svgInfo.getWidth()));
			svgEle.addAttribute("height", String.valueOf(svgInfo.getHeight()));
			Attribute viewBox = svgEle.attribute("viewBox");
			if (viewBox == null) {
				svgEle.addAttribute("viewBox",
					String.format("0 0 %d %d", svgInfo.getWidth(), svgInfo.getHeight()));
			}
			list.add(svgEle);
		}
		return list;
	}

	private static final String[] svgReferenceProperties = new String[]{"style", "fill", "stroke",
		"filter", "clip-path", "mask", "marker-start", "marker-end", "marker-mid"};
	private static void handleReference(Document doc, SvgInfo svgInfo) {
		final String id = "_" + svgInfo.getUuid();
		Map<String, String> domIds = new HashMap<>();
		// select feGaussianBlur
		List<Node> nodes = doc.selectNodes("//*[local-name()='feGaussianBlur']");
		nodes.forEach(node -> node.getParent().remove(node));

		// 处理ID标签
		doc.selectNodes("//*[@id]").forEach(node -> {
			Element ele = (Element) node;
			Attribute idAttr = ele.attribute("id");
			String newValue = idAttr.getValue() + "-" + id;
			domIds.put("#" + idAttr.getValue(), newValue);
			idAttr.setValue(newValue);
		});
		// 处理xlink标签
		doc.selectNodes("//@xlink:href").forEach(node -> {
			Attribute xlinkAttr = (Attribute) node;
			String value = xlinkAttr.getValue();
			if (value.indexOf("data:") != 0 && domIds.containsKey(value)) {
				xlinkAttr.setValue("#" + domIds.get(value));
			}
		});
		// 处理其他引用标签
		for (String svgReferenceProperty : svgReferenceProperties) {
			doc.selectNodes("//@" + svgReferenceProperty).forEach(node -> {
				Attribute referenceAttr = (Attribute) node;
				String value = referenceAttr.getValue();
				// 替换URL
				referenceAttr.setValue(replaceIdAndClassnameReferences(value, domIds));
			});
		}
		// 处理aria-labelledby
		Attribute labelledby = doc.getRootElement().attribute("aria-labelledby");
		if (labelledby != null) {
			String newValue = Arrays.stream(labelledby.getValue().split(" "))
				.map(label -> domIds.getOrDefault("#" + label, label)).collect(
					Collectors.joining(" "));
			labelledby.setValue(newValue);
		}

		Attribute svgClass = doc.getRootElement().attribute("class");
		if (svgClass != null) {
			svgClass.setValue(id + " " + svgClass.getValue());
		} else {
			doc.getRootElement().addAttribute("class", id);
		}

		//  处理style标签
		doc.selectNodes("//svg:style").forEach(node -> {
			Element ele = (Element) node;
			String cssText = ele.getText();
			// 使用正则解析
			try(StringReader stringReader = new StringReader(cssText)) {
				InputSource source = new InputSource(stringReader);
				CSSOMParser parser = new CSSOMParser(new SACParserCSS3());
				CSSStyleSheet sheet = parser.parseStyleSheet(source, null, null);
				CSSRuleList rules = sheet.getCssRules();
				for (int i = 0; i < rules.getLength(); i++) {
					final CSSStyleRule rule = (CSSStyleRule)rules.item(i);
					String selectorText = rule.getSelectorText();
					String newSelectorText = "." + id + " " + selectorText;

					// 循环style中样式
					CSSStyleDeclarationImpl style = (CSSStyleDeclarationImpl) rule.getStyle();
					for (Property property : style.getProperties()) {
						String value = property.getValue().getCssText();
						value = replaceIdAndClassnameReferences(value, domIds);
						property.getValue().setCssText(value);
					}
					rule.setSelectorText(newSelectorText);
				}
				ele.setText(rules.toString());
			} catch (IOException e) {
				LOG.warn("parse style error , svg is : {}; css is ：{}", svgInfo.getSource().getName(), cssText);
			}

		});
	}
	private static String replaceIdAndClassnameReferences(String value, Map<String,String> ids) {
		if (!ids.isEmpty()) {
			Pattern compile = Pattern.compile("url\\s*\\(\\s*[\"']?([^\\s\"'\\)]+)[\"']?\\s*\\)");
			Matcher matcher = compile.matcher(value);
			while (matcher.find()) {
				String group = matcher.group(1);
				if(!ids.containsKey(group)) return value;
				String orDefault = ids.getOrDefault(group, value);
				value = value.replace(group, "#" + orDefault);
			}
		}
		return value;
	}

	public static void main(String[] args) {
		List<SvgInfo> svgs = new ArrayList<>();
		SvgInfo svgInfo = new SvgInfo();
		svgInfo.setTitle("name1");
		svgInfo.setWidth(100);
		svgInfo.setHeight(110);
		svgInfo.setX(120);
		svgInfo.setY(130);
		svgs.add(svgInfo);
		SvgInfo svgInfo2 = new SvgInfo();
		svgInfo2.setTitle("name2");
		svgInfo2.setWidth(1000);
		svgInfo2.setHeight(1100);
		svgInfo2.setX(1200);
		svgInfo2.setY(1300);
		svgs.add(svgInfo2);

		System.out.println(iconInfoJson(svgs, 1));

	}
	public static String iconInfoJson(List<SvgInfo> svgs, int ratio) {
		StringBuilder builder = new StringBuilder("{");
		for (int i = 0; i < svgs.size(); i++) {
			SvgInfo svg = svgs.get(i);
			String svgStr = "\"%s\":{\"pixelRatio\":%d,\"width\":%d,\"height\":%d,\"x\":%d,\"y\":%d}";

			String format = String.format(svgStr, svg.getTitle(), ratio, svg.getWidth() * ratio,
				svg.getHeight() * ratio, svg.getX() * ratio, svg.getY() * ratio);
			builder.append(format);
			if (i < svgs.size() - 1) {
				builder.append(",");
			}
		}
		builder.append("}");
		return builder.toString();
	}
}
