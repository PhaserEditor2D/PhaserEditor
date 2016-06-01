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
package phasereditor.canvas.ui.editors.palette;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.FileLocator;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.BorderPane;

/**
 * @author arian
 *
 */
public class BasePaletteNode extends BorderPane implements IPaletteNode {
	private ImageView _imageView;

	public BasePaletteNode(IFile imageFile) {
		_imageView = new ImageView("file:" + imageFile.getLocation().makeAbsolute().toPortableString());
		setStyle("-fx-border-style:solid;-fx-border-color:silver;");
		setCenter(_imageView);
	}

	public ImageView getImageView() {
		return _imageView;
	}

	@Override
	public void configure(double size) {
		_imageView.setPreserveRatio(true);
		_imageView.setFitWidth(size);
		_imageView.setFitHeight(size);

		setMinSize(size + 10, size + 10);
		setMaxSize(size + 10, size + 10);

		// Image img = new Image(
		// "file:C:/Users/arian/Documents/Source/PhaserEditor/Public/source/phasereditor/phasereditor.ui/icons/preview-pattern.png");

		try {
			URL url = FileLocator
					.resolve(new URL("platform:/plugin/phasereditor.ui/icons/preview-pattern.png"));
			try (InputStream stream = url.openStream();) {
				Image img = new Image(stream);
				BackgroundImage backgroundImage = new BackgroundImage(img, BackgroundRepeat.REPEAT,
						BackgroundRepeat.REPEAT, BackgroundPosition.DEFAULT, BackgroundSize.DEFAULT);
				Background background = new Background(backgroundImage);
				setBackground(background);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
