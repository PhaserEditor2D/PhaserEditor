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
package phasereditor.project.ui;

import java.util.HashMap;
import java.util.function.Supplier;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.tm.terminal.connector.local.launcher.LocalLauncherDelegate;
import org.eclipse.tm.terminal.view.core.interfaces.constants.ITerminalsConnectorConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.ResourceUtil;
import org.eclipse.ui.internal.WorkbenchPartReference;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.statushandlers.StatusManager;

import phasereditor.project.core.ProjectCore;

/**
 * @author arian
 *
 */
public class ProjectUI {
	/**
	 * 
	 */
	public static final String PLUGIN_ID = Activator.PLUGIN_ID;
	private static final String OTHER_PROJECT_EDITOR = "OtherProjectEditor";

	public static void logError(Exception e) {
		e.printStackTrace();
		StatusManager.getManager().handle(new Status(IStatus.ERROR, PLUGIN_ID, e.getMessage(), e));
	}

	public static Action getOpenTerminalAction(Supplier<IResource> res) {
		var action = new Action("Open In Terminal") {
			@Override
			public void run() {
				openTerminal(res.get());
			}
		};
		action.setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin("org.eclipse.tm.terminal.view.ui",
				"icons/eview16/terminal_view.gif"));
		return action;
	}

	public static void openTerminal(IResource res) {
		var props = new HashMap<String, Object>();

		var folder = res;

		if (!(folder instanceof IContainer)) {
			folder = folder.getParent();
		}
		props.put(ITerminalsConnectorConstants.PROP_DELEGATE_ID,
				"org.eclipse.tm.terminal.connector.local.launcher.local");
		props.put(ITerminalsConnectorConstants.PROP_SELECTION, new StructuredSelection(folder));

		new LocalLauncherDelegate().execute(props, null);
	}

	public static void updateTitleOfEditors() {
		try {
			var refs = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getEditorReferences();

			for (var ref : refs) {
				var input = ref.getEditorInput();
				var file = ResourceUtil.getFile(input);
				if (file != null) {
					var project = file.getProject();
					var wRef = (WorkbenchPartReference) ref;
					var part = wRef.getModel();
					String label;
					if (project == ProjectCore.getActiveProject()) {
						label = input.getName();
						part.getTags().remove(OTHER_PROJECT_EDITOR);
					} else {
						label = input.getName() + "^" + project.getName();
						part.getTags().add(OTHER_PROJECT_EDITOR);
					}
					part.setLabel(label);
				}

			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void start() {
		ProjectCore.addActiveProjectListener(project -> {
			updateTitleOfEditors();
		});
	}
}
