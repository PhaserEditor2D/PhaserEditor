package phasereditor.ide.ui;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

import phasereditor.chains.ui.views.ChainsView;
import phasereditor.inspect.ui.views.JsdocView;
import phasereditor.inspect.ui.views.PhaserExamplesView;
import phasereditor.inspect.ui.views.PhaserFilesView;
import phasereditor.inspect.ui.views.PhaserHierarchyView;
import phasereditor.inspect.ui.views.PhaserTypesView;
import phasereditor.inspect.ui.views.PhaserVersionsView;
import phasereditor.project.ui.ProjectView;

public class LabsPerspectiveFactory implements IPerspectiveFactory {

	private static final String FILES_AND_VERSIONS = "filesAndVersions";
	private static final String LEFT_FOLDER = "phasereditor.ide.left";
	public static final String ID = "phasereditor.ide.ui.labs";

	@Override
	public void createInitialLayout(IPageLayout layout) {
		IFolderLayout leftFolder = layout.createFolder(LEFT_FOLDER, IPageLayout.LEFT, 0.2f, IPageLayout.ID_EDITOR_AREA);
		leftFolder.addView(PhaserHierarchyView.ID);
		leftFolder.addView(ProjectView.ID);

		layout.addView(PhaserTypesView.ID, IPageLayout.BOTTOM, 0.5f, LEFT_FOLDER);
		layout.addView(PhaserExamplesView.ID, IPageLayout.RIGHT, 0.8f, IPageLayout.ID_EDITOR_AREA);

		{
			var folder = layout.createFolder(FILES_AND_VERSIONS, IPageLayout.RIGHT, 0.7f, IPageLayout.ID_EDITOR_AREA);
			folder.addView(PhaserFilesView.ID);
			folder.addView(PhaserVersionsView.ID);
		}

		layout.addView(JsdocView.ID, IPageLayout.BOTTOM, 0.5f, FILES_AND_VERSIONS);

		layout.addView(ChainsView.ID, IPageLayout.BOTTOM, 0.6f, IPageLayout.ID_EDITOR_AREA);

	}

}
