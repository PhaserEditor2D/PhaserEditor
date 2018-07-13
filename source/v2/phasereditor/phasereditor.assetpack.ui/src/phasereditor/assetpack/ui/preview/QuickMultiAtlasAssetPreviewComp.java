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
package phasereditor.assetpack.ui.preview;

import static java.util.stream.Collectors.toList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;

import phasereditor.assetpack.core.MultiAtlasAssetModel;
import phasereditor.ui.FrameGridCanvas;

public class QuickMultiAtlasAssetPreviewComp extends FrameGridCanvas {

	private MultiAtlasAssetModel _model;

	public QuickMultiAtlasAssetPreviewComp(Composite parent, int style) {
		super(parent, style);
	}

	public void setModel(MultiAtlasAssetModel model) {
		_model = model;

		var frames = getSortedFrames();

		Map<IFile, Image> imageCache = new HashMap<>();

		loadFrameProvider(new IFrameProvider() {

			@Override
			public String getFrameTooltip(int index) {
				return null;
			}

			@Override
			public Rectangle getFrameRectangle(int index) {
				return frames.get(index).getFrameData().src;
			}

			@Override
			public Image getFrameImage(int index) {
				MultiAtlasAssetModel.Frame frame = frames.get(index);
				IFile file = frame.getAsset().getFileFromUrl(frame.getTextureUrl());

				if (file == null) {
					return null;
				}

				if (imageCache.containsKey(file)) {
					return imageCache.get(file);
				}

				Image img = new Image(getDisplay(), file.getLocation().toFile().getAbsolutePath());
				imageCache.put(file, img);

				return img;
			}

			@Override
			public int getFrameCount() {
				return frames.size();
			}
		});

		imageCache.clear();

		resetZoom();

	}

	@Override
	public void dispose() {

		disposeImages();

		super.dispose();
	}

	public MultiAtlasAssetModel getModel() {
		return _model;
	}

	private List<MultiAtlasAssetModel.Frame> getSortedFrames() {
		return getModel().getSubElements().stream()
				.sorted((f1, f2) -> f1.getKey().toLowerCase().compareTo(f2.getKey().toLowerCase())).collect(toList());
	}
}
