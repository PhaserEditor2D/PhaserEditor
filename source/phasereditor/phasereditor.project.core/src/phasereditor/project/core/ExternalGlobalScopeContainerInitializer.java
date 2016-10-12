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
package phasereditor.project.core;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import org.eclipse.wst.jsdt.core.JsGlobalScopeContainerInitializer;
import org.eclipse.wst.jsdt.core.compiler.libraries.LibraryLocation;
import org.eclipse.wst.jsdt.core.compiler.libraries.SystemLibraryLocation;

public abstract class ExternalGlobalScopeContainerInitializer extends JsGlobalScopeContainerInitializer {

	@SuppressWarnings("synthetic-access")
	class ExternalLibraryLocation extends SystemLibraryLocation {

		@Override
		public char[][] getLibraryFileNames() {
			return _libFileNameArray;
		}

		@Override
		public boolean isExternalLibrary() {
			return true;
		}

		@Override
		public InputStream createExternalLibraryStream() {
			java.nio.file.Path file = getLibFolderPath().resolve(_libFileName);
			try {
				return Files.newInputStream(file);
			} catch (IOException e) {
				ProjectCore.logError(e);
				return null;
			}
		}
	}

	private String _libFileName;
	private ExternalLibraryLocation _libLocation;
	private String _description;
	private char[][] _libFileNameArray;

	public ExternalGlobalScopeContainerInitializer(String libFileName, String description) {
		super();
		_libFileName = libFileName;
		_libFileNameArray = new char[][] { libFileName.toCharArray() };
		_description = description;
	}

	@Override
	public LibraryLocation getLibraryLocation() {
		if (_libLocation == null) {
			_libLocation = new ExternalLibraryLocation();
		}
		return _libLocation;
	}

	@Override
	public String getDescription() {
		return _description;
	}

	@Override
	public boolean allowAttachJsDoc() {
		return false;
	}

	public abstract java.nio.file.Path getLibFolderPath();
}
