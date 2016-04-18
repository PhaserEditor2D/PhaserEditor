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
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import phasereditor.assetpack.core.AudioSpriteAssetModel;
import phasereditor.assetpack.core.AudioSpriteAssetModel.AssetAudioSprite;
import phasereditor.assetpack.ui.AssetLabelProvider;
import phasereditor.audiosprite.core.AudioSpriteCore;
import phasereditor.audiosprite.ui.GdxMusicControl;

public class AudioSpriteAssetPreviewComp extends Composite {

	private GdxMusicControl _musicControl;
	private ComboViewer _spritesViewer;
	private List<AssetAudioSprite> _sprites;
	private AudioSpriteAssetModel _model;
	private Label _filesLabel;

	private static class AudioSpritesLabelProvider extends AssetLabelProvider {

		public AudioSpritesLabelProvider() {
		}

		@Override
		public String getText(Object element) {
			if (element instanceof AssetAudioSprite) {
				AssetAudioSprite sprite = (AssetAudioSprite) element;
				return sprite.getName() + " [" + sprite.getStart() + ", " + sprite.getEnd() + "]";
			}
			return super.getText(element);
		}

	}

	/**
	 * Create the composite.
	 * 
	 * @param parent
	 * @param style
	 */
	public AudioSpriteAssetPreviewComp(Composite parent, int style) {
		super(parent, style);

		setLayout(new GridLayout(1, false));

		_spritesViewer = new ComboViewer(this, SWT.READ_ONLY);
		Combo combo = _spritesViewer.getCombo();
		combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		_spritesViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				spriteSelected();
			}
		});
		_spritesViewer.setLabelProvider(new AudioSpritesLabelProvider());
		_spritesViewer.setContentProvider(new ArrayContentProvider());

		_filesLabel = new Label(this, SWT.WRAP);
		_filesLabel.setText("files");

		_musicControl = new GdxMusicControl(this, SWT.NONE);
		_musicControl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		GridLayout gridLayout = (GridLayout) _musicControl.getLayout();
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;

		afterCreateWidgets();
	}

	private void afterCreateWidgets() {
		//
	}

	protected void spriteSelected() {
		IStructuredSelection sel = (IStructuredSelection) _spritesViewer.getSelection();
		AssetAudioSprite sprite = (AssetAudioSprite) sel.getFirstElement();
		if (sprite != null && sprite.isValid()) {
			updateSprite(sprite);
		} else {
			updateSprite(null);
		}
	}

	private void updateSprite(AssetAudioSprite sprite) {
		_musicControl.stop();
		int i = _sprites.indexOf(sprite);
		_musicControl.setTimePartitionSelection(i);
	}

	public void setModel(AudioSpriteAssetModel model) {
		_model = model;

		List<String> urls = model.getUrls();
		List<IFile> files = model.getFilesFromUrls(urls);

		{
			String label = "";
			for (IFile file : files) {
				if (label.length() > 0) {
					label += ", ";
				}
				label += file.getName();
			}
			_filesLabel.setText("files: " + label);
		}

		List<AssetAudioSprite> sprites = model.getSpriteMap();
		_sprites = sprites;
		_spritesViewer.setInput(_sprites);
		// TODO: missing
		// _audioCanvas.setContent(_sprites, null);
		IFile file = pickFileWithoutExtension(files, "ogg", "mp3");
		_musicControl.load(file);

		_musicControl.setTimePartition(AudioSpriteCore.createTimePartition(sprites));

		if (!_sprites.isEmpty()) {
			selectElement(_sprites.get(0));
		}
	}

	public AudioSpriteAssetModel getModel() {
		return _model;
	}

	public void disposeMusicControl() {
		_musicControl.stop();
		_musicControl.disposeMusic();
	}

	public void selectElement(Object element) {
		_spritesViewer.setSelection(new StructuredSelection(element));
		_musicControl.setTimePartitionSelection(_spritesViewer.getCombo().getSelectionIndex());
	}
}
