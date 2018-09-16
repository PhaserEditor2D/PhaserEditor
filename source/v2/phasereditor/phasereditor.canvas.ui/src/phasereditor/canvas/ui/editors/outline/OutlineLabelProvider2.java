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
package phasereditor.canvas.ui.editors.outline;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import phasereditor.assetpack.ui.AssetLabelProvider;
import phasereditor.canvas.core.AssetSpriteModel;
import phasereditor.canvas.core.BaseObjectModel;
import phasereditor.canvas.core.CanvasType;
import phasereditor.canvas.core.GroupModel;
import phasereditor.canvas.core.TextModel;
import phasereditor.canvas.ui.editors.CanvasEditor;
import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.IEditorSharedImages;

/**
 * @author arian
 *
 */
public class OutlineLabelProvider2 extends LabelProvider implements IEditorSharedImages {

	private CanvasEditor _editor;

	public OutlineLabelProvider2(CanvasEditor editor) {
		_editor = editor;
	}

	@Override
	public String getText(Object element) {

		if (element instanceof BaseObjectModel) {
			return ((BaseObjectModel) element).getEditorName();
		}

		return super.getText(element);
	}

	@Override
	public Image getImage(Object element) {

		if (element instanceof BaseObjectModel) {
			var model = (BaseObjectModel) element;

			if (model.isPrefabInstance()) {
				return EditorSharedImages.getImage(IMG_PACKAGE_2);
			}

			if (model instanceof TextModel) {
				return EditorSharedImages.getImage(IMG_FONT);
			}
			
			if (model instanceof AssetSpriteModel<?>) {
				var asset = ((AssetSpriteModel<?>) model).getAssetKey();
				return AssetLabelProvider.GLOBAL_16.getImage(asset);
			}

			CanvasType type = _editor.getModel().getType();

			if (type == CanvasType.GROUP) {
				if (model == model.getWorld().findGroupPrefabRoot()) {
					return EditorSharedImages.getImage(IMG_PACKAGE_2);
				}
			}

			if (model instanceof GroupModel) {
				return EditorSharedImages.getImage(IMG_SHAPE_GROUP_NODE);
			}
		}

		return EditorSharedImages.getImage(IMG_SHAPE);
	}
}
