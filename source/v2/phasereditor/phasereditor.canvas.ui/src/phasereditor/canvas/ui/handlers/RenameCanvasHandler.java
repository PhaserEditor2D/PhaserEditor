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

import phasereditor.canvas.core.CanvasFile;

@SuppressWarnings("restriction")
public class RenameCanvasHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IStructuredSelection sel = HandlerUtil.getCurrentStructuredSelection(event);

		List<IFile> toRename = new ArrayList<>();

		Object[] array = sel.toArray();

		for (Object obj : array) {
			CanvasFile cfile = (CanvasFile) obj;
			toRename.add(cfile.getFile());
		}

		LTKLauncher.openRenameWizard(new StructuredSelection(toRename));

		return null;
	}
}
