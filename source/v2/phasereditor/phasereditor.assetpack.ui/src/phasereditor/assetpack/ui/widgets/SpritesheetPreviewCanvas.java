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
package phasereditor.assetpack.ui.widgets;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;

import phasereditor.assetpack.core.FrameData;
import phasereditor.assetpack.core.SpritesheetAssetModel;
import phasereditor.assetpack.core.SpritesheetAssetModel.FrameModel;
import phasereditor.assetpack.ui.AssetPackUI;
import phasereditor.ui.ImageCanvas;
import phasereditor.ui.PhaserEditorUI;

public class SpritesheetPreviewCanvas extends ImageCanvas implements MouseMoveListener, KeyListener, MouseListener {

	private SpritesheetAssetModel _spritesheet;
	private List<FrameData> _rects;
	private boolean _controlPressed;
	private List<Integer> _selectedFrames = new ArrayList<>();
	private boolean _shiftPressed;
	private Font _font;

	public SpritesheetPreviewCanvas(Composite parent, int style) {
		super(parent, style);
		setPreferredSize(new Point(100, 100));
		addMouseMoveListener(this);
		addMouseListener(this);
		addKeyListener(this);

		FontData fd = getFont().getFontData()[0];
		_font = new Font(getDisplay(), new FontData(fd.getName(), fd.getHeight(), SWT.BOLD));
		setFont(_font);
	}

	@Override
	public void dispose() {
		super.dispose();
		_font.dispose();
	}

	@SuppressWarnings("boxing")
	@Override
	protected void drawMore(GC gc, int srcW, int srcH, int dstW, int dstH, int dstX, int dstY) {
		if (_spritesheet == null) {
			return;
		}

		SpritesheetAssetModel spritesheet = _spritesheet;
		Rectangle canvasBounds = getBounds();
		Rectangle imgBounds = _image.getBounds();

		boolean paintBorders = PhaserEditorUI.get_pref_Preview_Spritesheet_paintFramesBorder();
		boolean paintLabels = PhaserEditorUI.get_pref_Preview_Spritesheet_paintFramesLabels();
		Color borderColor = PhaserEditorUI.get_pref_Preview_Spritesheet_borderColor();
		Color labelsColor = PhaserEditorUI.get_pref_Preview_Spritesheet_labelsColor();
		Color colorBlack = getDisplay().getSystemColor(SWT.COLOR_BLACK);
		Color selectionColor = PhaserEditorUI.get_pref_Preview_Spritesheet_selectionColor();

		_rects = AssetPackUI.generateSpriteSheetRects(spritesheet, imgBounds);
		if (_rects.isEmpty()) {
			PhaserEditorUI.paintPreviewMessage(gc, canvasBounds, "Cannot compute the grid.");
		} else {

			ZoomCalculator calc = calc();

			for (FrameData fd : _rects) {
				calc.imgWidth = fd.src.width;
				calc.imgHeight = fd.src.height;

				Rectangle r = calc.imageToScreen(fd.dst);

				if (_selectedFrames.contains(fd.index)) {
					gc.setAlpha(100);
					gc.setBackground(selectionColor);
					gc.fillRectangle(r.x, r.y, r.width, r.height);
					gc.setAlpha(255);
				}

				if (!fd.visible) {
					gc.setAlpha(200);
					gc.setBackground(getBackground());
					gc.fillRectangle(r.x, r.y, r.width, r.height);
					gc.setAlpha(255);
				}

				if (paintBorders) {
					gc.setAlpha(125);
					if (r.width >= 16) {
						gc.setForeground(borderColor);
						// gc.drawRectangle(r.x, r.y, r.width, r.height);
						gc.drawLine(r.x, r.y, r.x, r.y + r.height);
						gc.drawLine(r.x, r.y, r.x + r.width, r.y);
					}
					gc.setAlpha(255);
				}
			}

			if (paintBorders) {
				// paint outer frame
				Rectangle rect = calc.imageToScreen(imgBounds);
				gc.setAlpha(125);
				gc.setForeground(borderColor);
				gc.drawRectangle(rect.x, rect.y, rect.width, rect.height);
				gc.setAlpha(255);
			}

			if (paintLabels) {
				for (FrameData fd : _rects) {
					Rectangle r = calc.imageToScreen(fd.dst);
					if (r.width < 64 || r.height < 64) {
						paintLabels = false;
						break;
					}
				}

				if (paintLabels) {
					for (FrameData fd : _rects) {
						calc.imgWidth = fd.dst.width;
						calc.imgHeight = fd.dst.height;
						Rectangle r = calc.imageToScreen(fd.dst);

						String label = Integer.toString(fd.index);
						Point labelRect = gc.stringExtent(Integer.toString(fd.index));
						int left = r.x + r.width / 2 - labelRect.x / 2;
						int top = r.y + r.height / 2 - labelRect.y / 2;

						gc.setForeground(colorBlack);
						gc.drawString(label, left - 1, top + 1, true);
						gc.setForeground(colorBlack);
						gc.drawString(label, left + 1, top - 1, true);
						gc.setForeground(labelsColor);
						gc.drawString(label, left, top, true);
					}
				}
			}
		}
	}

