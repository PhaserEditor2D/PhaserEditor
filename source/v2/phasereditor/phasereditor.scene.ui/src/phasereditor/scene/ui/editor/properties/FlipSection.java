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
package phasereditor.scene.ui.editor.properties;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import phasereditor.scene.core.FlipComponent;
import phasereditor.scene.core.ObjectModel;

/**
 * @author arian
 *
 */
public class FlipSection extends ScenePropertySection {

	private Button _flipXBtn;
	private Button _flipYBtn;

	public FlipSection(ScenePropertiesPage page) {
		super("Flip", page);
	}

	@Override
	public boolean canEdit(Object obj) {
		return obj instanceof FlipComponent;
	}

	@SuppressWarnings("unused")
	@Override
	public Control createContent(Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new GridLayout(4, false));

		// filp

		new Label(comp, SWT.NONE);

		_flipXBtn = new Button(comp, SWT.TOGGLE);
		_flipXBtn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		_flipXBtn.setText("Flip: X");

		new Label(comp, SWT.NONE);

		_flipYBtn = new Button(comp, SWT.TOGGLE);
		_flipYBtn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		_flipYBtn.setText("Flip: Y");

		update_UI_from_Model();

		return comp;
	}

	@Override
	@SuppressWarnings("boxing")
	public void update_UI_from_Model() {
		var models = List.of(getModels());

		{
			// x

			var value = flatValues_to_Boolean(
					models.stream().map(model -> FlipComponent.get_flipX((ObjectModel) model)));

			_flipXBtn.setSelection(value != null && value);

			listen(_flipXBtn, val -> {

				models.forEach(model -> FlipComponent.set_flipX((ObjectModel) model, val));

				_flipXBtn.setSelection(val);

				getEditor().setDirty(true);
			}, models);
		}

		{
			// y

			var value = flatValues_to_Boolean(
					models.stream().map(model -> FlipComponent.get_flipY((ObjectModel) model)));

			_flipYBtn.setSelection(value != null && value);

			listen(_flipYBtn, val -> {

				models.forEach(model -> FlipComponent.set_flipY((ObjectModel) model, val));

				_flipYBtn.setSelection(val);

				getEditor().setDirty(true);
			}, models);
		}
	}

}
