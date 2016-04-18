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

import java.nio.file.Path;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import phasereditor.assetpack.core.AssetGroupModel;
import phasereditor.assetpack.core.AssetModel;
import phasereditor.assetpack.core.AssetPackModel;
import phasereditor.assetpack.core.AssetSectionModel;
import phasereditor.assetpack.core.AtlasAssetModel;
import phasereditor.assetpack.core.AtlasAssetModel.FrameItem;
import phasereditor.assetpack.core.AudioAssetModel;
import phasereditor.assetpack.core.BitmapFontAssetModel;
import phasereditor.assetpack.core.IAssetElementModel;
import phasereditor.assetpack.core.ImageAssetModel;
import phasereditor.assetpack.core.SpritesheetAssetModel;
import phasereditor.assetpack.ui.AssetPackUI.FrameData;
import phasereditor.audio.core.AudioCore;
import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.IEditorSharedImages;
import phasereditor.ui.ImageFileCache;

public class AssetLabelProvider extends LabelProvider implements IEditorSharedImages {
	private static final int ICON_SIZE = 32;
	private WorkbenchLabelProvider _workbenchLabelProvider;
	private ImageFileCache _cache = new ImageFileCache();

	public AssetLabelProvider() {
		_workbenchLabelProvider = new WorkbenchLabelProvider();
	}

	@Override
	public void dispose() {
		super.dispose();
		_cache.dispose();
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

	@SuppressWarnings("deprecation")
	@Override
	public Image getImage(Object element) {
		if (element instanceof IResource) {
			return _workbenchLabelProvider.getImage(element);
		}

		{
			// the simple image assets

			IFile file = null;
			if (element instanceof ImageAssetModel) {
				file = ((ImageAssetModel) element).getUrlFile();
			}

			if (element instanceof AtlasAssetModel) {
				file = ((AtlasAssetModel) element).getTextureFile();
			}

			if (element instanceof BitmapFontAssetModel) {
				file = ((BitmapFontAssetModel) element).getTextureFile();
			}

			if (file != null) {
				try {
					Image img = getFileImage(file);
					return img;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		if (element instanceof AudioAssetModel) {
			AudioAssetModel asset = (AudioAssetModel) element;
			List<IFile> files = asset.getFilesFromUrls(asset.getUrls());
			Path wavesFile = null;
			for (IFile file : files) {
				wavesFile = AudioCore.getSoundWavesFile(file, false);
				if (wavesFile != null) {
					break;
				}
			}
			if (wavesFile != null) {
				Image orig = getFileImage(wavesFile);
				Image copy = new Image(Display.getCurrent(), ICON_SIZE, ICON_SIZE);
				GC gc = new GC(copy);
				gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_WHITE));
				gc.setXORMode(true);
				gc.drawImage(orig, 0, 0, orig.getBounds().width, orig.getBounds().height, 0, 0, ICON_SIZE, ICON_SIZE);
				gc.dispose();
				_cache.addExtraImageToDispose(copy);
				return copy;
			}
		}

		if (element instanceof SpritesheetAssetModel) {
			SpritesheetAssetModel asset = (SpritesheetAssetModel) element;
			IFile file = asset.getUrlFile();
			if (file != null) {
				try {
					Image img = getFileImage(file);
					if (img != null) {
						Rectangle b = img.getBounds();
						List<FrameData> frames = AssetPackUI.generateSpriteSheetRects(asset, b, b);
						if (frames.isEmpty()) {
							return img;
						}
						FrameData fd = frames.get(0);
						return buildFrameImage(img, fd);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		if (element instanceof FrameItem) {
			FrameItem frame = (FrameItem) element;
			IFile file = ((AtlasAssetModel) frame.getAsset()).getTextureFile();
			if (file != null) {
				try {
					Image texture = getFileImage(file);
					FrameData fd = new FrameData();
					fd.src = new Rectangle(frame.getFrameX(), frame.getFrameY(), frame.getFrameW(), frame.getFrameH());
					fd.dst = new Rectangle(0, 0, frame.getFrameW(), frame.getFrameH());
					return buildFrameImage(texture, fd);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		if (element instanceof SpritesheetAssetModel.FrameModel) {
			SpritesheetAssetModel.FrameModel frame = (SpritesheetAssetModel.FrameModel) element;
			SpritesheetAssetModel asset = (frame).getAsset();
			IFile file = asset.getUrlFile();
			if (file != null) {
				try {
					Image texture = getFileImage(file);
					FrameData fd = new FrameData();
					Rectangle b = frame.getBounds();
					fd.src = b;
					fd.dst = new Rectangle(0, 0, b.width, b.height);
					return buildFrameImage(texture, fd);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
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

	private Image buildFrameImage(Image texture, FrameData fd) {
		Image frameImg = new Image(Display.getCurrent(), fd.dst);

		GC gc = new GC(frameImg);
		gc.drawImage(texture, fd.src.x, fd.src.y, fd.src.width, fd.src.height, fd.dst.x, fd.dst.y, fd.dst.width,
				fd.dst.height);
		gc.dispose();
		_cache.addExtraImageToDispose(frameImg);
		return frameImg;
	}

	private Image getFileImage(IFile file) {
		return _cache.getImage(file);
	}

	private Image getFileImage(Path file) {
		return _cache.getImage(file);
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
			return pack.getFile().getProject().getName() + "/" + pack.getName();
		}

		if (element instanceof IAssetElementModel) {
			return ((IAssetElementModel) element).getName();
		}

		return super.getText(element);
	}
}