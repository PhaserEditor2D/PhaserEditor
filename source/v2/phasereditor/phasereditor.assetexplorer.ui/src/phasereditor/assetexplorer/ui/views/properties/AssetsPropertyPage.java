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
package phasereditor.assetexplorer.ui.views.properties;

import java.util.ArrayList;
import java.util.List;

import phasereditor.assetpack.core.AnimationsAssetModel;
import phasereditor.assetpack.core.AtlasAssetModel;
import phasereditor.assetpack.core.SpritesheetAssetModel;
import phasereditor.assetpack.core.animations.AnimationModel;
import phasereditor.assetpack.ui.properties.ManyAnimationPreviewSection;
import phasereditor.assetpack.ui.properties.ManyAnimationsPreviewSection;
import phasereditor.assetpack.ui.properties.ManyTexturesPreviewSection;
import phasereditor.assetpack.ui.properties.SingleAnimationPreviewSection;
import phasereditor.assetpack.ui.properties.SingleAtlasPreviewSection;
import phasereditor.assetpack.ui.properties.SingleFramePreviewSection;
import phasereditor.assetpack.ui.properties.SingleSpritesheetPreviewSection;
import phasereditor.ui.properties.FormPropertyPage;
import phasereditor.ui.properties.FormPropertySection;

/**
 * @author arian
 *
 */
public class AssetsPropertyPage extends FormPropertyPage {

	@Override
	protected Object getDefaultModel() {
		return null;
	}

	@Override
	protected List<FormPropertySection<?>> createSections(Object obj) {
		var list = new ArrayList<FormPropertySection<?>>();

		if (obj instanceof SpritesheetAssetModel) {
			list.add(new SingleSpritesheetPreviewSection());
		}

		if (SingleFramePreviewSection.canEdit2(obj)) {
			list.add(new SingleFramePreviewSection());
		}

		if (ManyTexturesPreviewSection.canEdit2(obj)) {
			list.add(new ManyTexturesPreviewSection());
		}

		if (obj instanceof AtlasAssetModel) {
			list.add(new SingleAtlasPreviewSection());
		}

		if (obj instanceof AnimationsAssetModel) {
			list.add(new ManyAnimationsPreviewSection());
		}

		if (obj instanceof AnimationModel) {
			list.add(new ManyAnimationPreviewSection());
			list.add(new SingleAnimationPreviewSection());
		}

		return list;
	}

}
