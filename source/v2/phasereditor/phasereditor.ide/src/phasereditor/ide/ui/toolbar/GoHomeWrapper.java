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
package phasereditor.ide.ui.toolbar;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;

import phasereditor.ide.IDEPlugin;
import phasereditor.ide.ui.StartPerspective;
import phasereditor.project.ui.ProjectUI;

class GoHomeWrapper {
	public GoHomeWrapper(Composite parent) {
		var btn = new Button(parent, SWT.PUSH);
		var homePersp = PlatformUI.getWorkbench().getPerspectiveRegistry().findPerspectiveWithId(StartPerspective.ID);
		btn.setImage(PerspectiveSwitcherWrapper.getPerspectiveIcon(homePersp));
		btn.addSelectionListener(SelectionListener.widgetSelectedAdapter(
				e -> PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().setPerspective(homePersp)));
		btn.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent e) {
				if (e.button == 3) {
					var serv = PlatformUI.getWorkbench().getService(ICommandService.class);
					try {
						serv.getCommand(ProjectUI.CMD_OPEN_PROJECT).executeWithChecks(new ExecutionEvent());
					} catch (Exception e1) {
						IDEPlugin.logError(e1);
					}
				}
			}
		});
	}
}