	@Override
	public void mouseDoubleClick(MouseEvent e) {
		// nothing
	}

	@SuppressWarnings("boxing")
	@Override
	public void mouseDown(MouseEvent e) {
		if (e.button == 1) {
			if (!_controlPressed) {
				int frame = findFrameAt(e);
				if (_selectedFrames.contains(frame)) {
					return;
				}

				_selectedFrames = new ArrayList<>();
				addFrameToSelection(e);
			}
		}
	}

	@Override
	public void mouseUp(MouseEvent e) {
		if (e.button == 1) {
			if (_controlPressed) {
				addFrameToSelection(e);
			}
		}
	}

	@SuppressWarnings("boxing")
	private void addFrameToSelection(MouseEvent e) {
		if (_rects == null || _rects.isEmpty()) {
			return;
		}

		int overFrame = findFrameAt(e);

		if (overFrame == -1) {
			_selectedFrames = new ArrayList<>();
		} else {
			if (_selectedFrames.contains(overFrame)) {
				_selectedFrames.remove((Object) overFrame);
			} else {
				_selectedFrames.add(overFrame);
			}
		}

		redraw();
	}

	/**
	 * @param e
	 * @return
	 */
	private int findFrameAt(MouseEvent e) {
		int overFrame = -1;

		ZoomCalculator calc = calc();

		for (FrameData fd : _rects) {
			if (!fd.visible) {
				continue;
			}

			Rectangle r = calc.imageToScreen(fd.dst);
			if (r.contains(e.x, e.y)) {
				overFrame = fd.index;
				break;
			}
		}
		return overFrame;
	}

	@SuppressWarnings("boxing")
	@Override
	public void mouseMove(MouseEvent e) {
		if (_shiftPressed) {
			int frame = findFrameAt(e);
			if (frame != -1 && !_selectedFrames.contains(frame)) {
				_selectedFrames.add(frame);
				redraw();
			}
		}
	}

	@Override
	public void keyPressed(KeyEvent e) {
		_controlPressed = e.keyCode == SWT.CONTROL;
		_shiftPressed = e.keyCode == SWT.SHIFT;
		if (e.character == SWT.ESC) {
			_selectedFrames = new ArrayList<>();
			redraw();
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
		_controlPressed = false;
		_shiftPressed = false;
	}

	public List<FrameModel> getSelectedFrames() {
		List<FrameModel> list = new ArrayList<>();
		for (int i : _selectedFrames) {
			list.add(_spritesheet.getFrames().get(i));
		}
		return list;
	}

	public SpritesheetAssetModel getSpritesheet() {
		return _spritesheet;
	}

	public void setSpritesheet(SpritesheetAssetModel spritesheet) {
		_spritesheet = spritesheet;
		_rects = null;
		_selectedFrames = new ArrayList<>();
	}

	public int getFrameCount() {
		if (_image == null) {
			return 0;
		}

		List<FrameData> list = AssetPackUI.generateSpriteSheetRects(_spritesheet, _image.getBounds());

		int count = 0;
		for (var fd : list) {
			count += fd.visible ? 1 : 0;
		}

		return count;
	}
}
