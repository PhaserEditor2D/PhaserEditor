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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import phasereditor.assetpack.core.AssetPackCore;
import phasereditor.assetpack.ui.ImagePreviewComp;
import phasereditor.audio.ui.Html5AudioPlayer;
import phasereditor.ui.views.IPreviewFactory;

@SuppressWarnings("rawtypes")
public class FilePreviewAdapterFactory implements IAdapterFactory {
	private static final Class<?>[] ADAPTERS = { IPreviewFactory.class };

	@SuppressWarnings("unchecked")
	@Override
	public Object getAdapter(Object adaptableObject, Class adapterType) {
		if (adaptableObject instanceof IFile) {
			IFile file = (IFile) adaptableObject;
			if (AssetPackCore.isImage(file)) {
				return createImageFilePreviewFactory();
			} else if (AssetPackCore.isVideo(file)) {
				return createVideoFilePreviewFactory();
			} else if (AssetPackCore.isAudio(file)) {
				return createAudioFilePreviewFactory();
			}
		}
		return null;
	}

	private static abstract class FilePreviewFactory implements IPreviewFactory {
		public FilePreviewFactory() {
		}

		@Override
		public void initPreviewControl(Control previewControl, IMemento initialMemento) {
			// nothing
		}

		@Override
		public void savePreviewControl(Control previewControl, IMemento memento) {
			//
		}

		@Override
		public String getTitle(Object element) {
			return ((IFile) element).getName();
		}

		@Override
		public IPersistableElement getPersistable(Object elem) {
			return new FilePersistableElement((IFile) elem);
		}

		private WorkbenchLabelProvider _labelProvider = new WorkbenchLabelProvider();

		@Override
		public Image getIcon(Object element) {
			return _labelProvider.getImage(element);
		}
	}

	private static IPreviewFactory createImageFilePreviewFactory() {
		return new FilePreviewFactory() {

			@Override
			public void updateControl(Control preview, Object element) {
				IFile file = (IFile) element;
				if (file.exists()) {
					((ImagePreviewComp) preview).setImageFile(file);
				}
			}

			@Override
			public Control createControl(Composite previewContainer) {
				return new ImagePreviewComp(previewContainer, 0);
			}

			@Override
			public boolean canReusePreviewControl(Control c, Object elem) {
				return c instanceof ImagePreviewComp;
			}

			@Override
			public void updateToolBar(IToolBarManager toolbar, Control preview) {
				((ImagePreviewComp) preview).createToolBar(toolbar);
			}
		};
	}

	private static IPreviewFactory createAudioFilePreviewFactory() {
		return new FilePreviewFactory() {

			@Override
			public void updateControl(Control preview, Object element) {
				IFile file = (IFile) element;
				if (file.exists()) {
					// ((GdxMusicControl) preview).load(file);
					((Html5AudioPlayer) preview).load(file);
				}
			}

			@Override
			public Control createControl(Composite previewContainer) {
				// return new GdxMusicControl(previewContainer, 0);
				return new Html5AudioPlayer(previewContainer, 0);
			}

			@Override
			public void hiddenControl(Control preview) {
				// GdxMusicControl control = (GdxMusicControl) preview;
				// control.stop();
				// control.disposeMusic();
				((Html5AudioPlayer) preview).load(null);
			}

			@Override
			public boolean canReusePreviewControl(Control c, Object elem) {
				// return c instanceof GdxMusicControl;
				return c instanceof Html5AudioPlayer;
			}
		};
	}

	private static IPreviewFactory createVideoFilePreviewFactory() {
		return new FilePreviewFactory() {

			@Override
			public void updateControl(Control preview, Object element) {
				IFile file = (IFile) element;
				if (file.exists()) {
					((VideoPreviewComp) preview).setVideoFile(file);
				}
			}

			@Override
			public Control createControl(Composite previewContainer) {
				return new VideoPreviewComp(previewContainer, 0);
			}

			@Override
			public void hiddenControl(Control preview) {
				((VideoPreviewComp) preview).setVideoFile(null);
			}

			@Override
			public boolean canReusePreviewControl(Control c, Object elem) {
				return c instanceof VideoPreviewComp;
			}
		};
	}

	@SuppressWarnings("unchecked")
	@Override
	public Class[] getAdapterList() {
		return ADAPTERS;
	}

}
