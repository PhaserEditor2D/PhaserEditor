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
package phasereditor.atlas.core.internal.libgdx;

import static java.lang.System.out;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.TextureAtlasData;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.TextureAtlasData.Page;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.TextureAtlasData.Region;
import com.badlogic.gdx.tools.texturepacker.TexturePacker;
import com.badlogic.gdx.tools.texturepacker.TexturePacker.Settings;

public class LibGDXTexturePackerLauncher {

	public static void main(String[] args) throws Exception {
		LibGDXTexturePackerLauncher launcher = new LibGDXTexturePackerLauncher();
		launcher.pack();
	}

	@SuppressWarnings("static-method")
	public void pack() throws Exception {
		Settings settings = new TexturePacker.Settings();
		// settings.maxWidth = 60;
		// settings.maxHeight = 60;
		settings.pot = false;
		settings.stripWhitespaceX = true;
		settings.stripWhitespaceY = true;
		settings.paddingX = 30;
		settings.paddingY = 30;
		settings.edgePadding = true;

		File dir = new File("C:/Users/arian/Documents/Source/Phaser Editor/Documents/Test/Trim/");

		File[] inputFiles = new File[] { new File(dir, "Circle.png"), new File(dir, "Rect.png") };

		// Sort input files by name to avoid platform-dependent atlas output
		// changes.
		Arrays.sort(inputFiles, new Comparator<File>() {
			@Override
			public int compare(File file1, File file2) {
				return file1.getName().compareTo(file2.getName());
			}
		});

		TexturePacker packer = new TexturePacker(settings);
		for (File file : inputFiles) {
			packer.addImage(file);
		}

		File gdxDir = new File(dir, "libgdx");
		packer.pack(gdxDir, "output");

		TextureAtlasData data = new TextureAtlasData(new FileHandle(new File(gdxDir, "output.atlas")),
				new FileHandle(gdxDir), false);

		for (Page page : data.getPages()) {
			out.println(page.textureFile.name());
		}
		for (Region r : data.getRegions()) {
			out.println(r.page.textureFile.name());
			out.println(r.name);
		}
		// processor.process(new File(input), new File(output));
	}
}
