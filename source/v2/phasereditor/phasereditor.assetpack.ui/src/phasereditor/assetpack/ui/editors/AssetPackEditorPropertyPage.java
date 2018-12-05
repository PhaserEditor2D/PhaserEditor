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
package phasereditor.assetpack.ui.editors;

import java.util.ArrayList;
import java.util.List;

import phasereditor.assetpack.core.AssetModel;
import phasereditor.assetpack.core.ImageAssetModel;
import phasereditor.assetpack.core.SpritesheetAssetModel;
import phasereditor.assetpack.core.SvgAssetModel;
import phasereditor.ui.properties.FormPropertyPage;
import phasereditor.ui.properties.FormPropertySection;

/**
 * @author arian
 *
 */
public class AssetPackEditorPropertyPage extends FormPropertyPage {

	private AssetPackEditor _editor;

	public AssetPackEditorPropertyPage(AssetPackEditor editor) {
		super();
		_editor = editor;
	}

	public AssetPackEditor getEditor() {
		return _editor;
	}

	@Override
	protected Object getDefaultModel() {
		return null;
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected List<FormPropertySection> createSections(Object obj) {
		var list = new ArrayList<FormPropertySection>();

		if (obj instanceof AssetModel) {
			list.add(new KeySection(this));
		}

		if (obj instanceof ImageAssetModel) {
			list.add(new ImageSection(this));
		}

		if (obj instanceof SvgAssetModel) {
			list.add(new SvgSection(this));
		}
		
		if (obj instanceof SpritesheetAssetModel) {
			list.add(new SpritesheetSection(this));
		}

		return list;
	}

}
