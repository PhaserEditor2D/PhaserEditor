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

import static java.util.stream.Collectors.toList;
import static phasereditor.ui.PhaserEditorUI.swtRun;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ScrollBar;
import org.json.JSONObject;

/**
 * @author arian
 *
 */
@SuppressWarnings({ "boxing" })
public class TreeCanvas extends BaseImageCanvas implements PaintListener, MouseWheelListener {
	public static final int ACTION_SPACE = 2;
	public static final int ACTION_PADDING = 2;
	public static final int MIN_ROW_HEIGHT = 20;

	private List<TreeCanvasItem> _roots;
	private List<TreeCanvasItem> _visibleItems;
	private List<TreeCanvasItem> _items;
	private int _indentSize;
	private int _imageSize;
	private int _fullHeight;
	private int _origin;
	private boolean _updateScroll;
	private FrameCanvasUtils _utils;
	protected TreeCanvasItemAction _overAction;
	private String _filterText;
	private HashSet<TreeCanvasItem> _filteredItems;
	private boolean _revealSelection;
	private boolean _showCheckbox;

	public TreeCanvas(Composite parent, int style) {
		super(parent, style | SWT.V_SCROLL | SWT.INHERIT_FORCE);

		_indentSize = 16;
		_imageSize = 48;

		_showCheckbox = false;

		_roots = List.of();
		_visibleItems = List.of();
		_items = List.of();
		_filteredItems = new HashSet<>();

		addPaintListener(this);
		addMouseWheelListener(this);

		_origin = 0;

		final ScrollBar vBar = getVerticalBar();
		vBar.addListener(SWT.Selection, e -> {
			_origin = -vBar.getSelection();
			redraw();
		});

		addListener(SWT.Resize, e -> {
			requestUpdateScroll();
		});

		_utils = new FrameCanvasUtils(this, true) {

			@Override
			public Point viewToModel(int x, int y) {
				return new Point(x, y - _origin);
			}

			@Override
			public Point modelToView(int x, int y) {
				return new Point(x, y + _origin);
			}

			@Override
			public boolean isInformationControlValidPosition(int index, int x, int y) {
				var item = _visibleItems.get(index);

				if (item._toggleHitArea != null && item._toggleHitArea.contains(x, y)) {
					return false;
				}

				if (item._checkHitArea != null && item._checkHitArea.contains(x, y)) {
					return false;
				}

				for (var action : item.getActions()) {
					if (action._hitArea != null && action._hitArea.contains(x, y)) {
						return false;
					}
				}

				return true;
			}

			@Override
			public Rectangle getSelectionFrameArea(int index) {
				var item = _visibleItems.get(index);
				var b = getBounds();
				return new Rectangle(0, item._y, b.width, item._rowHeight);
			}

			@Override
			public Rectangle get_DND_Image_SrcFrame(int index) {
				var item = _visibleItems.get(index);

				var renderer = item.getRenderer();

				FrameData fd = renderer.get_DND_Image_FrameData();
				if (fd != null) {
					return fd.src;
				}
				return null;
			}

			@Override
			public Image get_DND_Image(int index) {
				var item = _visibleItems.get(index);

				var renderer = item.getRenderer();

				return renderer.get_DND_Image();
			}

			@Override
			public int getFramesCount() {
				return _visibleItems.size();
			}

			@Override
			public Object getFrameObject(int index) {
				var item = _visibleItems.get(index);
				return item._data;
			}

			@Override
			public void mouseMove(MouseEvent e) {
				if (getToolTipText() != null) {
					setToolTipText(null);
				}

				_overAction = null;

				int index = getOverIndex();

				if (index != -1) {

					var item = _visibleItems.get(index);

					var modelPoint = _utils.viewToModel(e.x, e.y);

					for (var action : item.getActions()) {

						if (action._hitArea != null && action._hitArea.contains(modelPoint)) {

							if (_overAction != action) {
								_overAction = action;
								String tooltip = action.getTooltip();
								if (tooltip == null) {
									tooltip = "";
								}

								if (!tooltip.equals(getToolTipText())) {
									setToolTipText(tooltip);
								}

								redraw();
							}

							break;
						}

					}
				}

				super.mouseMove(e);
			}

			@Override
			public void mouseUp(MouseEvent e) {

				int index = getOverIndex();

				if (index != -1) {

					var item = _visibleItems.get(index);

					var modelPoint = _utils.viewToModel(e.x, e.y);

					if (item._toggleHitArea != null) {

						if (item._toggleHitArea.contains(modelPoint)) {
							toggleItem(item);

							return;
						}
					}

					if (item._checkHitArea != null) {

						if (item._checkHitArea.contains(modelPoint)) {
							item._checked = !item._checked;
							redraw();
							return;
						}
					}

					if (_overAction != null) {
						_overAction.run(e);
					}
				}

				super.mouseUp(e);
			}

			@Override
			protected void shiftSelection(int dir) {
				super.shiftSelection(dir);

				_revealSelection = true;
			}
		};
	}

