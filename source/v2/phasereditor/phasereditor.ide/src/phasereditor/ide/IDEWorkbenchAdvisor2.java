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
package phasereditor.ide;

import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;
import org.eclipse.ui.internal.WorkbenchWindow;
import org.eclipse.ui.internal.ide.application.DelayedEventsProcessor;
import org.eclipse.ui.internal.ide.application.IDEWorkbenchAdvisor;
import org.eclipse.ui.internal.ide.application.IDEWorkbenchWindowAdvisor;

import phasereditor.ide.ui.StartPerspective;

/**
 * @author arian
 *
 */
public class IDEWorkbenchAdvisor2 extends IDEWorkbenchAdvisor {
	public IDEWorkbenchAdvisor2(DelayedEventsProcessor processor) {
		super(processor);
	}

	@Override
	public WorkbenchWindowAdvisor createWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
		return new MyWindowAdvisor(this, configurer);
	}

	private static class MyWindowAdvisor extends IDEWorkbenchWindowAdvisor {

		public MyWindowAdvisor(IDEWorkbenchAdvisor wbAdvisor, IWorkbenchWindowConfigurer configurer) {
			super(wbAdvisor, configurer);
		}

		// @Override
		// public void preWindowOpen() {
		// super.preWindowOpen();
		//
		// var configurer = getWindowConfigurer();
		// configurer.setShowCoolBar(false);
		// configurer.setShowPerspectiveBar(false);
		// }

		@Override
		public void postWindowOpen() {
			super.postWindowOpen();

			var win = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			var listener = new MyPerspectiveListener();
			win.addPerspectiveListener(listener);
			listener.perspectiveActivated(win.getActivePage(), win.getActivePage().getPerspective());
		}

	}

	private static class MyPerspectiveListener implements IPerspectiveListener {

		@Override
		public void perspectiveActivated(IWorkbenchPage page, IPerspectiveDescriptor perspective) {
			var window = (WorkbenchWindow) page.getWorkbenchWindow();
			var id = perspective.getId();

			if (StartPerspective.ID.equals(id)) {
				if (window.getCoolBarVisible()) {
					window.toggleToolbarVisibility();
				}

				page.setEditorAreaVisible(false);
			} else {
				page.setEditorAreaVisible(true);

				if (!window.getCoolBarVisible()) {
					window.toggleToolbarVisibility();
				}
			}
		}

		@Override
		public void perspectiveChanged(IWorkbenchPage page, IPerspectiveDescriptor perspective, String changeId) {
			// nothing
		}

	}
}
