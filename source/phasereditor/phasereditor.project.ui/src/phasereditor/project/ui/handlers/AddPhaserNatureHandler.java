package phasereditor.project.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.project.core.PhaserProjectNature;
import phasereditor.project.core.ProjectCore;
import phasereditor.project.core.codegen.SourceLang;

public class AddPhaserNatureHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IStructuredSelection sel = HandlerUtil.getCurrentStructuredSelection(event);
		for (Object obj : sel.toArray()) {
			if (obj instanceof IProject) {
				try {
					IProject project = (IProject) obj;
					if (!PhaserProjectNature.hasNature(project)) {
						SourceLang lang = ProjectCore.getProjectLanguage(project);
						PhaserProjectNature.addPhaserNature(project, lang, new NullProgressMonitor());
					}
				} catch (CoreException e) {
					throw new RuntimeException(e);
				}
			}
		}

		return null;
	}

}
