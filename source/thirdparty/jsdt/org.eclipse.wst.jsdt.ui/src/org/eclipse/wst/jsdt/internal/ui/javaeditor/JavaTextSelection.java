/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.javaeditor;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.BodyDeclaration;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.SingleVariableDeclaration;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment;
import org.eclipse.wst.jsdt.internal.corext.dom.Selection;
import org.eclipse.wst.jsdt.internal.corext.dom.SelectionAnalyzer;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.actions.SelectionConverter;

/**
 * A special text selection that gives access to the resolved and
 * enclosing element.
 */
public class JavaTextSelection extends TextSelection {

	private IJavaScriptElement fElement;
	private IJavaScriptElement[] fResolvedElements;

	private boolean fEnclosingElementRequested;
	private IJavaScriptElement fEnclosingElement;

	private boolean fPartialASTRequested;
	private JavaScriptUnit fPartialAST;

	private boolean fNodesRequested;
	private ASTNode[] fSelectedNodes;
	private ASTNode fCoveringNode;

	private boolean fInMethodBodyRequested;
	private boolean fInMethodBody;

	private boolean fInClassInitializerRequested;
	private boolean fInClassInitializer;

	private boolean fInVariableInitializerRequested;
	private boolean fInVariableInitializer;

	/**
	 * Creates a new text selection at the given offset and length.
	 */
	public JavaTextSelection(IJavaScriptElement element, IDocument document, int offset, int length) {
		super(document, offset, length);
		fElement= element;
	}

	/**
	 * Resolves the <code>IJavaScriptElement</code>s at the current offset. Returns
	 * an empty array if the string under the offset doesn't resolve to a
	 * <code>IJavaScriptElement</code>.
	 *
	 * @return the resolved java elements at the current offset
	 * @throws JavaScriptModelException passed from the underlying code resolve API
	 */
	public IJavaScriptElement[] resolveElementAtOffset() throws JavaScriptModelException {
		if (fResolvedElements != null)
			return fResolvedElements;
		// long start= System.currentTimeMillis();
		fResolvedElements= SelectionConverter.codeResolve(fElement, this);
		// System.out.println("Time resolving element: " + (System.currentTimeMillis() - start));
		return fResolvedElements;
	}

	public IJavaScriptElement resolveEnclosingElement() throws JavaScriptModelException {
		if (fEnclosingElementRequested)
			return fEnclosingElement;
		fEnclosingElementRequested= true;
		fEnclosingElement= SelectionConverter.resolveEnclosingElement(fElement, this);
		return fEnclosingElement;
	}

	public JavaScriptUnit resolvePartialAstAtOffset() {
		if (fPartialASTRequested)
			return fPartialAST;
		fPartialASTRequested= true;
		if (! (fElement instanceof IJavaScriptUnit))
			return null;
		// long start= System.currentTimeMillis();
		fPartialAST= JavaScriptPlugin.getDefault().getASTProvider().getAST(fElement, ASTProvider.WAIT_YES, null);
		// System.out.println("Time requesting partial AST: " + (System.currentTimeMillis() - start));
		return fPartialAST;
	}

	public ASTNode[] resolveSelectedNodes() {
		if (fNodesRequested)
			return fSelectedNodes;
		fNodesRequested= true;
		JavaScriptUnit root= resolvePartialAstAtOffset();
		if (root == null)
			return null;
		Selection ds= Selection.createFromStartLength(getOffset(), getLength());
		SelectionAnalyzer analyzer= new SelectionAnalyzer(ds, false);
		root.accept(analyzer);
		fSelectedNodes= analyzer.getSelectedNodes();
		fCoveringNode= analyzer.getLastCoveringNode();
		return fSelectedNodes;
	}

	public ASTNode resolveCoveringNode() {
		if (fNodesRequested)
			return fCoveringNode;
		resolveSelectedNodes();
		return fCoveringNode;
	}

	public boolean resolveInMethodBody() {
		if (fInMethodBodyRequested)
			return fInMethodBody;
		fInMethodBodyRequested= true;
		resolveSelectedNodes();
		ASTNode node= getStartNode();
		if (node == null) {
			fInMethodBody= true;
		} else {
			while (node != null) {
				int nodeType= node.getNodeType();
				if (nodeType == ASTNode.BLOCK && node.getParent() instanceof BodyDeclaration) {
					fInMethodBody= node.getParent().getNodeType() == ASTNode.FUNCTION_DECLARATION;
					break;
				} else if (nodeType == ASTNode.ANONYMOUS_CLASS_DECLARATION) {
					fInMethodBody= false;
					break;
				}
				node= node.getParent();
			}
		}
		return fInMethodBody;
	}

	public boolean resolveInClassInitializer() {
		if (fInClassInitializerRequested)
			return fInClassInitializer;
		fInClassInitializerRequested= true;
		resolveSelectedNodes();
		ASTNode node= getStartNode();
		if (node == null) {
			fInClassInitializer= true;
		} else {
			while (node != null) {
				int nodeType= node.getNodeType();
				if (node instanceof AbstractTypeDeclaration) {
					fInClassInitializer= false;
					break;
				} else if (nodeType == ASTNode.ANONYMOUS_CLASS_DECLARATION) {
					fInClassInitializer= false;
					break;
				} else if (nodeType == ASTNode.INITIALIZER) {
					fInClassInitializer= true;
					break;
				}
				node= node.getParent();
			}
		}
		return fInClassInitializer;
	}

	public boolean resolveInVariableInitializer() {
		if (fInVariableInitializerRequested)
			return fInVariableInitializer;
		fInVariableInitializerRequested= true;
		resolveSelectedNodes();
		ASTNode node= getStartNode();
		ASTNode last= null;
		while (node != null) {
			int nodeType= node.getNodeType();
			if (node instanceof AbstractTypeDeclaration) {
				fInVariableInitializer= false;
				break;
			} else if (nodeType == ASTNode.ANONYMOUS_CLASS_DECLARATION) {
				fInVariableInitializer= false;
				break;
			} else if (nodeType == ASTNode.VARIABLE_DECLARATION_FRAGMENT &&
					   ((VariableDeclarationFragment)node).getInitializer() == last) {
				fInVariableInitializer= true;
				break;
			} else if (nodeType == ASTNode.SINGLE_VARIABLE_DECLARATION &&
				       ((SingleVariableDeclaration)node).getInitializer() == last) {
				fInVariableInitializer= true;
				break;
			}
			last= node;
			node= node.getParent();
		}
		return fInVariableInitializer;
	}

	private ASTNode getStartNode() {
		if (fSelectedNodes != null && fSelectedNodes.length > 0)
			return fSelectedNodes[0];
		else
			return fCoveringNode;
	}
}
