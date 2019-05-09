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
package phasereditor.ui.properties;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;

import phasereditor.ui.PhaserEditorUI;

/**
 * @author arian
 *
 */
public abstract class ExtensibleFormPropertyPage extends FormPropertyPage {

	protected abstract String getPageName();

	@Override
	protected List<FormPropertySection<?>> createSections() {
		var pageName = getPageName();
		var list = new ArrayList<FormPropertySection<?>>();

		var elems = Platform.getExtensionRegistry().getConfigurationElementsFor("phasereditor.ui.formPropertySection");
		for (var elem : elems) {
			try {
				var pageName2 = elem.getAttribute("pageName");
				if (pageName.equals(pageName2)) {
					var provider = (IFormPropertySectionProvider) elem.createExecutableExtension("class");
					provider.createSections(this, list);
				}
			} catch (CoreException e) {
				PhaserEditorUI.logError(e);
			}
		}

		list.sort((a, b) -> {
			var a1 = a.isFillSpace() ? 1 : 0;
			var b1 = b.isFillSpace() ? 1 : 0;
			return Integer.compare(a1, b1);
		});

		return list;
	}

}
