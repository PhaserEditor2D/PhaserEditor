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

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IFile;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;

import phasereditor.assetpack.core.AtlasAssetModel;
import phasereditor.assetpack.core.AtlasAssetModel.Frame;
import phasereditor.ui.SpriteGridCanvas;

public class QuickAtlasAssetPreviewComp extends SpriteGridCanvas {

	private AtlasAssetModel _model;

	public QuickAtlasAssetPreviewComp(Composite parent, int style) {
		super(parent, style);
	}

	public void setModel(AtlasAssetModel model) {
		_model = model;
		String url = model.getTextureURL();
		IFile file = model.getFileFromUrl(url);

		Image img = file == null? null : new Image(getDisplay(), file.getLocation().toFile().getAbsolutePath());

		if (img == null) {
			setImage(img);
		} else {
			List<Frame> sortedFrames = getSortedFrames();

			setImage(img);
			setFrames(sortedFrames.stream().map(f -> f.getFrameData().src).collect(Collectors.toList()));

			getDisplay().asyncExec(() -> {
				fitWindow();
				redraw();
			});
		}
	}

	public AtlasAssetModel getModel() {
		return _model;
	}

	private List<Frame> getSortedFrames() {
		return getModel().getAtlasFrames().stream()
				.sorted((f1, f2) -> f1.getKey().toLowerCase().compareTo(f2.getKey().toLowerCase())).collect(toList());
	}
}
