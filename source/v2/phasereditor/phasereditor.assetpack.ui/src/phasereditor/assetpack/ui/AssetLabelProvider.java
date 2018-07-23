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

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;

import javax.imageio.ImageIO;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import phasereditor.assetpack.core.AssetGroupModel;
import phasereditor.assetpack.core.AssetModel;
import phasereditor.assetpack.core.AssetPackModel;
import phasereditor.assetpack.core.AssetSectionModel;
import phasereditor.assetpack.core.AssetType;
import phasereditor.assetpack.core.AtlasAssetModel;
import phasereditor.assetpack.core.AtlasAssetModel.Frame;
import phasereditor.assetpack.core.AudioAssetModel;
import phasereditor.assetpack.core.BitmapFontAssetModel;
import phasereditor.assetpack.core.IAssetElementModel;
import phasereditor.assetpack.core.ImageAssetModel;
import phasereditor.assetpack.core.MultiAtlasAssetModel;
import phasereditor.assetpack.core.ScriptAssetModel;
import phasereditor.assetpack.core.SpritesheetAssetModel;
import phasereditor.assetpack.core.TilemapAssetModel;
import phasereditor.assetpack.core.VideoAssetModel;
import phasereditor.assetpack.ui.AssetsContentProvider.Container;
import phasereditor.atlas.core.AtlasData;
import phasereditor.atlas.core.AtlasFrame;
import phasereditor.audio.core.AudioCore;
import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.IEditorSharedImages;
import phasereditor.ui.IconCache;

public class AssetLabelProvider extends LabelProvider implements IEditorSharedImages {

	public final static AssetLabelProvider GLOBAL_16 = new AssetLabelProvider(16, true);
	public final static AssetLabelProvider GLOBAL_48 = new AssetLabelProvider(48, true);
	public final static AssetLabelProvider GLOBAL_64 = new AssetLabelProvider(64, true);

	private final int _iconSize;
	private WorkbenchLabelProvider _workbenchLabelProvider;
	private IconCache _cache = new IconCache();
	private BufferedImage _filmOverlay;
	private boolean _global;

