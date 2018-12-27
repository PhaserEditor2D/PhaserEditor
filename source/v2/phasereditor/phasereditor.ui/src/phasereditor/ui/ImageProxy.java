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

import static java.lang.System.currentTimeMillis;
import static java.lang.System.out;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.eclipse.core.resources.IFile;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;

/**
 * @author arian
 *
 */
public class ImageProxy {

	private static int MAX_SIZE;

	static {
		MAX_SIZE = 512;
		var str = System.getProperty("ImageProxy.MAX_SIZE");
		if (str != null) {
			try {
				var size = Integer.parseInt(str);
				MAX_SIZE = size;
			} catch (Exception e) {
				//
			}
		}
	}

	private File _file;
	private FrameData _fd;
	private Image _swtImage;
	private BufferedImage _currentFileBufferedImage;
	private FrameData _finalFrameData;
	private float _scale;

	private static Map<String, ImageProxy> _keyProxyMap = new HashMap<>();
	private static Map<File, BufferedImage> _fileBufferedImageMap = new HashMap<>();
	private static Map<File, Long> _fileModifiedMap = new HashMap<>();
	private static List<ImageProxy> _proxyList = new ArrayList<>();
	private static List<TrashItem> _trash = new ArrayList<>();

	public static ImageProxy get(IFile file, FrameData fd) {

		if (file == null) {
			return null;
		}

		return get(file.getLocation().toFile(), fd);
	}

