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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
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

public class AtlasCanvas extends ImageCanvas implements ControlListener, ISelectionProvider {

	private List<? extends AtlasFrame> _frames;
	private List<Rectangle> _framesRects;
	private FrameCanvasUtils _utils;

	@SuppressWarnings("synthetic-access")
	public AtlasCanvas(Composite parent, int style) {
		super(parent, style);
		addControlListener(this);
		
		
		_utils = new FrameCanvasUtils(this) {
			
			@Override
			public Point getRealPosition(int x, int y) {
				return new Point(x, y);
			}
			
			@Override
			public Rectangle getPaintFrame(int index) {
				return _framesRects.get(index);
			}
			
			@Override
			public Rectangle getImageFrame(int index) {
				return _frames.get(index).getFrameData().src;
			}
			
			@Override
			public IFile getImageFile(int index) {
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
	}

	@Override
	public void customPaintControl(PaintEvent e) {

		generateFramesRects();

		super.customPaintControl(e);

		if (_frames != null && _image != null) {
			GC gc = e.gc;

			int i = 0;
			ZoomCalculator calc = calc();
			for (Rectangle r : _framesRects) {
				@SuppressWarnings("boxing")
				boolean selected = _utils.getSelectedIndexes().contains(i);

				if (selected) {
					gc.setBackground(PhaserEditorUI.get_pref_Preview_Spritesheet_selectionColor());
					gc.fillRectangle(r);
					
					gc.setClipping(r);
					Rectangle src = _image.getBounds();
					Rectangle dst = calc.imageToScreen(src);
					gc.drawImage(_image, src.x, src.y, src.width, src.height, dst.x, dst.y, dst.width, dst.height);
					gc.setClipping((Rectangle) null);
					
					gc.drawRectangle(r);

				}

				if (selected || i == _utils.getOverIndex()) {
					gc.drawRectangle(r);
				}

				i++;
			}
		}
	}

	private void generateFramesRects() {
		List<Rectangle> list = new ArrayList<>();

		if (_frames != null && _image != null) {
			ZoomCalculator calc = calc();

			for (AtlasFrame item : _frames) {
				Rectangle src = item.getFrameData().src;
				Rectangle r = calc.imageToScreen(src);
				list.add(r);
			}
		}
		_framesRects = list;
	}

	public void setFrames(List<? extends AtlasFrame> frames) {
		_frames = frames;
		generateFramesRects();
	}

	public List<? extends AtlasFrame> getFrames() {
		return _frames;
	}

	public AtlasFrame getOverFrame() {
		return _frames.get(_utils.getOverIndex());
	}

	@Override
	public void controlMoved(ControlEvent e) {
		generateFramesRects();
	}

	@Override
	public void controlResized(ControlEvent e) {
		generateFramesRects();
	}

	private ListenerList<ISelectionChangedListener> _selectionListeners = new ListenerList<>();
	private IStructuredSelection _selection;
	private IFile _file;

	@Override
	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		_selectionListeners.add(listener);
	}

	@Override
	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		_selectionListeners.remove(listener);
	}

	@SuppressWarnings("boxing")
	@Override
	public void setSelection(ISelection selection) {
		HashSet<AtlasFrame> selectedFrames = new HashSet<>();

		HashSet<AtlasFrame> frameSet = new HashSet<>(_frames);

		List<Integer> list = new ArrayList<>();
		
		for (Object elem : ((IStructuredSelection) selection).toArray()) {
			if (frameSet.contains(elem)) {
				AtlasFrame frame = (AtlasFrame) elem;
				selectedFrames.add(frame);
				list.add(_frames.indexOf(frame));
			}
		}

		_selection = new StructuredSelection(selectedFrames.toArray());

		SelectionChangedEvent event = new SelectionChangedEvent(this, _selection);

		for (ISelectionChangedListener l : _selectionListeners) {
			l.selectionChanged(event);
		}
		
		_utils.setSelectedIndexes(list);
	}

	@Override
	public IStructuredSelection getSelection() {
		return _selection;
	}
	
	@Override
	public void setImageFile(IFile file) {
		super.setImageFile(file);
		_file = file;
	}
	
	public void selectAll() {
		_utils.selectAll();
	}
}
