package phasereditor.ide.ui;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.navigator.resources.ProjectExplorer;

import phasereditor.chains.ui.views.ChainsView;
import phasereditor.inspect.ui.views.JsdocView;
import phasereditor.inspect.ui.views.PhaserExamplesView;
import phasereditor.inspect.ui.views.PhaserFilesView;
import phasereditor.inspect.ui.views.PhaserHierarchyView;
import phasereditor.inspect.ui.views.PhaserTypesView;

public class LabsPerspectiveFactory implements IPerspectiveFactory {

	private static final String LEFT_FOLDER = "phasereditor.ide.left";

	@Override
	public void createInitialLayout(IPageLayout layout) {
		IFolderLayout leftFolder = layout.createFolder(LEFT_FOLDER, IPageLayout.LEFT, 0.2f, IPageLayout.ID_EDITOR_AREA);
		leftFolder.addView(PhaserHierarchyView.ID);
		leftFolder.addView(ProjectExplorer.VIEW_ID);

		layout.addView(PhaserTypesView.ID, IPageLayout.BOTTOM, 0.5f, LEFT_FOLDER);
		layout.addView(PhaserExamplesView.ID, IPageLayout.RIGHT, 0.8f, IPageLayout.ID_EDITOR_AREA);
		layout.addView(PhaserFilesView.ID, IPageLayout.RIGHT, 0.7f, IPageLayout.ID_EDITOR_AREA);
		layout.addView(JsdocView.ID, IPageLayout.BOTTOM, 0.5f, PhaserFilesView.ID);
		
		layout.addView(ChainsView.ID, IPageLayout.BOTTOM, 0.6f, IPageLayout.ID_EDITOR_AREA);
		
	}

}
