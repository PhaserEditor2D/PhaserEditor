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
package phasereditor.assetpack.ui.editor;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISharedImages;

import phasereditor.assetpack.core.AssetPackCore;
import phasereditor.assetpack.core.AssetPackModel;
import phasereditor.assetpack.core.HtmlTextureAssetModel;
import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.properties.TextListener;
import phasereditor.ui.properties.TextToIntListener;

/**
 * @author arian
 *
 */
public class HtmlTextureSection extends AssetPackEditorSection<HtmlTextureAssetModel> {

	public HtmlTextureSection(AssetPackEditorPropertyPage page) {
		super(page, "HTML");
	}

	@Override
	public boolean supportThisNumberOfModels(int number) {
		return true;
	}

	@Override
	public boolean canEdit(Object obj) {
		return obj instanceof HtmlTextureAssetModel;
	}

	@SuppressWarnings("unused")
	@Override
	public Control createContent(Composite parent) {
		var comp = new Composite(parent, 0);
		comp.setLayout(new GridLayout(3, false));

		{
			// url

			label(comp, "URL", Load_htmlTexture_url);

			var text = new Text(comp, SWT.BORDER);
			text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			new TextListener(text) {

				@Override
				protected void accept(String value) {
					wrapOperation(() -> {
						getModels().forEach(model -> {
							model.setUrl(value);
						});
					});
				}
			};

			addUpdate(() -> text.setText(flatValues_to_String(getModels().stream().map(model -> model.getUrl()))));

			var btn = new Button(comp, 0);
			btn.setImage(EditorSharedImages.getImage(ISharedImages.IMG_OBJ_FOLDER));
			btn.addSelectionListener(new AbstractBrowseFileListener() {

				{
					dialogName = "URL";
				}

				@Override
				protected void setUrl(String url) {
					wrapOperation(() -> {
						getModels().forEach(model -> {
							model.setUrl(url);
						});
					});
				}

				@SuppressWarnings("synthetic-access")
				@Override
				protected String getUrl() {
					return flatValues_to_String(getModels().stream().map(model -> model.getUrl()));
				}

				@Override
				protected List<IFile> discoverFiles(AssetPackModel pack) throws CoreException {

					List<IFile> files = AssetPackCore.discoverSimpleFiles(pack.getDiscoverFolder());

					AssetPackCore.sortFilesByExtension(files, "html");

					return files;
				}

			});
		}

		// width
		{
			label(comp, "Width", Load_htmlTexture_width);

			var text = new Text(comp, SWT.BORDER);
			text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
			new TextToIntListener(text) {

				@Override
				protected void accept(int value) {
					wrapOperation(() -> {
						getModels().forEach(model -> {
							model.setWidth(value);
						});
					});
				}
			};
			addUpdate(() -> setValues_to_Text(text, getModels(), HtmlTextureAssetModel::getWidth));
		}

		// height
		{
			label(comp, "Height", Load_htmlTexture_height);
			var text = new Text(comp, SWT.BORDER);
			text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
			new TextToIntListener(text) {

				@Override
				protected void accept(int value) {
					wrapOperation(() -> {
						getModels().forEach(model -> {
							model.setHeight(value);
						});
					});
				}
			};
			addUpdate(() -> setValues_to_Text(text, getModels(), HtmlTextureAssetModel::getHeight));
		}

		return comp;
	}

}
