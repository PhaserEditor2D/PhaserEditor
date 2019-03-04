// The MIT License (MIT)
//
// Copyright (c) 2015, 2019 Arian Fornaris
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
package phasereditor.scene.ui.editor.properties;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

import phasereditor.scene.core.ScrollFactorComponent;
import phasereditor.ui.properties.FormPropertyPage;

/**
 * @author arian
 *
 */
public class ScrollFactorSection extends ScenePropertySection {

	public ScrollFactorSection(FormPropertyPage page) {
		super("Scroll Factor", page);
		setStartCollapsed(true);
	}

	@Override
	public boolean canEdit(Object obj) {
		return obj instanceof ScrollFactorComponent;
	}

	@SuppressWarnings("unused")
	@Override
	public Control createContent(Composite parent) {
		var comp = new Composite(parent, 0);
		comp.setLayout(new GridLayout(5, false));

		label(comp, "Scroll Factor", "Phaser.GameObjects.Components.ScrollFactor.setScrollFactor");

		{
			label(comp, "X", "Phaser.GameObjects.Components.ScrollFactor.scrollFactorX");
			var text = new Text(comp, SWT.BORDER);
			text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			new SceneTextToFloat(text) {

				@Override
				protected void accept2(float value) {
					getModels().forEach(model -> ScrollFactorComponent.set_scrollFactorX(model, value));
					getEditor().setDirty(true);
				}
			};

			addUpdate(() -> {
				text.setText(flatValues_to_String(getModels().stream().map(ScrollFactorComponent::get_scrollFactorX)));
			});

		}

		{
			label(comp, "Y", "Phaser.GameObjects.Components.ScrollFactor.scrollFactorY");
			var text = new Text(comp, SWT.BORDER);
			text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			new SceneTextToFloat(text) {

				@Override
				protected void accept2(float value) {
					getModels().forEach(model -> ScrollFactorComponent.set_scrollFactorY(model, value));
					getEditor().setDirty(true);
				}
			};

			addUpdate(() -> {
				text.setText(flatValues_to_String(getModels().stream().map(ScrollFactorComponent::get_scrollFactorY)));
			});

		}

		return comp;
	}

}
