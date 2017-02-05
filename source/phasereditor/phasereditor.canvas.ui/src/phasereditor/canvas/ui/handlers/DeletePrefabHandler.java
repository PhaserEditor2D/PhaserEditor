package phasereditor.canvas.ui.handlers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.internal.ide.actions.LTKLauncher;

import phasereditor.canvas.core.Prefab;

@SuppressWarnings("restriction")
public class DeletePrefabHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IStructuredSelection sel = HandlerUtil.getCurrentStructuredSelection(event);

		List<IFile> toDelete = new ArrayList<>();

		Object[] array = sel.toArray();

		// TODO: ok, but the better way is to delete the source file in the
		// refactoring participant.
		
		for (Object obj : array) {
			Prefab prefab = (Prefab) obj;
			toDelete.add(prefab.getFile());
		}

		LTKLauncher.openDeleteWizard(new StructuredSelection(toDelete));

		return null;
	}

}