	@SuppressWarnings("boxing")
	public synchronized static ImageProxy get(File file, FrameData fd) {

		try {

			if (file == null || !file.exists()) {
				return null;
			}

			var lastModified = file.lastModified();

			var key = computeKey(file, fd, lastModified);

			var img = _keyProxyMap.get(key);

			if (img == null) {

				// There are these different reasons:
				//
				// - It is requesting a new file
				// - It is requesting a file that changed
				// - It is requesting a new frame inside the same file
				//

				// check if it is requesting a new file

				var isNewFile = true;
				for (var cacheImage : _proxyList) {
					if (cacheImage._file.equals(file)) {
						isNewFile = false;
						break;
					}
				}

				if (isNewFile) {
					// create the new file buffer
					var buffer = ImageIO.read(file);
					if (buffer == null) {
						// it is not an image file!
						return null;
					}
					_fileBufferedImageMap.put(file, buffer);

					// create the virtual image
					img = new ImageProxy(file, fd);

					// add the virtual image to maps
					_proxyList.add(img);
					_keyProxyMap.put(key, img);
					_fileModifiedMap.put(file, lastModified);

					return img;
				}

				// check if the file changed

				var cacheModified = _fileModifiedMap.get(file);
				if (lastModified != cacheModified.longValue()) {
					// The file changed, we need to recompute the buffered image. The SWT images of
					// the virtual images are recomputed by demand
					var buffer = ImageIO.read(file);
					if (buffer == null) {
						// it is not an image!
						return null;
					}
					_fileBufferedImageMap.put(file, buffer);
					_fileModifiedMap.put(file, lastModified);

					// not let's find the virtual image for that file and frame data
					for (var cacheImage : _proxyList) {
						if (cacheImage.sameFileAndFrameData(file, fd)) {
							_keyProxyMap.put(key, cacheImage);
							return cacheImage;
						}
					}
				}

				// so it looks that the key changed because it is requesting a new frame data
				// inside an existant texture, so let's create a new virtual image
				{
					img = new ImageProxy(file, fd);
					// add the virtual image to maps
					_proxyList.add(img);
					_keyProxyMap.put(key, img);
					_fileModifiedMap.put(file, lastModified);
				}

			}

			return img;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	private static class TrashItem {
		public Image image;
		public long time;
		public File file;

		public TrashItem(File file, Image image) {
			this.image = image;
			this.file = file;
			time = currentTimeMillis();
		}

		@Override
		public String toString() {
			return file.getName() + " (" + time + ")";
		}

	}

	public static void startGargabeCollector() {
		new Thread("ImageProxy Garbage Collector") {

			@Override
			public void run() {
				while (true) {
					try {
						sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					collectGarbage();

				}
			}
		}.start();
	}

	public synchronized static void disposeAll() {
		out.println("ImageProxy: disposing all images...");

		for (var item : _trash) {
			out.println("ImageProxy: disposing trash item " + item);
			item.image.dispose();
		}

		for (var proxy : _proxyList) {
			if (proxy._swtImage != null) {
				out.println("ImageProxy: disposing alive image " + proxy._file.getName() + "#" + proxy._fd);
				proxy._swtImage.dispose();
			}
		}
	}

	public synchronized static void collectGarbage() {

		out.println("ImageProxy: collecting garbage...");

		var t = currentTimeMillis();

		var dispose = new ArrayList<TrashItem>();
		var trash2 = new HashSet<TrashItem>();

		for (var item : _trash) {
			var dt = t - item.time;
			if (TimeUnit.MILLISECONDS.toMinutes(dt) >= 1) {
				dispose.add(item);
			} else {
				trash2.add(item);
			}
		}

		for (var item : dispose) {
			try {
				out.println("ImageProxy: disposing " + item + ".");
				item.image.dispose();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		_trash = new ArrayList<>(trash2);

		// for (var proxy : _proxyList) {
		// if (!proxy._file.exists() && proxy._swtImage != null) {
		// out.println("ImageProxy: the file " + proxy._file.getName()
		// + " as deleted, disposing its alive proxy image.");
		// proxy._swtImage.dispose();
		// proxy._swtImage = null;
		// }
		// }

		out.println("done!");

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
		return file.getAbsolutePath() + "$" + (fd == null ? "FULL" : fd.src.toString()) + "#" + lastModified;
	}

	public ImageProxy(File file, FrameData fd) {
		_file = file;
		_fd = fd;
	}

	/**
	 * The SWT image. It is taken from the FrameData of the file texture, so it
	 * should be painted complete, as it is.
	 */
	public synchronized Image getImage() {

		updateImages();

		return _swtImage;
	}

	public synchronized BufferedImage getFileBufferedImage() {
		return _fileBufferedImageMap.get(_file);
	}

	class ResizeInfo {
		public int width;
		public int height;
		public boolean changed;
		public float scale_view_to_proxy;

		public BufferedImage createImage(BufferedImage src, FrameData fd) {
			var buf = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

			var g2 = buf.createGraphics();

			g2.scale(scale_view_to_proxy, scale_view_to_proxy);

			g2.drawImage(src,

					fd.dst.x, fd.dst.y, fd.dst.x + fd.dst.width, fd.dst.y + fd.dst.height,

					fd.src.x, fd.src.y, fd.src.x + fd.src.width, fd.src.y + fd.src.height,

					null);

			g2.dispose();

			return buf;
		}
	}

	private ResizeInfo resizeInfo(int w, int h) {

		var info = new ResizeInfo();
		info.scale_view_to_proxy = 1;
		info.width = w;
		info.height = h;
		info.changed = false;

		if (w > MAX_SIZE || h > MAX_SIZE) {
			if (w > h) {
				var ratio = (float) h / w;
				info.width = MAX_SIZE;
				info.height = (int) (info.width * ratio);
				info.scale_view_to_proxy = (float) info.width / w;
				info.changed = true;
			} else {
				var ratio = (float) h / w;
				info.height = MAX_SIZE;
				info.width = (int) (info.height / ratio);
				info.scale_view_to_proxy = (float) info.height / h;
				info.changed = true;
			}
		}

		return info;
	}

	private void updateImages() {
		try {

			var newFileBufferedImage = _fileBufferedImageMap.get(_file);

			if (_currentFileBufferedImage != newFileBufferedImage || _swtImage == null) {

				_scale = 1;

				if (_swtImage != null) {
					var item = new TrashItem(_file, _swtImage);
					_trash.add(item);
					out.println("ImageProxy: send to trash " + item);
				}

				BufferedImage frameBufferedImage;

				if (_fd == null || theFrameDataIsTheCompleteImage(newFileBufferedImage, _fd)) {
					frameBufferedImage = newFileBufferedImage;

					int width = frameBufferedImage.getWidth();
					int height = frameBufferedImage.getHeight();

					var resize = resizeInfo(width, height);

					var fd = FrameData.fromSourceRectangle(new Rectangle(0, 0, width, height));

					if (resize.changed) {
						var temp = resize.createImage(frameBufferedImage, fd);
						frameBufferedImage = temp;
						_scale = resize.scale_view_to_proxy;
					}

					_finalFrameData = fd;
				} else {
					var resize = resizeInfo(_fd.srcSize.x, _fd.srcSize.y);

					frameBufferedImage = resize.createImage(newFileBufferedImage, _fd);

					_scale = resize.scale_view_to_proxy;

					_finalFrameData = FrameData.fromSourceRectangle(new Rectangle(0, 0, _fd.srcSize.x, _fd.srcSize.y));
				}

				_swtImage = PhaserEditorUI.image_Swing_To_SWT(frameBufferedImage);

			}

			_currentFileBufferedImage = newFileBufferedImage;
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	private static boolean theFrameDataIsTheCompleteImage(BufferedImage image, FrameData fd) {
		var w = image.getWidth();
		var h = image.getHeight();

		var rect = new Rectangle(0, 0, w, h);

		return fd.srcSize.x == w

				&& fd.srcSize.y == h

				&& fd.src.equals(rect)

				&& fd.dst.equals(rect);
	}

	public synchronized FrameData getFinalFrameData() {

		updateImages();

		return _finalFrameData;
	}

	public void paint(GC gc, int dstX, int dstY, int dstW, int dstH) {
		var image = getImage();
		var b = image.getBounds();

		gc.drawImage(image,

				0, 0, b.width, b.height,

				dstX, dstY, dstW, dstH);
	}

	public void paint(GC gc, int srcX, int srcY, int srcW, int srcH, int dstX, int dstY, int dstW, int dstH) {
		var image = getImage();
		gc.drawImage(image,

				(int) (srcX * _scale), (int) (srcY * _scale), (int) (srcW * _scale), (int) (srcH * _scale),

				dstX, dstY, dstW, dstH);
	}

	public void paint(GC gc, int x, int y) {
		var image = getImage();
		var b = image.getBounds();

		gc.drawImage(image,

				0, 0, b.width, b.height,

				x, y, _finalFrameData.srcSize.x, _finalFrameData.srcSize.y);
	}

	public void paintScaledInArea(GC gc, Rectangle renderArea) {
		paintScaledInArea(gc, renderArea, true);
	}

	public void paintScaledInArea(GC gc, Rectangle renderArea, boolean center) {

		var image = getImage();
		var fd = _finalFrameData;

		int frameHeight = renderArea.height;
		int frameWidth = renderArea.width;

		double imgW = fd.src.width;
		double imgH = fd.src.height;

		// compute the right width
		imgW = imgW * (frameHeight / imgH);
		imgH = frameHeight;

		// fix width if it goes beyond the area
		if (imgW > frameWidth) {
			imgH = imgH * (frameWidth / imgW);
			imgW = frameWidth;
		}

		double scaleX = imgW / fd.src.width;
		double scaleY = imgH / fd.src.height;

		var imgX = renderArea.x + (center ? frameWidth / 2 - imgW / 2 : 0);
		var imgY = renderArea.y + frameHeight / 2 - imgH / 2;

		double imgDstW = fd.src.width * scaleX;
		double imgDstH = fd.src.height * scaleY;

		if (imgDstW > 0 && imgDstH > 0) {
			gc.drawImage(image, (int) (fd.src.x * _scale), (int) (fd.src.y * _scale), (int) (fd.src.width * _scale),
					(int) (fd.src.height * _scale), (int) imgX, (int) imgY, (int) imgDstW, (int) imgDstH);
		}
	}

	public Rectangle getBounds() {
		var fd = getFinalFrameData();
		return fd == null ? null : fd.src;
	}

	public boolean hits(int x, int y) {

		var alpha = 0;

		Rectangle b = getBounds();

		if (b.contains(x, y)) {
			var img = getImage();

			if (img != null) {
				var data = img.getImageData();
				alpha = data.getAlpha((int) (x * _scale), (int) (y * _scale));
			}
		}

		return alpha != 0;
	}
}
