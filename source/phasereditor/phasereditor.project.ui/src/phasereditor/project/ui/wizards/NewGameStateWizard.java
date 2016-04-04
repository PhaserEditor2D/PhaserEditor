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
package phasereditor.project.ui.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

public class NewGameStateWizard extends NewPhaserSourceFileWizard {

	public NewGameStateWizard() {
		super("New Game State Source File",
				"Create a new game state source file.");
	}

	@Override
	protected PhaserSourceFileWizardPage createPage(
			IStructuredSelection selection, String pageTitle,
			String pageDescription) {
		return new PhaserSourceFileWizardPage(selection, pageTitle,
				pageDescription) {

			private Button _extendsButton;

			@Override
			protected void createAdvancedControls(Composite parent) {
				Composite comp = new Composite(parent, SWT.NONE);
				comp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				GridLayout layout = new GridLayout(1, false);
				layout.marginLeft = 0;
				comp.setLayout(layout);

				_extendsButton = new Button(comp, SWT.CHECK);
				GridData gd = new GridData(GridData.FILL_HORIZONTAL);
				gd.horizontalSpan = 1;
				gd.verticalSpan = 1;
				_extendsButton.setLayoutData(gd);
				_extendsButton
						.setText("Generate a prototype and extend the Phaser.State type.");
				_extendsButton.setSelection(false);

				super.createAdvancedControls(parent);
			}

			public boolean isExtendsPhaserState() {
				return _extendsButton.getSelection();
			}

			@Override
			protected String getTemplatePath() {
				if (isExtendsPhaserState()) {
					return "State-extends.js";
				}

				return "State.js";
			}
		};
	}

	@Override
	protected String getTemplatePath() {
		return null;
	}

}
