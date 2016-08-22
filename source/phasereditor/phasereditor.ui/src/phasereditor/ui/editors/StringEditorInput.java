// The MIT License (MIT)
//
// Copyright (c) 2015, 2016 Arian Fornaris
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
package phasereditor.ui.editors;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IStorageEditorInput;

@SuppressWarnings("synthetic-access")
public class StringEditorInput implements IStorageEditorInput {

	private final String _inputString;
	private String _title;
	private String _tooltip;
	private ImageDescriptor _imageDescriptor;

	public StringEditorInput(String title, String inputString) {
		_title = title;
		_inputString = inputString;
	}

	public String getTooltip() {
		return _tooltip;
	}

	public void setTooltip(String tooltip) {
		_tooltip = tooltip;
	}

	@Override
	public boolean exists() {
		return false;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return _imageDescriptor;
	}

	public void setImageDescriptor(ImageDescriptor imageDescriptor) {
		_imageDescriptor = imageDescriptor;
	}

	@Override
	public IPersistableElement getPersistable() {
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {

		return null;

	}

	@Override
	public String getName() {
		return _title;
	}

	@Override
	public String getToolTipText() {
		return _tooltip;

	}

	@Override
	public IStorage getStorage() throws CoreException {

		return new IStorage() {

			@Override
			public InputStream getContents() throws CoreException {
				return new ByteArrayInputStream(_inputString.getBytes());
			}

			@Override
			public IPath getFullPath() {
				return null;
			}

			@Override
			public String getName() {
				return StringEditorInput.this.getName();
			}

			@Override
			public boolean isReadOnly() {
				return true;
			}

			@SuppressWarnings("unchecked")
			@Override
			public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
				return null;

			}

		};

	}

}