	protected AssetLabelProvider(int iconSize, boolean global) {
		_iconSize = iconSize;
		_global = global;
		_workbenchLabelProvider = new WorkbenchLabelProvider();
		try {
			_filmOverlay = getFilmOverlay();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("static-method")
	protected BufferedImage getFilmOverlay() throws IOException, MalformedURLException {
		return ImageIO.read(new URL("platform:/plugin/phasereditor.ui/icons/film_overlay.png"));
	}

	public IconCache getCache() {
		return _cache;
	}

	@Override
	public void dispose() {
		super.dispose();
		if (!_global) {
			_cache.dispose();
		}
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
			return _workbenchLabelProvider.getImage(element);
		}

		{
			// the simple image assets

			IFile file = null;
			if (element instanceof ImageAssetModel) {
				file = ((ImageAssetModel) element).getUrlFile();
			}

			if (element instanceof ImageAssetModel.Frame) {
				file = ((ImageAssetModel.Frame) element).getImageFile();
			}

			if (element instanceof AtlasAssetModel) {
				file = ((AtlasAssetModel) element).getTextureFile();
			}

			if (element instanceof MultiAtlasAssetModel) {
				var frames = ((MultiAtlasAssetModel) element).getSubElements();
				if (!frames.isEmpty()) {
					return getImage(frames.get(0));
				}
			}

			if (element instanceof BitmapFontAssetModel) {
				file = ((BitmapFontAssetModel) element).getTextureFile();
			}

			if (file != null && file.exists()) {
				try {
					return getIcon(file);
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
					Image icon = _cache.getIcon(wavesFile, _iconSize, null);
					return icon == null ? getKeyImage() : icon;
				}
			}
		}

		if (element instanceof VideoAssetModel) {
			VideoAssetModel asset = (VideoAssetModel) element;
			List<IFile> files = asset.getUrlFiles();
			Path snapshotFile = null;
			for (IFile file : files) {
				snapshotFile = AudioCore.getVideoSnapshotFile(file, false);
				if (snapshotFile != null) {
					Image icon = _cache.getIcon(snapshotFile, _iconSize, _filmOverlay);
					return icon == null ? getKeyImage() : icon;
				}
			}
		}

		if (element instanceof SpritesheetAssetModel) {
			SpritesheetAssetModel asset = (SpritesheetAssetModel) element;
			IFile file = asset.getUrlFile();
			if (file != null) {
				// try {
				// Rectangle b = PhaserEditorUI.getImageBounds(file);
				// List<FrameData> frames = AssetPackUI.generateSpriteSheetRects(asset, b);
				// if (frames.isEmpty()) {
				// return getIcon(file);
				// }
				// FrameData fd = frames.get(0);
				// return _cache.getIcon(file, fd.src, _iconSize, null);
				// } catch (Exception e) {
				// e.printStackTrace();
				// }
				return _cache.getIcon(file, _iconSize, null);
			}
		}

		if (element instanceof Frame) {
			Frame frame = (Frame) element;
			IFile file = frame.getAsset().getTextureFile();
			var img = getAtlasFrameImage(frame, file);
			if (img != null) {
				return img;
			}
		}

		if (element instanceof MultiAtlasAssetModel.Frame) {
			var frame = (MultiAtlasAssetModel.Frame) element;
			var file = frame.getImageFile();
			var img = getAtlasFrameImage(frame, file);
			if (img != null) {
				return img;
			}
		}

		if (element instanceof SpritesheetAssetModel.FrameModel) {
			SpritesheetAssetModel.FrameModel frame = (SpritesheetAssetModel.FrameModel) element;
			SpritesheetAssetModel asset = (frame).getAsset();
			IFile file = asset.getUrlFile();
			if (file != null) {
				try {
					Rectangle b = frame.getBounds();
					return _cache.getIcon(file, b, _iconSize, null);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		if (element instanceof ScriptAssetModel) {
			IFile file = ((ScriptAssetModel) element).getUrlFile();
			if (file != null) {
				return WorkbenchLabelProvider.getDecoratingWorkbenchLabelProvider().getImage(file);
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

		if (element instanceof AssetModel) {
			return getKeyImage();
		}

		if (element instanceof AssetPackModel) {
			return getPackageImage();
		}

		if (element instanceof IAssetElementModel) {
			return getElementImage();
		}

		if (element instanceof AtlasData) {
			return EditorSharedImages.getImage(IEditorSharedImages.IMG_IMAGES);
		}

		if (element instanceof AssetType) {
			return getFileTypeImage();
		}

		return getFolderImage();
	}

	private Image getAtlasFrameImage(AtlasFrame frame, IFile imageFile) {
		if (imageFile != null) {
			try {
				Rectangle src;
				if (frame instanceof AtlasAssetModel.Frame) {
					src = ((AtlasAssetModel.Frame) frame).getFrameData().src;
				} else {
					src = new Rectangle(frame.getFrameX(), frame.getFrameY(), frame.getFrameW(), frame.getFrameH());
				}
				return _cache.getIcon(imageFile, src, _iconSize, null);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
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

	public Image getIcon(IFile file) {
		return _cache.getIcon(file, _iconSize, null);
	}

	public Image getIcon(String filename) {
		return _cache.getIcon(filename, _iconSize, null);
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

		if (element instanceof SpritesheetAssetModel.FrameModel) {
			SpritesheetAssetModel.FrameModel frame = (SpritesheetAssetModel.FrameModel) element;
			return frame.getIndex() + " " + frame.getAsset().getKey();
		}

		if (element instanceof IAssetElementModel) {
			return ((IAssetElementModel) element).getName();
		}

		if (element instanceof AtlasData) {
			return ((AtlasData) element).getFile().getName();
		}

		if (element instanceof Container) {
			return ((Container) element).name;
		}

		return super.getText(element);
	}
}