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
package phasereditor.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

/**
 * @author arian
 *
 */
public class FilteredTreeCanvas extends Composite {

	private Text _filterText;
	protected ModifyListener _textChanged;
	private TreeCanvas _tree;

	public FilteredTreeCanvas(Composite parent, int style) {
		super(parent, style);

		var layout = new GridLayout(1, false);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		setLayout(layout);

		_filterText = new Text(this, SWT.SINGLE | SWT.BORDER | SWT.SEARCH | SWT.ICON_CANCEL);
		_filterText.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		_filterText.setMessage("type filter text");
		_filterText.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.ARROW_DOWN) {
					getTree().setFocus();
				}
			}
		});

		_textChanged = e -> {
			_tree.filter(_filterText.getText());
		};

		_filterText.addModifyListener(_textChanged);

		_tree = createTree();
		_tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

	}

	protected TreeCanvas createTree() {
		return new TreeCanvas(this, SWT.NONE);
	}

	public Text getTextControl() {
		return _filterText;
	}
	
	public TreeCanvas getTree() {
		return _tree;
	}

	public String getFilterText() {
		return _filterText.getText();
	}

	public void setFilterText(String filterText) {
		_filterText.setText(filterText == null? "" : filterText);
	}

	public FrameCanvasUtils getUtils() {
		return _tree.getUtils();
	}
}
