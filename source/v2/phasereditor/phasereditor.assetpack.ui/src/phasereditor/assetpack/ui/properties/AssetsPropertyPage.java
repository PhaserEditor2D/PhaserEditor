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
package phasereditor.assetpack.ui.properties;

import java.util.List;

import phasereditor.project.core.ProjectCore;
import phasereditor.project.ui.ProjectPropertyPage;
import phasereditor.ui.properties.FormPropertySection;

/**
 * @author arian
 *
 */
public class AssetsPropertyPage extends ProjectPropertyPage {

	@Override
	protected Object getDefaultModel() {
		return ProjectCore.getActiveProject();
	}

	@Override
	protected List<FormPropertySection<?>> createSections() {
		var list = super.createSections();
		
		createAssetKeySection(list);
		
		// list.add(new AssetFilesSection());

		list.add(new SingleSpritesheetPreviewSection());

		list.add(new SingleFramePreviewSection());

		list.add(new ManyTexturesPreviewSection());

		list.add(new SingleAtlasPreviewSection());

		list.add(new SingleMultiAtlasPreviewSection());

		list.add(new ManyAnimationsPreviewSection());

		list.add(new ManyAnimationPreviewSection());
		list.add(new SingleAnimationPreviewSection());

		list.add(new SingleAudioAssetPreviewSection());
		list.add(new SingleAudioSpritePreviewSection());
		list.add(new SingleAudioSpriteAssetElementPreviewSection());

		list.add(new TilemapCSVPreviewSection());

		list.add(new BitmapFontPreviewSection());

		list.add(new HtmlPreviewSection());

		return list;
	}

	@SuppressWarnings("static-method")
	protected void createAssetKeySection(List<FormPropertySection<?>> list) {
		list.add(new AssetKeySection());
	}

}
