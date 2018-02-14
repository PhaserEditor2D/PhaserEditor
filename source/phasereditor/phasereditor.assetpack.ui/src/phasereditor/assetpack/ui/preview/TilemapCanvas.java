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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IMemento;
import org.eclipse.wb.swt.SWTResourceManager;
import org.json.JSONObject;
import org.json.JSONTokener;

import phasereditor.assetpack.core.AssetPackCore;
import phasereditor.assetpack.core.ImageAssetModel;
import phasereditor.assetpack.core.TilemapAssetModel;
import phasereditor.assetpack.ui.SelectTextureDialog;
import phasereditor.assetpack.ui.TextureListContentProvider;
import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.IEditorSharedImages;
import phasereditor.ui.ImageCanvas_Zoom_1_1_Action;
import phasereditor.ui.ImageCanvas_Zoom_FitWindow_Action;
import phasereditor.ui.PhaserEditorUI;
import phasereditor.ui.ZoomCanvas;

/**
 * @author arian
 *
 */
public class TilemapCanvas extends ZoomCanvas
		implements MouseMoveListener, MouseListener, KeyListener, ISelectionProvider {

	private static final String MEMENTO_IMAGE_KEY = "phasereditor.assetpack.ui.tilemap.csv.image";
	private static final String MEMENTO_TILE_WIDTH = "phasereditor.assetpack.ui.tilemap.csv.tileWidth";
	private static final String MEMENTO_TILE_HEIGHT = "phasereditor.assetpack.ui.tilemap.csv.tileHeight";

	private TilemapAssetModel _model;
	private Point _imageSize = new Point(1, 1);
	private int _tileWidth = 32;
	private int _tileHeight = 32;
	private Color[] _colors;
	protected ImageAssetModel _imageModel;
	private Image _tileSetImage;
	private Image _renderImage;
	private String _initialImageRef;
	private int _mouseX;
	private int _mouseY;
	private List<Point> _selectedCells;
	private int _mouseMapX;
	private int _mouseMapY;

	public TilemapCanvas(Composite parent, int style) {
		super(parent, style);

		_selectedCells = new ArrayList<Point>() {
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
	}

	public void generateColors(int n) {
		_colors = new Color[n];
		for (int i = 0; i < n; i++) {
			java.awt.Color c = java.awt.Color.getHSBColor((float) i / (float) n, 0.85f, 1.0f);
			_colors[i] = SWTResourceManager.getColor(c.getRed(), c.getGreen(), c.getBlue());
		}
	}

	public void setModel(TilemapAssetModel model) {
		if (model != null) {
			model.buildTilemap();
		}

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

		if (_initialImageRef != null) {
			if (model != null) {
				try {
					JSONObject obj = new JSONObject(new JSONTokener(_initialImageRef));
					Object asset = AssetPackCore.findAssetElement(model.getPack().getFile().getProject(), obj);
					if (asset instanceof ImageAssetModel) {
						_imageModel = (ImageAssetModel) asset;
						buildTilesetImage();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			_initialImageRef = null;
		}

		buildMapImage();

		reset();
	}

	void updateImageSize() {
		if (_model != null) {
			int[][] map = _model.getCsvData();
			if (map[0].length > 0) {
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

		redraw();
	}

	private void buildTilesetImage() {
		Image old = _tileSetImage;

		if (_imageModel == null) {
			_tileSetImage = null;
		} else {
			_tileSetImage = new Image(getDisplay(), _imageModel.getUrlFile().getLocation().toFile().getAbsolutePath());
		}

		if (old != null) {
			old.dispose();
		}
	}

	void buildMapImage() {
		if (_model != null && _tileSetImage != null) {

			int[][] map = _model.getCsvData();

			if (map != null && map.length > 0) {

				Point size = getImageSize();

				ImageData srcData = _tileSetImage.getImageData();
				ImageData data = new ImageData(size.x, size.y, srcData.depth, srcData.palette);
				data.setAlpha(0, 0, 0);
				Arrays.fill(data.alphaData, (byte) 0);

				Image image = new Image(getDisplay(), data);

				GC gc = new GC(image);

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

						Rectangle b = _tileSetImage.getBounds();
						int srcX = frame * _tileWidth % b.width;
						int srcY = frame * _tileWidth / b.width * _tileHeight;

						try {
							gc.drawImage(_tileSetImage, srcX, srcY, _tileWidth, _tileHeight, x, y, w, h);
						} catch (IllegalArgumentException e) {
							gc.drawText("Invalid parameters. Please check the tiles size.", 10, 10);
						}
					}
				}

				Image old = _renderImage;
				_renderImage = image;

				if (old != null) {
					old.dispose();
				}

				_renderImage = image;

				return;
			}
		}
	}

	@Override
	public void dispose() {
		super.dispose();
		if (_renderImage != null) {
			_renderImage.dispose();
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
	public void paintControl(PaintEvent e) {
		GC gc = e.gc;
		gc.setAdvanced(true);

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

							if (_tileSetImage == null) {
								// paint map with colors
								Color c = _colors[frame % _colors.length];
								gc.setBackground(c);
								gc.fillRectangle(x, y, w + 1, h + 1);
							}
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
					int fw = fm.getAverageCharWidth() + 3;

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

	public void initState(IMemento memento) {
		_initialImageRef = memento.getString(MEMENTO_IMAGE_KEY);

		{
			Integer value = memento.getInteger(MEMENTO_TILE_WIDTH);
			_tileWidth = value == null ? _tileWidth : value.intValue();
		}

		{
			Integer value = memento.getInteger(MEMENTO_TILE_HEIGHT);
			_tileHeight = value == null ? _tileHeight : value.intValue();
		}

		if (_initialImageRef != null) {
			setModel(_model);
		}
	}

	public void saveState(IMemento memento) {
		if (_imageModel != null) {
			JSONObject ref = AssetPackCore.getAssetJSONReference(_imageModel);
			memento.putString(MEMENTO_IMAGE_KEY, ref.toString());
		}

		memento.putInteger(MEMENTO_TILE_WIDTH, _tileWidth);
		memento.putInteger(MEMENTO_TILE_HEIGHT, _tileHeight);
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
		} else if (e.character == ' ') {
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

}
