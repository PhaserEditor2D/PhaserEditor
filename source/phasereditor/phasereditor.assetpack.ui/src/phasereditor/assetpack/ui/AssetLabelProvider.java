// The MIT License (MIT)
//
// Copyright (c) 2015 Arian Fornaris
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
package phasereditor.assetpack.ui;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import phasereditor.assetpack.core.AssetGroupModel;
import phasereditor.assetpack.core.AssetModel;
import phasereditor.assetpack.core.AssetPackModel;
import phasereditor.assetpack.core.AssetSectionModel;
import phasereditor.assetpack.core.IAssetElementModel;
import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.IEditorSharedImages;

public class AssetLabelProvider extends LabelProvider implements IEditorSharedImages {
	private WorkbenchLabelProvider _workbenchLabelProvider;

	public AssetLabelProvider() {
		_workbenchLabelProvider = new WorkbenchLabelProvider();
	}

	public static Image getFileImage() {
		return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FILE);
	}

	public static Image getFolderImage() {
		return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
	}

	public static Image getElementImage() {
		return EditorSharedImages.getImage(IMG_ASSET_ELEM_KEY);
	}

	public static Image getKeyImage() {
		return EditorSharedImages.getImage(IMG_ASSET_KEY);
	}

	@Override
	public Image getImage(Object element) {
		if (element instanceof IResource) {
			return _workbenchLabelProvider.getImage(element);
		}

		if (element instanceof AssetSectionModel) {
			return EditorSharedImages.getImage(IMG_ASSET_FOLDER);
		}

		if (element instanceof AssetGroupModel) {
			return EditorSharedImages.getImage(IMG_ASSET_FOLDER);
		}

		if (element instanceof AssetModel) {
			return getKeyImage();
		}

		if (element instanceof AssetPackModel) {
			return EditorSharedImages.getImage(IMG_PACKAGE);
		}

		if (element instanceof IAssetElementModel) {
			return getElementImage();
		}

		return getFolderImage();
	}

	@Override
	public String getText(Object element) {
		if (element instanceof IResource) {
			return _workbenchLabelProvider.getText(element);
		}

		if (element instanceof AssetSectionModel) {
			return ((AssetSectionModel) element).getKey();
		}

		if (element instanceof AssetGroupModel) {
			return ((AssetGroupModel) element).getType().name();
		}

		if (element instanceof AssetModel) {
			AssetModel asset = (AssetModel) element;
			return asset.getKey();
		}

		if (element instanceof AssetPackModel) {
			AssetPackModel pack = (AssetPackModel) element;
			return pack.getName();
		}

		if (element instanceof IAssetElementModel) {
			return ((IAssetElementModel) element).getName();
		}

		return super.getText(element);
	}
}