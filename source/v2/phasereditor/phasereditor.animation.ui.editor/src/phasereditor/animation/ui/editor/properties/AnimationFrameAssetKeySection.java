// The MIT License (MIT)
//
// Copyright (c) 2015, 2019 Arian Fornaris
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
package phasereditor.animation.ui.editor.properties;

import static java.util.stream.Collectors.toList;

import java.util.List;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import phasereditor.assetpack.core.IAssetKey;
import phasereditor.assetpack.core.animations.AnimationFrameModel;
import phasereditor.assetpack.ui.properties.AssetKeySection;

/**
 * @author arian
 *
 */
public class AnimationFrameAssetKeySection extends BaseAnimationSection<AnimationFrameModel> {

	private AssetKeySection _section;

	public AnimationFrameAssetKeySection(AnimationsPropertyPage page) {
		super(page, "Texture Key");
	}

	@Override
	public boolean canEdit(Object obj) {
		return obj instanceof AnimationFrameModel;
	}

	@Override
	public boolean supportThisNumberOfModels(int number) {
		return number == 1;
	}

	@Override
	public Control createContent(Composite parent) {

		_section = new AssetKeySection() {
			@Override
			public List<IAssetKey> getModels() {
				return AnimationFrameAssetKeySection.this.getModels()

						.stream()

						.map(model -> model.getAssetFrame())

						.collect(toList());
			}
		};

		addUpdate(_section::update_UI_from_Model);

		return _section.createContent(parent);
	}

	@Override
	public void fillToolbar(ToolBarManager manager) {
		_section.fillToolbar(manager);
	}

}
