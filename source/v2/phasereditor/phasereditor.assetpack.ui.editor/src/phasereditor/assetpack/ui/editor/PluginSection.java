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
import phasereditor.assetpack.core.AssetPackCore;
import phasereditor.assetpack.core.AssetPackModel;
import phasereditor.assetpack.core.AssetType;
import phasereditor.assetpack.core.PluginAssetModel;
import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.properties.CheckListener;
import phasereditor.ui.properties.TextListener;

/**
 * @author arian
 *
 */
public class PluginSection extends AssetPackEditorSection<PluginAssetModel> {

	public PluginSection(AssetPackEditorPropertyPage page) {
		super(page, "Plugin");
	}

	@Override
	public boolean canEdit(Object obj) {
		return obj instanceof PluginAssetModel;
	}

	@SuppressWarnings("unused")
	@Override
	public Control createContent(Composite parent) {
		var comp = new Composite(parent, 0);
		comp.setLayout(new GridLayout(3, false));

		{
			// url

			label(comp, "URL", AssetModel.getHelp(AssetType.plugin, "url"));

			var text = new Text(comp, SWT.BORDER);
			text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			new TextListener(text) {

				@Override
				protected void accept(String value) {
					getModels().forEach(model -> {
						model.setUrl(value);
						model.build(null);
					});
					getEditor().refresh();
					update_UI_from_Model();
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
					getModels().forEach(model -> {
						model.setUrl(url);
						model.build(null);
					});

					getEditor().refresh();
					update_UI_from_Model();
				}

				@SuppressWarnings("synthetic-access")
				@Override
				protected String getUrl() {
					return flatValues_to_String(getModels().stream().map(model -> model.getUrl()));
				}

				@Override
				protected List<IFile> discoverFiles(AssetPackModel pack) throws CoreException {

					List<IFile> files = AssetPackCore.discoverSimpleFiles(pack.getDiscoverFolder());

					AssetPackCore.sortFilesByExtension(files, "js");

					return files;
				}

			});
		}

		{
			// start
			var btn = new Button(comp, SWT.CHECK);
			btn.setText("Start");
			btn.setToolTipText(AssetModel.getHelp(AssetType.plugin, "start"));
			btn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
			new CheckListener(btn) {

				@Override
				protected void accept(boolean value) {
					getModels().get(0).setStart(value);
				}
			};

			addUpdate(() -> {
				btn.setSelection(getModels().get(0).isStart());
			});
		}

		{
			// mapping
			label(comp, "Mapping", AssetModel.getHelp(AssetType.plugin));
			var text = new Text(comp, SWT.BORDER);
			text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
			new TextListener(text) {

				@Override
				protected void accept(String value) {
					getModels().get(0).setMapping(value.trim().length() == 0 ? null : value);
				}
			};

			addUpdate(() -> {
				var mapping = getModels().get(0).getMapping();
				text.setText(mapping == null ? "" : mapping);
			});
		}

		return comp;
	}

}
