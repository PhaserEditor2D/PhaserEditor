package phasereditor.project.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.wst.jsdt.ui.ProjectLibraryRoot;

import phasereditor.project.core.PhaserProjectNature;
import phasereditor.project.core.ProjectCore;
import phasereditor.project.core.codegen.SourceLang;

public class ResetProjectLibsHanler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Object sel = ((IStructuredSelection) HandlerUtil.getCurrentSelection(event)).getFirstElement();
		ProjectLibraryRoot root = (ProjectLibraryRoot) sel;
		IProject project = root.getProject().getProject();
		WorkspaceJob job = new WorkspaceJob("Reset project libraries") {

			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
				SourceLang lang = ProjectCore.getProjectLanguage(project);
				PhaserProjectNature.resetProjectLibraries(project, lang, monitor);
				project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
				return Status.OK_STATUS;
			}

		};
		job.schedule();
		return null;
	}

}
