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

import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import phasereditor.scene.core.SceneModel;
import phasereditor.scene.ui.editor.undo.SceneSnapshotOperation;
import phasereditor.ui.properties.FormPropertyPage;

/**
 * @author arian
 *
 */
public class DisplaySection extends ScenePropertySection {

	private Text _widthText;
	private Text _heightText;
	private ColorSelector _bgColorSelector;
	private ColorSelector _fgColorSelector;

	public DisplaySection(FormPropertyPage page) {
		super("Display", page);
	}

	@Override
	public boolean canEdit(Object obj) {
		return obj instanceof SceneModel;
	}

	@SuppressWarnings("unused")
	@Override
	public Control createContent(Composite parent) {
		var comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new GridLayout(5, false));

		{
			// size

			label(comp, "Border Size", "*The broder size.");

			label(comp, "Width", "*The border width.");

			_widthText = new Text(comp, SWT.BORDER);
			_widthText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			label(comp, "Height", "*The border height.");

			_heightText = new Text(comp, SWT.BORDER);
			_heightText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		}

		{
			// color

			label(comp, "Editor Colors", "*The editor colors.");

			new Label(comp, 0);

			var colorSelector = new ColorSelector(comp);
			colorSelector.getButton().setText("BG");
			colorSelector.getButton().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			_bgColorSelector = colorSelector;
			_bgColorSelector.addListener(e -> {

				wrapOperation(() -> {
					getEditor().getSceneModel().setBackgroundColor((RGB) e.getNewValue());
					getEditor().getScene().redraw();
				});

			});

			new Label(comp, 0);

			colorSelector = new ColorSelector(comp);
			colorSelector.getButton().setText("FG");
			colorSelector.getButton().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			_fgColorSelector = colorSelector;
			_fgColorSelector.addListener(e -> {
				wrapOperation(() -> {
					getEditor().getSceneModel().setForegroundColor((RGB) e.getNewValue());
					getEditor().getScene().redraw();
				});
			});
		}

		update_UI_from_Model();

		return comp;
	}

	@Override
	public void update_UI_from_Model() {
		var sceneModel = getEditor().getSceneModel();

		_bgColorSelector.setColorValue(sceneModel.getBackgroundColor());
		_fgColorSelector.setColorValue(sceneModel.getForegroundColor());
	}

	private void wrapOperation(Runnable run) {
		var before = SceneSnapshotOperation.takeSnapshot(getEditor());

		run.run();

		var after = SceneSnapshotOperation.takeSnapshot(getEditor());

		getEditor().executeOperation(new SceneSnapshotOperation(before, after, "Change display property."));
	}

}
