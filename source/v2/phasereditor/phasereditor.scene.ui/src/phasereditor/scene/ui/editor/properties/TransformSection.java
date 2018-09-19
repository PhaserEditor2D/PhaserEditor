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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import phasereditor.scene.core.ObjectModel;
import phasereditor.scene.core.TransformComponent;

/**
 * @author arian
 *
 */
public class TransformSection extends ScenePropertySection {

	private Label _xLabel;
	private Text _xText;
	private Label _yLabel;
	private Text _yText;
	private Label _scaleXLabel;
	private Text _scaleXText;
	private Label _scaleYLabel;
	private Text _scaleYText;
	private Text _angleText;

	public TransformSection(ScenePropertiesPage page) {
		super("Transform", page);
	}

	@Override
	public boolean canEdit(Object obj) {
		return obj instanceof TransformComponent;
	}

	@SuppressWarnings("unused")
	@Override
	public Control createContent(Composite parent) {

		var comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new GridLayout(5, false));

		{
			// position

			var label = new Label(comp, SWT.NONE);
			label.setText("Position");

			_xLabel = new Label(comp, SWT.NONE);
			_xLabel.setText("X");

			_xText = new Text(comp, SWT.BORDER);
			_xText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

			_yLabel = new Label(comp, SWT.NONE);
			_yLabel.setText("Y");

			_yText = new Text(comp, SWT.BORDER);
			_yText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		}

		{
			// scale

			var label = new Label(comp, SWT.NONE);
			label.setText("Scale");

			_scaleXLabel = new Label(comp, SWT.NONE);
			_scaleXLabel.setText("X");

			_scaleXText = new Text(comp, SWT.BORDER);
			_scaleXText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

			_scaleYLabel = new Label(comp, SWT.NONE);
			_scaleYLabel.setText("Y");

			_scaleYText = new Text(comp, SWT.BORDER);
			_scaleYText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		}

		{
			// angle

			var label = new Label(comp, SWT.NONE);
			label.setText("Angle");

			new Label(comp, SWT.NONE);

			_angleText = new Text(comp, SWT.BORDER);
			_angleText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

			new Label(comp, SWT.NONE);
			new Label(comp, SWT.NONE);

		}

		update_UI_from_Model();

		return comp;
	}

	@SuppressWarnings("boxing")
	private void update_UI_from_Model() {

		var models = List.of(getModels());

		// x y

		_xText.setText(flatValues(models.stream().map(model -> TransformComponent.get_x((ObjectModel) model))));
		_yText.setText(flatValues(models.stream().map(model -> TransformComponent.get_y((ObjectModel) model))));

		listenFloat(_xText, value -> models.forEach(model -> TransformComponent.set_x((ObjectModel) model, value)));
		listenFloat(_yText, value -> models.forEach(model -> TransformComponent.set_y((ObjectModel) model, value)));

		// scale

		_scaleXText
				.setText(flatValues(models.stream().map(model -> TransformComponent.get_scaleX((ObjectModel) model))));
		_scaleYText
				.setText(flatValues(models.stream().map(model -> TransformComponent.get_scaleY((ObjectModel) model))));

		listenFloat(_scaleXText,
				value -> models.forEach(model -> TransformComponent.set_scaleX((ObjectModel) model, value)));
		listenFloat(_scaleYText,
				value -> models.forEach(model -> TransformComponent.set_scaleY((ObjectModel) model, value)));

		// angle

		_angleText.setText(flatValues(models.stream().map(model -> TransformComponent.get_angle((ObjectModel) model))));

		listenFloat(_angleText,
				value -> models.forEach(model -> TransformComponent.set_angle((ObjectModel) model, value)));
	}

}
