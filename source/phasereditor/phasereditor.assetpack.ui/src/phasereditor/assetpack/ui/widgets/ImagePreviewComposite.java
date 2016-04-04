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

import org.eclipse.core.resources.IFile;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import phasereditor.ui.ImageCanvas;

public class ImagePreviewComposite extends Composite {
	private Label _resolutionLabel;
	private ImageCanvas _imagePreviewCanvas;

	/**
	 * Create the composite.
	 * 
	 * @param parent
	 * @param style
	 */
	public ImagePreviewComposite(Composite parent, int style) {
		super(parent, style);

		setLayout(new GridLayout(1, false));

		_imagePreviewCanvas = new ImageCanvas(this, SWT.NONE);
		_imagePreviewCanvas.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		_resolutionLabel = new Label(this, SWT.CENTER);
		_resolutionLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
	}

	public void setImageFile(IFile file) {
		if (file == null) {
			_imagePreviewCanvas.setImage(null);
			_resolutionLabel.setText("<No image>");
		} else {
			_imagePreviewCanvas.setImageFile(file);
			_resolutionLabel.setText(file.getName() + " (" + _imagePreviewCanvas.getResolution() + ")");
		}
	}
}
