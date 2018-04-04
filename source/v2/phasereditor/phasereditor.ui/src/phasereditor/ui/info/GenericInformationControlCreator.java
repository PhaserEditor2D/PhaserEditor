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
package phasereditor.ui.info;

import java.util.function.Function;

import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IInformationControlCreatorExtension;
import org.eclipse.swt.widgets.Shell;

public class GenericInformationControlCreator
		implements IInformationControlCreator, IInformationControlCreatorExtension {

	private Class<? extends BaseInformationControl> _controlClass;
	private Function<Shell, IInformationControl> _generator;

	public GenericInformationControlCreator(Class<? extends BaseInformationControl> controlClass,
			Function<Shell, IInformationControl> generator) {
		_controlClass = controlClass;
		_generator = generator;
	}

	@Override
	public boolean canReuse(IInformationControl control) {
		return _controlClass == control.getClass();
	}

	@Override
	public boolean canReplace(IInformationControlCreator creator) {
		return false;
	}

	@Override
	public IInformationControl createInformationControl(Shell parent) {
		return _generator.apply(parent);
	}
}
