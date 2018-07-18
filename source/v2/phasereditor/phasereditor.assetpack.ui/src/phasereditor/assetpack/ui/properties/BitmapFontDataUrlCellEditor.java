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
import java.util.function.Function;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.DialogCellEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import phasereditor.assetpack.core.AssetPackModel;
import phasereditor.assetpack.core.BitmapFontAssetModel;
import phasereditor.assetpack.ui.AssetPackUI;

/**
 * @author arian
 *
 */
public class BitmapFontDataUrlCellEditor extends DialogCellEditor {

	private BitmapFontAssetModel _asset;

	public BitmapFontDataUrlCellEditor(Composite parent, BitmapFontAssetModel asset) {
		super(parent);
		_asset = asset;
	}

	@Override
	protected Object openDialogBox(Control cellEditorWindow) {
		try {
			AssetPackModel pack = _asset.getPack();
			IFile urlFile = _asset.getFontDataURLFile();
			Function<IFile, Boolean> isFontFile = new Function<>() {

				@Override
				public Boolean apply(IFile f) {
					String ext = f.getFileExtension();
					if (ext != null && (ext.equals("xml") || ext.equals("fnt"))) {
						return Boolean.TRUE;
					}
					return Boolean.FALSE;
				}
			};
			List<IFile> xmlFiles = pack.discoverFiles(isFontFile);

			return AssetPackUI.browseAssetFile(pack, "bitmap font XML/FNT", urlFile, xmlFiles,
					cellEditorWindow.getShell(), null);
		} catch (CoreException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

}
