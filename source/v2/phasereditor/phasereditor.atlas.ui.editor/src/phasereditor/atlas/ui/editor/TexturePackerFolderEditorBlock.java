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
package phasereditor.atlas.ui.editor;

import static phasereditor.ui.PhaserEditorUI.isImage;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.graphics.RGB;

import phasereditor.ui.Colors;
import phasereditor.ui.FrameGridCellRenderer;
import phasereditor.ui.ICanvasCellRenderer;
import phasereditor.ui.IEditorBlock;
import phasereditor.ui.IFrameProvider;
import phasereditor.ui.ImageFileEditorBlock;
import phasereditor.ui.ImageProxy;
import phasereditor.ui.ResourceEditorBlock;

/**
 * @author arian
 *
 */
public class TexturePackerFolderEditorBlock extends ResourceEditorBlock<IContainer> {

	private List<IEditorBlock> _children;
	private long _countImages;
	private List<ImageFileEditorBlock> _images;
	private Set<IFile> _usedImages;

	public TexturePackerFolderEditorBlock(IContainer resource, Set<IFile> usedImages) throws CoreException {
		super(resource);
		_usedImages = usedImages;
		_children = new ArrayList<>();
		_images = new ArrayList<>();
		build(_images);
	}

	private void build(List<ImageFileEditorBlock> images) throws CoreException {

		for (var member : getResource().members()) {
			if (member instanceof IContainer) {
				var folderBlock = new TexturePackerFolderEditorBlock((IContainer) member, _usedImages);
				images.addAll(folderBlock.getImagesinTree());

				if (!folderBlock._children.isEmpty()) {
					_children.add(folderBlock);
				}
			} else {
				if (!_usedImages.contains(member) && isImage(member)) {
					var image = new ImageFileEditorBlock((IFile) member);
					_children.add(image);
					images.add(image);
				}
			}
		}

		_countImages = _children.stream()

				.filter(b -> b instanceof ImageFileEditorBlock)

				.count();

	}

	public List<ImageFileEditorBlock> getImagesinTree() {
		return _images;
	}

	public long getCountImagesInChildren() {
		return _countImages;
	}

	@Override
	public String getKeywords() {
		return "folder";
	}

	@Override
	public List<IEditorBlock> getChildren() {
		return _children;
	}

	@Override
	public ICanvasCellRenderer getRenderer() {
		return new FrameGridCellRenderer(new IFrameProvider() {

			@Override
			public Object getFrameObject(int index) {
				return _images.get(index).getResource();
			}

			@Override
			public String getFrameLabel(int index) {
				return _images.get(index).getLabel();
			}

			@Override
			public ImageProxy getFrameImageProxy(int index) {
				var file = _images.get(index).getResource();
				return ImageProxy.get(file, null);
			}

			@Override
			public int getFrameCount() {
				return _images.size();
			}
		}, 8);
	}

	@Override
	public String getSortName() {
		return "00";
	}

	@Override
	public RGB getColor() {
		return Colors.GREEN.rgb;
	}

}
