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

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

import phasereditor.scene.core.SceneModel;
import phasereditor.ui.properties.FormPropertyPage;

/**
 * @author arian
 *
 */
public class SnappingSection extends ScenePropertySection {

	private Text _widthText;
	private Text _heightText;
	private Button _enabledBtn;

	public SnappingSection(FormPropertyPage page) {
		super("Snapping", page);
	}

	@Override
	public boolean canEdit(Object obj) {
		return obj instanceof SceneModel;
	}

	@Override
	public Control createContent(Composite parent) {
		var comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new GridLayout(5, false));

		{
			// enabled
			_enabledBtn = new Button(comp, SWT.CHECK);
			_enabledBtn.setText("Enabled");
			_enabledBtn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 5, 1));
		}
		
		{
			// size
			
			label(comp, "Size", "*The snapping size.");

			label(comp, "Width", "*The snapping width.");

			_widthText = new Text(comp, SWT.BORDER);
			_widthText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			label(comp, "Height", "*The snapping height.");

			_heightText = new Text(comp, SWT.BORDER);
			_heightText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		}
		
		update_UI_from_Model();

		return comp;
	}

	@SuppressWarnings("boxing")
	@Override
	public void update_UI_from_Model() {
		var sceneModel = getEditor().getSceneModel();
		
		_enabledBtn.setSelection(sceneModel.isSnapEnabled());
		_widthText.setText(Integer.toString(sceneModel.getSnapWidth()));
		_heightText.setText(Integer.toString(sceneModel.getSnapHeight()));
		
		listen(_enabledBtn, value -> {
			sceneModel.setSnapEnabled(value);
			
			getEditor().getScene().redraw();
			getEditor().setDirty(true);
		});
		
		listenInt(_widthText, value -> {
			sceneModel.setSnapWidth(value);

			getEditor().getScene().redraw();
			getEditor().setDirty(true);
			
		});

		listenInt(_heightText, value -> {
			sceneModel.setSnapHeight(value);

			getEditor().getScene().redraw();
			getEditor().setDirty(true);
			
		});
		
	}

}
