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
package phasereditor.assetpack.ui.preview;

import static java.lang.System.currentTimeMillis;
import static java.lang.System.out;
import static phasereditor.ui.PhaserEditorUI.swtRun;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;

import phasereditor.assetpack.core.AssetPackCore;
import phasereditor.assetpack.core.ImageAssetModel;
import phasereditor.assetpack.core.TilemapAssetModel;
import phasereditor.assetpack.ui.SelectTextureDialog;
import phasereditor.assetpack.ui.TextureListContentProvider;
import phasereditor.ui.Colors;
import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.IEditorSharedImages;
import phasereditor.ui.ImageCanvas_Zoom_1_1_Action;
import phasereditor.ui.ImageCanvas_Zoom_FitWindow_Action;
import phasereditor.ui.ImageProxy;
import phasereditor.ui.PhaserEditorUI;
import phasereditor.ui.ZoomCanvas;

/**
 * @author arian
 *
 */
public class TilemapCanvas extends ZoomCanvas
		implements MouseMoveListener, MouseListener, ISelectionProvider {

	private TilemapAssetModel _model;
	private Point _imageSize = new Point(1, 1);
	private int _tileWidth;
	private int _tileHeight;
	private Color[] _colors;
	protected ImageAssetModel _imageModel;
	private BufferedImage _tileSetImage;
	private Image _renderImage;
	private int _mouseX;
	private int _mouseY;
	private List<Point> _selectedCells;
	private int _mouseMapX;
	private int _mouseMapY;
	private boolean _buildingImage;
	private static ExecutorService _pool;

	public TilemapCanvas(Composite parent, int style) {
		super(parent, style);

		_selectedCells = new ArrayList<>() {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean add(Point e) {
				if (!contains(e)) {
					return super.add(e);
				}
				return false;
			}
		};

		addPaintListener(this);
		addMouseMoveListener(this);
		addMouseListener(this);
		addKeyListener(this);

		Point size = PhaserEditorUI.get_pref_Preview_Tilemap_size();
		_tileWidth = size.x;
		_tileHeight = size.y;
	}

	public void generateColors(int n) {
		_colors = new Color[n];
		for (int i = 0; i < n; i++) {
			java.awt.Color c = java.awt.Color.getHSBColor((float) i / (float) n, 0.85f, 1.0f);
			_colors[i] = Colors.color(c.getRed(), c.getGreen(), c.getBlue());
		}
	}

	public void setModel(TilemapAssetModel model) {

		_imageModel = null;
		_tileWidth = 32;
		_tileHeight = 32;

		disposeImages();

		_model = model;

		int[][] map = _model == null ? null : _model.getCsvData();

		if (map != null) {
			int max = 0;
			for (int i = 0; i < map.length; i++) {
				for (int j = 0; j < map[i].length; j++) {
					max = Math.max(map[i][j], max);
				}
			}

			generateColors(max);
			updateImageSize();
		}

		if (model != null) {
			var optData = AssetPackCore.loadCSVTilemapData(model);

			if (optData.isPresent()) {
				var data = optData.get();
				_tileWidth = data.tileWidth;
				_tileHeight = data.tileHeight;
				_imageModel = data.imageModel;

				updateImageSize();
				setImageModel(_imageModel);
			}
		}

		resetZoom();
	}

	void saveData() {
		try {
			AssetPackCore.saveCSVTilemapData(_model, _imageModel, _tileWidth, _tileHeight);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	void updateImageSize() {
		if (_model != null) {
			int[][] map = _model.getCsvData();
			if (map.length > 0 && map[0].length > 0) {
				_imageSize = new Point(map[0].length * getTileWidth(), map.length * getTileHeight());
				return;
			}
		}
		_imageSize = new Point(1, 1);
	}

	public void setImageModel(ImageAssetModel imageModel) {
		_imageModel = imageModel;

		buildTilesetImage();

		buildMapImage();
	}

	public ImageAssetModel getImageModel() {
		return _imageModel;
	}

	private void buildTilesetImage() {
		if (_imageModel == null) {
			_tileSetImage = null;
		} else {
			var proxy = ImageProxy.get(_imageModel.getUrlFile(), null);
			_tileSetImage = proxy == null ? null : proxy.getFileBufferedImage();
		}
	}

	void buildMapImage() {
		_buildingImage = true;
		redraw();

		if (_pool == null) {
			_pool = Executors.newCachedThreadPool();
		}

		_pool.execute(this::buildImageMap2);
	}

	private void buildImageMap2() {
		try {
			if (_model != null && _tileSetImage != null) {

				var t = currentTimeMillis();

				int[][] map = _model.getCsvData();

				if (map != null && map.length > 0) {

					Point size = getImageSize();

					int mapWidth = size.x;
					int mapHeight = size.y;

					var t2 = currentTimeMillis();

					out.println(
							"Created buffer " + mapWidth + "," + mapHeight + ": " + (currentTimeMillis() - t2 + "ms"));

					var bounds = new Rectangle(0, 0, _tileSetImage.getWidth(), _tileSetImage.getHeight());

					var mapImage = new BufferedImage(mapWidth, mapHeight, BufferedImage.TYPE_INT_ARGB);

					var gc = mapImage.createGraphics();

					for (int i = 0; i < map.length; i++) {
						int[] row = map[i];

						for (int j = 0; j < row.length; j++) {

							int frame = map[i][j];

							if (frame < 0) {
								continue;
							}

							int w = getTileWidth();
							int h = getTileHeight();
							int x = j * getTileWidth();
							int y = i * getTileHeight();

							int srcX = frame * _tileWidth % bounds.width;
							int srcY = frame * _tileWidth / bounds.width * _tileHeight;

							try {
								gc.drawImage(_tileSetImage, x, y, x + w, y + h, srcX, srcY, srcX + _tileWidth, srcY + _tileHeight, null);
							} catch (IllegalArgumentException e) {
								gc.drawString("Invalid parameters. Please check the tiles size.", 10, 10);
							}
						}
					}

					gc.dispose();

					var old = _renderImage;
					

					try {
						_renderImage = PhaserEditorUI.image_Swing_To_SWT(mapImage);
						
						if (old != null) {
							old.dispose();
						}
						
					} catch (IOException e) {
						e.printStackTrace();
					}

					out.println("Building CSV tilemap image done: " + (currentTimeMillis() - t));
				}
			}
		} finally {

			_buildingImage = false;

		}

		swtRun(() -> {
			resetZoom();
		});
	}

	@Override
	public void dispose() {
		super.dispose();

		disposeImages();
	}

	private void disposeImages() {
		if (_renderImage != null) {
			_renderImage.dispose();
			_renderImage = null;
		}
	}

	public TilemapAssetModel getModel() {
		return _model;
	}

	public List<Point> getSelectedCells() {
		return _selectedCells;
	}

	@Override
	protected Point getImageSize() {
		return _imageSize;
	}

	@Override
	public void customPaintControl(PaintEvent e) {
		GC gc = e.gc;

		if (_buildingImage) {
			var str = "Building tilemap image...";
			var extent = gc.stringExtent(str);
			gc.drawText(str, e.width / 2 - extent.x / 2, e.height / 2 - extent.y / 2);
			return;
		}

		if (_model != null) {

			int[][] map = _model.getCsvData();

			if (map != null && map.length > 0) {

				ZoomCalculator calc = calc();

				float scale = calc.scale;
				float offX = calc.offsetX;
				float offY = calc.offsetY;

				if (_renderImage == null) {

					for (int i = 0; i < map.length; i++) {
						int[] row = map[i];

						for (int j = 0; j < row.length; j++) {

							int frame = map[i][j];

							if (frame < 0) {
								continue;
							}

							int w = (int) (getTileWidth() * scale);
							int h = (int) (getTileHeight() * scale);
							int x = (int) (j * getTileWidth() * scale + offX);
							int y = (int) (i * getTileHeight() * scale + offY);

							// paint map with colors
							Color c = _colors[frame % _colors.length];
							gc.setBackground(c);
							gc.fillRectangle(x, y, w + 1, h + 1);
						}
					}
				} else {
					Rectangle b = _renderImage.getBounds();

					Rectangle dst = new Rectangle((int) offX, (int) offY, (int) (b.width * scale),
							(int) (b.height * scale));

					PhaserEditorUI.paintPreviewBackground(gc, dst);

					gc.drawImage(_renderImage, 0, 0, b.width, b.height, dst.x, dst.y, dst.width, dst.height);
				}

				// paint selection

				Color borderColor = PhaserEditorUI.get_pref_Preview_Tilemap_overTileBorderColor();
				Color labelsColor = PhaserEditorUI.get_pref_Preview_Tilemap_labelsColor();
				Color shadowColor = getDisplay().getSystemColor(SWT.COLOR_BLACK);
				Color selectionColor = PhaserEditorUI.get_pref_Preview_Tilemap_selectionBgColor();

				for (Point p : _selectedCells) {
					gc.setAlpha(100);
					gc.setBackground(selectionColor);
					gc.fillRectangle((int) (offX + p.x * _tileWidth * scale), (int) (offY + p.y * _tileHeight * scale),
							(int) (_tileWidth * scale), (int) (_tileHeight * scale));
					gc.setAlpha(255);
				}

				try {

					int frame = map[_mouseMapY][_mouseMapX];

					int cellY = (int) (offY + _mouseMapY * _tileHeight * scale);
					int cellX = (int) (offX + _mouseMapX * _tileWidth * scale);
					int cellW = (int) (_tileWidth * scale);
					int cellH = (int) (_tileHeight * scale);

					// paint border

					gc.setForeground(borderColor);

					gc.drawRectangle(cellX, cellY, cellW, cellH);

					// paint label

					FontMetrics fm = gc.getFontMetrics();
					int fh = fm.getHeight() + 3;
					int fw = (int) (fm.getAverageCharacterWidth() + 3);

					int y;
					int x;

					Rectangle b = getBounds();

					if (_mouseY < b.height / 2) {
						y = cellY + cellH;
					} else {
						y = cellY - fh;
					}

					if (_mouseX < b.width / 2) {
						x = cellX + cellW;
					} else {
						x = cellX - fw;
					}

					String label = Integer.toString(frame);
					gc.setForeground(shadowColor);
					gc.drawString(label, x - 1, y + 1, true);
					gc.setForeground(shadowColor);
					gc.drawString(label, x + 1, y - 1, true);
					gc.setForeground(labelsColor);
					gc.drawString(label, x, y, true);
				} catch (Exception e2) {
					// nothing
				}

				return;
			}
		}

		gc.drawText("Missing map data", 10, 10);
	}

	public void createToolBar(IToolBarManager toolbar) {
		toolbar.add(new ImageCanvas_Zoom_1_1_Action(this));
		toolbar.add(new ImageCanvas_Zoom_FitWindow_Action(this));

		toolbar.add(new Separator());

		toolbar.add(createSelectTilesetImageAction());
		toolbar.add(createSetTileSizeAction());
	}

	private Action createSetTileSizeAction() {
		return new Action("Set the tiles size") {
			{
				setImageDescriptor(EditorSharedImages.getImageDescriptor(IEditorSharedImages.IMG_SETTINGS));
			}

			@Override
			public void run() {
				InputDialog dlg = new InputDialog(getShell(), "Tile Size", "Set the tile size (eg. 16x16)",
						getTileWidth() + "x" + getTileHeight(), new IInputValidator() {

							@Override
							public String isValid(String str) {
								try {
									String[] tuple = str.split("x");
									Integer.parseInt(tuple[0]);
									Integer.parseInt(tuple[1]);
								} catch (Exception e) {
									return "Invalid format. Please enter something like 16x16";
								}

								return null;
							}
						});

				if (dlg.open() == Window.OK) {

					String result = dlg.getValue();
					String[] tuple = result.split("x");

					setTileWidth(Integer.parseInt(tuple[0]));
					setTileHeight(Integer.parseInt(tuple[1]));

					updateImageSize();
					buildMapImage();

					redraw();

					saveData();
				}
			}
		};
	}

	private Action createSelectTilesetImageAction() {
		return new Action("Select the tileset image.") {

			{
				setImageDescriptor(EditorSharedImages.getImageDescriptor(IEditorSharedImages.IMG_IMAGE));
			}

			@Override
			public void run() {
				TilemapAssetModel model = getModel();

				if (model == null) {
					return;
				}

				SelectTextureDialog dlg = new SelectTextureDialog(getShell(), "Tileset Image");
				dlg.setContentProvider(new TextureListContentProvider() {
					@Override
					protected boolean acceptAsset(Object assetKey) {
						return assetKey instanceof ImageAssetModel;
					}
				});

				dlg.setProject(model.getPack().getFile().getProject());

				if (dlg.open() == Window.OK) {
					ImageAssetModel image = (ImageAssetModel) dlg.getSelection().getFirstElement();
					setImageModel(image);

					saveData();
				}
			}
		};
	}

	public int getTileWidth() {
		return _tileWidth;
	}

	public void setTileWidth(int tileWidth) {
		_tileWidth = tileWidth;
	}

	public int getTileHeight() {
		return _tileHeight;
	}

	public void setTileHeight(int tileHeight) {
		_tileHeight = tileHeight;
	}

	@Override
	public void mouseMove(MouseEvent e) {
		_mouseX = e.x;
		_mouseY = e.y;

		ZoomCalculator calc = calc();

		_mouseMapX = (int) ((_mouseX - calc.offsetX) / calc.scale / _tileWidth);
		_mouseMapY = (int) ((_mouseY - calc.offsetY) / calc.scale / _tileHeight);

		redraw();
	}

	@Override
	public void mouseDoubleClick(MouseEvent e) {
		if (e.button == 1) {
			selectAllFrames();
		}
	}

	private void selectAllFrames() {
		try {
			boolean selected = _selectedCells.contains(new Point(_mouseMapX, _mouseMapY));
			int[][] map = _model.getCsvData();
			int frame = map[_mouseMapY][_mouseMapX];
			for (int i = 0; i < map.length; i++) {
				int[] row = map[i];
				for (int j = 0; j < row.length; j++) {
					if (frame == map[i][j]) {
						if (selected) {
							_selectedCells.remove(new Point(j, i));
						} else {
							_selectedCells.add(new Point(j, i));
						}
					}
				}
			}
			fireSelectionChanged(new StructuredSelection(_selectedCells));
			redraw();
		} catch (Exception e2) {
			// nothing
		}
	}

	public void selectAllFrames(List<Integer> frames) {
		selectAllFrames(frames, false);
	}

	@SuppressWarnings("boxing")
	public void selectAllFrames(List<Integer> frames, boolean clearSelection) {
		Set<Integer> set = new HashSet<>(frames);
		List<Point> newlist = new ArrayList<>();
		try {
			int[][] map = _model.getCsvData();
			for (int i = 0; i < map.length; i++) {
				int[] row = map[i];
				for (int j = 0; j < row.length; j++) {
					int frame = map[i][j];
					if (set.contains(frame)) {
						newlist.add(new Point(j, i));
					}
				}
			}

			if (clearSelection) {
				_selectedCells.clear();
			}

			for (Point p : newlist) {
				_selectedCells.add(p);
			}

			fireSelectionChanged(new StructuredSelection(_selectedCells));
			redraw();
		} catch (Exception e2) {
			// nothing
		}
	}

	@Override
	public void mouseDown(MouseEvent e) {
		if (e.button == 1) {
			try {
				// just validate the mouse is in range
				@SuppressWarnings("unused")
				int _tmp = getModel().getCsvData()[_mouseMapY][_mouseMapX];

				Point p = new Point(_mouseMapX, _mouseMapY);
				if (_selectedCells.contains(p)) {
					_selectedCells.remove(p);
				} else {
					_selectedCells.add(p);
				}
				fireSelectionChanged(new StructuredSelection(_selectedCells));
				redraw();
			} catch (Exception e2) {
				// nothing
			}
		}
	}

	@Override
	public void mouseUp(MouseEvent e) {
		// nothing
	}

	@Override
	public void keyPressed(KeyEvent e) {
		// nothing
	}

	@Override
	public void keyReleased(KeyEvent e) {
		if (e.keyCode == SWT.ESC) {
			_selectedCells.clear();
			fireSelectionChanged(new StructuredSelection(_selectedCells));
			redraw();
		} else if (e.character == SWT.SPACE) {
			selectAllFrames();
		}
	}

	private ListenerList<ISelectionChangedListener> _listenerList = new ListenerList<>();

	@Override
	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		_listenerList.add(listener);
	}

	@Override
	public ISelection getSelection() {
		return new StructuredSelection(_selectedCells);
	}

	@Override
	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		_listenerList.remove(listener);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void setSelection(ISelection selection) {
		_selectedCells.clear();
		((IStructuredSelection) selection).toList().forEach(o -> _selectedCells.add((Point) o));
		fireSelectionChanged(selection);
		redraw();
	}

	private void fireSelectionChanged(ISelection selection) {
		_listenerList.forEach(l -> l.selectionChanged(new SelectionChangedEvent(this, selection)));
	}

	@Override
	protected boolean hasImage() {
		return _renderImage != null;
	}

}
