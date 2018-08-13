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

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Point;

import phasereditor.assetpack.core.AtlasAssetModel;
import phasereditor.assetpack.core.IAssetFrameModel;
import phasereditor.assetpack.core.ImageAssetModel;
import phasereditor.assetpack.core.MultiAtlasAssetModel;
import phasereditor.assetpack.core.SpritesheetAssetModel;
import phasereditor.assetpack.core.animations.AnimationFrameModel;
import phasereditor.assetpack.core.animations.AnimationModel;
import phasereditor.ui.FrameData;
import phasereditor.ui.TreeCanvasViewer;
import phasereditor.ui.TreeCanvas;
import phasereditor.ui.TreeCanvas.IconType;
import phasereditor.ui.TreeCanvas.TreeCanvasItem;

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
	protected void setItemIconProperties(TreeCanvasItem item, Object elem) {

		IFile file = null;
		FrameData fd = null;

		if (elem instanceof IAssetFrameModel) {
			var asset = (IAssetFrameModel) elem;
			fd = asset.getFrameData();
			file = asset.getImageFile();
		} else if (elem instanceof ImageAssetModel) {
			var asset = (ImageAssetModel) elem;
			fd = asset.getFrame().getFrameData();
			file = asset.getFrame().getImageFile();
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
				fd = frame.getFrameData();
				file = frame.getImageFile();
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
					fd = assetFrame.getFrameData();
					file = assetFrame.getImageFile();
				}
			}
		}

		if (file == null || fd == null) {
			super.setItemIconProperties(item, elem);
		} else {
			item.setFrameData(fd);
			item.setImageFile(file);
			item.setIconType(IconType.IMAGE_FRAME);
		}
	}

}
