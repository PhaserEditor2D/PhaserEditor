// The MIT License (MIT)
//
// Copyright (c) 2015, 2019 Arian Fornaris
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
package phasereditor.audio.ui;

import static java.lang.System.out;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;

import org.eclipse.core.resources.IFile;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

import phasereditor.audio.core.AudioCore;

/**
 * @author arian
 *
 */
public class WebAudioPlayer extends Composite {

	private Browser _browser;
	private static String _playerHTML;

	public WebAudioPlayer(Composite parent, int style) {
		super(parent, style);

		setLayout(new FillLayout());

		_browser = new Browser(this, SWT.None);

	}

	public void load(IFile audioFile) {
		load(audioFile, false);
	}

	public void load(IFile audioFile, boolean autoplay) {

		if (audioFile == null) {
			_browser.setText("");
			return;
		}

		var imageFile = AudioCore.getSoundWavesFile(audioFile);

		var audioFileUrl = audioFile.getLocation().toFile().toPath().toUri().toString();
		var imageFileUrl = imageFile == null ? "" : imageFile.toUri().toString();
		var filename = audioFile.getName();

		out.println("Load audio file :" + audioFileUrl);
		out.println("Load image file :" + imageFileUrl);

		if (_playerHTML == null) {
			try {
				var url = new URL("platform:/plugin/phasereditor.audio.ui/html/player.html");
				try (InputStream input = url.openStream()) {
					var reader = new BufferedInputStream(input);
					var bytes = reader.readAllBytes();
					var html = new String(bytes);
					_playerHTML = html;
				}
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}

		var html = _playerHTML

				.replace("$image-url$", imageFileUrl)

				.replace("$audio-url$", audioFileUrl)

				.replace("$autoplay$", autoplay? "autoplay='true'" : "")
				
				.replace("$filename$", filename);

		out.println(html);
		
		_browser.setText(html);
	}

}
