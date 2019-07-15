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

import static java.util.stream.Collectors.toList;
import static phasereditor.ui.PhaserEditorUI.isImage;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.swt.graphics.RGB;

import phasereditor.assetpack.core.AssetPackCore;
import phasereditor.assetpack.ui.editor.AssetPackUIEditor;
import phasereditor.atlas.core.AtlasCore;
import phasereditor.audio.core.AudioCore;
import phasereditor.audio.ui.AudioFileEditorBlock;
import phasereditor.scene.core.SceneCore;
import phasereditor.ui.Colors;
import phasereditor.ui.FileEditorBlock;
import phasereditor.ui.GridCellRenderer;
import phasereditor.ui.ICanvasCellRenderer;
import phasereditor.ui.IEditorBlock;
import phasereditor.ui.ImageFileEditorBlock;
import phasereditor.ui.ResourceEditorBlock;

class AssetPackFolderBlock extends ResourceEditorBlock<IContainer> {

	private Set<IFile> _usedFiles;
	private List<IEditorBlock> _children;
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

							// skip TypeScript files.
							if ("ts".equals(file.getFileExtension())) {
								return false;
							}

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
							} else if (isSceneSourceFile(file)) {
								return new SceneSourceFileBlock(file);
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

	private static boolean isSceneSourceFile(IFile file) {
		if ("js".equals(file.getFileExtension())) {
			var sceneFile = file.getProject().getFile(file.getProjectRelativePath().removeFileExtension().addFileExtension("scene"));
			return  sceneFile.exists() && SceneCore.isSceneFile(sceneFile);
		}
		return false;
	}

	@Override
	public String getKeywords() {
		return "";
	}

	@Override
	public List<IEditorBlock> getChildren() {
		return _children;
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