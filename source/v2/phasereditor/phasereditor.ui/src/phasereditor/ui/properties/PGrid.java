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
package phasereditor.ui.properties;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.wb.swt.SWTResourceManager;

import phasereditor.ui.PhaserEditorUI;

/**
 * @author arian
 *
 */
public class PGrid extends Composite {

	TreeViewer _treeViewer;
	TreeViewerColumn _colProperty;
	private TreeViewerColumn _colValue;
	Tree _tree;
	protected boolean _mouseDown;
	protected boolean _resizing;
	private FilteredTree _filteredTree;
	private PGridEditingSupport _editSupport;
	private PGridValueLabelProvider _valueLabelProvider;
	private PGridKeyLabelProvider _keyLabelProvider;

	public PGrid(Composite parent, int style) {
		this(parent, style, true);
	}

	public PGrid(Composite parent, int style, boolean supportUndoRedo) {
		super(parent, style);

		setLayout(new FillLayout(SWT.HORIZONTAL));

		_filteredTree = new FilteredTree(this, SWT.FULL_SELECTION | SWT.BORDER, createPatternFilter(), true);
		_treeViewer = _filteredTree.getViewer();
		_tree = _treeViewer.getTree();

		if (!PhaserEditorUI.isMacPlatform()) {
			_tree.setBackground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_BACKGROUND));
		}

		_tree.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent e) {
				updateColumnsLayout();
			}
		});

		_tree.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				if (e.character == SWT.DEL) {
					setPropertyToNull();
				}
			}
		});

		_colProperty = new TreeViewerColumn(_treeViewer, SWT.NONE);
		_keyLabelProvider = createKeyLabelProvider();
		_colProperty.setLabelProvider(_keyLabelProvider);
		TreeColumn trclmnProperty = _colProperty.getColumn();
		trclmnProperty.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent e) {
				updateColumnsLayout();
			}
		});
		trclmnProperty.setWidth(100);
		trclmnProperty.setText("property");

		_colValue = new TreeViewerColumn(_treeViewer, SWT.NONE);
		_editSupport = createEditingSupport(_treeViewer,supportUndoRedo);
		_colValue.setEditingSupport(_editSupport);
		_valueLabelProvider = createValueLabelProvider();
		_colValue.setLabelProvider(_valueLabelProvider);
		TreeColumn trclmnValue = _colValue.getColumn();
		trclmnValue.setWidth(100);
		trclmnValue.setText("value");
		_treeViewer.setContentProvider(new PGridContentProvider());

		afterCreateWidgets();

	}
	
	public PGridValueLabelProvider getValueLabelProvider() {
		return _valueLabelProvider;
	}
	
	public PGridKeyLabelProvider getKeyLabelProvider() {
		return _keyLabelProvider;
	}

	protected PGridKeyLabelProvider createKeyLabelProvider() {
		return new PGridKeyLabelProvider(_treeViewer);
	}

	protected PGridValueLabelProvider createValueLabelProvider() {
		return new PGridValueLabelProvider(getViewer());
	}

	
	@SuppressWarnings("static-method")
	protected PGridEditingSupport createEditingSupport(TreeViewer viewer, boolean supportUndoRedo) {
		return new PGridEditingSupport(viewer, supportUndoRedo);
	}

	public PGridEditingSupport getEditSupport() {
		return _editSupport;
	}
	
	protected void setPropertyToNull() {
		Object elem = _treeViewer.getStructuredSelection().getFirstElement();
		if (elem != null && elem instanceof PGridProperty) {
			try {
				PGridProperty<?> prop = (PGridProperty<?>) elem;
				Object value = prop.getDefaultValue();
				_editSupport.executeChangePropertyValueOperation(value, prop);
			} catch (UnsupportedOperationException e) {
				// OK, it is not supported
			}
			_treeViewer.refresh(elem);
		}
	}

	public void setOnChanged(Runnable onChanged) {
		_editSupport.setOnChanged(onChanged);
	}

	private PatternFilter createPatternFilter() {
		PatternFilter filter = new PatternFilter() {

			@Override
			protected boolean isLeafMatch(Viewer viewer, Object element) {
				String text = getKeyLabelProvider().getText(element);
				return wordMatches(text);
			}

		};
		filter.setIncludeLeadingWildcard(true);
		return filter;
	}

	private void afterCreateWidgets() {
		ColumnViewerToolTipSupport.enableFor(_treeViewer);

		_tree.setLinesVisible(true);
		_tree.addMouseListener(new MouseListener() {

			@Override
			public void mouseUp(MouseEvent e) {
				_mouseDown = false;
				_resizing = false;
			}

			@Override
			public void mouseDown(MouseEvent e) {
				_mouseDown = true;
				_resizing = false;
			}

			@Override
			public void mouseDoubleClick(MouseEvent e) {
				_mouseDown = false;
				_resizing = false;
			}
		});
		_tree.addMouseTrackListener(new MouseTrackListener() {

			@Override
			public void mouseHover(MouseEvent e) {
				// nothing
			}

			@Override
			public void mouseExit(MouseEvent e) {
				_mouseDown = false;
			}

			@Override
			public void mouseEnter(MouseEvent e) {
				_mouseDown = false;
			}
		});

		_tree.addMouseMoveListener(new MouseMoveListener() {

			@Override
			public void mouseMove(MouseEvent e) {
				TreeColumn col0 = _tree.getColumn(0);
				boolean inzone = Math.abs(e.x + _tree.getClientArea().x - col0.getWidth()) < 5;
				Display display = getDisplay();
				if (inzone || _resizing) {
					_tree.setCursor(display.getSystemCursor(SWT.CURSOR_SIZEWE));
					if (_mouseDown) {
						_resizing = true;
						col0.setWidth(e.x);
						updateColumnsLayout();
					} else {
						_resizing = false;
					}
				} else {
					_resizing = false;
					_tree.setCursor(display.getSystemCursor(SWT.CURSOR_ARROW));
				}
			}
		});

//		if (!PhaserEditorUI.isMacPlatform()) {
//			_tree.addListener(SWT.EraseItem, new Listener() {
//
//				@Override
//				public void handleEvent(Event event) {
//					GC gc = event.gc;
//
//					TreeItem item = _tree.getItem(new Point(event.x, event.y));
//					if (item == null) {
//						return;
//					}
//
//					Object element = item.getData();
//					if (PGridLabelProvider.isModified(element)) {
//						RGB rgb = PGridLabelProvider
//								.brighter(PGridLabelProvider.brighter(_tree.getBackground().getRGB()));
//						gc.setBackground(SWTResourceManager.getColor(rgb));
//						gc.fillRectangle(0, event.y, _tree.getClientArea().width, event.height);
//					}
//				}
//			});
//		}
	}

	private PGridModel _model;

	public PGridModel getModel() {
		return _model;
	}

	public TreeViewer getViewer() {
		return _treeViewer;
	}

	public void setModel(PGridModel model) {
		if (model == null) {
			_treeViewer.setInput(null);
			_model = null;
			return;
		}

		if (model == _model) {
			_treeViewer.refresh();
		} else {
			Object[] expanded = _treeViewer.getExpandedElements();
			Object[] selected = ((IStructuredSelection) _treeViewer.getSelection()).toArray();

			_treeViewer.getTree().setRedraw(false);
			try {
				_treeViewer.setInput(model);

				List<Object> toexpand = new ArrayList<>();
				List<Object> toselect = new ArrayList<>();
				for (PGridSection section : model.getSections()) {

					for (Object obj : expanded) {
						if (obj instanceof PGridSection) {
							if (((PGridSection) obj).getName().equals(section.getName())) {
								toexpand.add(section);
							}
						}
					}

					for (PGridProperty<?> prop : section) {
						for (Object sel : selected) {
							if (sel instanceof PGridProperty) {
								String name = ((PGridProperty<?>) sel).getName();
								if (name.equals(prop.getName())) {
									toselect.add(prop);
								}
							}
						}
					}
				}

				_treeViewer.setExpandedElements(toexpand.toArray());
				_treeViewer.setSelection(new StructuredSelection(toselect.toArray()));
				_tree.showSelection();
			} finally {
				_treeViewer.getTree().setRedraw(true);
			}
		}

		// expand it first time
		if (_model == null) {
			_treeViewer.expandAll();
		}

		_model = model;
	}

	protected void updateColumnsLayout() {
		if (_colProperty == null || _colValue == null) {
			return;
		}

		int width = _tree.getClientArea().width;
		int newValueWidth = width - _colProperty.getColumn().getWidth();
		_colValue.getColumn().setWidth(newValueWidth);
	}

	public void refresh(PGridProperty<?> prop) {
		_treeViewer.refresh(prop);
	}

	public void refresh() {
		_treeViewer.refresh();
	}
}