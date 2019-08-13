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

import static phasereditor.ui.PhaserEditorUI.getWorkbenchLabelProvider;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import phasereditor.assetpack.core.AbstractFileAssetModel;
import phasereditor.assetpack.core.AnimationsAssetModel;
import phasereditor.assetpack.core.AssetGroupModel;
import phasereditor.assetpack.core.AssetModel;
import phasereditor.assetpack.core.AssetPackModel;
import phasereditor.assetpack.core.AssetSectionModel;
import phasereditor.assetpack.core.AssetType;
import phasereditor.assetpack.core.CssAssetModel;
import phasereditor.assetpack.core.IAssetElementModel;
import phasereditor.assetpack.core.MultiScriptAssetModel;
import phasereditor.assetpack.core.SceneFileAssetModel;
import phasereditor.assetpack.core.ScriptAssetModel;
import phasereditor.assetpack.core.SpritesheetAssetModel;
import phasereditor.assetpack.core.TilemapAssetModel;
import phasereditor.assetpack.core.animations.AnimationFrameModel;
import phasereditor.assetpack.core.animations.AnimationModel;
import phasereditor.assetpack.core.animations.AnimationsModel;
import phasereditor.assetpack.ui.AssetsContentProvider.Container;
import phasereditor.atlas.core.AtlasData;
import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.IEditorSharedImages;

public class AssetLabelProvider extends LabelProvider implements IEditorSharedImages {

	public final static AssetLabelProvider GLOBAL_16 = new AssetLabelProvider();

	protected AssetLabelProvider() {
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

	public static Image getGroupImage() {
		return EditorSharedImages.getImage(IMG_TYPE_VARIABLE_OBJ);
	}

	@Override
	public Image getImage(Object element) {
		if (element == null) {
			return null;
		}

		if (element instanceof IResource) {
			return getWorkbenchLabelProvider().getImage(element);
		}

		if (element instanceof MultiScriptAssetModel) {
			return EditorSharedImages.getImage(IMG_GENERIC_EDITOR);
		}

		if (element instanceof ScriptAssetModel || element instanceof CssAssetModel) {
			IFile file = ((AbstractFileAssetModel) element).getUrlFile();
			if (file != null) {
				return getWorkbenchLabelProvider().getImage(file);
			}
		}

		if (element instanceof SceneFileAssetModel) {
			IFile file = ((SceneFileAssetModel) element).getUrlFile();
			if (file != null) {
				return getWorkbenchLabelProvider().getImage(file);
			}
		}

		if (element instanceof TilemapAssetModel) {
			return EditorSharedImages.getImage(IMG_TILED);
		}

		if (element instanceof AssetSectionModel) {
			return getSectionImage();
		}

		if (element instanceof AssetGroupModel) {
			return getGroupImage();
		}

		if (element instanceof AssetPackModel) {
			return getPackageImage();
		}

		if (element instanceof AtlasData) {
			return EditorSharedImages.getImage(IEditorSharedImages.IMG_IMAGES);
		}

		if (element instanceof AssetType) {
			return getFileTypeImage();
		}

		if (element instanceof AnimationsAssetModel) {
			return getAnimationIcon();
		}

		if (element instanceof AnimationsModel) {
			return getAnimationIcon();
		}

		if (element instanceof AnimationModel) {
			var animation = (AnimationModel) element;
			var frames = animation.getFrames();

			if (frames.isEmpty()) {
				return getAnimationIcon();
			}

			var frame = frames.get((int) (frames.size() * 0.3));
			return getImage(frame.getAssetFrame());
		}

		if (element instanceof AnimationFrameModel) {
			var frame = (AnimationFrameModel) element;
			return getImage(frame.getAssetFrame());
		}

		// Keep this at the end!!!

		if (element instanceof AssetModel) {
			return getKeyImage();
		}

		if (element instanceof IAssetElementModel) {
			return getElementImage();
		}

		return getFolderImage();
	}

	public static Image getAnimationIcon() {
		return EditorSharedImages.getImage(IEditorSharedImages.IMG_FRAME_ANIMATION);
	}

	public static Image getFileTypeImage() {
		return EditorSharedImages.getImage(IEditorSharedImages.IMG_TYPE_VARIABLE_OBJ);
	}

	public static Image getPackageImage() {
		return EditorSharedImages.getImage(IMG_BOX);
	}

	public static Image getSectionImage() {
		// return EditorSharedImages.getImage(IMG_ASSET_FOLDER);
		return getFolderImage();
	}

	@Override
	public String getText(Object element) {
		if (element instanceof IResource) {
			return getWorkbenchLabelProvider().getText(element);
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

		if (element instanceof SpritesheetAssetModel.FrameModel) {
			SpritesheetAssetModel.FrameModel frame = (SpritesheetAssetModel.FrameModel) element;
			return frame.getIndex() + " " + frame.getAsset().getKey();
		}

		if (element instanceof AtlasData) {
			return ((AtlasData) element).getFile().getName();
		}

		if (element instanceof AnimationsModel) {
			IFile file = ((AnimationsModel) element).getFile();
			if (file != null) {
				return file.getName();
			}
		}

		if (element instanceof AnimationModel) {
			return ((AnimationModel) element).getKey();
		}

		if (element instanceof AnimationFrameModel) {
			AnimationFrameModel frame = (AnimationFrameModel) element;
			if (frame.getFrameName() == null) {
				return frame.getTextureKey();
			}
			return frame.getFrameName() + " - " + frame.getTextureKey();
		}

		// keep this at the end!!!

		if (element instanceof Container) {
			return ((Container) element).name;
		}

		if (element instanceof IAssetElementModel) {
			return ((IAssetElementModel) element).getName();
		}

		return super.getText(element);
	}
}