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

import static phasereditor.ui.PhaserEditorUI.swtRun;

import java.util.ArrayList;
import java.util.LinkedList;
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

/**
 * @author arian
 *
 */
@SuppressWarnings("synthetic-access")
public class TreeCanvas extends BaseImageCanvas implements PaintListener, MouseWheelListener {
	private static final int ACTION_SPACE = 2;
	private List<TreeCanvasItem> _roots;
	private List<TreeCanvasItem> _visibleItems;
	private int _indentSize;
	private int _imageSize;
	private int _fullHeight;
	private Point _origin;
	private boolean _updateScroll;
	private FrameCanvasUtils _utils;

	public TreeCanvas(Composite parent, int style) {
		super(parent, style | SWT.V_SCROLL);

		_indentSize = 16;
		_imageSize = 48;

		_roots = List.of();
		_visibleItems = List.of();

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
			public IFile getImageFile(int index) {
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
			public void mouseUp(MouseEvent e) {

				int index = getOverIndex();

				if (index != -1) {

					var item = _visibleItems.get(index);

					var modelPoint = _utils.viewToModel(e.x, e.y);

					if (item._toggleHitArea != null) {

						if (item._toggleHitArea.contains(modelPoint)) {
							item.setExpanded(!item.isExpanded());

							updateVisibleItems();

							redraw();

							return;
						}
					}
					
					for (var action : item.getActions()) {
						if (action._hitArea != null && action._hitArea.contains(modelPoint)) {
							action.run(e);
							return;
						}
					}
				}

				super.mouseUp(e);
			}
		};
	}

	@SuppressWarnings("null")
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

			int x = item.getDepth() * _indentSize + /* the expand/collapse icon */16;

			int textX = x;
			int rowHeight = 24;

			var isImageFrame = item.getIconType() == IconType.IMAGE_FRAME;
			var icon = item.getIcon();
			var iconBounds = icon == null ? null : icon.getBounds();
			var hasChildren = !item.getChildren().isEmpty();
			boolean selected = _utils.isSelectedIndex(i);

			if (isImageFrame) {
				rowHeight = _imageSize + 4;
				textX += _imageSize + 5;
			} else if (icon != null) {
				textX += iconBounds.width + 5;
			}

			if (selected) {
				gc.setBackground(PhaserEditorUI.getListSelectionColor());
				gc.fillRectangle(0, y, e.width, rowHeight);
			}

			PhaserEditorUI.paintListItemBackground(gc, i, new Rectangle(0, y, e.width, rowHeight));

			String label = item.getLabel();

			if (label != null) {
				gc.setForeground(selected ? PhaserEditorUI.getListSelectionTextColor() : getForeground());
				var extent = gc.textExtent(label);
				gc.drawText(label, textX, y + (rowHeight - extent.y) / 2, true);
			}

			if (isImageFrame) {
				var file = item.getImageFile();
				var fd = item.getFrameData();

				Image img = loadImage(file);

				if (img != null) {
					PhaserEditorUI.paintScaledImageInArea(gc, img, fd,
							new Rectangle(x + 2, y + 2, _imageSize, _imageSize), false);
				}

			} else if (icon != null) {
				gc.drawImage(icon, x, y + (rowHeight - iconBounds.height) / 2);
			}

			if (hasChildren) {
				var path = item.isExpanded() ? IEditorSharedImages.IMG_BULLET_COLLAPSE
						: IEditorSharedImages.IMG_BULLET_EXPAND;
				var img = EditorSharedImages.getImage(path);
				item._toggleHitArea = new Rectangle(x - 16, y + (rowHeight - img.getBounds().height) / 2, 16, 16);
				gc.drawImage(img, item._toggleHitArea.x, item._toggleHitArea.y);

			}

			{
				var actions = item.getActions();

				int actionX = e.width;

				for (int j = 0; j < actions.size(); j++) {
					var action = actions.get(j);

					if (action.getImage() != null) {
						actionX -= 16 + ACTION_SPACE;
						Rectangle r = new Rectangle(actionX, y + 2, 16, 16);
						action._hitArea = r;
						gc.drawImage(action.getImage(), action._hitArea.x, action._hitArea.y);
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

	public List<TreeCanvasItem> getRoots() {
		return _roots;
	}

	public FrameCanvasUtils getUtils() {
		return _utils;
	}

	public void setRoots(List<TreeCanvasItem> roots) {
		_roots = roots;

		updateVisibleItems();

		redraw();
	}

	private void updateVisibleItems() {
		_visibleItems = new ArrayList<>();

		for (var item : _roots) {
			item._depth = 0;
		}

		var queue = new LinkedList<>(_roots);

		while (!queue.isEmpty()) {
			var item = queue.removeFirst();
			_visibleItems.add(item);

			if (item.isExpanded()) {
				for (var child : item.getChildren()) {
					child._depth = item._depth + 1;
					queue.addFirst(child);
				}
			}
		}

		swtRun(this::requestUpdateScroll);
	}

	@Override
	public void mouseScrolled(MouseEvent e) {
		if ((e.stateMask & SWT.SHIFT) == 0) {
			return;
		}

		double f = e.count < 0 ? 0.8 : 1.2;
		_imageSize = (int) (_imageSize * f);
		if (_imageSize < 16) {
			_imageSize = 16;
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
		private IFile _imageFile;
		private FrameData _frameData;
		private IconType _iconType;
		private Image _icon;
		private List<TreeCanvasItem> _children;
		private List<TreeCanvasItemAction> _actions;
		private String _label;
		int _depth;
		private boolean _expanded;
		int _y;
		int _rowHeight;

		public TreeCanvasItem() {
			_children = new ArrayList<>();
			_iconType = IconType.COMMON_ICON;
			_actions = new ArrayList<>();
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

		public IFile getImageFile() {
			return _imageFile;
		}

		public void setImageFile(IFile imageFile) {
			_imageFile = imageFile;
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
	}

	public void expandAll() {

		for (var item : _roots) {
			expandAll(item);
		}

		updateVisibleItems();
		redraw();
	}

	private void expandAll(TreeCanvasItem item) {

		item.setExpanded(true);

		for (var item2 : item.getChildren()) {
			expandAll(item2);
		}
	}
}
