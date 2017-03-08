// The MIT License (MIT)
//
// Copyright (c) 2015, 2017 Arian Fornaris
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
package phasereditor.assetpack.ui.refactorings;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.LayoutConstants;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import phasereditor.assetpack.core.AssetModel;

/**
 * @author arian
 *
 */
public class AssetDeleteWizardPage extends UserInputWizardPage {

	public AssetDeleteWizardPage() {
		super("Delete Asset");
		setTitle("Remove Asset");
	}

	@Override
	public void createControl(Composite parent) {
		StringBuilder sb = new StringBuilder();

		sb.append("Are you sure do you want to delete ");

		Object[] assets = ((AssetDeleteRefactoring) getRefactoring()).getProcessor().getElements();

		if (assets.length == 1) {
			AssetModel asset = (AssetModel) assets[0];
			sb.append("'" + asset.getKey() + "'");
		} else {
			sb.append(assets.length + " elements?");
		}

		sb.append("?");


		initializeDialogUnits(parent);

		Point defaultSpacing= LayoutConstants.getSpacing();

		Composite composite= new Composite(parent, SWT.NONE);
		GridLayout gridLayout= new GridLayout(2, false);
		gridLayout.horizontalSpacing= defaultSpacing.x * 2;
		gridLayout.verticalSpacing= defaultSpacing.y;

		composite.setLayout(gridLayout);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Image image= parent.getDisplay().getSystemImage(SWT.ICON_QUESTION);
		Label imageLabel = new Label(composite, SWT.NULL);
		imageLabel.setBackground(image.getBackground());
		imageLabel.setImage(image);
		imageLabel.setLayoutData(new GridData(SWT.CENTER, SWT.BEGINNING, false, false));

		Label label= new Label(composite, SWT.WRAP);
		label.setText(sb.toString());
		
		GridData gridData= new GridData(SWT.FILL, SWT.FILL, true, false);
		gridData.widthHint= convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH);
		label.setLayoutData(gridData);

		setControl(composite);
		
	}

}
