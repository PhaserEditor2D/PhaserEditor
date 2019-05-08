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
package phasereditor.ui;

import static java.lang.System.out;
import static java.util.stream.Collectors.toList;
import static phasereditor.ui.IEditorSharedImages.IMG_BULLET_COLLAPSE;
import static phasereditor.ui.IEditorSharedImages.IMG_BULLET_EXPAND;
import static phasereditor.ui.PhaserEditorUI.isZoomEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPageListener;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

/**
 * @author arian
 *
 */
public class BlocksView extends ViewPart implements IWindowListener, IPageListener, IPartListener {

	class BlocksCanvas extends BaseCanvas implements PaintListener, MouseListener {

		private static final int MIN_ROW_HEIGHT = 48;
		private Map<IEditorBlock, Rectangle> _blockAreaMap;
		private Map<String, Boolean> _blockExpandMap;
		private HandModeUtils _handModeUtils;
		private MyScrollUtils _scrollUtils;
		private int _imageSize = 64;
		private List<IEditorBlock> _blocks;
		private FrameCanvasUtils _frameUtils;

		public BlocksCanvas(Composite parent, int style) {
			super(parent, style | SWT.V_SCROLL);

			addMouseListener(this);
			addListener(SWT.MouseVerticalWheel, this::mouseScrolled);
			addPaintListener(this);

			_blockAreaMap = new HashMap<>();
			_blockExpandMap = new HashMap<>();
			_handModeUtils = new HandModeUtils(this);
			_scrollUtils = new MyScrollUtils();
			_blocks = new ArrayList<>();
			_frameUtils = new MyFrameUtils();
			_frameUtils.setFilterInputWhenSetSelection(false);
		}

		class MyFrameUtils extends FrameCanvasUtils {

			public MyFrameUtils() {
				super(BlocksCanvas.this, true);
			}

			@Override
			public int getFramesCount() {
				return _blocks.size();
			}

			@Override
			public Rectangle getSelectionFrameArea(int index) {
				return _blockAreaMap.get(_blocks.get(index));
			}

			@Override
			public Point viewToModel(int x, int y) {
				return new Point(x, y - _scrollUtils.getOrigin().y);
			}

			@Override
			public Point modelToView(int x, int y) {
				return new Point(x, y + _scrollUtils.getOrigin().y);
			}

			@Override
			public Object getFrameObject(int index) {
				return _blocks.get(index).getObject();
			}

			@Override
			public Image get_DND_Disposable_Image(int index) {
				return _blocks.get(index).get_DND_Image();
			}

			@Override
			public void mouseUp(MouseEvent e) {
				var block = getBlockIfClickAtExpandIcon(e);
				if (block == null) {
					super.mouseUp(e);
				}
			}

		}

		class MyScrollUtils extends ScrollUtils {

			public MyScrollUtils() {
				super(BlocksCanvas.this);
			}

			@Override
			public Rectangle computeScrollArea() {
				return BlocksCanvas.this.computeScrollArea();
			}
		}

		public void mouseScrolled(Event e) {
			if (!isZoomEvent(_handModeUtils, e)) {
				return;
			}

			e.doit = false;

			var before = _imageSize;

			var f = e.count < 0 ? 0.8 : 1.2;

			_imageSize = (int) (_imageSize * f);

			if (_imageSize < MIN_ROW_HEIGHT) {
				_imageSize = MIN_ROW_HEIGHT;
			}

			if (_imageSize != before) {
				_scrollUtils.updateScroll();
			}
		}

		private List<IEditorBlock> expandList(List<IEditorBlock> list) {
			var list1 = new ArrayList<>(list);

			list1.sort((a, b) -> a.getSortName().compareTo(b.getSortName()));

			var list2 = new ArrayList<IEditorBlock>();
			for (var block : list1) {
				list2.add(block);
				if (isExpanded(block) || !block.isTerminal() && _filter != null) {
					list2.addAll(expandList(block.getChildren()));
				}
			}
			return list2;
		}

