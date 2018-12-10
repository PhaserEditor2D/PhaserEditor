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
package phasereditor.atlas.ui.editor;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.ExplainFrameDataCanvas;
import phasereditor.ui.PhaserEditorUI;

/**
 * @author arian
 *
 */
public class FrameSection extends TexturePackerSection<TexturePackerEditorFrame> {

	private Text _frameWidthText;
	private Text _frameHeightText;
	private Text _frameXText;
	private Text _frameYText;
	private Text _spriteXText;
	private Text _spriteYText;
	private Text _spriteWidthText;
	private Text _spriteHeightText;
	private Text _sourceWidthText;
	private Text _sourceHeightText;
	private Text _imageFileText;
	private Text _imageFileSizeText;
	private Text _frameNameText;
	private ExplainFrameDataCanvas _frameCanvas;
	private Action _deleteAction;

	public FrameSection(TexturePackerPropertyPage page) {
		super("Frame", page);

		setFillSpace(true);

	}

	@Override
	public boolean canEdit(Object obj) {
		return obj instanceof TexturePackerEditorFrame;
	}

	@Override
	protected void createActions() {
		super.createActions();

		_deleteAction = new Action("Delete", EditorSharedImages.getImageDescriptor(IMG_DELETE)) {
			@Override
			public void run() {
				getEditor().deleteSelection();
			}
		};
	}

	@Override
	public void fillToolbar(ToolBarManager manager) {

		manager.add(_deleteAction);

		manager.add(new Separator());

		super.fillToolbar(manager);

	}

	@SuppressWarnings("unused")
	@Override
	public Control createContent(Composite parent) {

		createActions();

		var comp = new Composite(parent, 0);
		comp.setLayout(new GridLayout(5, false));

		// Frame

		label(comp, "Frame", null);

		label(comp, "Name", null);
		_frameNameText = new Text(comp, SWT.BORDER);
		_frameNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		_frameNameText.setEditable(false);

		new Label(comp, 0);
		label(comp, "X", null, new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		_frameXText = new Text(comp, SWT.BORDER);
		_frameXText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		_frameXText.setEditable(false);

		label(comp, "Y", null, new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		_frameYText = new Text(comp, SWT.BORDER);
		_frameYText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		_frameYText.setEditable(false);

		new Label(comp, 0);

		label(comp, "Width", null, new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		_frameWidthText = new Text(comp, SWT.BORDER);
		_frameWidthText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		_frameWidthText.setEditable(false);

		label(comp, "Height", null, new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		_frameHeightText = new Text(comp, SWT.BORDER);
		_frameHeightText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		_frameHeightText.setEditable(false);

		{
			new Label(comp, SWT.SEPARATOR | SWT.HORIZONTAL)
					.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 5, 1));
		}

		// Sprite

		label(comp, "Sprite", null);

		label(comp, "X", null, new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		_spriteXText = new Text(comp, SWT.BORDER);
		_spriteXText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		_spriteXText.setEditable(false);

		label(comp, "Y", null, new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		_spriteYText = new Text(comp, SWT.BORDER);
		_spriteYText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		_spriteYText.setEditable(false);

		new Label(comp, 0);

		label(comp, "Width", null, new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		_spriteWidthText = new Text(comp, SWT.BORDER);
		_spriteWidthText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		_spriteWidthText.setEditable(false);

		label(comp, "Height", null, new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		_spriteHeightText = new Text(comp, SWT.BORDER);
		_spriteHeightText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		_spriteHeightText.setEditable(false);

		{
			new Label(comp, SWT.SEPARATOR | SWT.HORIZONTAL)
					.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 5, 1));
		}

		// Source

		label(comp, "Source", null);

		label(comp, "Width", null);
		_sourceWidthText = new Text(comp, SWT.BORDER);
		_sourceWidthText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		_sourceWidthText.setEditable(false);

		label(comp, "Height", null);
		_sourceHeightText = new Text(comp, SWT.BORDER);
		_sourceHeightText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		_sourceHeightText.setEditable(false);

		new Label(comp, 0);
		label(comp, "File", null);
		_imageFileText = new Text(comp, SWT.BORDER);
		_imageFileText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		_imageFileText.setEditable(false);

		new Label(comp, 0);
		label(comp, "File Size", null);
		_imageFileSizeText = new Text(comp, SWT.BORDER);
		_imageFileSizeText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		_imageFileSizeText.setEditable(false);

		// Image

		new Label(comp, SWT.SEPARATOR | SWT.HORIZONTAL)
				.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 5, 1));

		_frameCanvas = new ExplainFrameDataCanvas(comp, 0);
		_frameCanvas.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 5, 1));

		return comp;
	}

	@SuppressWarnings("boxing")
	@Override
	public void user_update_UI_from_Model() {
		var models = getModels();

		_frameNameText.setText(flatValues_to_String(models.stream().map(model -> model.getName())));

		_frameXText.setText(flatValues_to_String(models.stream().map(model -> model.getFrameX())));
		_frameYText.setText(flatValues_to_String(models.stream().map(model -> model.getFrameY())));
		_frameWidthText.setText(flatValues_to_String(models.stream().map(model -> model.getFrameW())));
		_frameHeightText.setText(flatValues_to_String(models.stream().map(model -> model.getFrameH())));

		_spriteXText.setText(flatValues_to_String(models.stream().map(model -> model.getSpriteX())));
		_spriteYText.setText(flatValues_to_String(models.stream().map(model -> model.getSpriteY())));
		_spriteWidthText.setText(flatValues_to_String(models.stream().map(model -> model.getSpriteW())));
		_spriteHeightText.setText(flatValues_to_String(models.stream().map(model -> model.getSpriteH())));

		_sourceWidthText.setText(flatValues_to_String(models.stream().map(model -> model.getSourceW())));
		_sourceHeightText.setText(flatValues_to_String(models.stream().map(model -> model.getSourceH())));

		_imageFileText.setText(flatValues_to_String(models.stream().map(model -> {
			var file = getEditor().findFile(model);
			if (file == null) {
				return null;
			}
			return file.getFullPath().toPortableString();
		})));
		_imageFileSizeText.setText(flatValues_to_String(models.stream().map(model -> {
			var file = getEditor().findFile(model);
			return PhaserEditorUI.getFileHumanSize(file.getLocation().toFile().length());
		})));

		if (models.size() != 1) {
			_frameCanvas.setImageFile((IFile) null);
		} else {
			var model = models.get(0);
			var page = model.getPage();

			_frameCanvas.setFrameData(model.getFrameData());
			_frameCanvas.setImageFile(page.getImageFile(), page.getImage());
		}
	}

}
