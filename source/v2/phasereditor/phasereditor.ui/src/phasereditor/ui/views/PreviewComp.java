// The MIT License (MIT)
//
// Copyright (c) 2015, 2017 Arian Fornaris
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
package phasereditor.ui.views;

import org.eclipse.core.runtime.IAdapterManager;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.wb.swt.SWTResourceManager;

/**
 * @author arian
 *
 */
public class PreviewComp extends Composite {

	private Composite _noPreviewComp;
	private IPreviewFactory _previewFactory;
	private Control _previewControl;

	public PreviewComp(Composite parent, int style) {
		super(parent, style);

		StackLayout sl_previewContainer = new StackLayout();
		this.setLayout(sl_previewContainer);

		_noPreviewComp = new Composite(this, SWT.NONE);
		_noPreviewComp.setBackground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_BACKGROUND));
		_noPreviewComp.setLayout(new GridLayout(1, false));
	}

	public boolean preview(Object elem) {
		if (_previewFactory != null && _previewControl != null) {
			_previewFactory.hiddenControl(_previewControl);
		}

		boolean availablePreview = false;
		Control preview = null;

		_previewFactory = null;
		_previewControl = null;

		if (elem == null) {
			preview = _noPreviewComp;
		} else {
			IAdapterManager adapterManager = Platform.getAdapterManager();

			IPreviewFactory factory = adapterManager.getAdapter(elem, IPreviewFactory.class);
			if (factory != null) {
				// try to reuse some of the already created controls
				for (Control c : getChildren()) {
					if (factory.canReusePreviewControl(c, elem)) {
						preview = c;
						break;
					}
				}

				// if there are not reusable controls, then create one
				if (preview == null) {
					preview = factory.createControl(this);
				}

				factory.updateControl(preview, elem);

				_previewFactory = factory;
				_previewControl = preview;

				availablePreview = true;
			}
		}

		if (preview != null) {
			StackLayout layout = (StackLayout) this.getLayout();
			layout.topControl = preview;
			this.layout();
		}

		return availablePreview;
	}

}
