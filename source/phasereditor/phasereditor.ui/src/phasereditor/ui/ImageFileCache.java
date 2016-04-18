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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

/**
 * A cache for images read from files. If the file is modified the cache is
 * updated.
 * 
 * @author arian
 *
 */
public class ImageFileCache {
	private static final int MISSING_IMAGE_SIZE = 16;
	private Map<String, Long> _timeCache;
	private Map<String, Image> _imgCache;
	private List<Image> _extraDispose;

	public ImageFileCache() {
		_timeCache = new HashMap<>();
		_imgCache = new HashMap<>();
		_extraDispose = new ArrayList<>();
	}

	public Image getImage(IFile file) {
		String filepath = file.getLocation().toPortableString();
		return getImage(filepath);
	}

	public Image getImage(Path file) {
		return getImage(file.toAbsolutePath().toString());
	}

	private Image getImage(String filepath) {
		Path file = Paths.get(filepath);
		String k = filepath;
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
				return _imgCache.get(k);
			}
		}
		_timeCache.put(k, Long.valueOf(t0));
		try {
			Image img = PhaserEditorUI.getFileImage(file);
			Image old = _imgCache.put(k, img);
			if (old != null) {
				_extraDispose.add(old);
			}
			return img;
		} catch (Exception e) {
			e.printStackTrace();
			Image img = getMissingImage();
			_imgCache.put(k, img);
			return img;
		}
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

	public void addExtraImageToDispose(Image img) {
		_extraDispose.add(img);
	}

	private static Image getMissingImage() {
		Image image = new Image(Display.getCurrent(), MISSING_IMAGE_SIZE, MISSING_IMAGE_SIZE);
		//
		GC gc = new GC(image);
		gc.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		gc.fillRectangle(0, 0, MISSING_IMAGE_SIZE, MISSING_IMAGE_SIZE);
		gc.dispose();
		//
		return image;
	}
}
