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
package phasereditor.ide.intro;

import org.eclipse.ui.internal.ide.application.DelayedEventsProcessor;
import org.eclipse.ui.internal.ide.application.IDEWorkbenchAdvisor;

@SuppressWarnings("restriction")
public class PhaserWorkbenchAdvisor extends IDEWorkbenchAdvisor {

	public PhaserWorkbenchAdvisor() {
		super();
	}

	public PhaserWorkbenchAdvisor(DelayedEventsProcessor processor) {
		super(processor);
	}

	@Override
	public String getInitialWindowPerspectiveId() {
		return "phasereditor.ide.ui.perspective";
	}

	// @Override
	// public WorkbenchWindowAdvisor
	// createWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
	// return new IDEWorkbenchWindowAdvisor(this, configurer) {
	// @Override
	// public void postWindowOpen() {
	// super.postWindowOpen();
	// EPTDefaults.setDefaults();
	// }
	//
	// };
	// }
}
