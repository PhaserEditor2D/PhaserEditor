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
package phasereditor.assetpack.ui.editor.blocks;

import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.ui.views.properties.IPropertySheetPage;

import phasereditor.assetpack.core.AssetPackCore;
import phasereditor.assetpack.core.AssetPackModel;
import phasereditor.assetpack.ui.editor.AssetPackEditor;
import phasereditor.project.ui.ProjectPropertyPage;
import phasereditor.ui.EditorBlockProvider;
import phasereditor.ui.IEditorBlock;
import phasereditor.ui.properties.FormPropertySection;

/**
 * @author arian
 *
 */
public class AssetPackEditorBlocksProvider extends EditorBlockProvider {

	private AssetPackEditor _editor;

	public AssetPackEditorBlocksProvider(AssetPackEditor editor) {
		_editor = editor;
	}
	
	public AssetPackEditor getEditor() {
		return _editor;
	}

	@Override
	public String getId() {
		return getClass().getCanonicalName() + "@" + getEditorFile().toString();
	}

	@Override
	public List<IEditorBlock> getBlocks() {
		var editorFile = getEditorFile();

		var packs = new ArrayList<AssetPackModel>();
		{
			var sharedPacks = AssetPackCore.getAssetPackModels(editorFile.getProject());

			for (var pack : sharedPacks) {
				if (!pack.getFile().equals(editorFile)) {
					packs.add(pack);
				}
			}
		}

		// always use the alive version of the editor pack
		packs.add(_editor.getModel());

		var usedFiles =

				new HashSet<>(packs.stream()

						.flatMap(pack -> pack.getAssets().stream())

						.flatMap(asset -> Arrays.stream(asset.computeUsedFiles()))

						.collect(toSet()));

		usedFiles.add(editorFile);

		var root = new AssetPackFolderBlock(editorFile.getParent(), usedFiles);

		return root.getChildren();
	}

	private IFile getEditorFile() {
		return _editor.getEditorInput().getFile();
	}

	@Override
	public IPropertySheetPage createPropertyPage() {
		var page = new ProjectPropertyPage() {
			@Override
			protected Object getDefaultModel() {
				return getEditorFile().getParent();
			}

			@Override
			protected List<FormPropertySection<?>> createSections() {
				var list = new ArrayList<FormPropertySection<?>>();

				list.add(new ImportFileSection(getView()));

				list.addAll(super.createSections());

				return list;
			}
		};
		return page;
	}
}
