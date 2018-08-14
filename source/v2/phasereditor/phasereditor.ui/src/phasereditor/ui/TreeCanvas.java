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

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.swt.SWT;
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
import org.eclipse.wb.swt.SWTResourceManager;

/**
 * @author arian
 *
 */
@SuppressWarnings("synthetic-access")
public class TreeCanvas extends BaseImageCanvas implements PaintListener, MouseWheelListener {
	private static final int MIN_ROW_HEIGHT = 20;
	private static final int ACTION_SPACE = 2;
	private static final int ACTION_PADDING = 2;
	private static final int ICON_AND_TEXT_SPACE = 5;
	private List<TreeCanvasItem> _roots;
	private List<TreeCanvasItem> _visibleItems;
	private List<TreeCanvasItem> _items;
	private int _indentSize;
	private int _imageSize;
	private int _fullHeight;
	private Point _origin;
	private boolean _updateScroll;
	private FrameCanvasUtils _utils;
	protected TreeCanvasItemAction _overAction;
	private String _filterText;
	private HashSet<TreeCanvasItem> _filteredItems;

	public TreeCanvas(Composite parent, int style) {
		super(parent, style | SWT.V_SCROLL);

		_indentSize = 16;
		_imageSize = 48;

		_roots = List.of();
		_visibleItems = List.of();
		_items = List.of();
		_filteredItems = new HashSet<>();

		addPaintListener(this);
		addMouseWheelListener(this);

		_origin = new Point(0, 0);

		final ScrollBar vBar = getVerticalBar();
		vBar.addListener(SWT.Selection, e -> {
			_origin.y = -vBar.getSelection();
			redraw();
		});

		addListener(SWT.Resize, e -> {
			requestUpdateScroll();
		});

		_utils = new FrameCanvasUtils(this, true) {

			@Override
			public Point viewToModel(int x, int y) {
				return new Point(x, y - _origin.y);
			}

			@Override
			public Point modelToView(int x, int y) {
				return new Point(x, y + _origin.y);
			}

			@Override
			public Rectangle getSelectionFrameArea(int index) {
				var item = _visibleItems.get(index);
				var b = getBounds();
				return new Rectangle(0, item._y, b.width, item._rowHeight);
			}

			@Override
			public Rectangle getRenderImageSrcFrame(int index) {
				var item = _visibleItems.get(index);

				if (item.getIconType() == IconType.IMAGE_FRAME) {
					return item._frameData.src;
				}

				return null;
			}

			@Override
			public File getImageFile(int index) {
				var item = _visibleItems.get(index);

				if (item.getIconType() == IconType.IMAGE_FRAME) {
					return item._imageFile;
				}

				return null;
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

					if (_overAction != null) {
						_overAction.run(e);
					}
				}

				super.mouseUp(e);
			}
		};
	}

	public static class TreeCanvasItemRenderer {
		protected final TreeCanvasItem _item;

		public TreeCanvasItemRenderer(TreeCanvasItem item) {
			super();
			_item = item;
		}
		

		public void render(TreeCanvas canvas, PaintEvent e, int index, int x, int y) {
			var gc = e.gc;

			int textX = x;
			int rowHeight = computeRowHeight(canvas);

			var isImageFrame = _item.getIconType() == IconType.IMAGE_FRAME;
			var icon = _item.getIcon();
			var iconBounds = icon == null ? null : icon.getBounds();
			boolean selected = canvas.getUtils().isSelectedIndex(index);

			if (isImageFrame) {
				textX += canvas.getImageSize() + ICON_AND_TEXT_SPACE;
			} else if (iconBounds != null) {
				textX += iconBounds.width + ICON_AND_TEXT_SPACE;
			}

			// paint text

			String label = _item.getLabel();

			if (label != null) {
				gc.setForeground(selected ? PhaserEditorUI.getListSelectionTextColor() : canvas.getForeground());
				var extent = gc.textExtent(label);

				if (_item.isHeader()) {
					gc.setFont(SWTResourceManager.getBoldFont(canvas.getFont()));
				}

				if (_item.isParentByNature() && _item.getChildren().isEmpty()) {
					gc.setAlpha(125);
				}

				gc.drawText(label, textX, y + (rowHeight - extent.y) / 2, true);

				gc.setAlpha(255);
				gc.setFont(canvas.getFont());
			}

			// paint icon or image

			if (isImageFrame) {

				// paint image

				var file = _item.getImageFile();
				var img = canvas.loadImage(file);
				var fd = _item.getFrameData();

				if (img != null) {
					PhaserEditorUI.paintScaledImageInArea(gc, img, fd,
							new Rectangle(x + 2, y + 2, canvas.getImageSize(), canvas.getImageSize()), false);
				}

			} else if (iconBounds != null) {

				// paint icon

				if (_item.isHeader()) {
					gc.drawImage(icon, textX - 16 - ICON_AND_TEXT_SPACE, y + (rowHeight - iconBounds.height) / 2);
				} else {
					gc.drawImage(icon, x, y + (rowHeight - iconBounds.height) / 2);
				}
			}
		}

		public int computeRowHeight(TreeCanvas canvas) {
			
			if (_item.getIconType() == IconType.IMAGE_FRAME) {
				return canvas.getImageSize() + 4;
			}
			
			return MIN_ROW_HEIGHT;
		}
	}

	@Override
	public void paintControl(PaintEvent e) {

		_fullHeight = 0;

		var gc = e.gc;

		Transform tx = new Transform(getDisplay());
		tx.translate(0, _origin.y);
		gc.setTransform(tx);

		int i = 0;
		int y = 0;
		for (var item : _visibleItems) {

			TreeCanvasItemRenderer renderer;
			if (item.getRenderer() == null) {
				renderer = new TreeCanvasItemRenderer(item);
			} else {
				renderer = item.getRenderer();
			}

			int x = item.getDepth() * _indentSize + /* the expand/collapse icon */16;

			int rowHeight = renderer.computeRowHeight(this);

			// paint background
			
			if (_utils.isSelectedIndex(i)) {
				gc.setBackground(PhaserEditorUI.getListSelectionColor());
				gc.fillRectangle(0, y, e.width, rowHeight);
			}

			PhaserEditorUI.paintListItemBackground(gc, i, new Rectangle(0, y, e.width, rowHeight));
			
			// render item
			
			renderer.render(this, e, i, x, y);
			
			// paint toogle

			if (item.hasChildren()) {
				var path = item.isExpanded() ? IEditorSharedImages.IMG_BULLET_COLLAPSE
						: IEditorSharedImages.IMG_BULLET_EXPAND;
				var img = EditorSharedImages.getImage(path);
				item._toggleHitArea = new Rectangle(x - 16, y + (rowHeight - img.getBounds().height) / 2, 16, 16);
				gc.drawImage(img, item._toggleHitArea.x, item._toggleHitArea.y);

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

			item._y = y;
			item._rowHeight = rowHeight;

			y += rowHeight;
			i++;
		}
		_fullHeight = y;

		if (_updateScroll) {
			_updateScroll = false;
			updateScrollNow();
		}
	}

	public void selectAll() {
		_utils.selectAll();
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
			redraw();
		}
	}

	public List<TreeCanvasItem> getItems() {
		return _items;
	}

	public List<TreeCanvasItem> getExpandedItems() {
		return _items.stream().filter(i -> i.isExpanded()).collect(toList());
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

		updateItemsList(_roots);

	}

	private void updateItemsList(List<TreeCanvasItem> items) {
		for (var item : items) {
			_items.add(item);
			updateItemsList(item.getChildren());
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
		vBar.setMaximum(_fullHeight);
		vBar.setThumb(Math.min(_fullHeight, client.height));
		int vPage = _fullHeight - client.height;
		int vSelection = vBar.getSelection();
		if (vSelection >= vPage) {
			if (vPage <= 0)
				vSelection = 0;
			_origin.y = -vSelection;
		}

		vBar.setVisible(_fullHeight > client.height);

		redraw();
	}

	public enum IconType {
		IMAGE_FRAME, COMMON_ICON
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
		public Rectangle _toggleHitArea;
		private Object _data;
		private File _imageFile;
		private FrameData _frameData;
		private IconType _iconType;
		private Image _icon;
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
		private TreeCanvasItemRenderer _renderer;

		public TreeCanvasItem() {
			_children = new ArrayList<>();
			_iconType = IconType.COMMON_ICON;
			_actions = new ArrayList<>();
		}

		public TreeCanvasItemRenderer getRenderer() {
			return _renderer;
		}

		public void setRenderer(TreeCanvasItemRenderer renderer) {
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

		public IconType getIconType() {
			return _iconType;
		}

		public void setIconType(IconType iconType) {
			_iconType = iconType;
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

		public File getImageFile() {
			return _imageFile;
		}

		public void setImageFile(File imageFile) {
			_imageFile = imageFile;
		}

		public void setImageFile(IFile file) {
			setImageFile(file.getLocation().toFile());
		}

		public FrameData getFrameData() {
			return _frameData;
		}

		public void setFrameData(FrameData frameData) {
			_frameData = frameData;
		}

		public Image getIcon() {
			return _icon;
		}

		public void setIcon(Image icon) {
			_icon = icon;
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
		return _filterText == null || item.getLabel() == null || item.getLabel().toLowerCase().contains(_filterText)
				|| (item.getKeywords() != null && item.getKeywords().contains(_filterText));
	}

	private void toggleItem(TreeCanvasItem item) {
		item.setExpanded(!item.isExpanded());

		// allways allow to expand an item, even if the children were filtered.
		_filteredItems.removeAll(item.getChildren());

		updateVisibleItemsList();

		redraw();
	}
}
