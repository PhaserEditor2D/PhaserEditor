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

import org.eclipse.swt.graphics.Image;

import phasereditor.assetpack.ui.AssetLabelProvider;
import phasereditor.canvas.core.AssetShapeModel;
import phasereditor.canvas.core.BaseObjectModel;
import phasereditor.canvas.core.GroupModel;
import phasereditor.canvas.core.IAssetFrameShapeModel;
import phasereditor.canvas.ui.shapes.BaseObjectControl;
import phasereditor.canvas.ui.shapes.GroupNode;
import phasereditor.canvas.ui.shapes.IObjectNode;
import phasereditor.canvas.ui.shapes.SpriteNode;
import phasereditor.ui.EditorSharedImages;

/**
 * @author arian
 *
 */
public class OutlineLabelProvider extends AssetLabelProvider {

	@Override
	public Image getImage(Object element) {

		if (element instanceof GroupNode) {
			return EditorSharedImages.getImage(IMG_SHAPE_GROUP_NODE);
		}

		if (element instanceof SpriteNode) {
			SpriteNode node = (SpriteNode) element;
			BaseObjectControl<?> control = node.getControl();
			BaseObjectModel model = control.getModel();

			if (model instanceof AssetShapeModel) {
				AssetShapeModel<?> asstModel = (AssetShapeModel<?>) model;
				return super.getImage(asstModel.getAsset());
			}

			if (model instanceof IAssetFrameShapeModel) {
				IAssetFrameShapeModel asstModel = (IAssetFrameShapeModel) model;
				return super.getImage(asstModel.getFrame());
			}
		}

		return EditorSharedImages.getImage(IMG_SHAPE);
	}

	@Override
	public String getText(Object element) {

		if (element instanceof IObjectNode) {
			StringBuilder sb = new StringBuilder();
			BaseObjectModel model = ((IObjectNode) element).getControl().getModel();
			
			sb.append(model.getEditorName() + " ");

			if (element instanceof GroupNode) {
				if (((GroupModel) model).isEditorClosed()) {
					sb.append("[c]");
				}
			}

			if (!model.isEditorPick()) {
				sb.append("[np]");
			}

			return sb.toString();
		}

		return super.getText(element);
	}
}
