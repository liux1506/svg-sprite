package org.liux1506.svg;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.dom4j.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * description:
 *
 * @author liuxing
 * @date Created on 2022/11/22
 **/
public class BatikTranscoder extends SVGTranscoder{

	private static final Logger LOG = LoggerFactory.getLogger(BatikTranscoder.class);

	@Override
	protected void transcoderImage(Document merge, Path targetPath, int[] containerWh)
		throws IOException {
		LOG.info("use batik svg to png transcoder");
		byte[] bytes = merge.asXML().getBytes(StandardCharsets.UTF_8);

		try (InputStream is = new ByteArrayInputStream(bytes);
			OutputStream os = Files.newOutputStream(targetPath);
		) {
			PNGTranscoder t = new PNGTranscoder();
			TranscoderInput input = new TranscoderInput(is);
			TranscoderOutput output = new TranscoderOutput(os);
			t.transcode(input, output);
		} catch (TranscoderException e) {
			throw new RuntimeException("svg to png error", e);
		}
	}
}
