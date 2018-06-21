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
package phasereditor.inspect.ui;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Adapters;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.subshell.snippets.jface.tooltip.tooltipsupport.ICustomInformationControlCreator;
import com.subshell.snippets.jface.tooltip.tooltipsupport.TableViewerInformationProvider;
import com.subshell.snippets.jface.tooltip.tooltipsupport.Tooltips;
import com.subshell.snippets.jface.tooltip.tooltipsupport.TreeViewerInformationProvider;

import phasereditor.inspect.core.jsdoc.IJsdocProvider;
import phasereditor.inspect.core.jsdoc.IPhaserMember;
import phasereditor.inspect.core.jsdoc.PhaserJsdocModel;
import phasereditor.inspect.ui.views.JsdocView;
import phasereditor.ui.PhaserEditorUI;

/**
 * @author arian
 *
 */
public class InspectUI {
	public static final String JSDOC_VIEW_ID = "phasereditor.inspect.ui.jsdoc";
	private static List<ICustomInformationControlCreator> _informationControls;

	public static void installJsdocTooltips(TreeViewer viewer) {
		List<ICustomInformationControlCreator> creators = getInformationControlCreatorsForTooltips();

		Tooltips.install(viewer.getControl(), new TreeViewerInformationProvider(viewer), creators, false);
	}

	public static void installJsdocTooltips(TableViewer viewer) {
		List<ICustomInformationControlCreator> creators = getInformationControlCreatorsForTooltips();

		Tooltips.install(viewer.getControl(), new TableViewerInformationProvider(viewer), creators, false);
	}

	private static List<ICustomInformationControlCreator> getInformationControlCreatorsForTooltips() {
		if (_informationControls == null) {
			_informationControls = new ArrayList<>();

			_informationControls.add(new ICustomInformationControlCreator() {

				@Override
				public IInformationControl createInformationControl(Shell parent) {
					return new PhaserJsdocInformationControl(parent);
				}

				@Override
				public boolean isSupported(Object info) {
					IJsdocProvider provider = Adapters.adapt(info, IJsdocProvider.class);
					return provider != null;
				}
			});
		}

		return _informationControls;
	}

	public static void showSourceCode(IPhaserMember member) {
		PhaserJsdocModel jsdoc = PhaserJsdocModel.getInstance();
		Path file = jsdoc.getMemberPath(member);
		if (file != null) {
			int line = member.getLine();
			int offset = member.getOffset();
			PhaserEditorUI.openJSEditor(line, offset, file);
		}
	}

	public static void showJavaDoc(IPhaserMember member) {
		try {
			IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

			JsdocView view = (JsdocView) page.showView(InspectUI.JSDOC_VIEW_ID, null, IWorkbenchPage.VIEW_CREATE);
			view.showJsdocFor(member);
			page.activate(view);
		} catch (PartInitException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

}