	@Override
	public void paintControl(PaintEvent e) {

		_fullHeight = 0;

		var gc = e.gc;

		prepareGC(gc);

		{
			int selectionStart = -1;
			int selectionHeight = -1;

			int i = 0;
			int y = 0;

			for (var item : _visibleItems) {

				BaseTreeCanvasItemRenderer renderer = item.getRenderer();

				int rowHeight = renderer.computeRowHeight(this);

				boolean selected = _utils.isSelectedIndex(i);

				if (selected) {
					selectionStart = y;
					selectionHeight = rowHeight;
				}

				item._y = y;
				item._rowHeight = rowHeight;

				y += rowHeight;
				i++;
			}

			_fullHeight = y;

			if (_revealSelection) {
				performRevealSelection(selectionStart, selectionHeight);
			} else {

				if (_updateScroll) {
					_updateScroll = false;
					updateScrollNow();
				}

			}
		}

		Transform tx = new Transform(getDisplay());
		tx.translate(0, _origin);
		gc.setTransform(tx);

		int i = 0;
		int y = 0;

		for (var item : _visibleItems) {

			BaseTreeCanvasItemRenderer renderer;

			renderer = item.getRenderer();

			int x = item.getDepth() * _indentSize;
			int collapseIconX = x;
			int checkboxIconX = collapseIconX + 2 + 16;

			if (_showCheckbox) {
				x = checkboxIconX + 16 + 5;
			} else {
				x = collapseIconX + 16 + 5;
			}

			int rowHeight = renderer.computeRowHeight(this);

			// paint background

			{
				boolean selected = _utils.isSelectedIndex(i);

				gc.setForeground(selected ? PhaserEditorUI.getListSelectionTextColor() : getForeground());

				if (selected) {
					gc.setBackground(PhaserEditorUI.getListSelectionColor());
					gc.fillRectangle(0, y, e.width, rowHeight);
				}

//				PhaserEditorUI.paintListItemBackground(gc, i, new Rectangle(0, y, e.width, rowHeight));
			}

			// render item

			renderer.render(e, i, x, y);

			// paint toogle

			if (item.hasChildren()) {
				var path = item.isExpanded() ? IEditorSharedImages.IMG_BULLET_COLLAPSE
						: IEditorSharedImages.IMG_BULLET_EXPAND;
				var icon = EditorSharedImages.getImage(path);
				Rectangle b = icon.getBounds();
				item._toggleHitArea = new Rectangle(collapseIconX + (8 - b.width / 2), y + (rowHeight - b.height) / 2,
						16, 16);
				gc.drawImage(icon, item._toggleHitArea.x, item._toggleHitArea.y);

			}

			// paint checkbox

			if (_showCheckbox) {
				var path = item.isChecked() ? IEditorSharedImages.IMG_CHECK_ON : IEditorSharedImages.IMG_CHECK_OFF;
				var icon = EditorSharedImages.getImage(path);
				Rectangle b = icon.getBounds();
				item._checkHitArea = new Rectangle(checkboxIconX + (8 - b.width / 2), y + (rowHeight - b.height) / 2,
						16, 16);
				gc.drawImage(icon, item._checkHitArea.x, item._checkHitArea.y);
			}

			// paint actions

			{
				var actions = item.getActions();

				int actionX = e.width;

				for (int j = 0; j < actions.size(); j++) {
					var action = actions.get(j);

					Image img = action.getImage();

					if (img != null) {

						int btnSize = 16 + ACTION_PADDING * 2;

						actionX -= btnSize + ACTION_SPACE;

						Rectangle btnArea = new Rectangle(actionX, y + 2, btnSize, btnSize);

						action._hitArea = btnArea;

						if (action == _overAction) {
							gc.setAlpha(20);
							gc.setBackground(getForeground());
							gc.fillRoundRectangle(btnArea.x, btnArea.y, btnArea.width, btnArea.height, btnSize / 2,
									btnSize / 2);
							gc.setAlpha(40);
							gc.drawRoundRectangle(btnArea.x, btnArea.y, btnArea.width, btnArea.height, btnSize / 2,
									btnSize / 2);
							gc.setAlpha(255);
						} else {
							// gc.setAlpha(120);
						}

						gc.drawImage(img, btnArea.x + ACTION_PADDING, btnArea.y + ACTION_PADDING);

						gc.setAlpha(255);
					}
				}
			}

			// paint droping line
			{
				int dropIndex = _utils.getDropIndex();

				if (dropIndex == i) {

					int loc = _utils.getDropLocation();

					gc.setForeground(getForeground());
					gc.setLineWidth(2);

					if (loc == TreeCanvasDropAdapter.LOCATION_BEFORE || loc == TreeCanvasDropAdapter.LOCATION_ON) {
						gc.drawLine(0, y, e.width, y);
					}

					if (loc == TreeCanvasDropAdapter.LOCATION_AFTER || loc == TreeCanvasDropAdapter.LOCATION_ON) {
						gc.drawLine(0, y + rowHeight, e.width, y + rowHeight);
					}

					gc.setLineWidth(1);
				}
			}

			y += rowHeight;
			i++;
		}

		tx.dispose();

	}

