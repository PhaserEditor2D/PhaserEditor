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
package phasereditor.assetpack.ui.preview;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;

import phasereditor.assetpack.core.PhysicsAssetModel;
import phasereditor.assetpack.ui.AssetLabelProvider;

public class PhysicsAssetPreviewComp extends Composite {
	private TableViewer _tableViewer;
	private PhysicsAssetModel _model;

	/**
	 * Create the composite.
	 * 
	 * @param parent
	 * @param style
	 */
	public PhysicsAssetPreviewComp(Composite parent, int style) {
		super(parent, style);

		GridLayout gridLayout = new GridLayout(1, false);
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		setLayout(gridLayout);

		Label lblSprites = new Label(this, SWT.NONE);
		lblSprites.setText("sprites");

		_tableViewer = new TableViewer(this, SWT.FULL_SELECTION);
		Table _table = _tableViewer.getTable();
		_table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		_tableViewer.setLabelProvider(AssetLabelProvider.GLOBAL_16);
		_tableViewer.setContentProvider(new ArrayContentProvider());

		afterCreateWidgets();

	}

	private void afterCreateWidgets() {
		//
	}

	public PhysicsAssetModel getModel() {
		return _model;
	}

	public void setModel(PhysicsAssetModel model) {
		_model = model;

		_tableViewer.setInput(model.getSprites());
	}

	public void selectElement(Object element) {
		_tableViewer.setSelection(new StructuredSelection(element));
	}

}
