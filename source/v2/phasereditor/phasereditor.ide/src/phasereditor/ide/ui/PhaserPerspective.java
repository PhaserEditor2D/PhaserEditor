// The MIT License (MIT)
//
// Copyright (c) 2015 Arian Fornaris
//
// Permission is hereby granted, free of charge, to any person obtaining a
// copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to permit
// persons to whom the Software is furnished to do so, subject to the
// following conditions: The above copyright notice and this permission
// notice shall be included in all copies or substantial portions of the
// Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
// OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
// NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
// DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
// OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
// USE OR OTHER DEALINGS IN THE SOFTWARE.
package phasereditor.ide.ui;

import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.navigator.resources.ProjectExplorer;

public class PhaserPerspective implements IPerspectiveFactory {

	private static final String PREVIEW_VIEW = "phasereditor.ui.preview";
	private static final String ASSETS_VIEW = "phasereditor.assetpack.views.assetExplorer";
	public static final String ID = "phasereditor.ide.ui.perspective";

	@Override
	public void createInitialLayout(IPageLayout layout) {
		layout.addView(ProjectExplorer.VIEW_ID, IPageLayout.LEFT, 0.3f, IPageLayout.ID_EDITOR_AREA);
		layout.addPlaceholder(ProjectExplorer.VIEW_ID + ":*", IPageLayout.BOTTOM, 0.4f, ProjectExplorer.VIEW_ID);

		layout.addView("org.eclipse.ui.views.PropertySheet", IPageLayout.RIGHT, 0.5f, ProjectExplorer.VIEW_ID);
		layout.addView("org.eclipse.ui.views.ContentOutline", IPageLayout.TOP, 0.4f,
				"org.eclipse.ui.views.PropertySheet");

		layout.addView(ASSETS_VIEW, IPageLayout.RIGHT, 0.7f, IPageLayout.ID_EDITOR_AREA);
		layout.addPlaceholder(ASSETS_VIEW + ":*", IPageLayout.TOP, 0.5f, ASSETS_VIEW);

		layout.addView(PREVIEW_VIEW, IPageLayout.BOTTOM, 0.6f, ASSETS_VIEW);
		layout.addPlaceholder(PREVIEW_VIEW + ":*", IPageLayout.BOTTOM, 0.5f, PREVIEW_VIEW);

	}

}
