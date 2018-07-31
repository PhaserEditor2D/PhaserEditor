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
package phasereditor.animation.ui;

import static java.lang.System.out;

import java.util.List;

import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ScrollBar;

import javafx.animation.Animation.Status;
import phasereditor.assetpack.core.animations.AnimationFrameModel;
import phasereditor.ui.BaseImageCanvas;
import phasereditor.ui.FrameData;

/**
 * @author arian
 *
 */
public class AnimationTimelineCanvas extends BaseImageCanvas implements PaintListener, MouseWheelListener {

	private AnimationModel_in_Editor _animation;
	private AnimationsEditor _editor;
	private double _widthFactor;
	private int _origin;
	private int _fullWidth;
	private boolean _updateScroll;

	public AnimationTimelineCanvas(Composite parent, int style) {
		super(parent, style | SWT.H_SCROLL | SWT.NO_REDRAW_RESIZE);

		_widthFactor = 1;

		addPaintListener(this);
		addMouseWheelListener(this);

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
					out.println(event.x);
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
		}
	}

	protected void selectionDropped(Object[] data) {

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

		{
			// update scroll form animation progress
			if (_editor != null) {
				var transition = _editor.getAnimationCanvas().getTransition();
				if (transition != null && transition.getStatus() != Status.STOPPED) {
					var frac = transition.getFraction();

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

					if (transition.getRate() > 0) {
						_origin = -x + e.width / 2;
					} else {
						_origin = -x - e.width / 2;
					}

					int topleft = -_fullWidth + thumb;
					if (_origin < topleft) {
						_origin = topleft;
					} else if (_origin > 0) {
						_origin = 0;
					}
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

		List<AnimationFrameModel> frames = _animation.getFrames();

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

			gc.setAlpha(60);
			gc.setBackground(getDisplay().getSystemColor(i % 2 == 0 ? SWT.COLOR_BLUE : SWT.COLOR_GRAY));
			gc.fillRectangle((int) frameX, 0, (int) frameWidth, e.height);
			gc.setAlpha(255);

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

	private int getFrameX(AnimationFrameModel animFrame) {
		return (int) (animFrame.getComputedFraction() * _fullWidth);
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
}
