// The MIT License (MIT)
//
// Copyright (c) 2015, 2016 Arian Fornaris
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
package phasereditor.ui;

import static java.lang.System.currentTimeMillis;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.eclipse.core.resources.IFile;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

/**
 * A cache for images read from files. If the file is modified the cache is
 * updated.
 * 
 * @author arian
 *
 */
public class IconCache {
	private Map<String, Long> _timeCache;
	private Map<String, Image> _imgCache;
	private List<Image> _extraDispose;

	public IconCache() {
		_timeCache = new HashMap<>();
		_imgCache = new HashMap<>();
		_extraDispose = new ArrayList<>();
	}

	private static Image scaleImage(String filepath, Rectangle src, int newSize) {
		try {
			BufferedImage swingimg = ImageIO.read(new File(filepath));
			BufferedImage swingimg2 = new BufferedImage(newSize, newSize, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g2 = swingimg2.createGraphics();
			Rectangle src2 = src == null ? new Rectangle(0, 0, swingimg.getWidth(), swingimg.getHeight()) : src;
			Rectangle z = PhaserEditorUI.computeImageZoom(src2, new Rectangle(0, 0, newSize, newSize));
			g2.drawImage(swingimg, z.x, z.y, z.x + z.width, z.y + z.height, src2.x, src2.y, src2.x + src2.width,
					src2.y + src2.height, null);
			g2.dispose();
			ByteArrayOutputStream memory = new ByteArrayOutputStream();
			ImageIO.write(swingimg2, "png", memory);
			Image img = new Image(Display.getCurrent(), new ByteArrayInputStream(memory.toByteArray()));
			return img;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public Image getIcon(Path file, int iconSize) {
		return getIcon(file.toAbsolutePath().toString(), iconSize);
	}

	public Image getIcon(String filepath, int newSize) {
		return getScaledImage(filepath, null, newSize);
	}

	public Image getIcon(IFile file, int newSize) {
		return getScaledImage(file.getLocation().toPortableString(), null, newSize);
	}

	public Image getIcon(IFile file, Rectangle src, int newSize) {
		return getScaledImage(file.getLocation().toPortableString(), src, newSize);
	}

	public Image getScaledImage(String filepath, Rectangle src, int newSize) {
		Path file = Paths.get(filepath);
		String k = computeKey(filepath, src, newSize);

		// check if the file changed

		long t0;
		try {
			t0 = Files.getLastModifiedTime(file).toMillis();
		} catch (IOException e1) {
			e1.printStackTrace();
			t0 = currentTimeMillis();
		}

		if (_imgCache.containsKey(k)) {
			long t1 = _timeCache.get(k).longValue();
			if (t0 == t1) {
				// file no changed, return cached
				return _imgCache.get(k);
			}
		}

		// time changed, create new cache

		_timeCache.put(k, Long.valueOf(t0));
		try {
			Image img = scaleImage(filepath, src, newSize);
			Image old = _imgCache.put(k, img);
			if (old != null) {
				_extraDispose.add(old);
			}
			return img;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private static String computeKey(String filepath, Rectangle src, int newSize) {
		return filepath + "#" + src + "#" + newSize;
	}

	public void dispose() {
		for (Object k : _imgCache.keySet()) {
			Image img = _imgCache.get(k);
			if (img.isDisposed()) {
				continue;
			}
			img.dispose();
		}

		for (Image img : _extraDispose) {
			if (img.isDisposed()) {
				img.dispose();
			}
		}

		_imgCache = new HashMap<>();
		_timeCache = new HashMap<>();
		_extraDispose = new ArrayList<>();
	}

	/**
	 * Reset the cache but does not dispose the images.
	 */
	public void resetCache() {
		_imgCache = new HashMap<>();
		_timeCache = new HashMap<>();
	}
}
