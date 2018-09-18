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
package phasereditor.canvas.ui.editors.properties;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Text;

import phasereditor.canvas.core.BaseObjectModel;

/**
 * @author arian
 *
 */
public class Object_BaseObjectSection extends CanvasPropertySection {

	private Label _xLabel;
	private Text _xText;
	private Label _yLabel;
	private Label _zLabel;
	private Text _zText;
	private Text _yText;
	private Label _scaleXLabel;
	private Text _scaleXText;
	private Label _scaleYLabel;
	private Text _scaleYText;
	private Label _originXLabel;
	private Text _originXText;
	private Label _originYLabel;
	private Text _originYText;
	private Button _flipXBtn;
	private Button _flipYBtn;
	private Text _angleText;

	public Object_BaseObjectSection(CanvasPropertiesPage page) {
		super("Object", page);
	}

	@Override
	public boolean canEdit(Object obj) {
		return obj instanceof BaseObjectModel;
	}

	@SuppressWarnings("unused")
	@Override
	public Control createContent(Composite parent) {

		var comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new GridLayout(7, false));

		{
			// position

			var label = new Label(comp, SWT.NONE);
			label.setText("Position");
			label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

			_xLabel = new Label(comp, SWT.NONE);
			_xLabel.setText("X");

			_xText = new Text(comp, SWT.BORDER);
			_xText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

			_yLabel = new Label(comp, SWT.NONE);
			_yLabel.setText("Y");

			_yText = new Text(comp, SWT.BORDER);
			_yText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

			_zLabel = new Label(comp, SWT.NONE);
			_zLabel.setText("Z");

			_zText = new Text(comp, SWT.BORDER);
			_zText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		}
		
		{
			// angle

			var label = new Label(comp, SWT.NONE);
			label.setText("Angle");

			new Label(comp, SWT.NONE);
			
			_angleText = new Text(comp, SWT.BORDER);
			_angleText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			
			new Label(comp, SWT.NONE);
			
			var scale = new Scale(comp, SWT.NONE);
			scale.setMinimum(0);
			scale.setMaximum(2);
			scale.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 3, 1));
			
		}
		
		// scale

		{
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

			new Label(comp, 0);

			var scale = new Scale(comp, SWT.NONE);
			scale.setMinimum(0);
			scale.setMaximum(2);
			scale.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		}

		{

			// origin

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

			new Label(comp, 0);

			var scale = new Scale(comp, SWT.NONE);
			scale.setMinimum(0);
			scale.setMaximum(360);
			scale.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		}

		{

			// filp

			var label = new Label(comp, SWT.NONE);
			label.setText("Flip");

			new Label(comp, SWT.NONE);

			_flipXBtn = new Button(comp, SWT.TOGGLE);
			_flipXBtn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			_flipXBtn.setText("Flip: X");

			new Label(comp, SWT.NONE);

			_flipYBtn = new Button(comp, SWT.TOGGLE);
			_flipYBtn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			_flipYBtn.setText("Flip: Y");

			new Label(comp, SWT.NONE);

			new Label(comp, SWT.NONE);

		}

		return comp;
	}

}
