package phasereditor.assetexplorer.ui.handlers;

import static phasereditor.ui.IEditorSharedImages.IMG_PHASER_PROJECT;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.assetexplorer.ui.views.AssetsView;
import phasereditor.project.core.ProjectCore;
import phasereditor.ui.EditorSharedImages;

public class SwitchProjectHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		var shell = HandlerUtil.getActiveShell(event);

		var manager = new MenuManager();

		var view = (AssetsView) HandlerUtil.getActivePart(event);

		var projectInContent = view.getProjectInContent();

		for (var project : ProjectCore.getPhaserProjects()) {

			var action = new Action(project.getName(), IAction.AS_CHECK_BOX) {
				{
					setImageDescriptor(EditorSharedImages.getImageDescriptor(IMG_PHASER_PROJECT));
				}

				@Override
				public void run() {
					view.forceToFocusOnProject(project);
				}
			};

			if (project.equals(projectInContent)) {
				action.setChecked(true);
			}

			manager.add(action);

		}

		var menu = manager.createContextMenu(shell);

		menu.setVisible(true);

		return null;
	}

}
