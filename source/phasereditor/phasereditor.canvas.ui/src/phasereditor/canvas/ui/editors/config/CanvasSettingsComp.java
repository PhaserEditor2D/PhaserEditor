// The MIT License (MIT)
//
// Copyright (c) 2015, 2017 Arian Fornaris
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
package phasereditor.canvas.ui.editors.config;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.dialogs.FilteredTree;

import phasereditor.canvas.core.CanvasModel;
import phasereditor.canvas.ui.editors.ObjectCanvas;
import phasereditor.canvas.ui.editors.grid.PGrid;
import phasereditor.ui.PatternFilter2;

/**
 * @author arian
 *
 */
public class CanvasSettingsComp extends Composite {

	// private GeneralEditorSettingsComp _generalEditorSettingsComp;
	private FilteredTree _outlineTree;
	private PGrid _pGrid;
	private CanvasModel _model;

	/**
	 * Create the composite.
	 * 
	 * @param parent
	 * @param style
	 */
	public CanvasSettingsComp(Composite parent, int style) {
		super(parent, style);

		setLayout(new FillLayout());

		SashForm sashForm = new SashForm(this, SWT.NONE);

		_outlineTree = new FilteredTree(sashForm, SWT.BORDER, new PatternFilter2(), true);
		_pGrid = new PGrid(sashForm, SWT.NONE, false);
		sashForm.setWeights(new int[] { 1, 2 });

		afterCreateWidgets();
	}

	private void afterCreateWidgets() {
		TreeViewer viewer = _outlineTree.getViewer();
		viewer.setLabelProvider(new ConfigurationLabelProvider());
		viewer.setContentProvider(new ConfigurationContentProvider(_pGrid.getViewer()));
		viewer.addSelectionChangedListener(e -> {
			IStructuredSelection sel = (IStructuredSelection) e.getSelection();

			if (sel.isEmpty()) {
				_pGrid.setModel(null);
				return;
			}

			ConfigItem item = (ConfigItem) sel.getFirstElement();
			_pGrid.setModel(item.getGridModel());
			_pGrid.getViewer().expandAll();
		});
	}

	public void setModel(CanvasModel model) {
		_model = model;
		_outlineTree.getViewer().setInput(model);

		Object data = _outlineTree.getViewer().getTree().getItems()[0].getData();
		_outlineTree.getViewer().setSelection(new StructuredSelection(data));

	}

	public CanvasModel getModel() {
		return _model;
	}

	public void setOnChanged(Runnable onChanged) {
		_pGrid.setOnChanged(onChanged);
	}

	@Override
	protected void checkSubclass() {
		//
	}

	public void setCanvas(ObjectCanvas canvas) {
		_pGrid.setCanvas(canvas);
	}
	
	public void refresh() {
		_pGrid.refresh();
	}
}
