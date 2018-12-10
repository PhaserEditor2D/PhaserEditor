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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import phasereditor.assetpack.core.TilemapAssetModel;
import phasereditor.ui.RM;

/**
 * @author arian
 *
 */
public class TilemapCSVAssetPreviewComp extends Composite implements ISelectionChangedListener {
	Text _selectedFramesText;
	private TilemapCanvas _tilemapCanvas;
	// 54,55,56,57,59,60,61,62,63,65,66,69,70,75,104,105,108
	private List<Integer> _selectedIndexes;

	/**
	 * Create the composite.
	 * 
	 * @param parent
	 * @param style
	 */
	public TilemapCSVAssetPreviewComp(Composite parent, int style) {
		super(parent, style);
		setLayout(new GridLayout(1, false));

		_tilemapCanvas = new TilemapCanvas(this, SWT.BORDER);
		_tilemapCanvas.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		Composite composite = new Composite(this, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		composite.setLayout(new GridLayout(4, false));

		_selectedFramesText = new Text(composite, SWT.BORDER);
		_selectedFramesText.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				if (e.character == '\r' || e.character == '\n') {
					loadSelectedFramesFromText();
				}
			}
		});
		_selectedFramesText.setToolTipText("The selected tile indexes, sepparated by a colon.");
		_selectedFramesText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Button btnNewButton_1 = new Button(composite, SWT.NONE);
		btnNewButton_1.setToolTipText("Copy tile indexes.");
		btnNewButton_1.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				_selectedFramesText.selectAll();
				_selectedFramesText.copy();
				_selectedFramesText.setSelection(0, 0);
			}
		});
		btnNewButton_1.setImage(RM.getPluginImage("org.eclipse.ui", "/icons/full/etool16/copy_edit.png"));

		Button btnNewButton_2 = new Button(composite, SWT.NONE);
		btnNewButton_2.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				_selectedFramesText.paste();
			}
		});
		btnNewButton_2.setToolTipText("Paste tile indexes.");
		btnNewButton_2.setImage(RM.getPluginImage("org.eclipse.ui", "/icons/full/etool16/paste_edit.png"));

		Button btnNewButton = new Button(composite, SWT.NONE);
		btnNewButton.setToolTipText("Select all entered tile indexes in the map.");
		btnNewButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				loadSelectedFramesFromText();
			}
		});
		btnNewButton.setImage(RM.getPluginImage("phasereditor.ui", "icons/accept.png"));

		afterCreateWidgets();

	}

	private void afterCreateWidgets() {
		_tilemapCanvas.addSelectionChangedListener(this);
	}

	@SuppressWarnings("boxing")
	protected void loadSelectedFramesFromText() {
		String text = _selectedFramesText.getText();
		try {
			List<Integer> frames;
			if (text.trim().length() == 0) {
				frames = new ArrayList<>();
			} else {
				frames = Arrays.stream(text.split(",")).map(t -> Integer.parseInt(t)).collect(Collectors.toList());
			}
			_tilemapCanvas.selectAllFrames(frames, true);
		} catch (Exception e) {
			// nothing
		}
	}

	public TilemapCanvas getTilemapCanvas() {
		return _tilemapCanvas;
	}

	public List<Integer> getSelectedIndexes() {
		return _selectedIndexes;
	}

	public void setModel(TilemapAssetModel model) {
		_tilemapCanvas.setModel(model);
	}

	public void fillToolBar(IToolBarManager toolbar) {
		_tilemapCanvas.createToolBar(toolbar);
	}

	@SuppressWarnings("boxing")
	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		List<Point> sel = _tilemapCanvas.getSelectedCells();
		_selectedIndexes = sel.stream().map(p -> _tilemapCanvas.getModel().getCsvData()[p.y][p.x]).distinct().sorted()
				.collect(Collectors.toList());
		String text = _selectedIndexes.stream().map(o -> o.toString()).collect(Collectors.joining(","));
		_selectedFramesText.setText(text);

	}

}
