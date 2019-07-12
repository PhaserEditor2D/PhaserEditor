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

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

import phasereditor.atlas.ui.AtlasCanvas_Unmanaged;
import phasereditor.ui.PhaserEditorUI;

/**
 * @author arian
 *
 */
public class PageSection extends TexturePackerSection<EditorPage> {

	private Text _tabNameText;
	private Text _imageFileText;
	private Text _imageSizeText;
	private Text _imageFileLengthText;

	public PageSection(TexturePackerPropertyPage page) {
		super("Packer Page", page);
	}

	@Override
	public boolean canEdit(Object obj) {
		return obj instanceof EditorPage;
	}

	@Override
	public Control createContent(Composite parent) {

		var comp = new Composite(parent, 0);
		comp.setLayout(new GridLayout(2, false));

		_tabNameText = createRow(comp, "Tab Name");

		_imageSizeText = createRow(comp, "Image Size");

		_imageFileText = createRow(comp, "Image File");

		_imageFileLengthText = createRow(comp, "Image File Size");

		return comp;
	}

	private Text createRow(Composite comp, String label) {
		label(comp, label, null);

		var text = new Text(comp, SWT.BORDER);

		text.setEditable(false);
		text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		return text;
	}

	@Override
	public void user_update_UI_from_Model() {
		var page = getModels().get(0);

		_tabNameText.setText(page.getName());

		_imageFileText.setText(page.getImageFile().getProjectRelativePath().toPortableString());

		_imageFileLengthText
				.setText(PhaserEditorUI.getFileHumanSize(page.getImageFile().getLocation().toFile().length()));

		{
			var text = "";
			AtlasCanvas_Unmanaged canvas = getEditor().getAtlasCanvas(page.getIndex());
			Image img = canvas.getImage();
			if (img != null) {
				var b = img.getBounds();
				text = b.width + " x " + b.height;
			}
			_imageSizeText.setText(text);
		}

	}

}
