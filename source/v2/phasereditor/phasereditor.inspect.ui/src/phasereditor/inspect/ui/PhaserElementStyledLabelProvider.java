// The MIT License (MIT)
//
// Copyright (c) 2015, 2018 Arian Fornaris
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
package phasereditor.inspect.ui;

import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.wb.swt.SWTResourceManager;

import phasereditor.inspect.core.jsdoc.PhaserGlobalScope;
import phasereditor.inspect.core.jsdoc.IPhaserMember;
import phasereditor.inspect.core.jsdoc.JsdocRenderer;
import phasereditor.inspect.core.jsdoc.PhaserMethod;
import phasereditor.inspect.core.jsdoc.PhaserMethodArg;
import phasereditor.inspect.core.jsdoc.PhaserNamespace;
import phasereditor.inspect.core.jsdoc.PhaserVariable;

/**
 * @author arian
 *
 */
public class PhaserElementStyledLabelProvider extends StyledCellLabelProvider {

	private Color _secondaryColor;

	public PhaserElementStyledLabelProvider() {
		RGB rgb = new RGB(154, 131, 80);
		_secondaryColor = SWTResourceManager.getColor(rgb);
	}

	@Override
	public void update(ViewerCell cell) {

		Object element = cell.getElement();

		if (element instanceof PhaserGlobalScope) {
			cell.setText("[global]");
			cell.setImage(JsdocRenderer.getInstance().getGlobalScopeImage());
			return;
		}

		if (!(element instanceof IPhaserMember)) {
			return;
		}

		IPhaserMember member = (IPhaserMember) element;

		String text;

		String[] returnTypes = new String[0];

		int secondaryTextIndex = -1;

		if (member instanceof PhaserNamespace) {
			text = ((PhaserNamespace) member).getSimpleName();
		} else {
			text = member.getName();

			if (member instanceof PhaserMethod) {
				PhaserMethod method = (PhaserMethod) member;
				StringBuilder sb = new StringBuilder();
				for (PhaserMethodArg arg : method.getArgs()) {
					if (sb.length() > 0) {
						sb.append(",");
					}
					sb.append(arg.getName());
				}
				text += "(" + sb + ")";
				returnTypes = method.getReturnTypes();
			} else if (member instanceof PhaserVariable) {
				returnTypes = ((PhaserVariable) member).getTypes();
			}

			if (returnTypes != null && returnTypes.length > 0) {
				StringBuilder sb = new StringBuilder();
				for (String type : returnTypes) {
					if (sb.length() > 0) {
						sb.append(",");
					}
					sb.append(type);
				}
				secondaryTextIndex = text.length();
				text += ": " + sb;
			}
		}
		if (secondaryTextIndex > -1) {
			// Color secondaryColor = ChainsUI.get_pref_Chains_secondaryFgColor();
			StyleRange styleRange = new StyleRange(secondaryTextIndex, text.length(), _secondaryColor, null);
			cell.setStyleRanges(new StyleRange[] { styleRange });
		}
		cell.setText(text);
		cell.setImage(JsdocRenderer.getInstance().getImage(member));

		super.update(cell);
	}

}
