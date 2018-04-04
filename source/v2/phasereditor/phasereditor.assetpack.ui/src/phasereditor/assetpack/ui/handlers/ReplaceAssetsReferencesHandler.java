package phasereditor.assetpack.ui.handlers;

import org.eclipse.search.ui.ISearchResultViewPart;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.swt.widgets.Display;

import phasereditor.assetpack.ui.search.SearchAssetResultPage;
import phasereditor.assetpack.ui.search.SearchAssetResultPage.ReplaceAction;

public class ReplaceAssetsReferencesHandler extends FindAssetReferencesHandler {

	@Override
	protected void searchFinished() {
		Display.getDefault().asyncExec(new Runnable() {

			@Override
			public void run() {
				ISearchResultViewPart view = NewSearchUI.getSearchResultView();
				SearchAssetResultPage page = (SearchAssetResultPage) view.getActivePage();
				ReplaceAction action = page.getReplaceAllAction();
				action.run();
			}
		});
	}
}
