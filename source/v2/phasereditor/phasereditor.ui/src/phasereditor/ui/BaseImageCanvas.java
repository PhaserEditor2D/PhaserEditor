// The MIT License (MIT)
//
// Copyright (c) 2015, 2018 Arian Fornaris
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

import static java.lang.System.out;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;

/**
 * @author arian
 *
 */
public class BaseImageCanvas extends Canvas {

	private static Map<String, Image> _keyFileMap;
	private static Map<IFile, Image> _fileImageMap;
	private static List<BaseImageCanvas> _gobalCanvases;
	private static List<Image> _globalImages;
	private Collection<ImageRef> _references;

	static {
		_keyFileMap = new HashMap<>();
		_fileImageMap = new HashMap<>();
		_gobalCanvases = new ArrayList<>();
		_globalImages = new ArrayList<>();
	}

	static class ImageRef {
		public Image image;
		public IFile file;

		public ImageRef(Image image, IFile file) {
			super();
			this.image = image;
			this.file = file;
		}
	}

	public BaseImageCanvas(Composite parent, int style) {
		super(parent, style);

		_gobalCanvases.add(this);

		_references = new ArrayList<>();

		addDisposeListener(this::widgetDisposed);
	}

	@SuppressWarnings("unused")
	private void widgetDisposed(DisposeEvent e) {
		if (PlatformUI.getWorkbench().isClosing()) {
			return;
		}

		_gobalCanvases.remove(this);

		this._references = new ArrayList<>();
	}

	protected Image loadImage(IFile file) {
		if (file == null || !file.exists()) {
			return null;
		}

		collectGarbage();

		String key = computeKey(file);

		if (_fileImageMap.containsKey(file)) {
			if (_keyFileMap.containsKey(key)) {

				Image image = _keyFileMap.get(key);

				addRefernce(file, image);

				return image;
			}
		}

		try {

			Image image = new Image(getDisplay(), file.getLocation().toFile().getAbsolutePath());

			_fileImageMap.put(file, image);
			_keyFileMap.put(key, image);
			_globalImages.add(image);

			addRefernce(file, image);

			return image;

		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	private void addRefernce(IFile file, Image image) {
		for (var ref : new ArrayList<>(this._references)) {
			if (ref.file.equals(file)) {
				this._references.remove(ref);
			}
		}

		ImageRef ref = new ImageRef(image, file);

		this._references.add(ref);
	}

	private static void collectGarbage() {
		for (var image : new ArrayList<>(_globalImages)) {
			if (isDisposableImage(image)) {
				out.println("BaseImageCanvas.disposeImages(): dispose image " + image);
				image.dispose();
				_globalImages.remove(image);
			}
		}
	}

	private static boolean isDisposableImage(Image image) {
		for (var canvas : new ArrayList<>(_gobalCanvases)) {
			for (var ref : canvas._references) {
				if (ref.image == image) {
					return false;
				}
			}
		}
		return true;
	}

	private static String computeKey(IFile file) {
		return file.getLocation().toPortableString() + "@" + file.getLocalTimeStamp();
	}
}
