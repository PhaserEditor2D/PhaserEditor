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
package phasereditor.project.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import phasereditor.ui.BaseTreeCanvasItemRenderer;
import phasereditor.ui.TreeCanvas;
import phasereditor.ui.TreeCanvas.TreeCanvasItem;
import phasereditor.ui.TreeCanvasViewer;

public class ProjectTreeViewer extends TreeCanvasViewer {

	private List<IFileRendererProvider> _renderProviders;

	public ProjectTreeViewer(TreeCanvas tree, ITreeContentProvider contentProvider) {
		this(tree, contentProvider, new LabelProvider());
	}

	public ProjectTreeViewer(TreeCanvas tree, ITreeContentProvider contentProvider, LabelProvider labelProvider) {
		super(tree, contentProvider, labelProvider instanceof WorkbenchLabelProvider ? labelProvider
				: new CompoundLabelProvider(labelProvider));

		_renderProviders = new ArrayList<>();
		var elems = Platform.getExtensionRegistry().getConfigurationElementsFor("phasereditor.project.ui.fileRenderer");
		for (var elem : elems) {
			try {
				IFileRendererProvider provider = (IFileRendererProvider) elem.createExecutableExtension("class");
				_renderProviders.add(provider);
			} catch (CoreException e) {
				ProjectUI.logError(e);
			}
		}
	}

	static class CompoundLabelProvider extends LabelProvider {
		private LabelProvider _base;

		public CompoundLabelProvider(LabelProvider base) {
			_base = base;
		}

		@Override
		public String getText(Object element) {
			if (element instanceof IResource) {
				return WorkbenchLabelProvider.getDecoratingWorkbenchLabelProvider().getText(element);
			}
			return _base.getText(element);
		}

		@Override
		public Image getImage(Object element) {
			if (element instanceof IResource) {
				return WorkbenchLabelProvider.getDecoratingWorkbenchLabelProvider().getImage(element);
			}
			return _base.getImage(element);
		}
	}

	@Override
	protected void setItemIconProperties(TreeCanvasItem item) {
		BaseTreeCanvasItemRenderer renderer = null;

		if (item.getData() instanceof IFile) {
			for (var provider : _renderProviders) {
				var renderer2 = provider.createRenderer(item);
				if (renderer2 != null) {
					renderer = renderer2;
					break;
				}
			}
		}

		if (renderer == null) {
			super.setItemIconProperties(item);
		} else {
			item.setRenderer(renderer);
		}
	}
}