		private void updateBlockList() {
			if (_provider == null) {
				_blocks = new ArrayList<>();
				return;
			}

			var list = expandList(_provider.getBlocks());

			if (_filter != null) {
				list = list.stream().filter(block -> {
					var test = (block.getLabel() + block.getKeywords()).toLowerCase();
					return test.contains(_filter);
				}).collect(toList());
			}

			_blocks = list;
			
		}

		public Rectangle computeScrollArea() {

			var e = getClientArea();

			var margin = 5;
			var size = _imageSize;
			var x = margin;
			var y = margin;

			for (int i = 0; i < _blocks.size(); i++) {
				if (x + size + margin > e.width) {
					x = margin;
					y += margin + size + 20;
				}
				x += size + margin;
			}

			return new Rectangle(0, 0, e.width, y + margin + size + 20);
		}

		@Override
		public void paintControl(PaintEvent e) {
			var gc = e.gc;

			prepareGC(gc);

			{
				Transform tx = new Transform(getDisplay());
				tx.translate(0, _scrollUtils.getOrigin().y);
				gc.setTransform(tx);
				tx.dispose();
			}

			gc.setBackground(getForeground());

			_blockAreaMap = new HashMap<>();

			var margin = 5;
			var startXMargin = e.width % (_imageSize + margin) / 2;
			var size = _imageSize;
			var x = startXMargin; // + margin;
			var y = margin;

			for (var block : _blocks) {
				var renderer = block.getRenderer();

				if (x + size + margin > e.width) {
					x = startXMargin;
					y += margin + size + 20;
				}

				var terminal = block.isTerminal();

				var rect = new Rectangle(x, y, size, size);
				_blockAreaMap.put(block, rect);

				var selected = _frameUtils.isSelected(block.getObject());

				if (selected) {
					gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION));
				} else {
					gc.setBackground(SwtRM.getColor(block.getColor()));
				}

				if (selected) {
					gc.fillRectangle(rect);
				}

				if (terminal) {
					if (!selected) {
						gc.setAlpha(50);
						gc.fillRectangle(rect);
						gc.setAlpha(255);
					}

					renderer.render(this, gc, x, y, size, size);
				} else {
					var tab = (int) (rect.height * 0.1);
					if (!selected) {
						gc.setAlpha(100);
						gc.fillRectangle(rect.x, rect.y, rect.width / 2, tab);
						gc.fillRectangle(rect.x, rect.y + tab, rect.width, rect.height - tab);
						gc.setAlpha(255);
					}
					renderer.render(this, gc, x, y + tab, size - 10, size - tab);
				}

				gc.setAlpha(100);
				// gc.drawRectangle(rect);
				gc.setAlpha(255);

				if (!terminal) {
					// gc.setAlpha(100);
					// gc.fillRectangle(x + size - 10, y, 10, size);
					//
					// gc.drawRectangle(x + size - 10, y, 10, size);
					// gc.setAlpha(255);

					var expanded = isExpanded(block);
					Image img = EditorSharedImages.getImage(expanded ? IMG_BULLET_COLLAPSE : IMG_BULLET_EXPAND);
					gc.drawImage(img, x + size - 16, y + size / 2 - 8);
				}

				{
					var label = new StringBuilder(block.getLabel());
					while (gc.textExtent(label.toString()).x > _imageSize) {
						label.setLength(label.length() - 1);
					}
					if (label.length() < block.getLabel().length()) {
						if (label.length() > 3) {
							label.setLength(label.length() - 3);
							label.append("...");
						}
					}
					var text = label.toString();
					var ext = gc.textExtent(text);
					gc.drawText(text, x + _imageSize / 2 - ext.x / 2, y + _imageSize + 2, true);
				}

