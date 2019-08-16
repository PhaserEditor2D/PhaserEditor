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
import java.util.stream.Collectors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISharedImages;

import phasereditor.assetpack.core.AudioAssetModel;
import phasereditor.ui.EditorSharedImages;

/**
 * @author arian
 *
 */
public class AudioSection extends AssetPackEditorSection<AudioAssetModel> {

	public AudioSection(AssetPackEditorPropertyPage page) {
		super(page, "Audio");
	}

	@Override
	public boolean canEdit(Object obj) {
		return obj.getClass() == AudioAssetModel.class;
	}

	@Override
	public Control createContent(Composite parent) {
		var comp = new Composite(parent, 0);
		comp.setLayout(new GridLayout(3, false));

		{

			// urls

			label(comp, "URLs", Load_audio_urls);

			var text = new Text(comp, SWT.BORDER);
			text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			addUpdate(() -> text.setText(flatValues_to_String(getModels().stream().map(model -> {
				return urlsToString(model);
			}))));

			var btn = new Button(comp, 0);
			btn.setImage(EditorSharedImages.getImage(ISharedImages.IMG_OBJ_FOLDER));
			btn.addSelectionListener(new AbstractBrowseAudioFilesListener() {

				@Override
				protected void setUrls(List<String> value) {
					wrapOperation(() -> {
						getModels().forEach(model -> model.setUrls(value));
					});
				}

				@SuppressWarnings({ "unchecked", "synthetic-access" })
				@Override
				protected List<String> getUrls() {
					return (List<String>) flatValues_to_Object(getModels().stream().map(model -> model.getUrls()));
				}
			});

		}

		return comp;
	}

	private static String urlsToString(AudioAssetModel model) {
		return "[" + model.getUrls().stream().collect(Collectors.joining(",")) + "]";
	}

}
