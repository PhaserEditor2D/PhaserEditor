// The MIT License (MIT)
//
// Copyright (c) 2015, 2019 Arian Fornaris
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
package phasereditor.assetpack.ui.editor;

import static phasereditor.ui.IEditorSharedImages.IMG_TYPE_VARIABLE_OBJ;

import java.util.Arrays;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;

import phasereditor.assetpack.core.AssetType;
import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.TreeArrayContentProvider;

public class AssetTypeDialog extends Dialog {

	private TreeViewer _viewer;
	private AssetType _result;

	public AssetTypeDialog(IShellProvider parentShell) {
		super(parentShell);
		setShellStyle(SWT.DIALOG_TRIM | SWT.RESIZE);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("File Type");
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		var container = super.createDialogArea(parent);

		var tree = new FilteredTree((Composite) container, SWT.SINGLE | SWT.BORDER, new PatternFilter(), true);
		var gd = new GridData();
		gd.heightHint = 400;
		tree.setLayoutData(gd);
		_viewer = tree.getViewer();
		_viewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				var type = (AssetType) element;
				return type.getCapitalName() + " - " + type.name();
			}

			@Override
			public Image getImage(Object element) {
				return EditorSharedImages.getImage(IMG_TYPE_VARIABLE_OBJ);
			}
		});
		_viewer.setContentProvider(new TreeArrayContentProvider());
		_viewer.setInput(Arrays.stream(AssetType.values())

				.filter(v -> AssetType.isTypeSupported(v.name()))

				.toArray()

		);

		_viewer.addDoubleClickListener(e -> okPressed());

		return container;
	}

	@Override
	protected void okPressed() {
		var sel = _viewer.getStructuredSelection();
		var result = (AssetType) sel.getFirstElement();
		if (result == null && _viewer.getTree().getItemCount() > 0) {
			result = (AssetType) _viewer.getTree().getItem(0).getData();
		}
		setResult(result);
		super.okPressed();
	}

	private void setResult(AssetType result) {
		_result = result;
	}

	public AssetType getResult() {
		return _result;
	}
}