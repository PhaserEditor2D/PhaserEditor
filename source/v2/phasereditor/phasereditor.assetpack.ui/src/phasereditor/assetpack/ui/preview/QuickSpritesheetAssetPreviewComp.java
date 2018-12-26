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
package phasereditor.assetpack.ui.preview;

import org.eclipse.core.resources.IFile;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import phasereditor.assetpack.core.SpritesheetAssetModel;
import phasereditor.assetpack.ui.SpritesheetPreviewCanvas;

public class QuickSpritesheetAssetPreviewComp extends Composite {
	private Label _resolutionLabel;
	private SpritesheetPreviewCanvas _canvas;

	/**
	 * Create the composite.
	 * 
	 * @param parent
	 * @param style
	 */
	public QuickSpritesheetAssetPreviewComp(Composite parent, int style) {
		super(parent, style);
		setLayout(new GridLayout(1, false));

		_canvas = new SpritesheetPreviewCanvas(this, SWT.NONE);
		_canvas.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		_resolutionLabel = new Label(this, SWT.CENTER);
		_resolutionLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
	}

	public void setModel(SpritesheetAssetModel asset) {
		String name = asset.getKey();
		if (name.length() > 15) {
			name = name.substring(0, 15) + "...";
		}
		_resolutionLabel.setText(name + " (" + asset.getFrameWidth() + "x" + asset.getFrameHeight() + ")");
		IFile file = asset.getUrlFile();
		if (file == null) {
			_canvas.clear();
		} else {
			_canvas.setSpritesheet(asset);
		}
	}

	public SpritesheetPreviewCanvas getCanvas() {
		return _canvas;
	}
}
