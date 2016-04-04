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

import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.StringLiteral;
import org.eclipse.wst.jsdt.internal.corext.dom.NodeFinder;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.ASTProvider;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.wst.jsdt.ui.JavaScriptUI;

import phasereditor.assetpack.core.AssetPackCore;
import phasereditor.project.core.ProjectCore;
import phasereditor.ui.PhaserEditorUI;

@SuppressWarnings("restriction")
public class TextUI {
	public static final String PLUGIN_IDE = Activator.PLUGIN_ID;

	public static void showJavaScriptElementInPreview(JavaEditor javaEditor) {
		List<Object> elements = getReferencedAssetElements(javaEditor);
		if (elements.isEmpty()) {
			PhaserEditorUI.openPreview(elements.get(0));
		}
	}

	/**
	 * Get the string literal in the the cursor.
	 * 
	 * @param javaEditor
	 *            The editor.
	 * @return The string value of the literal under the cursor, or null if no
	 *         literal is found.
	 */
	public static String getStringLiteralUnderCursor(JavaEditor javaEditor) {
		ISourceViewer viewer = javaEditor.getViewer();

		if (viewer == null)
			return null;

		Point selectedRange = viewer.getSelectedRange();
		int length = selectedRange.y;
		int offset = selectedRange.x;

		return getStringLiteralUnderCursor(javaEditor, offset, length);
	}

	/**
	 * Get the string literal in the the cursor.
	 * 
	 * @param javaEditor
	 *            The editor.
	 * @param offset
	 *            The cursor offset.
	 * @param length
	 *            The selection length.
	 * @return The string value of the literal under the cursor, or null if no
	 *         literal is found.
	 */
	public static String getStringLiteralUnderCursor(JavaEditor javaEditor, int offset, int length) {
		ISourceViewer viewer = javaEditor.getViewer();

		if (viewer == null)
			return null;

		IJavaScriptElement element = JavaScriptUI.getEditorInputJavaElement(javaEditor.getEditorInput());
		JavaScriptUnit ast = ASTProvider.getASTProvider().getAST(element, ASTProvider.WAIT_YES, null);

		if (ast == null)
			return null;

		NodeFinder finder = new NodeFinder(offset, length);
		ast.accept(finder);

		ASTNode node = finder.getCoveringNode();

		if (node instanceof StringLiteral) {
			StringLiteral literal = (StringLiteral) node;

			String stringValue = literal.getLiteralValue();
			return stringValue;
		}

		return null;
	}

	public static IFile getReferencedFile(JavaEditor javaEditor, String literal) {
		IEditorInput input = javaEditor.getEditorInput();
		if (input instanceof FileEditorInput) {
			IProject project = ((FileEditorInput) input).getFile().getProject();
			IContainer webContentFolder = ProjectCore.getWebContentFolder(project);
			if (webContentFolder != null) {
				IFile refFile = webContentFolder.getFile(new Path(literal));
				if (refFile.exists()) {
					return refFile;
				}
			}
		}
		return null;
	}

	public static List<Object> getReferencedAssetElements(JavaEditor javaEditor) {
		String key = getStringLiteralUnderCursor(javaEditor);

		if (key != null) {
			IEditorInput input = javaEditor.getEditorInput();
			if (input instanceof FileEditorInput) {
				IProject project = ((FileEditorInput) input).getFile().getProject();
				List<Object> matching = AssetPackCore.findAssetObjects(project, key);
				return matching;
			}
		}

		return Collections.emptyList();
	}

	public static Object getObjectFromString(String str, JavaEditor javaEditor) {
		Object result = str;
		result = TextUI.getReferencedFile(javaEditor, str);
		if (result == null) {
			// preview assets
			IProject project = EditorUtility.getJavaProject(javaEditor.getEditorInput()).getProject();
			String key = str;
			List<Object> assetObjects = AssetPackCore.findAssetObjects(project, key);
			if (assetObjects.size() > 0) {
				result = assetObjects.get(0);
			}
		}
		return result;
	}

	public static JavaEditor getCurrentJavaEditor() {
		IEditorPart editor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();

		if (editor == null) {
			return null;
		}

		if (editor instanceof JavaEditor) {
			return (JavaEditor) editor;
		}

		return null;
	}

	public static String getHoverInfoMessage(Object object) {
		return object == null ? null : object.toString();
	}
}
