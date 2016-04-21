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

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Link;
import org.eclipse.wb.swt.ResourceManager;

import javafx.scene.media.MediaPlayer.Status;
import phasereditor.assetpack.core.VideoAssetModel;

/**
 * @author arian
 *
 */
public class VideoPreviewComp extends Composite {

	VideoCanvas _videoCanvas;
	private Button _controlButton;
	private Link _link;

	public VideoPreviewComp(Composite parent, int style) {
		super(parent, style);
		GridLayout gridLayout = new GridLayout(1, false);
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		gridLayout.horizontalSpacing = 0;
		gridLayout.verticalSpacing = 0;
		setLayout(gridLayout);

		_videoCanvas = new VideoCanvas(this, SWT.NONE);
		_videoCanvas.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		Composite composite = new Composite(this, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));

		_controlButton = new Button(composite, SWT.TOGGLE);
		_controlButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (_videoCanvas.getMediaView().getMediaPlayer().getStatus() == Status.PLAYING) {
					_videoCanvas.getMediaView().getMediaPlayer().stop();
				} else {
					_videoCanvas.getMediaView().getMediaPlayer().play();
				}
			}
		});
		_controlButton.setImage(ResourceManager.getPluginImage("phasereditor.ui", "icons/control_play.png"));

		_link = new Link(composite, SWT.NONE);
		_link.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				openInSystem();
			}
		});
		_link.setText("<a>Open in system player</a>");

		afterCreateWidgets();
	}

	private void afterCreateWidgets() {
		_videoCanvas.setAutoPlay(false);
	}

	protected void openInSystem() {
		if (_videoCanvas.getVideoFile() != null) {
			try {
				Desktop.getDesktop().open(new File(_videoCanvas.getVideoFile().getLocation().toPortableString()));
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
	}

	public VideoCanvas getVideoCanvas() {
		return _videoCanvas;
	}

	public void setModel(VideoAssetModel model) {
		_videoCanvas.setModel(model);
		videoSet();
	}

	public void setVideoFile(IFile file) {
		_videoCanvas.setVideoFile(file);
		videoSet();
	}

	private void videoSet() {
		_controlButton.setSelection(_videoCanvas.isAutoPlay());
	}

}
