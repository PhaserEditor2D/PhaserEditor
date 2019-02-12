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
package phasereditor.animation.ui.editor.properties;

import java.util.ArrayList;
import java.util.List;

import phasereditor.animation.ui.editor.AnimationsEditor;
import phasereditor.ui.properties.FormPropertyPage;
import phasereditor.ui.properties.FormPropertySection;

/**
 * @author arian
 *
 */
public class AnimationsPropertyPage extends FormPropertyPage {

	private AnimationsEditor _editor;

	public AnimationsPropertyPage(AnimationsEditor editor) {
		super();
		_editor = editor;
	}

	public AnimationsEditor getEditor() {
		return _editor;
	}

	@Override
	protected List<FormPropertySection<?>> createSections() {
		var list = new ArrayList<FormPropertySection<?>>();

		list.add(new AnimationFrameSection(this));
		list.add(new AnimationFrameAssetKeySection(this));
		list.add(new AnimationFramePreviewSection(this));
		list.add(new AnimationSection(this));

		return list;
	}

	@Override
	protected Object getDefaultModel() {
		return null;
	}

}
