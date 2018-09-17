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
package phasereditor.canvas.ui.editors;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.wb.swt.SWTResourceManager;

import phasereditor.canvas.core.BaseObjectModel;
import phasereditor.canvas.core.CanvasModel;
import phasereditor.canvas.core.EditorSettings;
import phasereditor.canvas.core.GroupModel;
import phasereditor.canvas.core.WorldModel;
import phasereditor.ui.ZoomCanvas;

/**
 * @author arian
 *
 */
public class ObjectCanvas2 extends ZoomCanvas {

	private static final int X_LABELS_HEIGHT = 18;
	private static final int Y_LABEL_WIDTH = 18;
	private CanvasEditor _editor;
	private EditorSettings _settingsModel;
	private WorldModel _worldModel;
	private WorldRenderer _worldRenderer;
	private float _renderModelSnapX;
	private float _renderModelSnapY;
	private List<Object> _selection;

	public ObjectCanvas2(Composite parent, int style) {
		super(parent, style);

		_selection = new ArrayList<>();

		addPaintListener(this);
		addMouseListener(new SelectionMouseListener());
	}

	public void init(CanvasEditor editor, CanvasModel model) {
		_editor = editor;
		_settingsModel = model.getSettings();
		_worldModel = model.getWorld();

		_worldRenderer = new WorldRenderer(this);
	}

	public CanvasEditor getEditor() {
		return _editor;
	}

	public WorldModel getWorldModel() {
		return _worldModel;
	}

	public WorldRenderer getWorldRenderer() {
		return _worldRenderer;
	}

	@Override
	public void dispose() {
		super.dispose();

		_worldRenderer.dispose();
	}

	@Override
	protected void customPaintControl(PaintEvent e) {
		renderBackground(e);

		renderGrid(e);

		var tx = new Transform(getDisplay());
		tx.translate(Y_LABEL_WIDTH, X_LABELS_HEIGHT);

		_worldRenderer.renderWorld(e.gc, tx, _worldModel);

		renderSelection(e.gc);

		renderLabels(e);
	}

