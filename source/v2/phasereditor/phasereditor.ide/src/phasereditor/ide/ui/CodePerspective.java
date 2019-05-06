// The MIT License (MIT)
//
// Copyright (c) 2015, 2019 Arian Fornaris
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

import phasereditor.chains.ui.views.ChainsView;
import phasereditor.inspect.ui.views.JsdocView;

/**
 * @author arian
 *
 */
public class CodePerspective implements IPerspectiveFactory {

	@Override
	public void createInitialLayout(IPageLayout layout) {
		// layout.addView(IPageLayout.ID_OUTLINE, IPageLayout.LEFT, 0.2f,
		// IPageLayout.ID_EDITOR_AREA);

		var folder = layout.createFolder("bottomViews", IPageLayout.BOTTOM, 0.5f, IPageLayout.ID_EDITOR_AREA);
		folder.addView(ChainsView.ID);
		folder.addView(IPageLayout.ID_PROBLEM_VIEW);

		layout.addView(JsdocView.ID, IPageLayout.RIGHT, 0.5f, ChainsView.ID);

	}

}
