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
package phasereditor.ui;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.wb.swt.SWTResourceManager;

import phasereditor.ui.views.PreviewView;

public class PhaserEditorUI {
	public static Color PREVIEW_BG_DARK = SWTResourceManager.getColor(180, 180, 180);
	public static Color PREVIEW_BG_LIGHT = SWTResourceManager.getColor(200, 200, 200);
	private static Set<Object> _supportedImageExts = new HashSet<>(Arrays.asList("png", "bmp", "jpg", "gif", "ico"));

	private PhaserEditorUI() {
	}

	public static void forEachEditor(Consumer<IEditorReference> visitor) {
		for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
			for (IWorkbenchPage page : window.getPages()) {
				for (IEditorReference editorRef : page.getEditorReferences()) {
					visitor.accept(editorRef);
				}
			}
		}
	}

	public static boolean forEachOpenFileEditor(IFile file, Consumer<IEditorPart> visitor) {
		boolean found = false;
		for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
			for (IWorkbenchPage page : window.getPages()) {
				for (IEditorReference editorRef : page.getEditorReferences()) {
					IEditorPart editor = editorRef.getEditor(false);
					
					if (editor == null) {
						continue;
					}
					
					IEditorInput input = editor.getEditorInput();
					
					if (input instanceof FileEditorInput) {
						IFile editorFile = ((FileEditorInput) input).getFile();						
						if (editorFile.equals(file)) {
							found = true;
							visitor.accept(editor);
						}
					}
				}
			}
		}
		return found;
	}

	public static boolean isEditorSupportedImage(IFile file) {
		return _supportedImageExts.contains(file.getFileExtension().toLowerCase());
	}

	public static IFile pickFileWithoutExtension(List<IFile> files, String... exts) {
		if (files.isEmpty()) {
			return null;
		}
		Set<String> set = new HashSet<>();
		for (String ext : exts) {
			set.add(ext.toLowerCase());
		}
		for (IFile file : files) {
			String ext = file.getFileExtension().toLowerCase();
			if (!set.contains(ext)) {
				return file;
			}
		}
		return files.get(0);
	}

	public static Path eclipseFileToJavaPath(IFile file) {
		return Paths.get(file.getLocation().toPortableString());
	}

	public interface ISearchFilter {
		public void setSearchText(String str);
	}

	public static void initSearchText(Text text, StructuredViewer viewer, ViewerFilter filter) {
		String msg = "type filter text";
		text.setText(msg);

		viewer.setFilters(new ViewerFilter[] { filter });

		text.addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent e) {
				if (text.getText().equals(msg)) {
					text.setText("");
				}
			}

			@Override
			public void focusLost(FocusEvent e) {
				if (text.getText().trim().length() == 0) {
					text.setText(msg);
				}
			}
		});

		text.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				String str = text.getText();
				((ISearchFilter) filter).setSearchText(str);
				viewer.refresh();
				if (viewer instanceof TreeViewer) {
					if (str.trim().length() > 0) {
						((TreeViewer) viewer).expandAll();
					}
				}
			}
		});
	}

	public static class SimpleSearchFilter extends ViewerFilter implements ISearchFilter {

		private String _searchString;
		private ILabelProvider _labelProvider;
		private Map<Object, Boolean> _cache;

		public SimpleSearchFilter(ILabelProvider labelProvider) {
			super();
			_labelProvider = labelProvider;
			_cache = new HashMap<>();
		}

		@Override
		public void setSearchText(String s) {
			this._searchString = ".*" + s.toLowerCase().replace("*", ".*") + ".*";
			_cache = new HashMap<>();
		}

		@Override
		public boolean select(Viewer viewer, Object parent, Object element) {
			if (_cache.containsKey(element)) {
				return _cache.get(element).booleanValue();
			}

			boolean b = select2(viewer, parent, element);

			_cache.put(element, Boolean.valueOf(b));

			return b;
		}

		private boolean select2(Viewer viewer, Object parent, Object element) {
			if (_searchString == null || _searchString.length() == 4) {
				return true;
			}

			{
				// if it is not final, then match it if any children match.
				IContentProvider provider = ((StructuredViewer) viewer).getContentProvider();
				if (provider instanceof ITreeContentProvider) {
					Object[] children = ((ITreeContentProvider) provider).getChildren(element);
					if (children.length > 0) {
						for (Object child : children) {
							if (select(viewer, element, child)) {
								return true;
							}
						}
					}
				}
			}

			if (matches(parent)) {
				return true;
			}

			return matches(element);
		}

		private boolean matches(Object element) {
			String text = _labelProvider.getText(element).toLowerCase();

			if (text.matches(_searchString)) {
				return true;
			}

			return false;
		}
	}

	public static void initSearchText(Text text, StructuredViewer viewer, ILabelProvider labelProvider) {
		initSearchText(text, viewer, new SimpleSearchFilter(labelProvider));
	}

	/**
	 * <p>
	 * Searches "searchTerm" in "content" and returns an array of int pairs
	 * (index, length) for each occurrence. The search is case-sensitive. The
	 * consecutive occurrences are merged together.
	 * </p>
	 * <p>
	 * Examples:
	 * </p>
	 * 
	 * <pre>
	 * content = "123123x123"
	 * searchTerm = "1"
	 * --> [0, 1, 3, 1, 7, 1]
	 * content = "123123x123"
	 * searchTerm = "123"
	 * --> [0, 6, 7, 3]
	 * </pre>
	 * 
	 * http://www.vogella.com/tutorials/EclipseJFaceTableAdvanced/article.html
	 * 
	 * @param searchTerm
	 *            can be null or empty. int[0] is returned in this case!
	 * @param content
	 *            a not-null string (can be empty!)
	 * @return an array of int pairs (index, length)
	 */

	public static int[] getSearchTermOccurrences(final String searchTerm, final String content) {
		if (searchTerm == null || searchTerm.length() == 0) {
			return new int[0];
		}
		if (content == null) {
			throw new IllegalArgumentException("content is null");
		}
		final List<Integer> list = new ArrayList<>();
		int searchTermLength = searchTerm.length();
		int index;
		int fromIndex = 0;
		int lastIndex = -1;
		int lastLength = 0;
		while (true) {
			index = content.indexOf(searchTerm, fromIndex);
			if (index == -1) {
				// no occurrence of "searchTerm" in "content" starting from
				// index "fromIndex"
				if (lastIndex != -1) {
					// but there was a previous occurrence
					list.add(Integer.valueOf(lastIndex));
					list.add(Integer.valueOf(lastLength));
				}
				break;
			}
			if (lastIndex == -1) {
				// the first occurrence of "searchTerm" in "content"
				lastIndex = index;
				lastLength = searchTermLength;
			} else {
				if (lastIndex + lastLength == index) {
					// the current occurrence is right after the previous
					// occurrence
					lastLength += searchTermLength;
				} else {
					// there is at least one character between the current
					// occurrence and the previous one
					list.add(Integer.valueOf(lastIndex));
					list.add(Integer.valueOf(lastLength));
					lastIndex = index;
					lastLength = searchTermLength;
				}
			}
			fromIndex = index + searchTermLength;
		}
		final int n = list.size();
		final int[] result = new int[n];
		for (int i = 0; i != n; i++) {
			result[i] = list.get(i).intValue();
		}
		return result;
	}

	public static StyledCellLabelProvider createSearchLabelProvider(Supplier<String> getStr,
			Function<Object, String> toStr) {
		return new StyledCellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				String search = getStr.get();
				String cellText = toStr.apply(cell.getElement());
				cell.setText(cellText);
				if (search != null && search.length() > 0) {
					int intRangesCorrectSize[] = getSearchTermOccurrences(search, cellText);
					List<StyleRange> styleRange = new ArrayList<>();
					for (int i = 0; i < intRangesCorrectSize.length / 2; i++) {
						int start = intRangesCorrectSize[i];
						int length = intRangesCorrectSize[++i];
						StyleRange myStyledRange = new StyleRange(start, length, null, null);
						myStyledRange.font = SWTResourceManager.getBoldFont(cell.getFont());
						styleRange.add(myStyledRange);
					}
					cell.setStyleRanges(styleRange.toArray(new StyleRange[styleRange.size()]));
				} else {
					cell.setStyleRanges(null);
				}

				super.update(cell);

			}
		};
	}

	public static void paintPreviewBackground(GC gc, Rectangle canvasBounds) {
		paintPreviewBackground(gc, canvasBounds, 10);
	}

	public static void paintPreviewBackground(GC gc, Rectangle canvasBounds, int space) {
		Rectangle oldClip = gc.getClipping();
		gc.setClipping(canvasBounds);
		int nx = canvasBounds.width / space + 2;
		int ny = canvasBounds.height / space + 2;
		for (int x = -1; x < nx; x++) {
			for (int y = 0; y < ny; y++) {
				int fx = canvasBounds.x / space * space + x * space;
				int fy = canvasBounds.y / space * space + y * space;
				if ((fx / space + fy / space) % 2 == 0) {
					gc.setBackground(PREVIEW_BG_DARK);
				} else {
					gc.setBackground(PREVIEW_BG_LIGHT);
				}
				gc.fillRectangle(fx, fy, space, space);
			}
		}
		gc.setClipping(oldClip);
	}

	public static void paintPreviewMessage(GC gc, Rectangle canvasBounds, String msg) {
		Point msgSize = gc.stringExtent(msg);
		int x = canvasBounds.width / 2 - msgSize.x / 2;
		if (x < 0) {
			x = 5;
		}
		int y = canvasBounds.height / 2 - msgSize.y / 2;
		gc.setBackground(PREVIEW_BG_DARK);
		gc.setForeground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
		gc.drawText(msg, x, y);
	}

	public static void swtRunRedraw(Control c) {
		swtRun(c, (c2) -> {
			c2.redraw();
		});
	}

	public static void swtRun(Runnable run) {
		try {

			if (PlatformUI.getWorkbench().isClosing()) {
				return;
			}

			Display display = Display.getDefault();
			display.asyncExec(new Runnable() {

				@Override
				public void run() {
					try {
						run.run();
					} catch (SWTException e) {
						// nothing
					}
				}
			});
		} catch (SWTException e) {
			// nothing
		}
	}

	public static <T extends Control> void swtRun(final T c, Consumer<T> action) {
		Display display = Display.getDefault();

		if (display.isDisposed()) {
			return;
		}

		if (Thread.currentThread() == display.getThread()) {
			if (c.isDisposed()) {
				return;
			}

			action.accept(c);
			return;
		}

		try {
			display.asyncExec(new Runnable() {

				@Override
				public void run() {
					if (!c.isDisposed()) {
						action.accept(c);
					}
				}
			});
		} catch (SWTException e) {
			//
		}
	}

	public static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			// e.printStackTrace();
		}
	}

	public static void openPreview(Object obj) {
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		try {
			PreviewView view = (PreviewView) page.showView(PreviewView.ID);
			view.preview(obj);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	// http://stackoverflow.com/questions/3758606/how-to-convert-byte-size-into-human-readable-format-in-java
	private static final String[] Q = new String[] { "", "k", "m", "g", "t", "P", "E" };

	public static String getFileHumanSize(long bytes) {
		for (int i = 6; i > 0; i--) {
			double step = Math.pow(1024, i);
			if (bytes > step)
				return String.format("%3.1f %sb", Double.valueOf(bytes / step), Q[i]);
		}
		return Long.toString(bytes);
	}

	public static Rectangle computeImageZoom(Rectangle srcBounds, Rectangle dstBounds) {
		int srcW = srcBounds.width;
		int srcH = srcBounds.height;
		int dstW = srcW;
		int dstH = srcH;
		double ratio = srcW / (double) srcH;
		int controlHeight = dstBounds.height;
		int controlWidth = dstBounds.width;

		if (srcW >= dstW || srcH >= dstH) {
			if (srcW > srcH && srcW > controlWidth) {
				dstW = controlWidth;
				dstH = (int) (dstW / ratio);
			} else if (srcH > controlHeight) {
				dstH = controlHeight;
				dstW = (int) (dstH * ratio);
			}

			if (dstW > controlWidth) {
				dstW = controlWidth;
				dstH = (int) (dstW / ratio);
			}

			if (dstH > controlHeight) {
				dstH = controlHeight;
				dstW = (int) (dstH * ratio);
			}
		}

		int dstX = (controlWidth - dstW) / 2;
		int dstY = (controlHeight - dstH) / 2;

		return new Rectangle(dstX, dstY, dstW, dstH);
	}

	public static String readString(InputStream input) throws IOException {
		StringBuilder sb = new StringBuilder();
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		String line;
		while ((line = reader.readLine()) != null) {
			sb.append(line + "\n");
		}
		return sb.toString();
	}

	public static String getNameFromFilename(String name) {
		String name2 = name;
		name2 = Paths.get(name).getFileName().toString();
		int i = name2.lastIndexOf(".");
		if (i > 0) {
			name2 = name2.substring(0, i);
		}
		return name2;
	}

	public static String getExtensionFromFilename(String name) {
		int i = name.lastIndexOf(".");
		if (i > 0) {
			return name.substring(i + 1, name.length());
		}
		return "";
	}

	public static void setStyle(String style, Control... controls) {
		for (Control control : controls) {
			control.setData("org.eclipse.e4.ui.css.CssClassName", style);
		}
	}

	public static Image getFileImage(IFile file) {
		try (InputStream contents = file.getContents();) {
			Image img = getStreamImage(contents);
			return img;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static Image getFileImage(Path file) {
		try (InputStream contents = Files.newInputStream(file)) {
			Image img = getStreamImage(contents);
			return img;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static Image getStreamImage(InputStream stream) throws IOException {
		try {
			Display display = Display.getCurrent();
			ImageData data = new ImageData(stream);
			if (data.transparentPixel > 0) {
				return new Image(display, data, data.getTransparencyMask());
			}
			return new Image(display, data);
		} finally {
			stream.close();
		}
	}

	public static Rectangle getImageBounds(String filepath) {
		try (FileImageInputStream input = new FileImageInputStream(new File(filepath))) {
			ImageReader reader = ImageIO.getImageReaders(input).next();
			reader.setInput(input);
			int w = reader.getWidth(0);
			int h = reader.getHeight(0);
			return new Rectangle(0, 0, w, h);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static Rectangle getImageBounds(IFile file) {
		return getImageBounds(file.getLocation().toPortableString());
	}

	public static Image makeColorIcon(RGB rgb) {
		RGB black = new RGB(0, 0, 0);
		PaletteData dataPalette = new PaletteData(new RGB[] { black, black, rgb });
		ImageData data = new ImageData(16, 16, 4, dataPalette);
		data.transparentPixel = 0;

		int start = 3;
		int end = 16 - start;
		for (int y = start; y <= end; y++) {
			for (int x = start; x <= end; x++) {
				if (x == start || y == start || x == end || y == end) {
					data.setPixel(x, y, 1);
				} else {
					data.setPixel(x, y, 2);
				}
			}
		}

		Image img = new Image(Display.getCurrent(), data);

		return img;
	}
}
