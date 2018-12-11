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

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.DialogCellEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import phasereditor.assetpack.core.AssetPackCore;
import phasereditor.assetpack.core.AssetPackModel;
import phasereditor.assetpack.core.AssetType;
import phasereditor.assetpack.core.AbstractFileAssetModel;
import phasereditor.assetpack.ui.AssetPackUI;

/**
 * @author arian
 *
 */
public class TextUrlCellEditor extends DialogCellEditor {

	private AbstractFileAssetModel _asset;

	public TextUrlCellEditor(Composite parent, AbstractFileAssetModel asset) {
		super(parent);
		_asset = asset;
	}

	@Override
	protected Object openDialogBox(Control cellEditorWindow) {

		try {
			AssetPackModel pack = _asset.getPack();
			IFile urlFile = _asset.getFileFromUrl(_asset.getUrl());
			List<IFile> files = AssetPackCore.discoverSimpleFiles(pack.getWebContentFolder());
			AssetType type = _asset.getType();
			String[] exts = {};
			switch (type) {
			case text:
				exts = new String[] { "txt", "text" };
				break;
			case json:
				exts = new String[] { "json" };
				break;
			case xml:
				exts = new String[] { "xml" };
				break;
			case script:
				exts = new String[] { "js" };
				break;
			case glsl:
				exts = new String[] { "glsl" };
				break;
			default:
				break;
			}

			AssetPackCore.sortFilesByExtension(files, exts);

			String result = AssetPackUI.browseAssetFile(pack, type.name(), urlFile, files, cellEditorWindow.getShell(),
					null);

			if (result == null) {
				return _asset.getUrl();
			}

			return result;

		} catch (CoreException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

}
