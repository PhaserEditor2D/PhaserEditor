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
package phasereditor.assetpack.ui.editors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class EmptyAssetEditorComp extends Composite {

	/**
	 * Create the composite.
	 * 
	 * @param parent
	 * @param style
	 */
	public EmptyAssetEditorComp(Composite parent, int style) {
		super(parent, style);
		setLayout(new GridLayout(1, false));

		Label lblSelectSectionOr = new Label(this, SWT.WRAP);
		lblSelectSectionOr.setText(
				"An Asset Pack is a means to control the loading of assets into Phaser via a JSON file. Use Phaser.Loader.pack to load your data file. The file is split into sections. Sections are a way for you to control the splitting-up of asset loading, so you don't have to load everything at once. Within each section is an Array of objects (assets). Each object corresponds to a single file to be loaded. Note that lots of the file properties are optional. See the Loader API Documentation to find out which ones (also you can read the tooltips of the labels), as they match the API calls exactly. Where a file type has a callback, such as \"script\", the context in which the callback is run should be passed to the Phaser.Loader.pack method.");
		GridData gd_lblSelectSectionOr = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
		gd_lblSelectSectionOr.heightHint = 50;
		gd_lblSelectSectionOr.widthHint = 200;
		lblSelectSectionOr.setLayoutData(gd_lblSelectSectionOr);

	}
}
