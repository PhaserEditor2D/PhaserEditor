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
package phasereditor.canvas.ui.editors.grid.editors;

import java.util.List;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;

import phasereditor.assetpack.ui.preview.TilemapCSVAssetPreviewComp;
import phasereditor.assetpack.ui.preview.TilemapCanvas;
import phasereditor.canvas.core.TilemapSpriteModel;
import phasereditor.ui.ImageCanvas_Zoom_1_1_Action;
import phasereditor.ui.ImageCanvas_Zoom_FitWindow_Action;

/**
 * @author arian
 *
 */
public class SelectTilemapIndexesDialog extends Dialog {
	private TilemapCSVAssetPreviewComp _tilemapCSVAssetPreviewComp;
	private TilemapSpriteModel _model;
	private ToolBar _toolbar;

	public SelectTilemapIndexesDialog(Shell parentShell) {
		super(parentShell);
		setShellStyle(SWT.MAX | SWT.RESIZE);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		GridLayout gridLayout = (GridLayout) container.getLayout();
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;

		_toolbar = new ToolBar(container, SWT.NONE);
		_toolbar.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));

		_tilemapCSVAssetPreviewComp = new TilemapCSVAssetPreviewComp(container, SWT.NONE);
		_tilemapCSVAssetPreviewComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		afterCreateWidgets();

		return container;
	}

	private void afterCreateWidgets() {
		TilemapCanvas canvas = _tilemapCSVAssetPreviewComp.getTilemapCanvas();

		canvas.setTileWidth(_model.getTileWidth());
		canvas.setTileHeight(_model.getTileHeight());
		canvas.setImageModel(_model.getTilesetImage());
		canvas.setModel(_model.getAssetKey());

		canvas.selectAllFrames(_model.getCollisionIndexes());

		ToolBarManager manager = new ToolBarManager(_toolbar);

		manager.add(new ImageCanvas_Zoom_1_1_Action(canvas));
		manager.add(new ImageCanvas_Zoom_FitWindow_Action(canvas));

		manager.update(true);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	@Override
	protected Point getInitialSize() {
		return new Point(581, 416);
	}

	public void setModel(TilemapSpriteModel model) {
		_model = model;
	}

	public List<Integer> getSelection() {
		return _tilemapCSVAssetPreviewComp.getSelectedIndexes();
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Tilemap Indexes Selection");
	}

}
