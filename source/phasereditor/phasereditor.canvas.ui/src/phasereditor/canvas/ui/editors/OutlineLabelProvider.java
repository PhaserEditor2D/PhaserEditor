// The MIT License (MIT)
//
// Copyright (c) 2015, 2016 Arian Fornaris
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
package phasereditor.canvas.ui.editors;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import phasereditor.assetpack.ui.AssetLabelProvider;
import phasereditor.canvas.core.AssetSpriteModel;
import phasereditor.canvas.core.AtlasSpriteModel;
import phasereditor.canvas.core.BaseObjectModel;
import phasereditor.canvas.core.CanvasType;
import phasereditor.canvas.core.GroupModel;
import phasereditor.canvas.core.TextModel;
import phasereditor.canvas.ui.shapes.BaseObjectControl;
import phasereditor.canvas.ui.shapes.GroupNode;
import phasereditor.canvas.ui.shapes.IObjectNode;
import phasereditor.canvas.ui.shapes.ISpriteNode;
import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.IEditorSharedImages;

/**
 * @author arian
 *
 */
public class OutlineLabelProvider extends LabelProvider implements IEditorSharedImages {

	@Override
	public Image getImage(Object element) {
		if (element instanceof IObjectNode) {
			IObjectNode node = (IObjectNode) element;
			BaseObjectModel model = node.getModel();

			if (model.isPrefabInstance()) {
				return EditorSharedImages.getImage(IMG_PACKAGE_2);
			}
			
			if (model instanceof TextModel) {
				return EditorSharedImages.getImage(IMG_FONT);
			}

			CanvasType type = node.getControl().getCanvas().getEditor().getModel().getType();
			if (type == CanvasType.GROUP) {
				if (model == model.getWorld().findGroupPrefabRoot()) {
					return EditorSharedImages.getImage(IMG_PACKAGE_2);
				}
			}
		}

		if (element instanceof GroupNode) {
			return EditorSharedImages.getImage(IMG_SHAPE_GROUP_NODE);
		}

		if (element instanceof ISpriteNode) {
			ISpriteNode node = (ISpriteNode) element;
			BaseObjectControl<?> control = node.getControl();
			BaseObjectModel model = control.getModel();

			if (model instanceof AssetSpriteModel) {
				AssetSpriteModel<?> asstModel = (AssetSpriteModel<?>) model;
				return AssetLabelProvider.GLOBAL_16.getImage(asstModel.getAssetKey());
			}

			if (model instanceof AtlasSpriteModel) {
				return AssetLabelProvider.GLOBAL_16.getImage(((AtlasSpriteModel) model).getAssetKey());
			}
		}

		return EditorSharedImages.getImage(IMG_SHAPE);
	}

	@Override
	public String getText(Object element) {

		if (element instanceof IObjectNode) {

			if (((IObjectNode) element).getGroup() == null) {
				return "<world>";
			}

			StringBuilder sb = new StringBuilder();

			IObjectNode node = (IObjectNode) element;
			BaseObjectModel model = node.getModel();

			CanvasType type = node.getControl().getCanvas().getEditor().getModel().getType();
			if (type == CanvasType.GROUP) {
				if (model == model.getWorld().findGroupPrefabRoot()) {
					sb.append("[prefab] ");
				}
			}

			if (sb.length() == 0) {
				sb.append(model.getEditorName() + " ");
			}

			if (element instanceof GroupNode) {
				GroupModel group = (GroupModel) model;
				if (group.isEditorClosed() && !group.isPrefabInstance()) {
					sb.append("[c]");
				}
			}

			if (!model.isEditorPick()) {
				sb.append("[-p]");
			}

			return sb.toString();
		}

		return super.getText(element);
	}
}
