package phasereditor.project.ui.handlers;

import static java.lang.System.currentTimeMillis;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.navigator.resources.ProjectExplorer;

public class OpenNewProjectExplorerHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchPage page = HandlerUtil.getActiveWorkbenchWindow(event).getActivePage();
		try {
			ProjectExplorer view = (ProjectExplorer) page.showView(ProjectExplorer.VIEW_ID, ProjectExplorer.VIEW_ID + "." + currentTimeMillis(),
					IWorkbenchPage.VIEW_CREATE);
			view.setLinkingEnabled(true);
			page.activate(view);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}
