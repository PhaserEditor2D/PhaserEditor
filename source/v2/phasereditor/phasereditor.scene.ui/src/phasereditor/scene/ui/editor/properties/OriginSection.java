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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import phasereditor.scene.core.ObjectModel;
import phasereditor.scene.core.OriginComponent;
import phasereditor.ui.EditorSharedImages;

/**
 * @author arian
 *
 */
public class OriginSection extends ScenePropertySection {

	private Label _originXLabel;
	private Text _originXText;
	private Label _originYLabel;
	private Text _originYText;

	public OriginSection(ScenePropertyPage page) {
		super("Origin", page);
	}

	@Override
	public boolean canEdit(Object obj) {
		return obj instanceof OriginComponent;
	}

	@Override
	public Control createContent(Composite parent) {

		Composite comp = new Composite(parent, SWT.NONE);

		comp.setLayout(new GridLayout(6, false));

		var manager = new ToolBarManager();
		manager.add(new Action("Origin", EditorSharedImages.getImageDescriptor(IMG_EDIT_OBJ_PROPERTY)) {
			//
		});
		manager.createControl(comp);

		var label = new Label(comp, SWT.NONE);
		label.setText("Origin");

		_originXLabel = new Label(comp, SWT.NONE);
		_originXLabel.setText("X");

		_originXText = new Text(comp, SWT.BORDER);
		_originXText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		_originYLabel = new Label(comp, SWT.NONE);
		_originYLabel.setText("Y");

		_originYText = new Text(comp, SWT.BORDER);
		_originYText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		update_UI_from_Model();

		return comp;
	}

	@Override
	@SuppressWarnings("boxing")
	public void update_UI_from_Model() {

		var models = List.of(getModels());

		// origin

		_originXText.setText(
				flatValues_to_String(models.stream().map(model -> OriginComponent.get_originX((ObjectModel) model))));
		_originYText.setText(
				flatValues_to_String(models.stream().map(model -> OriginComponent.get_originY((ObjectModel) model))));

		listenFloat(_originXText, value -> {
			models.forEach(model -> OriginComponent.set_originX((ObjectModel) model, value));
			getEditor().setDirty(true);
		}, models);

		listenFloat(_originYText, value -> {
			models.forEach(model -> OriginComponent.set_originY((ObjectModel) model, value));
			getEditor().setDirty(true);
		}, models);

	}

}
