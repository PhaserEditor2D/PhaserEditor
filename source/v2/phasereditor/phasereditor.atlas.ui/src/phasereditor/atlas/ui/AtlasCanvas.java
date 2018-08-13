// The MIT License (MIT)
//
// Copyright (c) 2015 Arian Fornaris
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
package phasereditor.atlas.ui;

import java.io.File;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;

import phasereditor.atlas.core.AtlasFrame;
import phasereditor.ui.FrameCanvasUtils;
import phasereditor.ui.ImageCanvas;
import phasereditor.ui.PhaserEditorUI;

public class AtlasCanvas extends ImageCanvas implements ControlListener {

	private List<? extends AtlasFrame> _frames;
	private Rectangle[] _framesRects;
	private FrameCanvasUtils _utils;

	@SuppressWarnings("synthetic-access")
	public AtlasCanvas(Composite parent, int style, boolean addDragAndDropSupport) {
		super(parent, style);

		addControlListener(this);

		_utils = new FrameCanvasUtils(this, addDragAndDropSupport) {

			@Override
			public Point viewToModel(int x, int y) {
				return new Point(x, y);
			}
			
			@Override
			public Point modelToView(int x, int y) {
				return new Point(x, y);
			}
			
			@Override
			public Rectangle getSelectionFrameArea(int index) {
				return _framesRects[index];
			}

			@Override
			public Rectangle getRenderImageSrcFrame(int index) {
				AtlasFrame frame = _frames.get(index);
				return frame.getFrameData().src;
			}

			@Override
			public File getImageFile(int index) {
				return _file;
			}

			@Override
			public int getFramesCount() {
				return _frames.size();
			}

			@Override
			public Object getFrameObject(int index) {
				return _frames.get(index);
			}
		};

		_frames = List.of();
		_framesRects = new Rectangle[0];
	}

	@Override
	public void customPaintControl(PaintEvent e) {

		generateFramesRects();

		super.customPaintControl(e);

		if (_image != null) {
			GC gc = e.gc;

			int i = 0;
			ZoomCalculator calc = calc();
			for (Rectangle r : _framesRects) {
				
				var obj = _frames.get(i);
				
				boolean selected = _utils.isSelected(obj);

				if (selected) {
					gc.setBackground(PhaserEditorUI.getListSelectionColor());
					gc.fillRectangle(r);

					gc.setClipping(r);
					Rectangle src = _image.getBounds();
					Rectangle dst = calc.imageToScreen(src);
					gc.drawImage(_image, src.x, src.y, src.width, src.height, dst.x, dst.y, dst.width, dst.height);
					gc.setClipping((Rectangle) null);

					gc.drawRectangle(r);

				}

				if (selected || _utils.isOver(obj)) {
					gc.drawRectangle(r);
				}

				i++;
			}
		}
	}

	private void generateFramesRects() {

		_framesRects = new Rectangle[_frames.size()];

		ZoomCalculator calc = calc();

		int i = 0;
		for (AtlasFrame item : _frames) {
			Rectangle src = item.getFrameData().src;
			Rectangle r = calc.imageToScreen(src);
			_framesRects[i] = r;
			i++;
		}
	}

	public void setFrames(List<? extends AtlasFrame> frames) {
		_frames = frames;
		generateFramesRects();
	}

	public List<? extends AtlasFrame> getFrames() {
		return _frames;
	}

	public AtlasFrame getOverFrame() {
		return (AtlasFrame) _utils.getOverObject();
	}

	@Override
	public void controlMoved(ControlEvent e) {
		generateFramesRects();
	}

	@Override
	public void controlResized(ControlEvent e) {
		generateFramesRects();
	}

	private File _file;

	public void setSelection(IStructuredSelection sel, boolean fireChanged) {
		_utils.setSelection(sel, fireChanged);

	}

	@Override
	public void setImageFile(IFile file) {
		super.setImageFile(file);
		_file = file == null? null : file.getLocation().toFile();
	}

	public void selectAll() {
		_utils.selectAll();
	}

	public List<Object> getSelectedObjects() {
		return _utils.getSelectedObjects();
	}

	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		_utils.addSelectionChangedListener(listener);
	}

	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		_utils.removeSelectionChangedListener(listener);
	}

	public IStructuredSelection getSelection() {
		return (IStructuredSelection) _utils.getSelection();
	}
}
