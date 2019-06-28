package phasereditor.ide.ui.wizards;

import static phasereditor.ui.PhaserEditorUI.swtRun;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import phasereditor.ide.ui.views.StartView;
import phasereditor.ui.TreeArrayContentProvider;

public class OpenProjectDialog extends Dialog {

	private FilteredTree _filteredTree;
	private TreeViewer _viewer;

	public OpenProjectDialog(Shell parent) {
		super(parent);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Open Project");
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		_filteredTree = new FilteredTree((Composite) super.createDialogArea(parent), SWT.SINGLE | SWT.BORDER,
				new PatternFilter(), true);
		_viewer = _filteredTree.getViewer();
		_viewer.setContentProvider(new TreeArrayContentProvider());
		_viewer.setLabelProvider(WorkbenchLabelProvider.getDecoratingWorkbenchLabelProvider());
		_viewer.setInput(StartView.getWorkspaceProjects());

		_viewer.addDoubleClickListener(e -> {
			this.okPressed();
		});

		return _filteredTree;
	}

	@Override
	protected void okPressed() {
		var selection = _viewer.getSelection();
		if (!selection.isEmpty()) {
			var project = (IProject) _viewer.getStructuredSelection().getFirstElement();
			swtRun(() -> {
				StartView.openProject(project);
			});
		}
		super.okPressed();
	}
}
