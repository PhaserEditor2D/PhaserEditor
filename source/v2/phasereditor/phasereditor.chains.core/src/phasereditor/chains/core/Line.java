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
package phasereditor.chains.core;

import java.nio.file.Path;

import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.IAdaptable;

import phasereditor.inspect.core.examples.PhaserExampleModel;
import phasereditor.ui.ISourceLocation;

public class Line implements IAdaptable, ISourceLocation {
	public String text;
	public int linenum;
	public PhaserExampleModel example;

	@Override
	public Path getFilePath() {
		return example.getMainFilePath();
	}

	@Override
	public int getLine() {
		return linenum;
	}

	@Override
	public String toString() {
		return text + " - " + example.getFullName() + " [" + linenum + "]";
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		return Adapters.adapt(example, adapter);
	}
}