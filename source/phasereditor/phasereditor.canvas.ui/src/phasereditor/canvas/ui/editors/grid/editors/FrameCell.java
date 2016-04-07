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
package phasereditor.canvas.ui.editors.grid.editors;

import org.eclipse.swt.graphics.Rectangle;

import javafx.geometry.Rectangle2D;
import javafx.scene.control.ListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import phasereditor.assetpack.ui.AssetPackUI.FrameData;

/**
 * @author arian
 *
 */
public class FrameCell extends ListCell<FrameData> {

	private Image _image;

	public FrameCell(Image image) {
		super();
		_image = image;
	}

	@Override
	protected void updateItem(FrameData item, boolean empty) {
		super.updateItem(item, empty);

		if (empty || item == null) {
			setGraphic(null);
			setText(null);
			return;
		}

		ImageView imgView = new ImageView(_image);
		Rectangle src = item.src;
		imgView.setViewport(new Rectangle2D(src.x, src.y, src.width, src.height));
		setGraphic(imgView);
	}
}
