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

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;

import phasereditor.assetpack.core.animations.AnimationModel;
import phasereditor.assetpack.ui.preview.AnimationPreviewComp;
import phasereditor.ui.views.IPreviewFactory;

/**
 * @author arian
 *
 */
public class AnimationModelPreviewFactory implements IPreviewFactory {

	@Override
	public boolean canReusePreviewControl(Control c, Object elem) {
		return c instanceof AnimationPreviewComp && elem instanceof AnimationModel;
	}

	@Override
	public Control createControl(Composite previewContainer) {
		return new AnimationPreviewComp(previewContainer, SWT.NONE);
	}

	@Override
	public void updateControl(Control preview, Object element) {
		((AnimationPreviewComp) preview).setModel((AnimationModel) element);
	}

	@Override
	public void updateToolBar(IToolBarManager toolbar, Control preview) {
		((AnimationPreviewComp) preview).createToolBar(toolbar);
	}

	@Override
	public String getTitle(Object element) {
		return ((AnimationModel) element).getKey();
	}

	@Override
	public IPersistableElement getPersistable(Object elem) {
		return (AnimationModel) elem;
	}

	@Override
	public void savePreviewControl(Control previewControl, IMemento memento) {
		//
	}

	@Override
	public void initPreviewControl(Control previewControl, IMemento initialMemento) {
		//
	}

}
