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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.wb.swt.SWTResourceManager;

import phasereditor.assetpack.core.ImageAssetModel;
import phasereditor.assetpack.core.TilemapAssetModel;
import phasereditor.assetpack.ui.SelectTextureDialog;
import phasereditor.assetpack.ui.TextureListContentProvider;
import phasereditor.ui.ImageCanvas_Zoom_1_1_Action;
import phasereditor.ui.ImageCanvas_Zoom_FitWindow_Action;
import phasereditor.ui.ZoomCanvas;

/**
 * @author arian
 *
 */
public class TilemapCSVAssetPreviewComp extends ZoomCanvas {

	private TilemapAssetModel _model;
	private Point _imageSize = new Point(1, 1);
	private int _tileWidth = 16;
	private int _tileHeight = 16;
	private Color[] _colors;
	protected ImageAssetModel _imageModel;
	private Image _tileSetImage;
	private Image _renderImage;

	public TilemapCSVAssetPreviewComp(Composite parent, int style) {
		super(parent, style);

		addPaintListener(this);
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
			if (map[0].length > 0) {
				_imageSize = new Point(map[0].length * getTileWidth(), map.length * getTileHeight());
			} else {
				_imageSize = new Point(1, 1);
			}
		}

		buildMapImage();

		redraw();
		// reset();
	}

	public void setImageModel(ImageAssetModel imageModel) {
		_imageModel = imageModel;

		Image old = _tileSetImage;

		if (_imageModel == null) {
			_tileSetImage = null;
		} else {
			_tileSetImage = new Image(getDisplay(), _imageModel.getUrlFile().getLocation().toFile().getAbsolutePath());
		}

		if (old != null) {
			old.dispose();
		}

		buildMapImage();

		redraw();
	}

	void buildMapImage() {
		if (_model != null && _tileSetImage != null) {

			int[][] map = _model.getCsvData();

			if (map != null && map.length > 0) {

				Point size = getImageSize();

				ImageData srcData = _tileSetImage.getImageData();
				ImageData data = new ImageData(size.x, size.y, srcData.depth, srcData.palette);

				Image image = new Image(getDisplay(), data);

				GC gc = new GC(image);

				for (int i = 0; i < map.length; i++) {
					int[] row = map[i];

					for (int j = 0; j < row.length; j++) {

						int frame = map[i][j];

						int w = getTileWidth();
						int h = getTileHeight();
						int x = j * getTileWidth();
						int y = i * getTileHeight();

						Rectangle b = _tileSetImage.getBounds();
						int srcX = frame * _tileWidth % b.width;
						int srcY = frame * _tileWidth / b.width * _tileHeight;

						gc.drawImage(_tileSetImage, srcX, srcY, _tileWidth, _tileHeight, x, y, w, h);
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

				if (_renderImage == null) {

					for (int i = 0; i < map.length; i++) {
						int[] row = map[i];

						for (int j = 0; j < row.length; j++) {

							int frame = map[i][j];

							int w = (int) (getTileWidth() * calc.scale);
							int h = (int) (getTileHeight() * calc.scale);
							int x = (int) (j * getTileWidth() * calc.scale + calc.offsetX);
							int y = (int) (i * getTileHeight() * calc.scale + calc.offsetY);

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
					gc.drawImage(_renderImage, 0, 0, b.width, b.height, (int) calc.offsetX, (int) calc.offsetY,
							(int) (b.width * calc.scale), (int) (b.height * calc.scale));
				}

				return;
			}
		}

		gc.drawText("Missing map data", 10, 10);
	}

	public void createToolBar(IToolBarManager toolbar) {
		toolbar.add(new ImageCanvas_Zoom_1_1_Action(this));
		toolbar.add(new ImageCanvas_Zoom_FitWindow_Action(this));
		toolbar.add(new Action(getTileWidth() + "x" + getTileHeight()) {
			@Override
			public void run() {
				InputDialog dlg = new InputDialog(getShell(), "Tile Size", "Set the tile size (eg. 16x16)",
						getTileWidth() + "x" + getTileHeight(), new IInputValidator() {

							@Override
							public String isValid(String newText) {
								return null;
							}
						});
				if (dlg.open() == Window.OK) {
					String result = dlg.getValue();
					String[] tuple = result.split("x");
					setTileWidth(Integer.parseInt(tuple[0]));
					setTileHeight(Integer.parseInt(tuple[1]));
					buildMapImage();
					redraw();
				}
			}
		});
		toolbar.add(new Action("T") {
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
		});
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

}
