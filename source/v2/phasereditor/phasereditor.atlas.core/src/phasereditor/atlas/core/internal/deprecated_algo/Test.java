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
package phasereditor.atlas.core.internal.deprecated_algo;

import static java.lang.System.out;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.Display;

public class Test {

	public static void main(String[] args) throws Exception {
		
		Display device = Display.getDefault();

		try (AtlasBuilder_deprecated builder = new AtlasBuilder_deprecated()) {
			Files.walk(
			// Paths.get("C:/Users/arian/Documents/Source/EclipsePhaserTools/phasereditor.atlas.core/data2"))
					Paths.get("C:/Users/arian/Documents/Source/html5games/atlantis-defense/assets"))
			// Paths.get("C:/Users/arian/Documents/Source/html5games/squareships/assets"))
					.forEach(
							p -> {
								if (!Files.isDirectory(p)
										&& p.getFileName().toString()
												.endsWith(".png")) {
									Image img = new Image(device, p
											.toAbsolutePath().toString());
									builder.addImage(img);

								}
							});

			Atlas atlas = builder.buildAtlas();
			out.println(atlas.getWidth() * atlas.getHeight());
			Image img = atlas.createImage(Display.getDefault());
			ImageLoader loader = new ImageLoader();
			loader.backgroundPixel = 0;
			loader.data = new ImageData[] { img.getImageData() };
			loader.save("result1.png", SWT.IMAGE_PNG);
		}
	}
}
