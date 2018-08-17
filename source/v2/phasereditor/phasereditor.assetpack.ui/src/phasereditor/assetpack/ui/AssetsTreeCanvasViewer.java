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
package phasereditor.assetpack.ui;

import static java.util.stream.Collectors.joining;

import java.util.LinkedHashSet;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Point;

import phasereditor.assetpack.core.AssetPackModel;
import phasereditor.assetpack.core.AtlasAssetModel;
import phasereditor.assetpack.core.IAssetFrameModel;
import phasereditor.assetpack.core.IAssetKey;
import phasereditor.assetpack.core.ImageAssetModel;
import phasereditor.assetpack.core.MultiAtlasAssetModel;
import phasereditor.assetpack.core.SpritesheetAssetModel;
import phasereditor.assetpack.core.animations.AnimationFrameModel;
import phasereditor.assetpack.core.animations.AnimationModel;
import phasereditor.assetpack.core.animations.AnimationsModel;
import phasereditor.ui.FrameData;
import phasereditor.ui.TreeCanvas;
import phasereditor.ui.TreeCanvas.IconType;
import phasereditor.ui.TreeCanvas.TreeCanvasItem;
import phasereditor.ui.TreeCanvasViewer;

/**
 * @author arian
 *
 */
public class AssetsTreeCanvasViewer extends TreeCanvasViewer {

	public AssetsTreeCanvasViewer(TreeCanvas canvas, ITreeContentProvider contentProvider,
			LabelProvider labelProvider) {
		super(canvas, contentProvider, labelProvider);
	}

	@Override
	protected void setItemIconProperties(TreeCanvasItem item) {
		var elem = item.getData();
		IFile file = null;
		FrameData fd = null;

		if (elem instanceof IAssetFrameModel) {
			var asset = (IAssetFrameModel) elem;
			file = asset.getImageFile();
			if (file != null) {
				fd = asset.getFrameData();
			}
		} else if (elem instanceof ImageAssetModel) {
			var asset = (ImageAssetModel) elem;
			file = asset.getFrame().getImageFile();
			if (file != null) {
				fd = asset.getFrame().getFrameData();
			}
		} else if (elem instanceof AtlasAssetModel) {
			var asset = (AtlasAssetModel) elem;
			fd = new FrameData(0);
			fd.src = asset.getImageSize();
			if (fd.src != null) {
				fd.dst = fd.src;
				fd.srcSize = new Point(fd.src.width, fd.src.height);
				file = asset.getTextureFile();
			}
		} else if (elem instanceof MultiAtlasAssetModel) {
			var asset = (MultiAtlasAssetModel) elem;
			var frames = asset.getSubElements();
			if (!frames.isEmpty()) {
				var frame = frames.get(0);
				file = frame.getImageFile();
				if (file != null) {
					var img = getCanvas().loadImage(file);
					if (img != null) {
						fd = FrameData.fromImage(img);
					}
				}
			}
		} else if (elem instanceof SpritesheetAssetModel) {
			var asset = (SpritesheetAssetModel) elem;
			fd = new FrameData(0);
			fd.src = asset.getImageSize();
			if (fd.src != null) {
				fd.dst = fd.src;
				fd.srcSize = new Point(fd.src.width, fd.src.height);
				file = asset.getUrlFile();
			}
		} else if (elem instanceof AnimationFrameModel || elem instanceof AnimationModel) {
			AnimationFrameModel animFrame = null;

			if (elem instanceof AnimationFrameModel) {
				animFrame = (AnimationFrameModel) elem;
			} else {
				var anim = (AnimationModel) elem;
				if (!anim.getFrames().isEmpty()) {
					animFrame = anim.getFrames().get(0);
				}
			}

			if (animFrame != null) {
				var assetFrame = animFrame.getFrameAsset();

				if (assetFrame != null) {
					file = assetFrame.getImageFile();
					if (file != null) {
						fd = assetFrame.getFrameData();
					}
				}
			}
		}

		LinkedHashSet<String> keywords = new LinkedHashSet<>();

		addKeywords(elem, keywords);

		item.setKeywords(keywords.isEmpty() ? null : keywords.stream().collect(joining(",")));

		if (file == null || fd == null) {
			super.setItemIconProperties(item);
		} else {
			// we do this to the loading at once!
			getCanvas().loadImage(file);

			item.setFrameData(fd);
			item.setImageFile(file);
			item.setIconType(IconType.IMAGE_FRAME);
		}
	}

	@SuppressWarnings("static-method")
	private void addKeywords(Object elem, LinkedHashSet<String> keywords) {
		if (elem instanceof IAssetKey) {
			var asset = ((IAssetKey) elem).getAsset();

			if (asset instanceof ImageAssetModel) {
				keywords.add("image");
				keywords.add("texture");
			}

			if (asset instanceof SpritesheetAssetModel) {
				keywords.add("texture");
				keywords.add("spritesheet");
			}

			if (asset instanceof AtlasAssetModel || asset instanceof MultiAtlasAssetModel) {
				keywords.add("texture");
				keywords.add("atlas");
			}

			if (elem instanceof IAssetFrameModel) {
				keywords.add("texture");
				keywords.add("frame");
			}

		}

		if (elem instanceof AnimationsModel || elem instanceof AnimationModel) {
			keywords.add("animation");
		}

		if (elem instanceof AssetPackModel) {
			keywords.add("pack");
		}

		if (elem instanceof IProject) {
			keywords.add("project");
		}
	}

}
