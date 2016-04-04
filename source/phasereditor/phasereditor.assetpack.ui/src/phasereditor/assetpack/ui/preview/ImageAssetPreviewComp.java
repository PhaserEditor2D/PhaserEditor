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
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import phasereditor.assetpack.core.ImageAssetModel;
import phasereditor.ui.ImageCanvas;

public class ImageAssetPreviewComp extends Composite {

	private ImageCanvas _imagePreviewCanvas;
	private Label _resolutionLabel;
	private ImageAssetModel _model;

	/**
	 * Create the composite.
	 * 
	 * @param parent
	 * @param style
	 */
	public ImageAssetPreviewComp(Composite parent, int style) {
		super(parent, style);

		GridLayout gridLayout = new GridLayout(1, false);
		gridLayout.verticalSpacing = 0;
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		gridLayout.horizontalSpacing = 0;
		setLayout(gridLayout);

		_imagePreviewCanvas = new ImageCanvas(this, SWT.NONE);
		_imagePreviewCanvas.setPreferredSize(new Point(200, 200));
		_imagePreviewCanvas.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		_resolutionLabel = new Label(this, SWT.NONE);
		_resolutionLabel.setText("0x0");
		_resolutionLabel.setAlignment(SWT.CENTER);
		_resolutionLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
	}

	public void setModel(ImageAssetModel model) {
		_model = model;
		IFile file = model.getUrlFile();
		_imagePreviewCanvas.setImageFile(file);
		_resolutionLabel.setText(_imagePreviewCanvas.getResolution());
	}

	public ImageAssetModel getModel() {
		return _model;
	}
}
