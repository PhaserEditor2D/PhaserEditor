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
package phasereditor.assetpack.ui.editor;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static phasereditor.ui.PhaserEditorUI.isImage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.views.properties.IPropertySheetPage;

import phasereditor.assetpack.core.AssetPackCore;
import phasereditor.assetpack.core.AssetPackModel;
import phasereditor.atlas.core.AtlasCore;
import phasereditor.audio.core.AudioCore;
import phasereditor.audio.ui.AudioFileEditorBlock;
import phasereditor.project.ui.ProjectPropertyPage;
import phasereditor.scene.core.SceneCore;
import phasereditor.ui.Colors;
import phasereditor.ui.FileEditorBlock;
import phasereditor.ui.GridCellRenderer;
import phasereditor.ui.ICanvasCellRenderer;
import phasereditor.ui.IEditorBlock;
import phasereditor.ui.IEditorBlockProvider;
import phasereditor.ui.ImageFileEditorBlock;
import phasereditor.ui.ResourceEditorBlock;

/**
 * @author arian
 *
 */
public class AssetPackEditorBlocksProvider implements IEditorBlockProvider {

	private AssetPackEditor _editor;
	private Runnable _refreshHandler;

	public AssetPackEditorBlocksProvider(AssetPackEditor editor) {
		_editor = editor;
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
	public void setRefreshHandler(Runnable refresh) {
		_refreshHandler = refresh;
	}

	public void refresh() {
		_refreshHandler.run();
	}

	@Override
	public IPropertySheetPage getPropertyPage() {
		return new ProjectPropertyPage() {
			@Override
			protected Object getDefaultModel() {
				return getEditorFile().getParent();
			}
		};
	}

}

class AssetPackFolderBlock extends ResourceEditorBlock<IContainer> {

	private Set<IFile> _usedFiles;
	private List<ResourceEditorBlock<? extends IResource>> _children;
	private GridCellRenderer _renderer;

	private static Set<String> SKIP_CONTENT_TYPE_ID = Set.of(new String[] {

			AtlasCore.EDITOR_ATLAS_FILE_CONTENT_TYPE_ID,

			SceneCore.EDITOR_SCENE_FILE_CONTENT_TYPE,
			
			AssetPackCore.EDITOR_ASSET_PACK_FILE_CONTENT_TYPE,

	});

	public AssetPackFolderBlock(IContainer resource, Set<IFile> usedFiles) {
		super(resource);

		_usedFiles = usedFiles;

		try {
			_children = Arrays.stream(getResource().members())

					.filter(r -> {
						if (_usedFiles.contains(r)) {
							return false;
						}

						if (r instanceof IFile) {
							var file = (IFile) r;
							IContentDescription desc;
							try {
								desc = file.getContentDescription();
								if (desc != null) {
									var type = desc.getContentType();
									if (type != null) {
										var id = type.getId();
										if (SKIP_CONTENT_TYPE_ID.contains(id)) {
											return false;
										}
									}
								}
							} catch (CoreException e) {
								//
							}

						}

						return true;
					})

					.map(r -> {
						if (r instanceof IFile) {
							var file = (IFile) r;

							if (isImage(file)) {
								return new ImageFileEditorBlock(file);
							} else if (AudioCore.isSupportedAudio(file)) {
								return new AudioFileEditorBlock(file);
							}

							return new FileEditorBlock((IFile) r);
						}
						return new AssetPackFolderBlock((IContainer) r, _usedFiles);
					})

					.filter(block ->

					!(block instanceof AssetPackFolderBlock)

							|| !((AssetPackFolderBlock) block).getChildren().isEmpty()

					)

					.collect(toList());
		} catch (CoreException e) {
			AssetPackUIEditor.logError(e);
			_children = Collections.emptyList();
		}
	}

	@Override
	public String getKeywords() {
		return "";
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public List<IEditorBlock> getChildren() {
		return (List) _children;
	}

	@Override
	public ICanvasCellRenderer getRenderer() {
		if (_renderer == null) {
			_renderer = new GridCellRenderer(_children.stream().map(b -> b.getRenderer()).collect(toList()), 8);
		}
		return _renderer;
	}

	@Override
	public String getSortName() {
		return "000";
	}

	@Override
	public RGB getColor() {
		return Colors.GREEN.rgb;
	}

}
