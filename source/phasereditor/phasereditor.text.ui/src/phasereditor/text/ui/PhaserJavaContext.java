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
package phasereditor.text.ui;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.internal.corext.template.java.JavaContext;

@SuppressWarnings("restriction")
class PhaserJavaContext extends JavaContext {

	public PhaserJavaContext(TemplateContextType type, IDocument document,
			int completionOffset, int completionLength,
			IJavaScriptUnit compilationUnit) {
		super(type, document, completionOffset, completionLength,
				compilationUnit);
	}

	public PhaserJavaContext(TemplateContextType type, IDocument document,
			Position completionPosition, IJavaScriptUnit compilationUnit) {
		super(type, document, completionPosition, compilationUnit);
	}

	@Override
	public boolean canEvaluate(Template template) {
		if (fForceEvaluation) {
			return true;
		}

		// the same of the super method, but it uses a "contains" matching.

		String key = getKey();
		if (template.matches(key, getContextType().getId())) {
			String name = template.getName().replace(" ", "");
			String pattern = template.getPattern().toLowerCase();
			String key2 = key.toLowerCase();
			boolean b = name.contains(key2) || pattern.contains(key2);
			return b;
		}
		return false;
	}
}