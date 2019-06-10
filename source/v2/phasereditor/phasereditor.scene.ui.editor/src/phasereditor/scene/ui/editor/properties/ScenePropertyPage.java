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

import org.eclipse.swt.widgets.Composite;

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
	public void createControl(Composite parent) {
		super.createControl(parent);

		registerUndoRedoActions();
	}

	private void registerUndoRedoActions() {
		_editor.getUndoRedoGroup().fillActionBars(getSite().getActionBars());
	}

	@Override
	protected List<FormPropertySection<?>> createSections() {
		var list = new ArrayList<FormPropertySection<?>>();

		// Scene sections

		list.add(new SnappingSection(this));

		list.add(new DisplaySection(this));

		list.add(new CompilerSection(this));

		list.add(new WebViewSection(this));

		// Object sections

		list.add(new VariableSection(this));

		list.add(new GameObjectEditorSection(this));

		list.add(new GameObjectSection(this));

		list.add(new TransformSection(this));

		list.add(new OriginSection(this));

		list.add(new FlipSection(this));

		list.add(new ScrollFactorSection(this));

		list.add(new TintSection(this));

		list.add(new TextureSection(this));

		list.add(new TileSpriteSection(this));

		list.add(new TextualSection(this));

		list.add(new BitmapTextSection(this));

		list.add(new DynamicBitmapTextSection(this));

		list.add(new TextObjectSection(this));

		list.add(new AnimationsSection(this));

		list.add(new GroupSection(this));

		// list.add(new EmptyBodySection(this));

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
