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
package phasereditor.inspect.ui.views;

import static java.lang.System.out;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.Adapters;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.part.ViewPart;

import phasereditor.inspect.core.InspectCore;
import phasereditor.inspect.core.jsdoc.IMemberContainer;
import phasereditor.inspect.core.jsdoc.IPhaserMember;
import phasereditor.inspect.core.jsdoc.PhaserJsdocModel;
import phasereditor.inspect.ui.PhaserFileContentProvider;
import phasereditor.inspect.ui.PhaserFileLabelProvider;
import phasereditor.inspect.ui.PhaserFileStyledLabelProvider;
import phasereditor.ui.PatternFilter2;
import phasereditor.ui.handlers.ShowSourceCodeHandler;

/**
 * @author arian
 *
 */
public class PhaserFilesView extends ViewPart implements ISelectionListener, IPropertyChangeListener {

	public static final String ID = "phasereditor.inspect.ui.views.PhaserFilesView"; //$NON-NLS-1$
	private FilteredTree _filteredTree;
	private TreeViewer _viewer;
	private PhaserFileStyledLabelProvider _styledLabelProvider;

	public PhaserFilesView() {
	}

	@Override
	public void createPartControl(Composite parent) {
		_filteredTree = new FilteredTree(parent, SWT.NONE, new PatternFilter2(), true);
		_viewer = _filteredTree.getViewer();
		_viewer.setLabelProvider(new PhaserFileLabelProvider());
		_viewer.setContentProvider(new PhaserFileContentProvider());

		TreeViewerColumn viewerColumn = new TreeViewerColumn(_viewer, SWT.NONE);
		_styledLabelProvider = new PhaserFileStyledLabelProvider();
		viewerColumn.setLabelProvider(_styledLabelProvider);
		TreeColumn column = viewerColumn.getColumn();
		column.setWidth(1000);

		afterCreateWidgets();
	}

	@Override
	public void dispose() {

		getViewSite().getWorkbenchWindow().getSelectionService().removeSelectionListener(this);
		JFaceResources.getColorRegistry().removeListener(this);

		super.dispose();
	}

	private void afterCreateWidgets() {
		_viewer.setInput(InspectCore.getPhaserFiles().getSrcFolder());
		_viewer.addDoubleClickListener(new IDoubleClickListener() {

			@Override
			public void doubleClick(DoubleClickEvent event) {
				ShowSourceCodeHandler.run((IStructuredSelection) event.getSelection());
			}
		});

		// InspectUI.installJsdocTooltips(_viewer);

		getViewSite().setSelectionProvider(_viewer);

		getViewSite().getWorkbenchWindow().getSelectionService().addSelectionListener(this);

		JFaceResources.getColorRegistry().addListener(this);
	}

	@Override
	public void setFocus() {
		_filteredTree.getViewer().getTree().setFocus();
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if (this == part) {
			return;
		}

		if (selection instanceof IStructuredSelection) {
			Object elem = ((IStructuredSelection) selection).getFirstElement();
			IPhaserMember member = Adapters.adapt(elem, IPhaserMember.class);
			if (member != null) {
				Object[] path = memberPath(member);
				out.println(Arrays.toString(path));
				_viewer.reveal(new TreePath(path));
				_viewer.setSelection(new StructuredSelection(member));

			}
		}
	}

	private Object[] memberPath(Object obj) {
		List<Object> treepath = new ArrayList<>();
		memberPath(obj, treepath);
		return treepath.toArray();
	}

	private void memberPath(Object obj, List<Object> treepath) {
		if (obj instanceof Path) {

			if (((Path) obj).getFileName().toString().equals("src")) {
				return;
			}

			memberPath(((Path) obj).getParent(), treepath);
			treepath.add(obj);
		} else {
			IPhaserMember member = (IPhaserMember) obj;
			Object parent;
			IMemberContainer container = member.getContainer();

			if (container != null && container.getFile().equals(member.getFile())) {
				parent = container;
			} else {
				parent = PhaserJsdocModel.getInstance().getMemberPath(member);
			}

			memberPath(parent, treepath);
			treepath.add(member);
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		_styledLabelProvider.updateStyleValues();
		_viewer.refresh();
	}

}
