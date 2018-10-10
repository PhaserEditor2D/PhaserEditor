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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

import phasereditor.scene.core.DynamicBitmapTextComponent;
import phasereditor.scene.core.DynamicBitmapTextModel;
import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.properties.FormPropertyPage;

/**
 * @author arian
 *
 */
public class DynamicBitmapTextSection extends ScenePropertySection {

	private Text _displayCallbackText;
	private Text _cropWidthText;
	private Text _cropHeightText;
	private Text _scrollXText;
	private Text _scrollYText;

	public DynamicBitmapTextSection(FormPropertyPage page) {
		super("Dynamic Bitmap Text", page);
	}

	@Override
	public boolean canEdit(Object obj) {
		return obj instanceof DynamicBitmapTextModel;
	}

	@Override
	public Control createContent(Composite parent) {

		Composite comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new GridLayout(6, false));

		{

			label(comp, "Display Callback", "Phaser.GameObjects.DynamicBitmapText.displayCallback",
					new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));

			_displayCallbackText = new Text(comp, SWT.BORDER);
			_displayCallbackText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		}

		{
			// crop size

			{
				var manager = new ToolBarManager();
				manager.add(new Action("", EditorSharedImages.getImageDescriptor(IMG_EDIT_OBJ_PROPERTY)) {
					//
				});
				manager.createControl(comp);
			}

			label(comp, "Crop Size", "Phaser.GameObjects.DynamicBitmapText.setSize");

			label(comp, "Width", "Phaser.GameObjects.DynamicBitmapText.cropWidth");

			_cropWidthText = new Text(comp, SWT.BORDER);
			_cropWidthText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

			label(comp, "Height", "Phaser.GameObjects.DynamicBitmapText.cropHeight");

			_cropHeightText = new Text(comp, SWT.BORDER);
			_cropHeightText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		}

		{
			// scroll

			{
				var manager = new ToolBarManager();
				manager.add(new Action("", EditorSharedImages.getImageDescriptor(IMG_EDIT_OBJ_PROPERTY)) {
					//
				});
				manager.createControl(comp);
			}

			label(comp, "Scroll", "*The scroll position of the Bitmap Text");

			label(comp, "X", "Phaser.GameObjects.DynamicBitmapText.scrollX");

			_scrollXText = new Text(comp, SWT.BORDER);
			_scrollXText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

			label(comp, "Y", "Phaser.GameObjects.DynamicBitmapText.scrollY");

			_scrollYText = new Text(comp, SWT.BORDER);
			_scrollYText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		}

		update_UI_from_Model();

		return comp;
	}

	@SuppressWarnings("boxing")
	@Override
	public void update_UI_from_Model() {
		var models = getModels();

		setValues_to_Text(_displayCallbackText, models, DynamicBitmapTextComponent::get_displayCallback);

		setValues_to_Text(_cropWidthText, models, DynamicBitmapTextComponent::get_cropWidth);
		setValues_to_Text(_cropHeightText, models, DynamicBitmapTextComponent::get_cropHeight);

		setValues_to_Text(_scrollXText, models, DynamicBitmapTextComponent::get_scrollX);
		setValues_to_Text(_scrollYText, models, DynamicBitmapTextComponent::get_scrollY);

		listen(_displayCallbackText, value -> {
			models.forEach(model -> DynamicBitmapTextComponent.set_displayCallback(model, value));
			getEditor().setDirty(true);

		}, models);

		listenInt(_cropWidthText, value -> {
			models.forEach(model -> {
				DynamicBitmapTextComponent.set_cropWidth(model, value);
			});

			setModelsToDirty();

			getEditor().setDirty(true);

		}, models);

		listenInt(_cropHeightText, value -> {
			models.forEach(model -> {
				DynamicBitmapTextComponent.set_cropHeight(model, value);
			});

			setModelsToDirty();

			getEditor().setDirty(true);

		}, models);

		listenFloat(_scrollXText, value -> {
			models.forEach(model -> {
				DynamicBitmapTextComponent.set_scrollX(model, value);
			});

			setModelsToDirty();

			getEditor().setDirty(true);

		}, models);

		listenFloat(_scrollYText, value -> {
			models.forEach(model -> {
				DynamicBitmapTextComponent.set_scrollY(model, value);
			});

			setModelsToDirty();

			getEditor().setDirty(true);

		}, models);

	}

}