	private void performRevealSelection(int selectionStart, int selectionHeight) {

		_revealSelection = false;
		var viewPoint = _utils.modelToView(0, selectionStart);
		var r = getClientArea();

		if (viewPoint.y < r.y) {
			scrollTo(selectionStart);
			redraw();
		} else if (viewPoint.y + selectionHeight > r.height) {
			scrollTo(selectionStart - r.height + selectionHeight);
			redraw();
		}
	}

	public void selectAll() {
		_utils.selectAll();
	}

	public boolean isShowCheckbox() {
		return _showCheckbox;
	}

	public void setShowCheckbox(boolean showCheckbox) {
		_showCheckbox = showCheckbox;
	}

	public int getIndentSize() {
		return _indentSize;
	}

	public void setIndentSize(int indentSize) {
		_indentSize = indentSize;
	}

	public int getImageSize() {
		return _imageSize;
	}

	public void setImageSize(int imageSize) {
		_imageSize = imageSize;
	}
	
	public int getOrigin() {
		return _origin;
	}

	public List<TreeCanvasItem> getRoots() {
		return _roots;
	}

	public FrameCanvasUtils getUtils() {
		return _utils;
	}

	public void setRoots(List<TreeCanvasItem> roots) {
		_roots = roots;

		updateItemsList();

		if (_filterText != null) {
			filter(_filterText);
		} else {
			updateVisibleItemsList();
		}
	}

	public List<TreeCanvasItem> getVisibleItems() {
		return _visibleItems;
	}

	public List<TreeCanvasItem> getItems() {
		return _items;
	}

	public List<TreeCanvasItem> getExpandedItems() {
		return _items.stream().filter(i -> i.isExpanded()).collect(toList());
	}

