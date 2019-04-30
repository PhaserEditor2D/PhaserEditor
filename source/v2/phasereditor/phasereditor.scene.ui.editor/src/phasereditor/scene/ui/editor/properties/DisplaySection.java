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

import phasereditor.ui.properties.FormPropertyPage;
import phasereditor.ui.properties.TextToIntListener;

/**
 * @author arian
 *
 */
public class DisplaySection extends BaseDesignSection {

	private Text _borderWidthText;
	private Text _borderHeightText;
	private ColorSelector _bgColorSelector;
	private ColorSelector _fgColorSelector;
	private Text _borderXText;
	private Text _borderYText;

	public DisplaySection(FormPropertyPage page) {
		super("Display", page);
	}

	@SuppressWarnings({ "unused" })
	@Override
	public Control createContent(Composite parent) {
		var comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new GridLayout(5, false));

		{
			// border XYs

			label(comp, "Border", "*The broder bounds.");

			label(comp, "X", "*The border X.");

			_borderXText = new Text(comp, SWT.BORDER);
			_borderXText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			new TextToIntListener(_borderXText) {

				@Override
				protected void accept(int value) {
					wrapOperation(() -> {
						getSceneModel().setBorderX(value);
					});
				}
			};

			label(comp, "Y", "*The border Y.");

			_borderYText = new Text(comp, SWT.BORDER);
			_borderYText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			new TextToIntListener(_borderYText) {

				@Override
				protected void accept(int value) {
					wrapOperation(() -> {
						getSceneModel().setBorderY(value);
					});

				}
			};

			// border size

			new Label(comp, 0);

			label(comp, "Width", "*The border width.");

			_borderWidthText = new Text(comp, SWT.BORDER);
			_borderWidthText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			new TextToIntListener(_borderWidthText) {

				@Override
				protected void accept(int value) {
					wrapOperation(() -> {
						getSceneModel().setBorderWidth(value);
					});
				}
			};

			label(comp, "Height", "*The border height.");

			_borderHeightText = new Text(comp, SWT.BORDER);
			_borderHeightText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			new TextToIntListener(_borderHeightText) {

				@Override
				protected void accept(int value) {
					wrapOperation(() -> {
						getSceneModel().setBorderHeight(value);
					});
				}
			};

		}

		{
			// color

			label(comp, "Colors", "*The editor colors.");

			label(comp, "Background", "*The canvas background color.");

			var colorSelector = new ColorSelector(comp);
			colorSelector.getButton().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			_bgColorSelector = colorSelector;
			_bgColorSelector.addListener(e -> {

				wrapOperation(() -> {
					getEditor().getSceneModel().setBackgroundColor((RGB) e.getNewValue());
				});

			});

			label(comp, "Foreground", "*The canvas foreground color.");

			colorSelector = new ColorSelector(comp);
			colorSelector.getButton().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			_fgColorSelector = colorSelector;
			_fgColorSelector.addListener(e -> {
				wrapOperation(() -> {
					getEditor().getSceneModel().setForegroundColor((RGB) e.getNewValue());
				});
			});
		}

		return comp;
	}

	@Override
	public void user_update_UI_from_Model() {
		var sceneModel = getEditor().getSceneModel();

		_borderXText.setText(Integer.toString(sceneModel.getBorderX()));
		_borderYText.setText(Integer.toString(sceneModel.getBorderY()));
		_borderWidthText.setText(Integer.toString(sceneModel.getBorderWidth()));
		_borderHeightText.setText(Integer.toString(sceneModel.getBorderHeight()));

		_bgColorSelector.setColorValue(sceneModel.getBackgroundColor());
		_fgColorSelector.setColorValue(sceneModel.getForegroundColor());
	}

}
