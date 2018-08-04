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
package phasereditor.animation.ui.model;

import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;
import org.json.JSONObject;

import phasereditor.animation.ui.AnimationModelPreviewFactory;
import phasereditor.assetpack.core.animations.AnimationModel;
import phasereditor.assetpack.core.animations.AnimationsModel;
import phasereditor.ui.views.IPreviewFactory;

/**
 * @author arian
 *
 */

public class AnimationModel_Persistable extends AnimationModel implements IPersistableElement {

	public AnimationModel_Persistable(AnimationsModel animations, JSONObject jsonData) {
		super(animations, jsonData);
	}

	public AnimationModel_Persistable(AnimationsModel animations) {
		super(animations);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == IPreviewFactory.class) {
			return (T) new AnimationModelPreviewFactory();
		}
		return null;
	}

	@Override
	public void saveState(IMemento memento) {
		memento.putString("file", getAnimations().getFile().getFullPath().toPortableString());
		memento.putString("key", getKey());
		memento.putString("dataKey", getAnimations().getDataKey());
	}

	@Override
	public String getFactoryId() {
		return AnimationModelElementFactory.ID;
	}
}