	public List<Integer> getExpandedIndexes() {
		return _items.stream().filter(i -> i.isExpanded()).map(i -> i.getIndex()).collect(toList());
	}

	public void setExpandedIndexes(List<Integer> indexes) {
		var set = new HashSet<>(indexes);

		for (var item : _items) {
			item.setExpanded(set.contains(item.getIndex()));
		}

		updateVisibleItemsList();
	}

	public void setExpandedObjects(List<Object> objects) {
		var set = new HashSet<>(objects);
		for (var item : _items) {
			var expanded = item.getData() != null && set.contains(item.getData());
			item.setExpanded(expanded);
		}

		updateVisibleItemsList();
	}

	public List<Object> getExpandedObjects() {
		return _items.stream().filter(i -> i.isExpanded()).map(i -> i.getData()).collect(toList());
	}

	private void updateItemsList() {
		_items = new ArrayList<>();

		updateItemsList(null, _roots);

	}

	private void updateItemsList(TreeCanvasItem parent, List<TreeCanvasItem> items) {
		for (var item : items) {
			item._index = _items.size();
			item._parent = parent;
			_items.add(item);
			updateItemsList(item, item.getChildren());
		}
	}

	private void updateVisibleItemsList() {
		_visibleItems = new ArrayList<>();

		for (var item : _roots) {
			updateVisibleItemsList_rec(item, 0);
		}

		swtRun(this::requestUpdateScroll);
	}

	private void updateVisibleItemsList_rec(TreeCanvasItem item, int depth) {
		if (_filteredItems.contains(item)) {
			return;
		}

		item._depth = depth;

		_visibleItems.add(item);

		if (item.isExpanded()) {
			for (var item2 : item.getChildren()) {
				updateVisibleItemsList_rec(item2, depth + 1);
			}
		}
	}

	@Override
	public void mouseScrolled(MouseEvent e) {
		if ((e.stateMask & SWT.SHIFT) == 0) {
			return;
		}

		double f = e.count < 0 ? 0.8 : 1.2;
		_imageSize = (int) (_imageSize * f);
		if (_imageSize < MIN_ROW_HEIGHT) {
			_imageSize = MIN_ROW_HEIGHT;
		}

		requestUpdateScroll();

	}

	void requestUpdateScroll() {
		_updateScroll = true;
		redraw();
	}

	void updateScrollNow() {
		Rectangle client = getClientArea();
		ScrollBar vBar = getVerticalBar();
		int vSelection = vBar.getSelection();
		vBar.setMaximum(_fullHeight);
		vBar.setThumb(Math.min(_fullHeight, client.height));
		int vPage = _fullHeight - client.height;
		if (vSelection >= vPage) {
			if (vPage <= 0)
				vSelection = 0;
			_origin = -vSelection;
		}

		vBar.setVisible(_fullHeight > client.height);
	}

	private void scrollTo(int y) {

		var vBar = getVerticalBar();

		int thumb = vBar.getThumb();

		int sel = y;

		if (sel < 0) {
			sel = 0;
		} else if (sel > _fullHeight - thumb) {
			sel = _fullHeight - thumb;
		}

		vBar.setSelection(sel);

		_origin = -sel;
	}

	public static class TreeCanvasItemAction {

		private Image _image;
		private String _tooltip;
		Rectangle _hitArea;

		public TreeCanvasItemAction() {
		}

		public TreeCanvasItemAction(Image image, String tooltip) {
			super();
			_image = image;
			_tooltip = tooltip;
		}

		public String getTooltip() {
			return _tooltip;
		}

		public void setTooltip(String tooltip) {
			_tooltip = tooltip;
		}

		public Image getImage() {
			return _image;
		}

		public void setImage(Image image) {
			_image = image;
		}

		@SuppressWarnings("unused")
		public void run(MouseEvent event) {
			// nothing
		}
	}

