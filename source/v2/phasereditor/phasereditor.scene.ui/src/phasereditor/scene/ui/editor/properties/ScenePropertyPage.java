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
package phasereditor.scene.ui.editor.properties;

import java.util.ArrayList;
import java.util.List;

import phasereditor.scene.core.EditorComponent;
import phasereditor.scene.core.FlipComponent;
import phasereditor.scene.core.OriginComponent;
import phasereditor.scene.core.SpriteModel;
import phasereditor.scene.core.TextureComponent;
import phasereditor.scene.core.TransformComponent;
import phasereditor.scene.core.VisibleComponent;
import phasereditor.scene.ui.editor.SceneEditor;
import phasereditor.ui.properties.FormPropertyPage;
import phasereditor.ui.properties.FormPropertySection;

/**
 * @author arian
 *
 */
public class ScenePropertyPage extends FormPropertyPage {

	private SceneEditor _editor;

	public ScenePropertyPage(SceneEditor editor) {
		super();
		_editor = editor;
	}

	public SceneEditor getEditor() {
		return _editor;
	}

	@Override
	protected List<FormPropertySection> createSections(Object obj) {
		List<FormPropertySection> list = new ArrayList<>();

		if (obj instanceof EditorComponent) {
			list.add(new EditorSection(this));
		}

		if (obj instanceof TransformComponent) {
			list.add(new TransformSection(this));
		}

		if (obj instanceof OriginComponent) {
			list.add(new OriginSection(this));
		}

		if (obj instanceof FlipComponent) {
			list.add(new FlipSection(this));
		}

		if (obj instanceof VisibleComponent) {
			list.add(new VisibleSection(this));
		}

		if (obj instanceof TextureComponent) {
			list.add(new TextureSection(this));
		}

		if (obj instanceof SpriteModel) {
			list.add(new EmptyBodySection(this));
		}

		return list;

	}

	@Override
	public void dispose() {

		_editor.removePropertyPage(this);

		super.dispose();
	}

}
