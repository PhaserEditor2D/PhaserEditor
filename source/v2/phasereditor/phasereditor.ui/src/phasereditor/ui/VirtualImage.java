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

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.eclipse.core.resources.IFile;
import org.eclipse.swt.graphics.Image;

/**
 * @author arian
 *
 */
public class VirtualImage {

	private File _file;
	private FrameData _fd;
	private Image _swtImage;
	private BufferedImage _currentFileBufferedImage;

	private static Map<String, VirtualImage> _keyVirtualImageMap = new HashMap<>();
	private static Map<File, BufferedImage> _fileBufferedImageMap = new HashMap<>();
	private static Map<File, Long> _fileModifiedMap = new HashMap<>();
	private static List<VirtualImage> _virtualImages = new ArrayList<>();
	private static List<Image> _garbage = new ArrayList<>();

	public static VirtualImage lookup(IFile file, FrameData fd) {
		return lookup(file.getLocation().toFile(), fd);
	}

	@SuppressWarnings("boxing")
	public static VirtualImage lookup(File file, FrameData fd) {

		out.println("\nVirtualImage.lookup() " + file + " " + fd);

		try {
			if (file == null || !file.exists()) {
				return null;
			}

			var lastModified = file.lastModified();

			var key = computeKey(file, fd, lastModified);
			out.println("Key: " + key);

			var img = _keyVirtualImageMap.get(key);

			if (img == null) {

				out.println("No registered virtual image.");

				// There are these different reasons:
				//
				// - It is requesting a new file
				// - It is requesting a file that changed
				// - It is requesting a new frame inside the same file
				//

				// check if it is requesting a new file

				var isNewFile = true;
				for (var cacheImage : _virtualImages) {
					if (cacheImage._file.equals(file)) {
						isNewFile = false;
						break;
					}
				}

				if (isNewFile) {
					out.println("New file, create a new virtual image.");
					// create the new file buffer
					_fileBufferedImageMap.put(file, ImageIO.read(file));

					// create the virtual image
					img = new VirtualImage(file, fd);

					// add the virtual image to maps
					_virtualImages.add(img);
					_keyVirtualImageMap.put(key, img);
					_fileModifiedMap.put(file, lastModified);

					return img;
				}

				// check if the file changed

				var cacheModified = _fileModifiedMap.get(file);
				if (lastModified != cacheModified.longValue()) {
					out.println("File modified: search for an already created virtual image.");
					// The file changed, we need to recompute the buffered image. The SWT images of
					// the virtual images are recomputed by demand
					_fileBufferedImageMap.put(file, ImageIO.read(file));
					_fileModifiedMap.put(file, lastModified);

					// not let's find the virtual image for that file and frame data
					for (var cacheImage : _virtualImages) {
						if (cacheImage.sameFileAndFrameData(file, fd)) {
							out.println("Found!");
							_keyVirtualImageMap.put(key, cacheImage);
							return cacheImage;
						}
					}
					out.println("Not found!");
				}

				// so it looks that the key changed because it is requesting a new frame data
				// inside an existant texture, so let's create a new virtual image
				{
					out.println("Create new virtual image.");
					img = new VirtualImage(file, fd);
					// add the virtual image to maps
					_virtualImages.add(img);
					_keyVirtualImageMap.put(key, img);
					_fileModifiedMap.put(file, lastModified);
				}

			}

			return img;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	private boolean sameFileAndFrameData(File file, FrameData fd) {
		if (_file.equals(file)) {
			if (_fd == null && fd == null) {
				return true;
			}

			if (_fd == null || fd == null) {
				return false;
			}

			return _fd.srcSize.equals(fd.srcSize)

					&& _fd.src.equals(fd.src)

					&& _fd.dst.equals(fd.dst);
		}
		return false;
	}

	private static String computeKey(File file, FrameData fd, long lastModified) {
		return file.getAbsolutePath() + "$" + (fd == null ? "FULL" : fd.toString()) + "#" + lastModified;
	}

	public VirtualImage(File file, FrameData fd) {
		_file = file;
		_fd = fd;
	}

	/**
	 * The SWT image. It is taken from the FrameData of the file texture, so it
	 * should be painted complete, as it is.
	 */
	public Image getImage() {

		updateImages();

		return _swtImage;
	}

	public BufferedImage getFileBufferedImage() {
		return _fileBufferedImageMap.get(_file);
	}

	private void updateImages() {
		out.println("VirtualImage.updateImages():");
		out.println("\t" + _file + " " + _fd);
		try {
			var newFileBufferedImage = _fileBufferedImageMap.get(_file);

			if (_currentFileBufferedImage != newFileBufferedImage || _swtImage == null) {

				out.println("Underlaying buffered image changed.");

				if (_swtImage != null) {
					out.println("Addd current SWT image to garbarge.");
					_garbage.add(_swtImage);
				}

				out.println("Create new SWT image.");

				if (_fd == null) {
					_swtImage = PhaserEditorUI.image_Swing_To_SWT(newFileBufferedImage);
				} else {
					var buf = new BufferedImage(_fd.srcSize.x, _fd.srcSize.y, BufferedImage.TYPE_INT_ARGB);
					var g2 = buf.createGraphics();
					g2.drawImage(newFileBufferedImage,

							_fd.dst.x, _fd.dst.y, _fd.dst.x + _fd.dst.width, _fd.dst.y + _fd.dst.height,

							_fd.src.x, _fd.src.y, _fd.src.x + _fd.src.width, _fd.src.y + _fd.src.height,

							null);
					g2.dispose();

					_swtImage = PhaserEditorUI.image_Swing_To_SWT(buf);
				}
			}

			_currentFileBufferedImage = newFileBufferedImage;
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
}
