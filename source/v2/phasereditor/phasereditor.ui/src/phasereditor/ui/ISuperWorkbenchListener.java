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
package phasereditor.ui;

import org.eclipse.ui.IPageListener;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * @author arian
 *
 */
@SuppressWarnings("all")
public interface ISuperWorkbenchListener extends IWorkbenchListener, IWindowListener, IPartListener, IPageListener {

	@Override
	default void pageActivated(IWorkbenchPage page) {

	}

	@Override
	default void pageClosed(IWorkbenchPage page) {

	}

	@Override
	default void pageOpened(IWorkbenchPage page) {

	}

	@Override
	default void partActivated(IWorkbenchPart part) {

	}

	@Override
	default void partBroughtToTop(IWorkbenchPart part) {

	}

	@Override
	default void partClosed(IWorkbenchPart part) {

	}

	@Override
	default void partDeactivated(IWorkbenchPart part) {

	}

	@Override
	default void partOpened(IWorkbenchPart part) {

	}

	@Override
	default void windowActivated(IWorkbenchWindow window) {

	}

	@Override
	default void windowDeactivated(IWorkbenchWindow window) {

	}

	@Override
	default void windowClosed(IWorkbenchWindow window) {

	}

	@Override
	default void windowOpened(IWorkbenchWindow window) {

	}

	@Override
	default boolean preShutdown(IWorkbench workbench, boolean forced) {

		return false;
	}

	@Override
	default void postShutdown(IWorkbench workbench) {

	}

}
