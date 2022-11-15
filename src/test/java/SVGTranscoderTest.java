import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.liux1506.svg.SVGTranscoder;

/**
 * description:
 *
 * @author liuxing
 * @date Created on 2022/11/14
 **/
public class SVGTranscoderTest {

//	public static void main(String[] args) throws IOException {
//		SVGTranscoder svgTranscoder = new SVGTranscoder();
//		List<File> list = Files.list(Paths.get("D:\\Document\\图标库\\下载基础公共雪碧图\\test"))
//			.filter(path -> path.getFileName().toString().endsWith(".svg"))
//			.map(Path::toFile)
//			.collect(Collectors.toList());
//		svgTranscoder.transcoder(Paths.get("D:\\Document\\图标库\\下载基础公共雪碧图\\test\\ww"), list, new int[]{1, 2});
//	}

/*	public static void main(String[] args) throws IOException {
		SVGTranscoder svgTranscoder = new SVGTranscoder();
		List<File> list = Files.list(Paths.get("D:\\Document\\图标库\\下载基础公共雪碧图\\test"))
			.filter(path -> path.getFileName().toString().endsWith(".svg"))
			.map(Path::toFile)
			.collect(Collectors.toList());
		byte[] transcoder = svgTranscoder.transcoder(list, new int[]{1, 2});
		IOUtils.write(transcoder, Files.newOutputStream(Paths.get("D:\\Document\\图标库\\下载基础公共雪碧图\\test\\ww\\sprite.zip")));
	}*/

	public static void main(String[] args) throws IOException {
		SVGTranscoder svgTranscoder = new SVGTranscoder();
		List<File> list = Files.list(Paths.get("example/icon"))
			.filter(path -> path.getFileName().toString().endsWith(".svg"))
			.map(Path::toFile)
			.collect(Collectors.toList());
		byte[] transcoder = svgTranscoder.transcoder(list, new int[]{1, 2});
		IOUtils.write(transcoder, Files.newOutputStream(Paths.get("example/sprite/sprite.zip")));
	}
}
