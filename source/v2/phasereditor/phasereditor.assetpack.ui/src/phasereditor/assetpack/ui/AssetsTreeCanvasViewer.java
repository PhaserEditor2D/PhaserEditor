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

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;

import phasereditor.animation.ui.AnimationTreeCanvasItemRenderer;
import phasereditor.animation.ui.AnimationsAssetTreeCanvasItemRenderer;
import phasereditor.assetpack.core.AnimationsAssetModel;
import phasereditor.assetpack.core.AssetPackModel;
import phasereditor.assetpack.core.AtlasAssetModel;
import phasereditor.assetpack.core.AudioAssetModel;
import phasereditor.assetpack.core.AudioSpriteAssetModel;
import phasereditor.assetpack.core.BitmapFontAssetModel;
import phasereditor.assetpack.core.IAssetFrameModel;
import phasereditor.assetpack.core.IAssetKey;
import phasereditor.assetpack.core.ImageAssetModel;
import phasereditor.assetpack.core.MultiAtlasAssetModel;
import phasereditor.assetpack.core.SpritesheetAssetModel;
import phasereditor.assetpack.core.animations.AnimationFrameModel;
import phasereditor.assetpack.core.animations.AnimationModel;
import phasereditor.assetpack.core.animations.AnimationsModel;
import phasereditor.ui.ImageProxy;
import phasereditor.ui.ImageProxyTreeCanvasItemRenderer;
import phasereditor.ui.TreeCanvas;
import phasereditor.ui.TreeCanvas.TreeCanvasItem;
import phasereditor.ui.TreeCanvasViewer;

/**
 * @author arian
 *
 */
public class AssetsTreeCanvasViewer extends TreeCanvasViewer {

	public AssetsTreeCanvasViewer(TreeCanvas tree, ITreeContentProvider contentProvider, LabelProvider labelProvider) {

		super(tree, contentProvider, labelProvider);

		AssetPackUI.installAssetTooltips(tree, tree.getUtils());
	}

	public AssetsTreeCanvasViewer(TreeCanvas tree) {
		this(tree, null, null);
	}

	@Override
	protected void setItemIconProperties(TreeCanvasItem item) {
		var elem = item.getData();

		LinkedHashSet<String> keywords = new LinkedHashSet<>();

		addKeywords(elem, keywords);

		item.setKeywords(keywords.isEmpty() ? null : keywords.stream().collect(joining(",")));

		var imageRenderer = createImageRenderer(item, elem);

		if (imageRenderer == null) {
			super.setItemIconProperties(item);
		} else {
			item.setRenderer(imageRenderer);
		}

		if (elem instanceof AnimationsAssetModel) {
			item.setRenderer(new AnimationsAssetTreeCanvasItemRenderer(item));
		} else if (elem instanceof AnimationModel) {
			item.setRenderer(new AnimationTreeCanvasItemRenderer(item));
		} else if (elem instanceof AudioSpriteAssetModel) {
			item.setRenderer(new AudioSpriteAssetTreeCanvasItemRenderer(item));
		} else if (elem instanceof AudioSpriteAssetModel.AssetAudioSprite) {
			item.setRenderer(new AudioSpriteAssetElementTreeCanvasRenderer(item));
		} else if (elem instanceof AudioAssetModel) {
			item.setRenderer(new AudioAssetTreeCanvasItemRenderer(item));
		} else if (elem instanceof BitmapFontAssetModel) {
			item.setRenderer(new BitmapFontTreeCanvasRenderer(item));
		} else if (elem instanceof MultiAtlasAssetModel) {
			item.setRenderer(new MultiAtlasAssetTreeCanvasItemRenderer(item));
		}
	}

	public static ImageProxyTreeCanvasItemRenderer createImageRenderer(TreeCanvasItem item, Object element) {

		ImageProxy proxy = getAssetKeyImageProxy(element);

		return new ImageProxyTreeCanvasItemRenderer(item, proxy);
	}

	public static ImageProxy getAssetKeyImageProxy(Object element) {
		ImageProxy proxy = null;

		if (element instanceof IAssetFrameModel) {

			var asset = (IAssetFrameModel) element;
			proxy = AssetPackUI.getImageProxy(asset);

		} else if (element instanceof ImageAssetModel) {

			var asset = (ImageAssetModel) element;
			proxy = AssetPackUI.getImageProxy(asset.getFrame());

		} else if (element instanceof AtlasAssetModel) {
			var asset = (AtlasAssetModel) element;
			proxy = ImageProxy.get(asset.getTextureFile(), null);
		} else if (element instanceof SpritesheetAssetModel) {
			var asset = (SpritesheetAssetModel) element;
			var file = asset.getUrlFile();
			proxy = ImageProxy.get(file, null);
		} else if (element instanceof AnimationFrameModel) {
			AnimationFrameModel animFrame = (AnimationFrameModel) element;
			var assetFrame = animFrame.getAssetFrame();
			proxy = AssetPackUI.getImageProxy(assetFrame);
		}

		if (proxy == null) {
			return null;
		}
		return proxy;
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

			if (elem instanceof BitmapFontAssetModel) {
				keywords.add("font");
				keywords.add("bitmap");
				keywords.add("text");
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
