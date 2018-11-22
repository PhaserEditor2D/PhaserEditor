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

import phasereditor.scene.core.AnimationsComponent;
import phasereditor.scene.core.BitmapTextComponent;
import phasereditor.scene.core.DynamicBitmapTextComponent;
import phasereditor.scene.core.FlipComponent;
import phasereditor.scene.core.GameObjectEditorComponent;
import phasereditor.scene.core.GroupComponent;
import phasereditor.scene.core.OriginComponent;
import phasereditor.scene.core.SceneModel;
import phasereditor.scene.core.TextualComponent;
import phasereditor.scene.core.TextureComponent;
import phasereditor.scene.core.TileSpriteComponent;
import phasereditor.scene.core.TransformComponent;
import phasereditor.scene.core.VariableComponent;
import phasereditor.scene.ui.editor.SceneEditor;
import phasereditor.ui.properties.FormPropertyPage;
import phasereditor.ui.properties.FormPropertySection;

/**
 * @author arian
 *
 */
@SuppressWarnings("rawtypes")
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

		if (obj instanceof SceneModel) {
			list.add(new SnappingSection(this));
			list.add(new CompilerSection(this));
			list.add(new DisplaySection(this));
			list.add(new AssetsSection(this));
		}

		if (VariableComponent.is(obj)) {
			list.add(new VariableSection(this));
		}

		if (GameObjectEditorComponent.is(obj)) {
			list.add(new EditorSection(this));
		}

		if (TransformComponent.is(obj)) {
			list.add(new TransformSection(this));
		}

		if (OriginComponent.is(obj)) {
			list.add(new OriginSection(this));
		}

		if (FlipComponent.is(obj)) {
			list.add(new FlipSection(this));
		}

		// if (VisibleComponent.is(obj)) {
		// list.add(new VisibleSection(this));
		// }

		if (TextureComponent.is(obj)) {
			list.add(new TextureSection(this));
		}

		if (TileSpriteComponent.is(obj)) {
			list.add(new TileSpriteSection(this));
		}

		if (TextualComponent.is(obj)) {
			list.add(new TextualSection(this));
		}

		if (BitmapTextComponent.is(obj)) {
			list.add(new BitmapTextSection(this));
		}

		if (DynamicBitmapTextComponent.is(obj)) {
			list.add(new DynamicBitmapTextSection(this));
		}

		if (AnimationsComponent.is(obj)) {
			list.add(new AnimationsSection(this));
		}

		if (GroupComponent.is(obj)) {
			list.add(new GroupSection(this));
		}

		// if (obj instanceof SpriteModel) {
		// list.add(new EmptyBodySection(this));
		// }

		return list;

	}

	@Override
	public void dispose() {

		_editor.removePropertyPage(this);

		super.dispose();
	}

	@Override
	protected Object getDefaultModel() {
		return getEditor().getSceneModel();
	}

}
