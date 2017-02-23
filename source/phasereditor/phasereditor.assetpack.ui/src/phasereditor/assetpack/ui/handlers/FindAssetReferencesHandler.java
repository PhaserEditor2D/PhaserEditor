package phasereditor.assetpack.ui.handlers;

import static java.lang.System.out;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.assetpack.core.IAssetKey;
import phasereditor.assetpack.ui.search.SearchAssetQuery;

public class FindAssetReferencesHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		Object sel = HandlerUtil.getCurrentStructuredSelection(event).getFirstElement();

		out.println("Find references of " + sel);

		ProgressMonitorDialog dlg = new ProgressMonitorDialog(HandlerUtil.getActiveShell(event));

		try {
			dlg.run(true, false, new IRunnableWithProgress() {

				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					Display.getDefault().asyncExec(new Runnable() {

						@Override
						public void run() {
							NewSearchUI.runQueryInForeground(dlg, new SearchAssetQuery((IAssetKey) sel));
						}
					});
				}
			});
		} catch (InvocationTargetException | InterruptedException e) {
			throw new RuntimeException(e);
		}
		return null;
	}

}
