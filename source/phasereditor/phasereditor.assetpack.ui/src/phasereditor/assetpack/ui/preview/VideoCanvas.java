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
package phasereditor.assetpack.ui.preview;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

import javafx.embed.swt.FXCanvas;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaPlayer.Status;
import javafx.scene.media.MediaView;
import phasereditor.assetpack.core.VideoAssetModel;
import phasereditor.assetpack.ui.AssetPackUI;

/**
 * @author arian
 *
 */
public class VideoCanvas extends Composite {
	private FXCanvas _canvas;
	private MediaView _mediaView;
	private boolean _autoPlay = true;
	private IFile _file;

	/**
	 * Create the composite.
	 * 
	 * @param parent
	 * @param style
	 */
	public VideoCanvas(Composite parent, int style) {
		super(parent, style);
		setLayout(new FillLayout(SWT.HORIZONTAL));

		_canvas = new FXCanvas(this, SWT.NONE);

		afterCreateWidgets();
	}

	private void afterCreateWidgets() {
		// video player
		_mediaView = new MediaView();
		BorderPane pane = new BorderPane(_mediaView);
		pane.setStyle("-fx-background-color:black");
		Scene scene = new Scene(pane);
		_canvas.setScene(scene);
		_mediaView.setPreserveRatio(true);
		_mediaView.fitWidthProperty().bind(scene.widthProperty());
		_mediaView.fitHeightProperty().bind(scene.heightProperty());

		addDisposeListener(e -> {
			if (_mediaView != null) {
				MediaPlayer player = _mediaView.getMediaPlayer();
				if (player != null) {
					player.dispose();
				}
			}
		});
	}

	public void setAutoPlay(boolean autoPlay) {
		_autoPlay = autoPlay;
	}

	public boolean isAutoPlay() {
		return _autoPlay;
	}

	public MediaView getMediaView() {
		return _mediaView;
	}

	public void setVideoFile(IFile file) {
		_file = file;
		try {
			MediaPlayer player = _mediaView.getMediaPlayer();
			if (player != null) {
				if (player.getStatus() == Status.PLAYING) {
					player.stop();
				}
				player.dispose();
			}

			if (file == null) {
				return;
			}

			String source = file.getLocationURI().toURL().toString();
			Media media = new Media(source);
			player = new MediaPlayer(media);
			player.setAutoPlay(_autoPlay);
			player.setCycleCount(MediaPlayer.INDEFINITE);
			_mediaView.setMediaPlayer(player);
		} catch (Exception e) {
			AssetPackUI.showError(e);
		}
	}

	public IFile getVideoFile() {
		return _file;
	}

	public void setModel(VideoAssetModel model) {
		List<IFile> list = model.getUrlFiles();
		if (list.isEmpty()) {
			setVideoFile(null);
			return;
		}

		setVideoFile(list.get(0));
	}

}
