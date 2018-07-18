// The MIT License (MIT)
//
// Copyright (c) 2015, 2018 Arian Fornaris
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
package phasereditor.assetpack.ui.properties;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.DialogCellEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.json.JSONArray;

import com.phasereditor2d.json.JSONUtils;

import phasereditor.assetpack.core.AssetPackModel;
import phasereditor.assetpack.core.AudioAssetModel;
import phasereditor.assetpack.ui.AssetPackUI;

/**
 * @author arian
 *
 */
public class AudioUrlsCellEditor extends DialogCellEditor {

	private AudioAssetModel _asset;

	public AudioUrlsCellEditor(AudioAssetModel asset, Composite parent) {
		super(parent);
		_asset = asset;
	}

	@Override
	protected Object openDialogBox(Control cellEditorWindow) {

		AssetPackModel packModel = _asset.getPack();
		try {
			List<IFile> currentFiles = new ArrayList<>();
			for (String url : _asset.getUrls()) {
				IFile file = _asset.getFileFromUrl(url);
				if (file != null) {
					currentFiles.add(file);
				}
			}

			String json = AssetPackUI.browseAudioUrl(packModel, currentFiles, packModel.discoverAudioFiles(),
					cellEditorWindow.getShell(), null);
			if (json == null) {
				return null;
			}

			JSONArray urls = AudioAssetModel.parseUrlsJSONArray(json);

			var result = JSONUtils.<String>toList(urls);

			return result;

		} catch (CoreException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public AudioAssetModel getAsset() {
		return _asset;
	}

}