				x += size + margin;
			}
		}

		private boolean isExpanded(IEditorBlock block) {
			var id = block.getId();
			return _blockExpandMap.getOrDefault(id, Boolean.FALSE).booleanValue();
		}

		@Override
		public void mouseDoubleClick(MouseEvent e) {
			//
		}

		@Override
		public void mouseDown(MouseEvent e) {
			//
		}

		@Override
		public void mouseUp(MouseEvent e) {
			var block = getBlockIfClickAtExpandIcon(e);
			if (block != null) {
				var expanded = !isExpanded(block);
				_blockExpandMap.put(block.getId(), Boolean.valueOf(expanded));
				_scrollUtils.updateScroll();
				redraw();
			}
		}

		private IEditorBlock getBlockIfClickAtExpandIcon(MouseEvent e) {
			for (var entry : _blockAreaMap.entrySet()) {
				var rect = entry.getValue();
				var block = entry.getKey();
				if (!block.isTerminal()) {
					var rect2 = getExpandIconRect(rect);
					if (rect2.contains(e.x, e.y - _scrollUtils.getOrigin().y)) {
						return block;
					}
				}
			}
			return null;
		}

		private Rectangle getExpandIconRect(Rectangle rect) {
			return new Rectangle(rect.x + rect.width - 16, rect.y, 16, rect.height);
		}

		public void updateFromProvider() {
			updateBlockList();
			_scrollUtils.updateScroll();

			if (_provider != null) {
				_provider.installTooltips(this, _frameUtils);
			}

			redraw();
		}

		public void filter() {
			updateBlockList();
			_scrollUtils.updateScroll();
		}

	}

	private BlocksCanvas _canvas;
	private IEditorPart _currentEditor;
	private IEditorBlockProvider _provider;
	private Text _filterText;
	private String _filter;

	@Override
	public void createPartControl(Composite parent) {
		var comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new GridLayout(1, false));
		_filterText = new Text(comp, SWT.SINGLE | SWT.BORDER | SWT.SEARCH | SWT.ICON_CANCEL);
		_filterText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		_filterText.setMessage("type filter text");

		_filterText.addModifyListener(e -> {
			var text = _filterText.getText().trim().toLowerCase();
			_filter = text.length() == 0 ? null : text;
			_canvas.filter();
		});

		_canvas = new BlocksCanvas(comp, SWT.NONE);
		_canvas.setLayoutData(new GridData(GridData.FILL_BOTH));

		PlatformUI.getWorkbench().addWindowListener(this);
		var win = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		var page = win.getActivePage();
		win.addPageListener(this);
		page.addPartListener(this);

		updateFromPageChange();

	}

	@Override
	public void dispose() {
		PlatformUI.getWorkbench().removeWindowListener(this);
		for (var win : PlatformUI.getWorkbench().getWorkbenchWindows()) {
			win.removePageListener(this);
			for (var page : win.getPages()) {
				page.removePartListener(this);
			}
		}

		super.dispose();
	}

	private void updateFromPageChange() {
		var editor = getSite().getPage().getActiveEditor();

		if (editor == null) {
			out.println("Disconnect from editor");
		} else {
			if (_currentEditor != editor) {
				out.println("Process editor " + editor.getTitle());
				_provider = editor.getAdapter(IEditorBlockProvider.class);
				_canvas.updateFromProvider();
			}
		}

	}

	@Override
	public void setFocus() {
		_canvas.setFocus();
	}

	@Override
	public void windowClosed(IWorkbenchWindow window) {
		window.removePageListener(this);
	}

	@Override
	public void windowOpened(IWorkbenchWindow window) {
		window.addPageListener(this);
	}

	@Override
	public void pageClosed(IWorkbenchPage page) {
		page.removePartListener(this);
	}

	@Override
	public void pageOpened(IWorkbenchPage page) {
		page.addPartListener(this);
	}

	@Override
	public void partActivated(IWorkbenchPart part) {
		updateFromPageChange();
	}

	@Override
	public void partDeactivated(IWorkbenchPart part) {
		updateFromPageChange();
	}

	@Override
	public void partBroughtToTop(IWorkbenchPart part) {
		//
	}

	@Override
	public void partClosed(IWorkbenchPart part) {
		//
	}

	@Override
	public void partOpened(IWorkbenchPart part) {
		//
	}

	@Override
	public void pageActivated(IWorkbenchPage page) {
		//
	}

	@Override
	public void windowActivated(IWorkbenchWindow window) {
		//
	}

	@Override
	public void windowDeactivated(IWorkbenchWindow window) {
		//
	}

}
