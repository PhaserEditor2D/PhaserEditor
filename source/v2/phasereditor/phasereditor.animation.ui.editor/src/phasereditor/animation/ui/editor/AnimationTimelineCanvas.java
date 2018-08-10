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
package phasereditor.animation.ui.editor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ScrollBar;

import javafx.animation.Animation.Status;
import phasereditor.assetpack.core.IAssetFrameModel;
import phasereditor.assetpack.core.ImageAssetModel;
import phasereditor.assetpack.core.SpritesheetAssetModel;
import phasereditor.assetpack.core.animations.AnimationFrameModel;
import phasereditor.assetpack.ui.AssetLabelProvider;
import phasereditor.assetpack.ui.AssetPackUI;
import phasereditor.ui.BaseImageCanvas;
import phasereditor.ui.FrameData;
import phasereditor.ui.PhaserEditorUI;

/**
 * @author arian
 *
 */
public class AnimationTimelineCanvas extends BaseImageCanvas
		implements PaintListener, MouseWheelListener, MouseListener, DragSourceListener, KeyListener {

	private AnimationModel_in_Editor _animation;
	private AnimationsEditor _editor;
	private double _widthFactor;
	private int _origin;
	private int _fullWidth;
	private boolean _updateScroll;
	int _dropIndex;
	protected boolean _dropping;
	private Set<AnimationFrameModel_in_Editor> _selectedFrames = new LinkedHashSet<>();
	private AnimationFrameModel_in_Editor _lastSelectedFrame;

	public AnimationTimelineCanvas(Composite parent, int style) {
		super(parent, style | SWT.H_SCROLL | SWT.NO_REDRAW_RESIZE);

		_widthFactor = 1;

		addPaintListener(this);
		addMouseWheelListener(this);
		addKeyListener(this);

		_origin = 0;

		final ScrollBar hBar = getHorizontalBar();
		hBar.addListener(SWT.Selection, e -> {
			_origin = -hBar.getSelection();
			redraw();
		});

		addListener(SWT.Resize, e -> {
			_updateScroll = true;
		});

		init_DND_Support();

		initMouseSupport();

	}

	private void initMouseSupport() {
		addMouseListener(this);
	}

	private void init_DND_Support() {
		{
			int options = DND.DROP_MOVE | DND.DROP_DEFAULT;
			DropTarget target = new DropTarget(this, options);
			Transfer[] types = { LocalSelectionTransfer.getTransfer() };
			target.setTransfer(types);
			target.addDropListener(new DropTargetAdapter() {

				@Override
				public void dragOver(DropTargetEvent event) {
					_dropping = true;
					updateDropPosition(event.x);
				}

				@Override
				public void dragLeave(DropTargetEvent event) {
					_dropping = false;
					redraw();
				}

				@Override
				public void drop(DropTargetEvent event) {
					if (event.data instanceof Object[]) {
						selectionDropped((Object[]) event.data);
					}
					if (event.data instanceof IStructuredSelection) {
						selectionDropped(((IStructuredSelection) event.data).toArray());
					}
				}
			});

			DragSource source = new DragSource(this, DND.DROP_MOVE | DND.DROP_DEFAULT);
			source.setTransfer(new Transfer[] { TextTransfer.getInstance(), LocalSelectionTransfer.getTransfer() });
			source.addDragListener(this);
		}
	}

	public Collection<AnimationFrameModel_in_Editor> getSelectedFrames() {
		return _selectedFrames;
	}

	public void clearSelection() {
		if (_animation == null) {
			return;
		}

		_selectedFrames = new LinkedHashSet<>();

		getEditor().getEditorSite().getSelectionProvider().setSelection(new StructuredSelection(_animation));

		redraw();
	}

	protected void updateDropPosition(int displayX) {

		var x = -_origin + displayX - toDisplay(0, 0).x;

		int newDropIndex = 0;

		var frames = _animation.getFrames();

		int size = frames.size();

		for (int i = 0; i < size; i++) {

			double x1 = getFrameX(i);
			double x2;

			if (i + 1 < size) {
				x2 = getFrameX(i + 1);
			} else {
				x2 = _fullWidth;
			}

			double middle = (x1 + x2) / 2;

			if (x > middle) {
				newDropIndex = i + 1;
			}
		}

		if (newDropIndex != _dropIndex) {
			_dropIndex = newDropIndex;
			redraw();
		}
	}

	@SuppressWarnings("boxing")
	protected void selectionDropped(Object[] data) {

		if (_animation == null) {
			_editor.createAnimationsWithDrop(data);
			return;
		}

		var restart = !_editor.getAnimationCanvas().isStopped();

		_editor.getAnimationCanvas().stop();

		List<AnimationFrameModel_in_Editor> framesFromTheOutside = new ArrayList<>();
		List<AnimationFrameModel_in_Editor> framesFromTimeline = new ArrayList<>();

		var frames = _animation.getFrames();

		for (var obj : data) {
			IAssetFrameModel frame = null;
			AnimationFrameModel_in_Editor inEditorFrame = null;
			AnimationFrameModel_in_Editor alienFrame = null;

			if (obj instanceof IAssetFrameModel) {
				frame = (IAssetFrameModel) obj;
			} else if (obj instanceof ImageAssetModel) {
				frame = ((ImageAssetModel) obj).getFrame();
			} else if (frames.contains(obj)) {
				inEditorFrame = (AnimationFrameModel_in_Editor) obj;
			} else if (obj instanceof AnimationFrameModel) {
				AnimationFrameModel anim = (AnimationFrameModel) obj;
				alienFrame = new AnimationFrameModel_in_Editor(_animation, anim.toJSON());
				alienFrame.setFrameAsset(anim.getFrameAsset());
			}

			if (frame != null) {
				alienFrame = new AnimationFrameModel_in_Editor(_animation);
				alienFrame.setFrameAsset(frame);
				alienFrame.setTextureKey(frame.getAsset().getKey());

				if (frame.getAsset() instanceof ImageAssetModel) {
					// nothing
				} else if (frame instanceof SpritesheetAssetModel.FrameModel) {
					alienFrame.setFrameName(((SpritesheetAssetModel.FrameModel) frame).getIndex());
				} else {
					alienFrame.setFrameName(frame.getKey());
				}
			}

			if (alienFrame != null) {
				framesFromTheOutside.add(alienFrame);
			}

			if (inEditorFrame != null) {
				framesFromTimeline.add(inEditorFrame);
			}
		}

		if (framesFromTheOutside.isEmpty()) {
			// the frames are from the timeline, this is a move
			if (_dropIndex == frames.size()) {
				// just move the frames and add them to the end
				frames.removeAll(framesFromTimeline);
				frames.addAll(framesFromTimeline);
			} else {
				var pivotFrame = frames.get(_dropIndex);
				framesFromTimeline.remove(pivotFrame);
				frames.removeAll(framesFromTimeline);
				int i = frames.indexOf(pivotFrame);
				if (i == -1) {
					i = 0;
				}
				frames.addAll(i, framesFromTimeline);
			}
		} else {
			// the frames are created by dropping assets
			if (_dropIndex == frames.size()) {
				frames.addAll(framesFromTheOutside);
			} else {
				frames.addAll(_dropIndex, framesFromTheOutside);
			}
		}

		_animation.buildTimeline();

		if (_editor.getAnimationCanvas().getImage() == null) {
			if (!frames.isEmpty()) {
				_editor.getAnimationCanvas().showFrame(0);
			}
		}

		if (restart) {
			_editor.getAnimationCanvas().play();
		}

		if (_editor.getOutliner() != null) {
			_editor.getOutliner().refresh();
		}

		_editor.setDirty();

		redraw();
	}

	void updateScroll() {
		Rectangle client = getClientArea();
		ScrollBar hBar = getHorizontalBar();
		hBar.setMaximum(_fullWidth);
		hBar.setThumb(Math.min(_fullWidth, client.width));
		hBar.setVisible(_fullWidth > client.width);
		int hPage = _fullWidth - client.width;
		int hSelection = hBar.getSelection();
		if (hSelection >= hPage) {
			if (hPage <= 0) {
				hSelection = 0;
			}
			_origin = -hSelection;
		}
	}

	public void setEditor(AnimationsEditor editor) {
		_editor = editor;
	}

	public AnimationsEditor getEditor() {
		return _editor;
	}

	public void setAnimation(AnimationModel_in_Editor animation) {
		_animation = animation;

		_selectedFrames = new LinkedHashSet<>();

		redraw();
	}

	public AnimationModel_in_Editor getAnimation() {
		return _animation;
	}

	@Override
	public void paintControl(PaintEvent e) {
		if (_animation == null) {
			return;
		}

		if (_animation.getFrames().isEmpty()) {
			String msg = "Please, drop some frames here, from the Assets window.";
			var b = e.gc.textExtent(msg);
			int x = Math.max(0, e.width / 2 - b.x / 2);
			int y = e.height / 2 - b.y / 2;
			e.gc.drawText(msg, x, y);
			return;
		}

		{
			// update scroll form animation progress
			if (_editor != null) {
				var transition = _editor.getAnimationCanvas().getTransition();
				if (transition != null && transition.getStatus() != Status.STOPPED) {
					var frac = transition.getFraction();

					scrollTo(frac, transition.getRate());
				}
			}
		}

		var gc = e.gc;

		Transform tx = new Transform(getDisplay());
		tx.translate(_origin, 0);
		gc.setTransform(tx);

		_fullWidth = (int) (e.width * _widthFactor);

		int margin = 20;

		int frameHeight = e.height - margin * 2;

		var frames = _animation.getFrames();

		var globalMinFrameWidth = Double.MAX_VALUE;

		for (int i = 0; i < frames.size(); i++) {
			var animFrame = frames.get(i);

			var frame = animFrame.getFrameAsset();

			if (frame == null) {
				continue;
			}

			double x = getFrameX(animFrame);
			double x2 = i + 1 < frames.size() ? getFrameX(frames.get(i + 1)) : _fullWidth;
			double w = x2 - x;
			globalMinFrameWidth = Math.min(w, globalMinFrameWidth);
		}

		for (int i = 0; i < frames.size(); i++) {

			var animFrame = frames.get(i);

			var frame = animFrame.getFrameAsset();

			if (frame == null) {
				continue;
			}

			var img = loadImage(frame.getImageFile());
			FrameData fd = frame.getFrameData();

			double frameX = getFrameX(animFrame);
			double frameX2 = i + 1 < frames.size() ? getFrameX(frames.get(i + 1)) : _fullWidth;
			double frameWidth = frameX2 - frameX;

			var selected = _selectedFrames.contains(animFrame);

			if (selected) {
				gc.setAlpha(100);
				gc.setBackground(PhaserEditorUI.get_pref_Preview_frameSelectionColor());
			} else {
				gc.setAlpha(60);
				gc.setBackground(getDisplay().getSystemColor(i % 2 == 0 ? SWT.COLOR_WHITE : SWT.COLOR_GRAY));
			}

			gc.fillRectangle((int) frameX, 0, (int) frameWidth, e.height);
			gc.setAlpha(255);

			if (selected) {
				gc.drawRectangle((int) frameX, 0, (int) frameWidth, e.height - 1);
			}

			if (frameHeight > 0) {
				double imgW = fd.srcSize.x;
				double imgH = fd.srcSize.y;

				{
					imgW = imgW * (frameHeight / imgH);
					imgH = frameHeight;
				}

				// fix width, do not go beyond the global min frame width
				if (imgW > globalMinFrameWidth) {
					imgH = imgH * (globalMinFrameWidth / imgW);
					imgW = globalMinFrameWidth;
				}

				double scaleX = imgW / fd.srcSize.x;
				double scaleY = imgH / fd.srcSize.y;

				var imgX = frameX + frameWidth / 2 - imgW / 2 + fd.dst.x * scaleX;
				var imgY = margin + frameHeight / 2 - imgH / 2 + fd.dst.y * scaleY;

				double imgDstW = fd.dst.width * scaleX;
				double imgDstH = fd.dst.height * scaleY;

				gc.drawImage(img, fd.src.x, fd.src.y, fd.src.width, fd.src.height, (int) imgX, (int) imgY,
						(int) imgDstW, (int) imgDstH);
			}

		}

		if (_dropping) {
			int x;
			if (_dropIndex == frames.size()) {
				x = _fullWidth;
			} else {
				x = getFrameX(_dropIndex);
			}
			var lw = gc.getLineWidth();
			gc.setLineWidth(3);
			gc.setAlpha(255);
			gc.drawLine(x, 0, x, e.height);
			gc.setLineWidth(lw);
		}

		if (_editor != null) {
			var transition = _editor.getAnimationCanvas().getTransition();
			if (transition != null && transition.getStatus() != Status.STOPPED) {
				var frac = transition.getFraction();
				var x = (int) (_fullWidth * frac);
				gc.drawLine(x, 0, x, e.height);
			}
		}

		if (_updateScroll) {
			_updateScroll = false;
			updateScroll();
		}

	}

	private void scrollTo(double frac, double rate) {

		var x = (int) (_fullWidth * frac);

		var hBar = getHorizontalBar();

		int thumb = hBar.getThumb();
		int sel = (int) ((_fullWidth - thumb) * frac);

		if (sel < 0) {
			sel = 0;
		} else if (sel > _fullWidth - thumb) {
			sel = _fullWidth - thumb;
		}

		hBar.setSelection(sel);

		var b = getClientArea();

		if (rate > 0) {
			_origin = -x + b.width / 2;
		} else {
			_origin = -x - b.width / 2;
		}

		int topleft = -_fullWidth + thumb;
		if (_origin < topleft) {
			_origin = topleft;
		} else if (_origin > 0) {
			_origin = 0;
		}
	}

	private int getFrameX(AnimationFrameModel animFrame) {
		return (int) (animFrame.getComputedFraction() * _fullWidth);
	}

	private int getFrameX(int index) {
		return getFrameX(_animation.getFrames().get(index));
	}

	private int getFrameIndex(int x) {
		if (x > _fullWidth) {
			return -1;
		}

		int result = -1;

		int i = 0;

		for (var frame : _animation.getFrames()) {
			var fx = getFrameX(frame);

			if (x >= fx) {
				result = i;
			} else {
				break;
			}

			i++;
		}

		return result;
	}

	AnimationFrameModel_in_Editor getFrameAtX(int x) {
		int i = getFrameIndex(x);
		if (i == -1) {
			return null;
		}
		return (AnimationFrameModel_in_Editor) _animation.getFrames().get(i);
	}

	@Override
	public void mouseScrolled(MouseEvent e) {
		if (e.count < 0) {
			_widthFactor -= 0.2;
		} else {
			_widthFactor += 0.2;
		}

		if (_widthFactor < 0.2) {
			_widthFactor = 0.1;
		}

		_updateScroll = true;

		redraw();
	}

	@Override
	public void mouseDoubleClick(MouseEvent e) {
		if (!_selectedFrames.isEmpty()) {
			var frame = _selectedFrames.iterator().next();
			_editor.getAnimationCanvas().showFrame(getFrameIndex(frame));
		}
	}

	@Override
	public void mouseDown(MouseEvent e) {
		// nothing
	}

	@Override
	public void mouseUp(MouseEvent e) {
		if (e.button != 1) {
			return;
		}

		var updateLastSelectionFrame = true;
		var x = -_origin + e.x;
		var frame = getFrameAtX(x);

		if (frame != null) {

			if ((e.stateMask & SWT.CTRL) != 0) {

				// control pressed

				if (_selectedFrames.contains(frame)) {
					_selectedFrames.remove(frame);
				} else {
					_selectedFrames.add(frame);
				}

			} else if ((e.stateMask & SWT.SHIFT) != 0 && !_selectedFrames.isEmpty()) {

				// select the whole range

				List<AnimationFrameModel> frames = _animation.getFrames();

				int a = frames.indexOf(_lastSelectedFrame);
				int b = frames.indexOf(frame);

				int from = Math.min(a, b);
				int to = Math.max(a, b);

				_selectedFrames = new LinkedHashSet<>();

				for (int i = from; i <= to; i++) {
					_selectedFrames.add((AnimationFrameModel_in_Editor) frames.get(i));
				}

				updateLastSelectionFrame = false;

			} else {

				// just select that frame

				_selectedFrames = new LinkedHashSet<>();
				_selectedFrames.add(frame);

			}

			updateSelectionProvider();
		}

		if (updateLastSelectionFrame) {
			_lastSelectedFrame = frame;
		}

		redraw();
	}

	private void updateSelectionProvider() {
		_editor.getEditorSite().getSelectionProvider().setSelection(new StructuredSelection(_selectedFrames.toArray()));
	}

	@Override
	public void dragStart(DragSourceEvent event) {
		LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();
		var selectedFrames = getSelectedFrames();

		var frame = getFrameAtX(event.x);

		if (frame == null) {
			event.doit = false;
			return;
		}

		if (selectedFrames.contains(frame)) {
			event.image = AssetLabelProvider.GLOBAL_48.getImage(selectedFrames.iterator().next());
			transfer.setSelection(new StructuredSelection(selectedFrames.toArray()));
		} else {
			transfer.setSelection(new StructuredSelection(frame));
			IAssetFrameModel assetFrame = frame.getFrameAsset();

			if (assetFrame != null) {
				AssetPackUI.set_DND_Image(event, assetFrame);
			}

			_selectedFrames = new LinkedHashSet<>();
			updateSelectionProvider();
		}
	}

	@Override
	public void dragSetData(DragSourceEvent event) {
		var frames = getSelectedFrames();
		if (!frames.isEmpty()) {
			event.data = frames.iterator().next().getFrameName();
		}
	}

	@Override
	public void dragFinished(DragSourceEvent event) {
		if (event.image != null) {
			event.image.dispose();
		}
	}

	@Override
	public void keyPressed(KeyEvent e) {

		switch (e.character) {
		case SWT.ESC:
			clearSelection();
			break;
		default:
			break;
		}

		switch (e.keyCode) {
		case SWT.ARROW_LEFT:
			shiftSelection(-1);
			break;
		case SWT.ARROW_RIGHT:
			shiftSelection(1);
			break;
		case SWT.HOME:
			// TODO: scroll to the start
			break;
		case SWT.END:
			// TODO: scroll to the end
			break;
		default:
			break;
		}
	}

	private int getFrameIndex(AnimationFrameModel_in_Editor frame) {
		return _animation.getFrames().indexOf(frame);
	}

	@Override
	public void keyReleased(KeyEvent e) {
		// nothing
	}

	private void shiftSelection(int dir) {

		if (_lastSelectedFrame == null) {
			return;
		}

		List<AnimationFrameModel> frames = _animation.getFrames();

		if (frames.isEmpty()) {
			return;
		}

		if (_lastSelectedFrame != null) {
			int i = getFrameIndex(_lastSelectedFrame);
			int j = i + dir;
			if (j >= 0 && j < frames.size()) {
				_selectedFrames = new LinkedHashSet<>();
				_lastSelectedFrame = (AnimationFrameModel_in_Editor) frames.get(j);
				_selectedFrames.add(_lastSelectedFrame);

				getEditor().getAnimationCanvas().showFrame(j);

				updateSelectionProvider();

				scrollTo(_lastSelectedFrame.getComputedFraction(), 1);
				updateScroll();

				redraw();
			}
		}

	}

	public void selectAll() {
		if (_animation == null) {
			return;
		}

		_selectedFrames = new LinkedHashSet<>(_animation.getFrames().stream()
				.map(f -> (AnimationFrameModel_in_Editor) f).collect(Collectors.toList()));

		updateSelectionProvider();

		redraw();
	}
}