	public static class TreeCanvasItem {
		protected boolean _checked;
		public Rectangle _checkHitArea;
		public Rectangle _toggleHitArea;
		private Object _data;
		private List<TreeCanvasItem> _children;
		private List<TreeCanvasItemAction> _actions;
		private String _label;
		private String _keywords;
		private boolean _header;
		private boolean _parentByNature;
		int _depth;
		int _index;
		private boolean _expanded;
		int _y;
		int _rowHeight;
		private BaseTreeCanvasItemRenderer _renderer;
		TreeCanvasItem _parent;
		private float _alpha;
		private TreeCanvas _canvas;

		public TreeCanvasItem(TreeCanvas canvas) {
			_children = new ArrayList<>();
			_actions = new ArrayList<>();
			_alpha = 1;
			_canvas = canvas;
			_renderer = new IconTreeCanvasItemRenderer(this, null);
		}

		public boolean isChecked() {
			return _checked;
		}

		public void setChecked(boolean checked) {
			_checked = checked;
		}

		public TreeCanvas getCanvas() {
			return _canvas;
		}

		public float getAlpha() {
			return _alpha;
		}

		public void setAlpha(float alpha) {
			_alpha = alpha;
		}

		public TreeCanvasItem getParent() {
			return _parent;
		}

		public BaseTreeCanvasItemRenderer getRenderer() {
			return _renderer;
		}

		public void setRenderer(BaseTreeCanvasItemRenderer renderer) {
			_renderer = renderer;
		}

		public String getKeywords() {
			return _keywords;
		}

		public void setKeywords(String keywords) {
			_keywords = keywords;
		}

		public int getIndex() {
			return _index;
		}

		public boolean isParentByNature() {
			return _parentByNature;
		}

		public void setParentByNature(boolean parentByNature) {
			_parentByNature = parentByNature;
		}

		public boolean isHeader() {
			return _header;
		}

		public void setHeader(boolean header) {
			_header = header;
		}

		public List<TreeCanvasItemAction> getActions() {
			return _actions;
		}

		public void setActions(List<TreeCanvasItemAction> actions) {
			_actions = actions;
		}

		public void add(TreeCanvasItem item) {
			_children.add(item);
		}

		public int getDepth() {
			return _depth;
		}

		public boolean isExpanded() {
			return _expanded;
		}

		public void setExpanded(boolean expanded) {
			_expanded = expanded;
		}

		public String getLabel() {
			return _label;
		}

		public void setLabel(String label) {
			_label = label;
		}

		public Object getData() {
			return _data;
		}

		public void setData(Object data) {
			_data = data;
		}

		public List<TreeCanvasItem> getChildren() {
			return _children;
		}

		public boolean hasChildren() {
			return !_children.isEmpty();
		}
	}

	public void expandAll() {

		for (var item : _roots) {
			expandAll(item);
		}

		updateVisibleItemsList();

		redraw();
	}

	private void expandAll(TreeCanvasItem item) {

		item.setExpanded(true);

		for (var item2 : item.getChildren()) {
			expandAll(item2);
		}
	}

	public void filter(String text) {
		List<TreeCanvasItem> expanded = null;
		if (text == null || text.equals("")) {
			_filterText = null;
			expanded = getExpandedItems();
		} else {
			_filterText = text.toLowerCase();
		}

		performFilter();

		if (expanded != null) {
			setExpandedItems(expanded);
		}

		updateVisibleItemsList();
	}

	public String getFilterText() {
		return _filterText;
	}

	public void setExpandedItems(List<TreeCanvasItem> expanded) {
		for (var item : _items) {
			item.setExpanded(false);
		}
		for (var item : expanded) {
			item.setExpanded(true);
		}
	}

	public void collapseAll() {
		for (var item : _items) {
			item.setExpanded(false);
		}

		updateVisibleItemsList();
	}

	private void performFilter() {
		_filteredItems = new HashSet<>();

		for (var item : _roots) {
			performFilter(item);
		}
	}

