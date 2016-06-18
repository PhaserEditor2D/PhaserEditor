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

import java.util.List;

import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.embed.swt.FXCanvas;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.ImagePattern;
import javafx.util.Duration;
import phasereditor.assetpack.core.FrameData;
import phasereditor.assetpack.core.IAssetFrameModel;
import phasereditor.canvas.core.AnimationModel;
import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.IEditorSharedImages;
import phasereditor.ui.ImageCache;

/**
 * @author arian
 *
 */
public class AnimationCanvas extends FXCanvas {

	private AnimationModel _model;
	private IndexTransition _anim;
	private ImageView _imgView;
	private Pane _container;

	public AnimationCanvas(Composite parent, int style) {
		super(parent, style);
		_imgView = new ImageView();
		_container = new Pane(_imgView);
		BorderPane root = new BorderPane(_container);
		ImagePattern fill = new ImagePattern(EditorSharedImages.getFXImage(IEditorSharedImages.IMG_PREVIEW_PATTERN), 0,
				0, 16, 16, false);
		root.setBackground(new Background(new BackgroundFill(fill, null, null)));
		Scene scene = new Scene(root);
		// scene.setFill(fill);
		setScene(scene);
	}

	public void setModel(AnimationModel model) {
		_model = model;

		if (_anim != null) {
			_anim.stop();
		}

		if (_model == null || _model.getFrames().isEmpty()) {
			_imgView.setImage(null);
			return;
		}

		List<IAssetFrameModel> frames = _model.getFrames();

		Image image = ImageCache.getFXImage(frames.get(0).getImageFile());
		_imgView.setImage(image);

		int size = model.getFrames().size();
		_anim = new IndexTransition(Duration.seconds(size / (double) model.getFrameRate()), size);
		if (model.isLoop()) {
			_anim.setCycleCount(Animation.INDEFINITE);
		}
		_anim.play();
	}

	void showFrame(int index) {
		List<IAssetFrameModel> frames = _model.getFrames();
		if (index >= frames.size()) {
			return;
		}

		IAssetFrameModel frame = frames.get(index);
		FrameData fd = frame.getFrameData();
		Rectangle src = fd.src;
		_container.setMinSize(fd.srcSize.x, fd.srcSize.y);
		_container.setMaxSize(fd.srcSize.x, fd.srcSize.y);
		_imgView.relocate(fd.dst.x, fd.dst.y);
		_imgView.setViewport(new Rectangle2D(src.x, src.y, src.width, src.height));
	}

	class IndexTransition extends Transition {

		private int _length;
		private int _last;

		public IndexTransition(Duration duration, int length) {
			super();
			setCycleDuration(duration);
			setInterpolator(Interpolator.LINEAR);
			_length = length;
			_last = -1;
		}

		@Override
		protected void interpolate(double frac) {
			int i = (int) (frac * _length);
			if (i != _last) {
				showFrame(i);
				_last = i;
			}
		}

	}
}
