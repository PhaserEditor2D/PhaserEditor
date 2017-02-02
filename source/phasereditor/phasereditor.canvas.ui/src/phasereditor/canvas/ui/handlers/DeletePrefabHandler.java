package phasereditor.canvas.ui.handlers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.canvas.core.CanvasCore;
import phasereditor.canvas.core.CanvasCore.PrefabReference;
import phasereditor.canvas.core.Prefab;
import phasereditor.canvas.ui.CanvasUI;

public class DeletePrefabHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IStructuredSelection sel = HandlerUtil.getCurrentStructuredSelection(event);

		List<PrefabReference> refs = new ArrayList<>();

		for (Object obj : sel.toArray()) {
			Prefab prefab = (Prefab) obj;
			refs.addAll(CanvasUI.findPrefabReferences(prefab));
		}

		if (refs.isEmpty() || MessageDialog.openConfirm(HandlerUtil.getActiveShell(event), "Delete",
				"There are " + refs.size() + " references to this prefab, do you want to delete it?")) {
			WorkspaceJob job = new WorkspaceJob("Delete prefabs") {

				@Override
				public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
					Object[] array = sel.toArray();
					monitor.beginTask("Deleting prefabs", array.length);

					Set<IProject> projects = new HashSet<>();

					for (Object obj : array) {
						Prefab prefab = (Prefab) obj;
						projects.add(prefab.getFile().getProject());
						CanvasCore.deletePrefab(prefab);
						monitor.worked(1);
					}

					for (IProject project : projects) {
						project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
					}

					return Status.OK_STATUS;
				}

			};
			job.schedule();
		}
		return null;
	}

}
