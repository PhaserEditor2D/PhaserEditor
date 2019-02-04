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

import static phasereditor.ui.PhaserEditorUI.pickFileWithoutExtension;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import phasereditor.assetpack.core.AudioSpriteAssetModel.AssetAudioSprite;
import phasereditor.audio.ui.WebAudioSpritePlayer;
import phasereditor.audiosprite.core.AudioSpriteCore;
import phasereditor.ui.info.BaseInformationControl;

public class AudioSpriteAssetElementInformationControl extends BaseInformationControl {

	public AudioSpriteAssetElementInformationControl(Shell parentShell) {
		super(parentShell);
	}

	@Override
	protected Control createContent2(Composite parentComp) {
		// return new GdxAutoMusicControl(parentComp, SWT.NONE);
		return new WebAudioSpritePlayer(parentComp, 0);
	}

	@Override
	protected void updateContent(Control control, Object model) {

		// AssetAudioSprite sprite = (AssetAudioSprite) model;
		// AudioSpriteAssetModel asset = sprite.getAsset();
		//
		// GdxAutoMusicControl audioControl = (GdxAutoMusicControl) control;
		//
		// List<AssetAudioSprite> sprites = asset.getSpriteMap();
		//
		// audioControl.setTimePartition(AudioSpriteCore.createTimePartition(sprites));
		// audioControl.setTimePartitionSelection(sprites.indexOf(sprite));
		//
		// IFile file =
		// pickFileWithoutExtension(asset.getFilesFromUrls(asset.getUrls()), "ogg",
		// "mp3");
		// audioControl.load(file);

		var sprite = (AssetAudioSprite) model;
		var asset = sprite.getAsset();

		var audioControl = (WebAudioSpritePlayer) control;

		List<AssetAudioSprite> sprites = asset.getSpriteMap();

		IFile file = pickFileWithoutExtension(asset.getFilesFromUrls(asset.getUrls()), "ogg", "mp3");

		audioControl.load(file, () -> {
			audioControl.setTimePartition(AudioSpriteCore.createTimePartition(sprites));
			audioControl.setTimePartitionSelection(sprites.indexOf(sprite));
			audioControl.play();
		});

	}

	@Override
	protected void handleHidden(Control control) {
		// GdxAutoMusicControl musicControl = (GdxAutoMusicControl) control;
		// musicControl.stop();
		// musicControl.disposeMusic();
		var player = (WebAudioSpritePlayer) control;
		player.load(null);
	}

}
