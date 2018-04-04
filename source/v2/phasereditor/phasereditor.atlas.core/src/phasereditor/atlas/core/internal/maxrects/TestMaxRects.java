// The MIT License (MIT)
//
// Copyright (c) 2015 Arian Fornaris
//
// Permission is hereby granted, free of charge, to any person obtaining a
// copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to permit
// persons to whom the Software is furnished to do so, subject to the
// following conditions: The above copyright notice and this permission
// notice shall be included in all copies or substantial portions of the
// Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
// OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
// NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
// DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
// OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
// USE OR OTHER DEALINGS IN THE SOFTWARE.
//package phasereditor.atlas.core.internal.maxrects;
//
//import static java.lang.System.out;
//
//import java.awt.image.BufferedImage;
//import java.io.File;
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Paths;
//import java.util.List;
//
//import javax.imageio.ImageIO;
//
//import phasereditor.atlas.core.Settings;
//
//public class TestMaxRects {
//	public static void main(String[] args) throws IOException {
//		MaxRectsPacker packer = new MaxRectsPacker();
//
//		Array<Rect> input = new Array<>();
//		Rect rect = new Rect(null, 100, 100, false);
//		input.add(rect);
//
//		rect = new Rect(null, 50, 50, false);
//		input.add(rect);
//		Array<Page> pages = packer.pack(input);
//		for (Page page : pages) {
//			out.println("page\n\n");
//			for (Rect r : page.outputRects) {
//				out.println(r);
//			}
//		}
//
//		// ---
//
//		Settings s = new Settings();
//		s.pot = false;
//		s.maxWidth = 2048;
//		s.maxHeight = 1048;
//		s.paddingX = 0;
//		s.paddingY = 0;
//		AtlasBuilder builder = new AtlasBuilder(s);
//		Files.walk(
//		// Paths.get("C:/Users/arian/Documents/Source/EclipsePhaserTools/phasereditor.atlas.core/data2"))
//				Paths.get("C:/Users/arian/Documents/Source/html5games/atlantis-defense/assets"))
//		// Paths.get("C:/Users/arian/Documents/Source/html5games/squareships/assets"))
//				.forEach(
//						p -> {
//							if (!Files.isDirectory(p)
//									&& p.getFileName().toString()
//											.endsWith(".png")) {
//								BufferedImage img;
//								try {
//									img = ImageIO.read(p.toFile());
//									builder.addImage(img, p.getFileName()
//											.toString());
//								} catch (Exception e) {
//									throw new RuntimeException(e);
//								}
//
//							}
//						});
//
//		List<BufferedImage> list = builder.buildAtlasImages();
//
//		int i = 0;
//		for (BufferedImage img : list) {
//			ImageIO.write(img, "png", new File("atlas" + i + ".png"));
//			i++;
//		}
//
//	}
// }