	private void renderSelection(GC gc) {
		gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_BLUE));

		for (var obj : _selection) {
			if (obj instanceof BaseObjectModel) {
				var bounds = _worldRenderer.getObjectBounds((BaseObjectModel) obj);
				if (bounds != null) {
					gc.drawPolygon(new int[] { (int) bounds[0], (int) bounds[1], (int) bounds[2], (int) bounds[3],
							(int) bounds[4], (int) bounds[5], (int) bounds[6], (int) bounds[7] });
				}
			}
		}
	}

	private void renderBackground(PaintEvent e) {
		var gc = e.gc;

		var bgColor = SWTResourceManager.getColor(_settingsModel.getBackgroundColor());
		var fgColor = SWTResourceManager.getColor(_settingsModel.getGridColor());

		gc.setBackground(bgColor);
		gc.setForeground(fgColor);

		gc.fillRectangle(0, 0, e.width, e.height);
	}

	private void renderGrid(PaintEvent e) {
		var gc = e.gc;

		gc.setForeground(SWTResourceManager.getColor(_settingsModel.getGridColor()));

		// paint labels

		var calc = calc();

		var initialModelSnapX = 5f;
		var initialModelSnapY = 5f;

		if (_settingsModel.isEnableStepping()) {
			initialModelSnapX = _settingsModel.getStepWidth();
			initialModelSnapY = _settingsModel.getStepHeight();
		}

		var modelSnapX = 10f;
		var modelSnapY = 10f;
		var viewSnapX = 0f;
		var viewSnapY = 0f;

		int i = 1;
		while (viewSnapX < 10) {
			modelSnapX = (float) Math.pow(initialModelSnapX, i);
			viewSnapX = calc.modelToViewWidth(modelSnapX);
			i++;
		}

		_renderModelSnapX = modelSnapX;
		_renderModelSnapY = modelSnapY;

		var modelNextSnapX = modelSnapX * 4;
		var modelNextNextSnapX = modelSnapX * 8;

		i = 1;
		while (viewSnapY < 10) {
			modelSnapY = (float) Math.pow(initialModelSnapY, i);
			viewSnapY = calc.modelToViewHeight(modelSnapY);
			i++;
		}

		var modelNextSnapY = modelSnapY * 4;
		var modelNextNextSnapY = modelSnapY * 8;

		var modelStartX = calc.viewToModelX(0);
		var modelStartY = calc.viewToModelY(0);

		var modelRight = calc.viewToModelX(e.width);
		var modelBottom = calc.viewToModelY(e.height);

		modelStartX = (int) (modelStartX / modelSnapX) * modelSnapX;
		modelStartY = (int) (modelStartY / modelSnapY) * modelSnapY;

		i = 0;
		while (true) {

			var modelX = modelStartX + i * modelSnapX;

			if (modelX > modelRight) {
				break;
			}

			gc.setLineWidth(1);
			if (modelX % modelNextNextSnapX == 0) {
				gc.setAlpha(255);
				gc.setLineWidth(2);
			} else if (modelX % modelNextSnapX == 0) {
				gc.setAlpha(200);
			} else {
				gc.setAlpha(150);
			}

			var viewX = calc.modelToViewX(modelX) + Y_LABEL_WIDTH;

			gc.drawLine((int) viewX, X_LABELS_HEIGHT, (int) viewX, e.height);

			i++;
		}

		gc.setAlpha(255);

		i = 0;
		while (true) {

			var modelY = modelStartY + i * modelSnapY;

			if (modelY > modelBottom) {
				break;
			}

			var viewY = calc.modelToViewY(modelY) + X_LABELS_HEIGHT;

			gc.setLineWidth(1);
			if (modelY % modelNextNextSnapY == 0) {
				gc.setLineWidth(2);
				gc.setAlpha(255);
			} else if (modelY % modelNextSnapY == 0) {
				gc.setAlpha(200);
			} else {
				gc.setAlpha(150);
			}

			gc.drawLine(X_LABELS_HEIGHT, (int) viewY, e.width, (int) viewY);

			i++;
		}

		gc.setAlpha(255);
	}

	private void renderLabels(PaintEvent e) {
		var gc = e.gc;

		gc.setForeground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
		gc.setBackground(SWTResourceManager.getColor(_settingsModel.getBackgroundColor()));

		gc.setAlpha(220);
		gc.fillRectangle(0, 0, e.width, X_LABELS_HEIGHT);
		gc.fillRectangle(0, 0, Y_LABEL_WIDTH, e.height);
		gc.setAlpha(255);

		// paint labels

		var modelSnapX = _renderModelSnapX;
		var modelSnapY = 10f;

		var calc = calc();
		var modelStartX = calc.viewToModelX(0);
		var modelStartY = calc.viewToModelY(0);

		var modelRight = calc.viewToModelX(e.width);
		var modelBottom = calc.viewToModelY(e.height);

		int i;

		i = 2;
		while (true) {
			float viewSnapX = calc.modelToViewWidth(modelSnapX);
			if (viewSnapX > 80) {
				break;
			}
			modelSnapX = _renderModelSnapX * i;
			i++;
		}

		i = 2;
		while (true) {
			float viewSnapY = calc.modelToViewWidth(modelSnapY);
			if (viewSnapY > 80) {
				break;
			}
			modelSnapY = _renderModelSnapY * i;
			i++;
		}

		modelStartX = (int) (modelStartX / modelSnapX) * modelSnapX;
		modelStartY = (int) (modelStartY / modelSnapY) * modelSnapY;

		i = 0;
		while (true) {

			var modelX = modelStartX + i * modelSnapX;

			if (modelX > modelRight) {
				break;
			}

			var viewX = calc.modelToViewX(modelX) + Y_LABEL_WIDTH;

			if (viewX >= Y_LABEL_WIDTH && viewX <= e.width - Y_LABEL_WIDTH) {
				String label = Float.toString(modelX);

				gc.setForeground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
				gc.drawString(label, (int) viewX + 5, 0, true);
				gc.setForeground(SWTResourceManager.getColor(_settingsModel.getGridColor()));
				gc.drawLine((int) viewX, 0, (int) viewX, X_LABELS_HEIGHT);
			}

			i++;
		}

		gc.drawLine(Y_LABEL_WIDTH, X_LABELS_HEIGHT, e.width, X_LABELS_HEIGHT);

		i = 0;
		while (true) {

			var modelY = modelStartY + i * modelSnapY;

			if (modelY > modelBottom) {
				break;
			}

			var viewY = calc.modelToViewY(modelY) + X_LABELS_HEIGHT;

			if (viewY >= X_LABELS_HEIGHT && viewY <= e.height - X_LABELS_HEIGHT) {

				String label = Float.toString(modelY);
				var labelExtent = gc.stringExtent(label);

				var tx = new Transform(getDisplay());

				tx.translate(0, viewY + 5 + labelExtent.x);
				tx.rotate(-90);

				gc.setTransform(tx);

				gc.setForeground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
				gc.drawString(label, 0, 0, true);

				gc.setTransform(null);
				tx.dispose();

				gc.setForeground(SWTResourceManager.getColor(_settingsModel.getGridColor()));
				gc.drawLine(0, (int) viewY, Y_LABEL_WIDTH, (int) viewY);
			}

			i++;
		}

		gc.drawLine(Y_LABEL_WIDTH, X_LABELS_HEIGHT, Y_LABEL_WIDTH, e.height);

		gc.setAlpha(255);
	}

	@Override
	protected Point getImageSize() {
		return new Point(1, 1);
	}

	private class SelectionMouseListener implements MouseListener {

		public SelectionMouseListener() {
		}

		@Override
		public void mouseDoubleClick(MouseEvent e) {
			//
		}

		@Override
		public void mouseDown(MouseEvent e) {
			//
		}

		@Override
		public void mouseUp(MouseEvent e) {
			updateSelectionWithMouseEvent(e);
		}

	}

	BaseObjectModel pickObject(int x, int y) {

		var pick = pickObject(_worldModel, x, y);

		return pick;
	}

	private BaseObjectModel pickObject(BaseObjectModel model, int x, int y) {
		if (model instanceof GroupModel) {
			var groupModel = (GroupModel) model;

			if (groupModel.isEditorClosed() || groupModel.isPrefabInstance()) {

				var polygon = _worldRenderer.getObjectBounds(model);

				if (hitsPolygon(x, y, polygon)) {
					return model;
				}

			} else {

				for (var model2 : groupModel.getChildren()) {
					var pick = pickObject(model2, x, y);
					if (pick != null) {
						return pick;
					}
				}

			}

			return null;
		}

		var polygon = _worldRenderer.getObjectBounds(model);

		if (hitsPolygon(x, y, polygon)) {
			return model;
		}

		return null;
	}

	void updateSelectionWithMouseEvent(MouseEvent e) {
		var fireUpdateSelection = false;

		var pick = pickObject(e.x, e.y);

		if (pick == null) {
			fireUpdateSelection = !_selection.isEmpty();
			_selection = new ArrayList<>();
		} else {
			if ((e.stateMask & SWT.CTRL) != 0) {
				if (_selection.contains(pick)) {
					_selection.remove(pick);
				} else {
					_selection.add(pick);
				}
			} else {
				_selection = new ArrayList<>();
				_selection.add(pick);
			}

			fireUpdateSelection = true;
		}

		if (fireUpdateSelection) {
			redraw();

			_editor.getEditorSite().getSelectionProvider().setSelection(new StructuredSelection(_selection));
		}
	}

	private static boolean hitsPolygon(int x, int y, float[] polygon) {
		if (polygon == null) {
			return false;
		}

		int npoints = polygon.length / 2;

		if (npoints <= 2) {
			return false;
		}

		var xpoints = new int[npoints];
		var ypoints = new int[npoints];

		for (int i = 0; i < npoints; i++) {
			xpoints[i] = (int) polygon[i * 2];
			ypoints[i] = (int) polygon[i * 2 + 1];
		}

		int hits = 0;

		int lastx = xpoints[npoints - 1];
		int lasty = ypoints[npoints - 1];
		int curx, cury;

		// Walk the edges of the polygon
		for (int i = 0; i < npoints; lastx = curx, lasty = cury, i++) {
			curx = xpoints[i];
			cury = ypoints[i];

			if (cury == lasty) {
				continue;
			}

			int leftx;
			if (curx < lastx) {
				if (x >= lastx) {
					continue;
				}
				leftx = curx;
			} else {
				if (x >= curx) {
					continue;
				}
				leftx = lastx;
			}

			double test1, test2;
			if (cury < lasty) {
				if (y < cury || y >= lasty) {
					continue;
				}
				if (x < leftx) {
					hits++;
					continue;
				}
				test1 = x - curx;
				test2 = y - cury;
			} else {
				if (y < lasty || y >= cury) {
					continue;
				}
				if (x < leftx) {
					hits++;
					continue;
				}
				test1 = x - lastx;
				test2 = y - lasty;
			}

			if (test1 < (test2 / (lasty - cury) * (lastx - curx))) {
				hits++;
			}
		}

		return ((hits & 1) != 0);
	}

}
