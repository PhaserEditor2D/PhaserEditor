package phasereditor.canvas.ui.handlers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.canvas.core.CanvasCore;
import phasereditor.canvas.core.CanvasFile;

public class CompileCanvasFileHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		Object[] array = HandlerUtil.getCurrentStructuredSelection(event).toArray();

		List<CanvasFile> files = new ArrayList<>();

		for (Object obj : array) {
			if (obj instanceof CanvasFile) {
				files.add((CanvasFile) obj);
			}
		}

		WorkspaceJob job = new WorkspaceJob("Compiling canvas files.") {

			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
				monitor.beginTask("Compiling canvas files", files.size());

				for (CanvasFile file : files) {
					CanvasCore.compile(file, monitor);
					monitor.worked(1);
				}

				return Status.OK_STATUS;
			}
		};
		job.schedule();

		return null;
	}

}
