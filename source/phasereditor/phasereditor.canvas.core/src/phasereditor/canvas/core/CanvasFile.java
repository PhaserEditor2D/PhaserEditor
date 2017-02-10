// The MIT License (MIT)
//
// Copyright (c) 2015, 2017 Arian Fornaris
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
package phasereditor.canvas.core;

import org.eclipse.core.resources.IFile;

/**
 * @author arian
 *
 */
public class CanvasFile {
	private IFile _file;
	private String _className;
	private CanvasType _type;

	public CanvasFile(IFile file, CanvasType type) {
		super();
		_file = file;
		_type = type;

		{
			String name = _file.getName();
			_className = name.substring(0, name.length() - _file.getFileExtension().length() - 1);
		}
	}

	public IFile getFile() {
		return _file;
	}
	
	public void setFile(IFile file) {
		_file = file;
	}


	public String getClassName() {
		return _className;
	}

	public CanvasType getType() {
		return _type;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((_file == null) ? 0 : _file.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CanvasFile other = (CanvasFile) obj;
		if (_file == null) {
			if (other._file != null)
				return false;
		} else if (!_file.equals(other._file))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + ":" + _file + "@" + hashCode();
	}
}
