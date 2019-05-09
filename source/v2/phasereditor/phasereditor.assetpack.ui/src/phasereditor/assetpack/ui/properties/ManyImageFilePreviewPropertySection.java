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
package phasereditor.assetpack.ui.properties;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import phasereditor.assetpack.core.AssetPackCore;
import phasereditor.ui.FrameGridCanvas;
import phasereditor.ui.IFrameProvider;
import phasereditor.ui.ImageProxy;
import phasereditor.ui.properties.FormPropertySection;

/**
 * @author arian
 *
 */
public class ManyImageFilePreviewPropertySection extends FormPropertySection<IFile> {

	public ManyImageFilePreviewPropertySection() {
		super("Images Preview");
		setFillSpace(true);
	}

	@Override
	public boolean supportThisNumberOfModels(int number) {
		return number > 1;
	}
	
	@Override
	public boolean canEdit(Object obj) {
		return obj instanceof IFile && AssetPackCore.isImage((IResource) obj);
	}

	@Override
	public Control createContent(Composite parent) {
		var comp = new FrameGridCanvas(parent, 0, true);
		addUpdate(() -> {
			comp.loadFrameProvider(new ImageFileFrameProvider(getModels()));
		});

		return comp;
	}

	static class ImageFileFrameProvider implements IFrameProvider {
		private List<IFile> _files;

		public ImageFileFrameProvider(List<IFile> files) {
			_files = files;
		}

		@Override
		public int getFrameCount() {
			return _files.size();
		}

		@Override
		public ImageProxy getFrameImageProxy(int index) {
			return ImageProxy.get(_files.get(index), null);
		}

		@Override
		public Object getFrameObject(int index) {
			return _files.get(index);
		}

		@Override
		public String getFrameLabel(int index) {
			return _files.get(index).getName();
		}

	}

}