	private boolean performFilter(TreeCanvasItem item) {
		boolean matches = matches(item);

		var childrenMatches = false;

		var children = item.getChildren();

		for (var child : children) {
			childrenMatches = performFilter(child) || childrenMatches;
		}

		item.setExpanded(childrenMatches);

		matches = matches || childrenMatches;

		if (!matches) {
			_filteredItems.add(item);
		}

		return matches;
	}

	private boolean matches(TreeCanvasItem item) {
		if (_filterText == null) {
			return true;
		}

		var label = item.getLabel();

		if (label == null) {
			return true;
		}

		label = label.toLowerCase();

		if (label.contains(_filterText)) {
			return true;
		}

		var keywords = item.getKeywords();

		if (keywords != null) {

			keywords = keywords.toLowerCase();

			if (keywords.contains(_filterText)) {
				return true;
			}
		}

		return false;
	}

	private void toggleItem(TreeCanvasItem item) {
		item.setExpanded(!item.isExpanded());

		// allways allow to expand an item, even if the children were filtered.
		_filteredItems.removeAll(item.getChildren());

		updateVisibleItemsList();

		requestUpdateScroll();
	}

	@Override
	public void saveState(JSONObject jsonState) {
		super.saveState(jsonState);

		jsonState.put("TreeCanvas.imageSize", _imageSize);
	}

	@Override
	public void restoreState(JSONObject jsonState) {
		super.restoreState(jsonState);

		_imageSize = jsonState.optInt("TreeCanvas.imageSize", 48);
	}

	public void expandToLevel(Object elem, int level) {
		if (elem == null) {
			return;
		}

		for (var item : _items) {
			if (item.getData() == elem) {
				itemExpandToRoot(item);
				itemExpandToLevel(item, level);
			}
		}

		updateVisibleItemsList();
	}

	public void reveal(Object... elems) {
		for (var item : _items) {
			for (Object elem : elems) {
				if (item.getData() == elem) {
					itemExpandToRoot(item.getParent());
				}
			}
		}

		_revealSelection = true;

		updateVisibleItemsList();
	}

	private void itemExpandToRoot(TreeCanvasItem item) {
		if (item != null) {
			item.setExpanded(true);
			itemExpandToRoot(item.getParent());
		}
	}

	private void itemExpandToLevel(TreeCanvasItem item, int level) {
		item.setExpanded(true);

		if (level > 0) {
			for (var child : item.getChildren()) {
				itemExpandToLevel(child, level - 1);
			}
		}
	}

	public void addDragSupport(int operations, Transfer[] transferTypes, DragSourceListener listener) {
		final DragSource dragSource = new DragSource(this, operations);
		dragSource.setTransfer(transferTypes);
		dragSource.addDragListener(listener);
	}

	public void addDropSupport(int operations, Transfer[] transferTypes, final DropTargetListener listener) {
		DropTarget dropTarget = new DropTarget(this, operations);
		dropTarget.setTransfer(transferTypes);
		dropTarget.addDropListener(listener);
	}

	public void setCheckedElements(Object[] elements) {
		var set = new HashSet<>(List.of(elements));
		for (var item : _items) {
			var checked = set.contains(item.getData());
			item.setChecked(checked);
		}
		redraw();
	}

	public Object[] getCheckedElements() {
		return _items.stream().filter(item -> item.isChecked()).map(item -> item.getData()).toArray();
	}

	private Runnable _copyAction;
	private Runnable _cutAction;
	private Runnable _pasteAction;

	public void copy() {
		if (_copyAction != null) {
			_copyAction.run();
		}
	}

	public void cut() {
		if (_cutAction != null) {
			_cutAction.run();
		}
	}

	public void paste() {
		if (_pasteAction != null) {
			_pasteAction.run();
		}
	}

	public void setEditActions(Runnable copy, Runnable cut, Runnable paste) {
		_copyAction = copy;
		_cutAction = cut;
		_pasteAction = paste;
	}

}
