/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.core.util;

import org.eclipse.wst.jsdt.core.IInitializer;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.ISourceRange;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ASTVisitor;
import org.eclipse.wst.jsdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.wst.jsdt.core.dom.ClassInstanceCreation;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.IBinding;
import org.eclipse.wst.jsdt.core.dom.ImportDeclaration;
import org.eclipse.wst.jsdt.core.dom.Initializer;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.PackageDeclaration;
import org.eclipse.wst.jsdt.core.dom.TypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment;
import org.eclipse.wst.jsdt.internal.core.SourceRefElement;

public class DOMFinder extends ASTVisitor {

	public ASTNode foundNode = null;
	public IBinding foundBinding = null;

	private JavaScriptUnit ast;
	private SourceRefElement element;
	private boolean resolveBinding;
	private int rangeStart = -1, rangeLength = 0;

	public DOMFinder(JavaScriptUnit ast, SourceRefElement element, boolean resolveBinding) {
		this.ast = ast;
		this.element = element;
		this.resolveBinding = resolveBinding;
	}

	protected boolean found(ASTNode node, ASTNode name) {
		if (name.getStartPosition() == this.rangeStart && name.getLength() == this.rangeLength) {
			this.foundNode = node;
			return true;
		}
		return false;
	}

	public ASTNode search() throws JavaScriptModelException {
		ISourceRange range = null;
		if (this.element instanceof IMember && !(this.element instanceof IInitializer))
			range = ((IMember) this.element).getNameRange();
		else
			range = this.element.getSourceRange();
		this.rangeStart = range.getOffset();
		this.rangeLength = range.getLength();
		this.ast.accept(this);
		return this.foundNode;
	}

	public boolean visit(AnonymousClassDeclaration node) {
		ASTNode name;
		ASTNode parent = node.getParent();
		switch (parent.getNodeType()) {
			case ASTNode.CLASS_INSTANCE_CREATION:
				name = ((ClassInstanceCreation) parent).getType();
				break;
			default:
				return true;
		}
		if (found(node, name) && this.resolveBinding)
			this.foundBinding = node.resolveBinding();
		return true;
	}

	public boolean visit(ImportDeclaration node) {
		if (found(node, node) && this.resolveBinding)
			this.foundBinding = node.resolveBinding();
		return true;
	}

	public boolean visit(Initializer node) {
		// note that no binding exists for an Initializer
		found(node, node);
		return true;
	}

	public boolean visit(FunctionDeclaration node) {
		if (found(node, node.getName()) && this.resolveBinding)
			this.foundBinding = node.resolveBinding();
		return true;
	}

	public boolean visit(PackageDeclaration node) {
		if (found(node, node) && this.resolveBinding)
			this.foundBinding = node.resolveBinding();
		return true;
	}

	public boolean visit(TypeDeclaration node) {
		if (found(node, node.getName()) && this.resolveBinding)
			this.foundBinding = node.resolveBinding();
		return true;
	}

	public boolean visit(VariableDeclarationFragment node) {
		if (found(node, node.getName()) && this.resolveBinding)
			this.foundBinding = node.resolveBinding();
		return true;
	}
}
