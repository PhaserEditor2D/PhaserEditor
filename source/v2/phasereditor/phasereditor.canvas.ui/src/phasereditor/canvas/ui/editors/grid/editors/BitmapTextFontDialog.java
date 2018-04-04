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

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;

import phasereditor.assetpack.core.BitmapFontAssetModel;
import phasereditor.assetpack.ui.AssetLabelProvider;
import phasereditor.assetpack.ui.BitmapFontAssetContentProvider;
import phasereditor.assetpack.ui.preview.BitmapFontAssetPreviewComp;

/**
 * @author arian
 *
 */
public class BitmapTextFontDialog extends Dialog {

	private Composite _container;
	private TreeViewer _viewer;
	private BitmapFontAssetModel _result;
	private Object _selectedFont;
	private BitmapFontAssetPreviewComp _bitmapFontAssetPreviewComp;
	private String _initialText;
	private IProject _project;

	/**
	 * Create the dialog.
	 * 
	 * @param parentShell
	 */
	public BitmapTextFontDialog(Shell parentShell) {
		super(parentShell);
		setShellStyle(SWT.BORDER | SWT.MAX | SWT.RESIZE | SWT.TITLE);
	}

	/**
	 * Create contents of the dialog.
	 * 
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		_container = (Composite) super.createDialogArea(parent);
		FillLayout fl_container = new FillLayout(SWT.HORIZONTAL);
		fl_container.marginWidth = 5;
		fl_container.marginHeight = 5;
		_container.setLayout(fl_container);

		afterCreateWidgets();

		return _container;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("BitmapFont Selector");
	}

	private void afterCreateWidgets() {
		PatternFilter filter = new PatternFilter();
		filter.setIncludeLeadingWildcard(true);

		SashForm sashForm = new SashForm(_container, SWT.NONE);
		FilteredTree tree = new FilteredTree(sashForm, SWT.SINGLE | SWT.BORDER, filter, true);
		tree.setQuickSelectionMode(true);
		_viewer = tree.getViewer();

		_bitmapFontAssetPreviewComp = new BitmapFontAssetPreviewComp(sashForm, SWT.NONE);
		_viewer.setContentProvider(new BitmapFontAssetContentProvider());
		_viewer.setLabelProvider(AssetLabelProvider.GLOBAL_48);
		_viewer.setInput(_project);

		_viewer.addDoubleClickListener(new IDoubleClickListener() {

			@Override
			public void doubleClick(DoubleClickEvent event) {
				okPressed();
			}
		});
		_viewer.addSelectionChangedListener(new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				previewSelection();
			}
		});

		_viewer.setSelection(_selectedFont == null ? StructuredSelection.EMPTY : new StructuredSelection(_selectedFont),
				true);

		sashForm.setWeights(new int[] { 1, 1 });
	}

	protected void previewSelection() {
		BitmapFontAssetModel fontAsset = (BitmapFontAssetModel) _viewer.getStructuredSelection().getFirstElement();

		if (fontAsset == null) {
			return;
		}

		_bitmapFontAssetPreviewComp.setModel(fontAsset);

		if (_initialText != null) {
			_bitmapFontAssetPreviewComp.setText(_initialText);
		}
	}

	public void setProject(IProject project) {
		_project = project;
	}

	@Override
	protected void okPressed() {
		IStructuredSelection sel = (IStructuredSelection) _viewer.getSelection();
		_result = sel.isEmpty() ? null : (BitmapFontAssetModel) sel.getFirstElement();
		super.okPressed();
	}

	public String getInitialText() {
		return _initialText;
	}

	public void setInitialText(String initialText) {
		_initialText = initialText;
	}

	public BitmapFontAssetModel getSelectedFont() {
		return _result;
	}

	@Override
	protected Point getInitialSize() {
		return new Point(593, 447);
	}

	public void setSelectedFont(Object font) {
		_selectedFont = font;
	}

}
