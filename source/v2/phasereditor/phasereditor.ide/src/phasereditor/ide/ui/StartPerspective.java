package phasereditor.ide.ui;

import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

public class StartPerspective implements IPerspectiveFactory {
public static String ID = "phasereditor.ide.startPerspective";
	@Override
	public void createInitialLayout(IPageLayout layout) {
		layout.setEditorAreaVisible(false);
	}

}
