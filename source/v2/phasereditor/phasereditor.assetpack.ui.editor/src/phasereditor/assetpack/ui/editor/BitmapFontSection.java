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

import phasereditor.assetpack.core.AssetModel;
import phasereditor.assetpack.core.AssetPackModel;
import phasereditor.assetpack.core.AssetType;
import phasereditor.assetpack.core.BitmapFontAssetModel;
import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.properties.TextListener;

/**
 * @author arian
 *
 */
public class BitmapFontSection extends AssetPackEditorSection<BitmapFontAssetModel> {

	public BitmapFontSection(AssetPackEditorPropertyPage page) {
		super(page, "Bitmap Font");
	}

	@Override
	public boolean supportThisNumberOfModels(int number) {
		return number == 1;
	}
	
	@Override
	public boolean canEdit(Object obj) {
		return obj instanceof BitmapFontAssetModel;
	}

	@SuppressWarnings("unused")
	@Override
	public Control createContent(Composite parent) {
		
		var comp = new Composite(parent, 0);
		comp.setLayout(new GridLayout(3, false));

		{
			// fontDataURL

			var atlasType = getModels().get(0).getType();

			label(comp, "Font Data URL", AssetModel.getHelp(AssetType.bitmapFont, "fontDataURL"));

			var text = new Text(comp, SWT.BORDER);
			text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			new TextListener(text) {

				@Override
				protected void accept(String value) {
					getModels().forEach(model -> {
						model.setFontDataURL(value);
						model.build(null);
					});
					getEditor().refresh();
					update_UI_from_Model();
				}
			};

			addUpdate(() -> text.setText(flatValues_to_String(getModels().stream().map(model -> model.getFontDataURL()))));

			var btn = new Button(comp, 0);
			btn.setImage(EditorSharedImages.getImage(ISharedImages.IMG_OBJ_FOLDER));
			btn.addSelectionListener(new AbstractBrowseFileListener() {

				{
					dialogName = "Bitmap Font XML/FNT";
				}

				@Override
				protected void setUrl(String url) {
					getModels().forEach(model -> {
						model.setFontDataURL(url);
						model.build(null);
					});

					getEditor().refresh();
					update_UI_from_Model();
				}

				@SuppressWarnings("synthetic-access")
				@Override
				protected String getUrl() {
					return flatValues_to_String(getModels().stream().map(model -> model.getTextureURL()));
				}

				@Override
				protected List<IFile> discoverFiles(AssetPackModel pack) throws CoreException {
					return pack.discoverBitmapFontFiles();
				}

			});
		}
		{

			// textureURL

			label(comp, "Texture URL", AssetModel.getHelp(AssetType.bitmapFont, "textureURL"));

			var text = new Text(comp, SWT.BORDER);
			text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			new TextListener(text) {

				@Override
				protected void accept(String value) {
					getModels().forEach(model -> {
						model.setTextureURL(value);
						model.build(null);
					});
					getEditor().refresh();
					update_UI_from_Model();
				}
			};

			addUpdate(
					() -> text.setText(flatValues_to_String(getModels().stream().map(model -> model.getTextureURL()))));

			var btn = new Button(comp, 0);
			btn.setImage(EditorSharedImages.getImage(ISharedImages.IMG_OBJ_FOLDER));
			btn.addSelectionListener(new AbstractBrowseImageListener() {

				{
					dialogName = "Texture URL";
				}

				@Override
				protected void setUrl(String url) {
					getModels().forEach(model -> {
						model.setTextureURL(url);
						model.build(null);
					});
					getEditor().refresh();
					update_UI_from_Model();
				}

				@SuppressWarnings("synthetic-access")
				@Override
				protected String getUrl() {
					return flatValues_to_String(getModels().stream().map(model -> model.getTextureURL()));
				}

			});
		}
		
		{
			// normal map
			label(comp, "Normal Map", AssetModel.getHelp(AssetType.bitmapFont, "normalMap"));

			var text = new Text(comp, SWT.BORDER);
			text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			new TextListener(text) {

				@Override
				protected void accept(String value) {
					getModels().forEach(model -> {
						model.setNormalMap(value);
					});
				}
			};

			addUpdate(
					() -> text.setText(flatValues_to_String(getModels().stream().map(model -> model.getNormalMap()))));

			var btn = new Button(comp, 0);
			btn.setImage(EditorSharedImages.getImage(ISharedImages.IMG_OBJ_FOLDER));
			btn.addSelectionListener(new AbstractBrowseImageListener() {

				{
					dialogName = "normalMap";
				}

				@Override
				protected void setUrl(String url) {
					getModels().forEach(model -> model.setNormalMap(url));
				}

				@SuppressWarnings("synthetic-access")
				@Override
				protected String getUrl() {
					return flatValues_to_String(getModels().stream().map(model -> model.getNormalMap()));
				}
			});
		}

		
		return comp;
	}

}
