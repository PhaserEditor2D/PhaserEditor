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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHoverExtension2;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.StringLiteral;
import org.eclipse.wst.jsdt.internal.corext.dom.NodeFinder;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.ASTProvider;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.ClassFileEditor;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.IClassFileEditorInput;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.wst.jsdt.internal.ui.text.java.hover.AbstractJavaEditorTextHover;

import com.subshell.snippets.jface.tooltip.tooltipsupport.ICustomInformationControlCreator;

import phasereditor.assetpack.ui.AssetPackUI;

@SuppressWarnings("restriction")
public class PhaserStringHover extends AbstractJavaEditorTextHover implements ITextHoverExtension2 {

	private Object _object;
	private List<ICustomInformationControlCreator> _creators;

	public PhaserStringHover() {
		_creators =  new ArrayList<>( AssetPackUI.getInformationControlCreatorsForTooltips());
		_creators.add(new ICustomInformationControlCreator() {
			
			@Override
			public IInformationControl createInformationControl(Shell parent) {
				return new EasingInformationControl(parent);
			}
			
			@Override
			public boolean isSupported(Object info) {
				return info instanceof Function;
			}
		});
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.ui.text.java.hover.
	 * AbstractJavaEditorTextHover
	 * #getHoverRegion(org.eclipse.jface.text.ITextViewer, int)
	 */
	@Override
	public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
		if (!(getEditor() instanceof JavaEditor))
			return null;

		IJavaScriptElement je = getEditorInputJavaElement();
		if (je == null)
			return null;

		// Never wait for an AST in UI thread.
		JavaScriptUnit ast = JavaScriptPlugin.getDefault().getASTProvider().getAST(je, ASTProvider.WAIT_NO, null);
		if (ast == null)
			return null;

		ASTNode node = NodeFinder.perform(ast, offset, 1);
		if (node instanceof StringLiteral) {
			StringLiteral stringLiteral = (StringLiteral) node;
			return new Region(stringLiteral.getStartPosition(), stringLiteral.getLength());
		}

		return null;
	}

	private IJavaScriptElement getEditorInputJavaElement() {
		if (getEditor() instanceof CompilationUnitEditor)
			return JavaScriptPlugin.getDefault().getWorkingCopyManager().getWorkingCopy(getEditor().getEditorInput());
		else if (getEditor() instanceof ClassFileEditor) {
			IEditorInput editorInput = getEditor().getEditorInput();
			if (editorInput instanceof IClassFileEditorInput)
				return ((IClassFileEditorInput) editorInput).getClassFile();

		}
		return null;
	}

	@Override
	public Object getHoverInfo2(ITextViewer textViewer, IRegion hoverRegion) {
		IEditorPart editor = getEditor();
		if (editor instanceof JavaEditor) {
			JavaEditor javaEditor = (JavaEditor) editor;
			String str = TextUI.getStringLiteralUnderCursor(javaEditor, hoverRegion.getOffset(),
					hoverRegion.getLength());
			if (str != null) {
				Object obj = TextUI.getObjectFromString(str, javaEditor);
				_object = obj;
				return obj;
			}
		}
		return null;
	}

	public Object getObject() {
		return _object;
	}

	@Override
	public IInformationControlCreator getHoverControlCreator() {
		if (_object != null) {
			for (ICustomInformationControlCreator creator : _creators) {
				if (creator.isSupported(_object)) {
					return new IInformationControlCreator() {

						@Override
						public IInformationControl createInformationControl(Shell parent) {
							IInformationControl informationControl = creator.createInformationControl(parent);
							return informationControl;
						}
					};
				}
			}
		}
		return null;
	}
}
