package phasereditor.project.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.project.core.PhaserProjectNature;

public class RemovePhaserNatureHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IStructuredSelection sel = HandlerUtil.getCurrentStructuredSelection(event);
		for (Object obj : sel.toArray()) {
			if (obj instanceof IProject) {
				try {
					IProject project = (IProject) obj;
					if (PhaserProjectNature.hasNature(project)) {
						project.build(IncrementalProjectBuilder.CLEAN_BUILD, new NullProgressMonitor());
						PhaserProjectNature.removePhaserNature(project, new NullProgressMonitor());
					}
				} catch (CoreException e) {
					throw new RuntimeException(e);
				}
			}
		}
		
		return null;
	}

}
