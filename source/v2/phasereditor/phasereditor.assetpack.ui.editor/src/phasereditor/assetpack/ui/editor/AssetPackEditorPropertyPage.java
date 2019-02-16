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

import java.util.ArrayList;
import java.util.List;

import phasereditor.assetpack.core.AssetFactory;
import phasereditor.assetpack.core.AssetFactory.AbstractFileAssetFactory;
import phasereditor.assetpack.core.IAssetElementModel;
import phasereditor.assetpack.ui.properties.AssetKeySection;
import phasereditor.assetpack.ui.properties.AssetsPropertyPage;
import phasereditor.ui.properties.FormPropertySection;

/**
 * @author arian
 *
 */
public class AssetPackEditorPropertyPage extends AssetsPropertyPage {

	private AssetPackEditor _editor;

	public AssetPackEditorPropertyPage(AssetPackEditor editor) {
		super();
		_editor = editor;
	}

	@Override
	public void dispose() {
		_editor.getPropertyPageList().remove(this);
		super.dispose();
	}

	public AssetPackEditor getEditor() {
		return _editor;
	}

	@Override
	protected Object getDefaultModel() {
		return getEditor().getModel();
	}

	@Override
	protected List<FormPropertySection<?>> createSections() {
		var list = new ArrayList<FormPropertySection<?>>();

		list.add(new PackSection(this));

		list.add(new KeySection(this));

		list.add(new ImageSection(this));

		list.add(new SvgSection(this));

		list.add(new SpritesheetSection(this));

		list.add(new AtlasSection(this));

		list.add(new MultiAtlasSection(this));

		list.add(new AnimationsSection(this));

		list.add(new AudioSection(this));

		list.add(new AudioSpriteSection(this));

		list.add(new TilemapSection(this));

		list.add(new BitmapFontSection(this));

		list.add(new HtmlTextureSection(this));

		
		
		for (var factory : AssetFactory.getFactories()) {
			if (factory instanceof AbstractFileAssetFactory) {
				list.add(new FileSection(this, factory.getType()));
			}
		}

		list.add(new PluginSection(this));
		list.add(new ScenePluginSection(this));

		list.addAll(super.createSections());

		return list;
	}

	@Override
	protected void createAssetKeySection(ArrayList<FormPropertySection<?>> list) {

		list.add(new AssetKeySection() {
			@Override
			public boolean canEdit(Object obj) {
				return obj instanceof IAssetElementModel;
			}
		});

	}

}
