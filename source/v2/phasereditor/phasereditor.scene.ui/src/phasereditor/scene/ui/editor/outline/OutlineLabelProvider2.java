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
package phasereditor.scene.ui.editor.outline;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import phasereditor.scene.core.DisplayListModel;
import phasereditor.scene.core.EditorComponent;
import phasereditor.scene.core.GroupComponent;
import phasereditor.scene.core.GroupsModel;
import phasereditor.scene.core.ObjectModel;
import phasereditor.scene.ui.editor.SceneEditor;
import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.IEditorSharedImages;

/**
 * @author arian
 *
 */
public class OutlineLabelProvider2 extends LabelProvider implements IEditorSharedImages {

	private SceneEditor _editor;

	public OutlineLabelProvider2(SceneEditor editor) {
		_editor = editor;
	}

	public SceneEditor getEditor() {
		return _editor;
	}

	@Override
	public String getText(Object element) {
		if (element instanceof DisplayListModel) {
			return "Display List";
		}

		if (element instanceof GroupsModel) {
			return "Groups";
		}

		if (element instanceof EditorComponent) {
			return EditorComponent.get_editorName((ObjectModel) element);
		}

		if (element instanceof GroupComponent) {
			return GroupComponent.get_name((ObjectModel) element);
		}

		return super.getText(element);
	}

	@Override
	public Image getImage(Object element) {

		if (element instanceof DisplayListModel) {
			return EditorSharedImages.getImage(IMG_MONITOR);
		}

		if (element instanceof GroupsModel) {
			return EditorSharedImages.getImage(IMG_SCENE_GROUP);
		}
		
		if (element instanceof GroupComponent) {
			return EditorSharedImages.getImage(IMG_SCENE_GROUP);
		}

		return EditorSharedImages.getImage(IMG_SHAPE);
	}
}
