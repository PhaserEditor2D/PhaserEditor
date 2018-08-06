// The MIT License (MIT)
//
// Copyright (c) 2015, 2018 Arian Fornaris
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
package phasereditor.assetexplorer.ui.views;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.PlatformUI;

import phasereditor.assetpack.core.AssetPackCore;
import phasereditor.assetpack.ui.wizards.NewAssetPackWizard;
import phasereditor.project.ui.wizards.NewPhaserProjectWizard;

/**
 * @author arian
 *
 */
public class New {
	private Object _parent;

	public static Object[] children(Object parent, Object[] list) {
		return children(parent, List.of(list));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Object[] children(Object parent, List list) {
		var list2 = new ArrayList(list);
		list2.add(0, new New(parent));
		return list2.toArray();
	}

	public New(Object parent) {
		super();
		_parent = parent;
	}

	public Object getParent() {
		return _parent;
	}

	@Override
	public String toString() {
		return "New...";
	}

	private static Comparator<IFile> getNewerFileComp = (a,
			b) -> -Long.compare(a.getLocalTimeStamp(), b.getLocalTimeStamp());

	public void openWizard(IProject project) {
		INewWizard wizard = null;
		IStructuredSelection sel = null;

		var wb = PlatformUI.getWorkbench();

		if (_parent == AssetExplorer.PROJECTS_NODE) {
			wizard = new NewPhaserProjectWizard();
		} else if (_parent == AssetExplorer.PACK_NODE) {
			wizard = new NewAssetPackWizard();
			var packs = AssetPackCore.getAssetPackModels(project);
			if (packs.isEmpty()) {
				sel = new StructuredSelection(project);
			} else {
				var file = packs.stream().map(p -> p.getFile()).sorted(getNewerFileComp).findFirst().get();
				sel = new StructuredSelection(file.getParent());
			}
		}

		if (wizard != null) {
			wizard.init(wb, sel);
			var shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
			var dlg = new WizardDialog(shell, wizard);
			dlg.open();
		}
	}

}
