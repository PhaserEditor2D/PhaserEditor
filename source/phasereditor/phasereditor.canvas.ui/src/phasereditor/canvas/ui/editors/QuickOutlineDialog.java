// The MIT License (MIT)
//
// Copyright (c) 2015, 2016 Arian Fornaris
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

import java.util.function.Consumer;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.dialogs.PatternFilter;

/**
 * @author arian
 *
 */
public class QuickOutlineDialog extends PopupDialog {

	protected TreeViewer _viewer;
	private ObjectCanvas _canvas;
	protected Text _filterText;
	private PatternFilter _patternFilter;
	protected Consumer<Object> _resultHandler;
	protected Tree _tree;

	/**
	 * Create the dialog.
	 * 
	 * @param parentShell
	 */
	public QuickOutlineDialog(Shell parentShell) {
		super(parentShell, SWT.RESIZE, true, false, false, true, false, null, "Select an object to focus on");
	}

	/**
	 * Create contents of the dialog.
	 * 
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		_viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		_patternFilter = new PatternFilter();
		_patternFilter.setIncludeLeadingWildcard(true);
		_viewer.setFilters(new ViewerFilter[] { _patternFilter });
		_viewer.setLabelProvider(new OutlineLabelProvider());
		_viewer.setContentProvider(new OutlineContentProvider());
		_viewer.setInput(_canvas);
		_viewer.expandAll();
		_tree = _viewer.getTree();
		_viewer.getTree().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent e) {
				if (!_viewer.getSelection().isEmpty()) {
					handleSelection();
				}
			}
		});
		_viewer.getTree().addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				switch (e.keyCode) {
				case SWT.CR:
				case SWT.KEYPAD_CR:
					handleSelection();
					break;
				case SWT.ARROW_UP:
					TreeItem[] sel = _tree.getSelection();
					if (sel.length == 0) {
						return;
					}
					TreeItem item = sel[0];
					if (item == _tree.getItem(0)) {
						_filterText.setFocus();
					}
					break;

				default:
					break;
				}
			}
		});

		return _viewer.getControl();
	}

	protected void handleSelection() {
		Object elem = _viewer.getStructuredSelection().getFirstElement();
		if (elem == null && _tree.getItemCount() > 0) {
			elem = _tree.getItem(0).getData();
		}
		close();
		_resultHandler.accept(elem);
	}

	@Override
	protected Control getFocusControl() {
		return _filterText;
	}

	@Override
	protected Control createTitleControl(Composite parent) {
		_filterText = new Text(parent, SWT.NONE);

		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(_filterText);

		_filterText.addModifyListener(new ModifyListener() {
			@SuppressWarnings("synthetic-access")
			@Override
			public void modifyText(ModifyEvent e) {
				textChanged();
			}

		});

		_filterText.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				TreeItem[] selection = _tree.getSelection();
				switch (e.keyCode) {
				case SWT.CR:
				case SWT.KEYPAD_CR:
					handleSelection();
					break;
				case SWT.ARROW_DOWN:
					if (_tree.isFocusControl()) {
						break;
					}
					_tree.setFocus();

					if (selection.length > 0) {
						break;
					}
					_tree.setSelection(_tree.getItem(0));
					break;
				default:
					break;
				}
			}
		});

		return _filterText;
	}

	private void textChanged() {
		_patternFilter.setPattern(_filterText.getText());
		_viewer.getControl().setRedraw(false);
		try {
			_viewer.refresh(true);
			_viewer.expandAll();
		} finally {
			_viewer.getControl().setRedraw(true);
		}
	}

	public void setCanvas(ObjectCanvas canvas) {
		_canvas = canvas;
	}

	public void setResultHandler(Consumer<Object> resultHandler) {
		_resultHandler = resultHandler;
	}

	@Override
	protected Point getDefaultSize() {
		GC gc = new GC(getContents());
		FontMetrics fontMetrics = gc.getFontMetrics();
		gc.dispose();
		int x = Dialog.convertHorizontalDLUsToPixels(fontMetrics, 300);
		if (x < 350) {
			x = 350;
		}
		int y = Dialog.convertVerticalDLUsToPixels(fontMetrics, 270);
		if (y < 420) {
			y = 420;
		}
		return new Point(x, y);
	}

	@Override
	protected Point getDefaultLocation(Point initialSize) {
		Point size = new Point(400, 400);
		Rectangle parentBounds = getParentShell().getBounds();
		int x = parentBounds.x + parentBounds.width / 2 - size.x / 2;
		int y = parentBounds.y + parentBounds.height / 2 - size.y / 2;
		return new Point(x, y);
	}
}
