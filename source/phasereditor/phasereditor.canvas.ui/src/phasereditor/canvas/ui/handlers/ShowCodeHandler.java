package phasereditor.canvas.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.progress.WorkbenchJob;

import phasereditor.canvas.ui.editors.CanvasEditor;

public class ShowCodeHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		CanvasEditor editor = (CanvasEditor) HandlerUtil.getActiveEditor(event);
		Job job = new WorkbenchJob(HandlerUtil.getActiveShell(event).getDisplay(), "Open source file") {

			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				IFile file = editor.getFileToGenerate();
				if (!file.exists()) {
					editor.doSave(monitor);
				}
				IWorkbenchPage activePage = HandlerUtil.getActiveWorkbenchWindow(event).getActivePage();
				try {
					IDE.openEditor(activePage, file);
				} catch (PartInitException e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				}
				return Status.OK_STATUS;
			}
		};
		job.schedule();
		return null;
	}

}
