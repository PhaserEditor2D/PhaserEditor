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

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.wb.swt.SWTResourceManager;

import phasereditor.canvas.core.CanvasModel;
import phasereditor.canvas.core.EditorSettings;
import phasereditor.canvas.core.WorldModel;
import phasereditor.canvas.ui.editors.grid.CanvasPGrid;
import phasereditor.ui.ZoomCanvas;

/**
 * @author arian
 *
 */
public class ObjectCanvas2 extends ZoomCanvas {

	private static final int X_LABELS_HEIGHT = 25;
	private static final int Y_LABEL_WIDTH = 50;
	private CanvasEditor _editor;
	private EditorSettings _settingsModel;
	private WorldModel _worldModel;
	private CanvasPGrid _pgrid;
	private TreeViewer _outline;
	private SceneRenderer _worldRenderer;

	public ObjectCanvas2(Composite parent, int style) {
		super(parent, style);

		addPaintListener(this);
	}

	public void init(CanvasEditor editor, CanvasModel model, CanvasPGrid grid, TreeViewer outline) {
		_editor = editor;
		_settingsModel = model.getSettings();
		_worldModel = model.getWorld();
		_pgrid = grid;
		_outline = outline;

		_worldRenderer = new SceneRenderer(this);

	}

	@Override
	protected void customPaintControl(PaintEvent e) {
		renderBackground(e);

		var tx = new Transform(getDisplay());
		tx.translate(Y_LABEL_WIDTH, X_LABELS_HEIGHT);

		_worldRenderer.renderWorld(e.gc, tx, _worldModel);

		renderLabels(e);
	}

	private void renderBackground(PaintEvent e) {
		var gc = e.gc;

		var bgColor = SWTResourceManager.getColor(_settingsModel.getBackgroundColor());
		var fgColor = SWTResourceManager.getColor(_settingsModel.getGridColor());

		gc.setBackground(bgColor);
		gc.setForeground(fgColor);

		gc.fillRectangle(0, 0, e.width, e.height);
	}

	private void renderLabels(PaintEvent e) {
		var gc = e.gc;

		gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_DARK_GRAY));
		gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_GRAY));

		// paint labels

		var modelSnapX = 10f;
		var modelSnapY = 10f;

		var calc = calc();
		var modelStartX = calc.viewToModelX(0);
		var modelStartY = calc.viewToModelY(0);

		var modelRight = calc.viewToModelX(e.width);
		var modelBottom = calc.viewToModelY(e.height);

		var e1 = gc.stringExtent(Float.toString(modelStartX));
		var e2 = gc.stringExtent(Float.toString(modelRight));

		gc.fillRectangle(0, 0, e.width, X_LABELS_HEIGHT);
		gc.fillRectangle(0, 0, Y_LABEL_WIDTH, e.height);

		gc.fillRectangle(0, e.height - X_LABELS_HEIGHT, e.width, e.height);
		gc.fillRectangle(e.width - Y_LABEL_WIDTH, 0, e.width, e.height);

		var viewLabelWidth = Math.max(e1.x, e2.x);
		var viewLabelHeight = e1.y + 10;

		var modelLabelWidth = calc.viewToModelWidth(viewLabelWidth);
		var modelLabelHeight = calc.viewToModelWidth(viewLabelHeight);

		if (modelSnapX < modelLabelWidth) {
			modelSnapX = (int) (modelLabelWidth / modelSnapX) * modelSnapX + modelSnapX;
		}

		if (modelSnapY < modelLabelHeight) {
			modelSnapY = (int) (modelLabelHeight / modelSnapY) * modelSnapY + modelSnapY;
		}

		modelStartX = (int) (modelStartX / modelSnapX) * modelSnapX;
		modelStartY = (int) (modelStartY / modelSnapY) * modelSnapY;

		int i = 0;
		while (true) {

			var modelX = modelStartX + i * modelSnapX;

			if (modelX > modelRight) {
				break;
			}

			var viewX = calc.modelToViewX(modelX) + Y_LABEL_WIDTH;

			if (viewX >= Y_LABEL_WIDTH && viewX <= e.width - Y_LABEL_WIDTH) {
				String label = Float.toString(modelX);
				var labelExtent = gc.textExtent(label);

				gc.drawString(label, (int) viewX - labelExtent.x / 2, 0, true);
				gc.drawLine((int) viewX, X_LABELS_HEIGHT - 5, (int) viewX, X_LABELS_HEIGHT);

				gc.drawString(label, (int) viewX - labelExtent.x / 2, e.height - labelExtent.y, true);
				gc.drawLine((int) viewX, e.height - X_LABELS_HEIGHT, (int) viewX, e.height - X_LABELS_HEIGHT + 5);
			}

			i++;
		}

		i = 0;
		while (true) {

			var modelY = modelStartY + i * modelSnapY;

			if (modelY > modelBottom) {
				break;
			}

			var viewY = calc.modelToViewY(modelY) + X_LABELS_HEIGHT;

			if (viewY >= X_LABELS_HEIGHT && viewY <= e.height - X_LABELS_HEIGHT) {

				String label = Float.toString(modelY);
				var labelExtent = gc.textExtent(label);

				gc.drawString(label, Y_LABEL_WIDTH - labelExtent.x - 7, (int) viewY - labelExtent.y / 2, true);
				gc.drawLine(Y_LABEL_WIDTH - 5, (int) viewY, Y_LABEL_WIDTH, (int) viewY);

				gc.drawString(label, e.width - labelExtent.x - 7, (int) viewY - labelExtent.y / 2, true);
				gc.drawLine(e.width - Y_LABEL_WIDTH, (int) viewY, e.width - Y_LABEL_WIDTH + 5, (int) viewY);
			}

			i++;
		}

		gc.setAlpha(100);
		gc.setLineWidth(2);
		gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_WHITE));

		gc.drawLine(Y_LABEL_WIDTH, X_LABELS_HEIGHT, e.width - Y_LABEL_WIDTH, X_LABELS_HEIGHT);
		gc.drawLine(Y_LABEL_WIDTH, e.height - X_LABELS_HEIGHT, e.width - Y_LABEL_WIDTH, e.height - X_LABELS_HEIGHT);

		gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_BLACK));

		gc.drawLine(Y_LABEL_WIDTH, X_LABELS_HEIGHT, Y_LABEL_WIDTH, e.height - X_LABELS_HEIGHT);
		gc.drawLine(e.width - Y_LABEL_WIDTH, X_LABELS_HEIGHT, e.width - Y_LABEL_WIDTH, e.height - X_LABELS_HEIGHT);

		gc.setAlpha(255);
		gc.setLineWidth(1);
	}

	@Override
	protected Point getImageSize() {
		return new Point(1, 1);
	}

}
