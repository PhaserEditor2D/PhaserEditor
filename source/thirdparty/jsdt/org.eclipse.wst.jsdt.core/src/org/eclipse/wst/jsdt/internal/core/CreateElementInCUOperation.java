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
package org.eclipse.wst.jsdt.internal.core;

import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptModelStatus;
import org.eclipse.wst.jsdt.core.IJavaScriptModelStatusConstants;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ASTParser;
import org.eclipse.wst.jsdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.wst.jsdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.wst.jsdt.core.dom.rewrite.ListRewrite;
import org.eclipse.wst.jsdt.internal.core.util.Util;

/**
 * <p>This abstract class implements behavior common to <code>CreateElementInCUOperations</code>.
 * To create a compilation unit, or an element contained in a compilation unit, the
 * source code for the entire compilation unit is updated and saved.
 *
 * <p>The element being created can be positioned relative to an existing
 * element in the compilation unit via the methods <code>#createAfter</code>
 * and <code>#createBefore</code>. By default, the new element is positioned
 * as the last child of its parent element.
 *
 */
public abstract class CreateElementInCUOperation extends JavaModelOperation {
	/**
	 * The compilation unit AST used for this operation
	 */
	protected JavaScriptUnit cuAST;
	/**
	 * A constant meaning to position the new element
	 * as the last child of its parent element.
	 */
	protected static final int INSERT_LAST = 1;
	/**
	 * A constant meaning to position the new element
	 * after the element defined by <code>fAnchorElement</code>.
	 */
	protected static final int INSERT_AFTER = 2;

	/**
	 * A constant meaning to position the new element
	 * before the element defined by <code>fAnchorElement</code>.
	 */
	protected static final int INSERT_BEFORE = 3;
	/**
	 * One of the position constants, describing where
	 * to position the newly created element.
	 */
	protected int insertionPolicy = INSERT_LAST;
	/**
	 * The element that the newly created element is
	 * positioned relative to, as described by
	 * <code>fInsertPosition</code>, or <code>null</code>
	 * if the newly created element will be positioned
	 * last.
	 */
	protected IJavaScriptElement anchorElement = null;
	/**
	 * A flag indicating whether creation of a new element occurred.
	 * A request for creating a duplicate element would request in this
	 * flag being set to <code>false</code>. Ensures that no deltas are generated
	 * when creation does not occur.
	 */
	protected boolean creationOccurred = true;
	/**
	 * Constructs an operation that creates a Java Language Element with
	 * the specified parent, contained within a compilation unit.
	 */
	public CreateElementInCUOperation(IJavaScriptElement parentElement) {
		super(null, new IJavaScriptElement[]{parentElement});
		initializeDefaultPosition();
	}
	protected void apply(ASTRewrite rewriter, IDocument document, Map options) throws JavaScriptModelException {
		TextEdit edits = rewriter.rewriteAST(document, options);
 		try {
	 		edits.apply(document);
 		} catch (BadLocationException e) {
 			throw new JavaScriptModelException(e, IJavaScriptModelStatusConstants.INVALID_CONTENTS);
 		}
	}
	/**
	 * Only allow cancelling if this operation is not nested.
	 */
	protected void checkCanceled() {
		if (!isNested) {
			super.checkCanceled();
		}
	}
	/**
	 * Instructs this operation to position the new element after
	 * the given sibling, or to add the new element as the last child
	 * of its parent if <code>null</code>.
	 */
	public void createAfter(IJavaScriptElement sibling) {
		setRelativePosition(sibling, INSERT_AFTER);
	}
	/**
	 * Instructs this operation to position the new element before
	 * the given sibling, or to add the new element as the last child
	 * of its parent if <code>null</code>.
	 */
	public void createBefore(IJavaScriptElement sibling) {
		setRelativePosition(sibling, INSERT_BEFORE);
	}
	/**
	 * Execute the operation - generate new source for the compilation unit
	 * and save the results.
	 *
	 * @exception JavaScriptModelException if the operation is unable to complete
	 */
	protected void executeOperation() throws JavaScriptModelException {
		try {
			beginTask(getMainTaskName(), getMainAmountOfWork());
			JavaElementDelta delta = newJavaElementDelta();
			IJavaScriptUnit unit = getCompilationUnit();
			generateNewCompilationUnitAST(unit);
			if (this.creationOccurred) {
				//a change has really occurred
				unit.save(null, false);
				boolean isWorkingCopy = unit.isWorkingCopy();
				if (!isWorkingCopy)
					setAttribute(HAS_MODIFIED_RESOURCE_ATTR, TRUE);
				worked(1);
				resultElements = generateResultHandles();
				if (!isWorkingCopy // if unit is working copy, then save will have already fired the delta
						&& !Util.isExcluded(unit)
						&& unit.getParent().exists()) {
					for (int i = 0; i < resultElements.length; i++) {
						delta.added(resultElements[i]);
					}
					addDelta(delta);
				} // else unit is created outside classpath
				  // non-java resource delta will be notified by delta processor
			}
		} finally {
			done();
		}
	}

	/*
	 * Returns the property descriptor for the element being created.
	 */
	protected abstract StructuralPropertyDescriptor getChildPropertyDescriptor(ASTNode parent);

	/*
	 * Returns an AST node for the element being created.
	 */
	protected abstract ASTNode generateElementAST(ASTRewrite rewriter, IDocument document, IJavaScriptUnit cu) throws JavaScriptModelException;
	/*
	 * Generates a new AST for this operation and applies it to the given cu
	 */
	protected void generateNewCompilationUnitAST(IJavaScriptUnit cu) throws JavaScriptModelException {
		this.cuAST = parse(cu);

		AST ast = this.cuAST.getAST();
		ASTRewrite rewriter = ASTRewrite.create(ast);
		IDocument document = getDocument(cu);
		ASTNode child = generateElementAST(rewriter, document, cu);
		if (child != null) {
			ASTNode parent = ((JavaElement) getParentElement()).findNode(this.cuAST);
			if (parent == null)
				parent = this.cuAST;
			insertASTNode(rewriter, parent, child);
			apply(rewriter, document, cu.getJavaScriptProject().getOptions(true));
		}
		worked(1);
	}
	/**
	 * Creates and returns the handle for the element this operation created.
	 */
	protected abstract IJavaScriptElement generateResultHandle();
	/**
	 * Creates and returns the handles for the elements this operation created.
	 */
	protected IJavaScriptElement[] generateResultHandles() {
		return new IJavaScriptElement[]{generateResultHandle()};
	}
	/**
	 * Returns the compilation unit in which the new element is being created.
	 */
	protected IJavaScriptUnit getCompilationUnit() {
		return getCompilationUnitFor(getParentElement());
	}
	/**
	 * Returns the amount of work for the main task of this operation for
	 * progress reporting.
	 */
	protected int getMainAmountOfWork(){
		return 2;
	}
	/**
	 * Returns the name of the main task of this operation for
	 * progress reporting.
	 */
	public abstract String getMainTaskName();

	protected ISchedulingRule getSchedulingRule() {
		IResource resource = getCompilationUnit().getResource();
		IWorkspace workspace = resource.getWorkspace();
		return workspace.getRuleFactory().modifyRule(resource);
	}
	/**
	 * Sets the default position in which to create the new type
	 * member.
	 * Operations that require a different default position must
	 * override this method.
	 */
	protected void initializeDefaultPosition() {
		// By default, the new element is positioned as the
		// last child of the parent element in which it is created.
	}
	/**
	 * Inserts the given child into the given AST,
	 * based on the position settings of this operation.
	 *
	 * @see #createAfter(IJavaScriptElement)
	 * @see #createBefore(IJavaScriptElement)
	 */
	protected void insertASTNode(ASTRewrite rewriter, ASTNode parent, ASTNode child) throws JavaScriptModelException {
		StructuralPropertyDescriptor propertyDescriptor = getChildPropertyDescriptor(parent);
		if (propertyDescriptor instanceof ChildListPropertyDescriptor) {
			ChildListPropertyDescriptor childListPropertyDescriptor = (ChildListPropertyDescriptor) propertyDescriptor;
	 		ListRewrite rewrite = rewriter.getListRewrite(parent, childListPropertyDescriptor);
	 		switch (this.insertionPolicy) {
	 			case INSERT_BEFORE:
	 				ASTNode element = ((JavaElement) this.anchorElement).findNode(this.cuAST);
	 				if (childListPropertyDescriptor.getElementType().isAssignableFrom(element.getClass()))
		 				rewrite.insertBefore(child, element, null);
	 				else
	 					// case of an empty import list: the anchor element is the top level type and cannot be used in insertBefore as it is not the same type
	 					rewrite.insertLast(child, null);
	 				break;
	 			case INSERT_AFTER:
	 				element = ((JavaElement) this.anchorElement).findNode(this.cuAST);
	 				if (childListPropertyDescriptor.getElementType().isAssignableFrom(element.getClass()))
		 				rewrite.insertAfter(child, element, null);
	 				else
	 					// case of an empty import list: the anchor element is the top level type and cannot be used in insertAfter as it is not the same type
	 					rewrite.insertLast(child, null);
	 				break;
	 			case INSERT_LAST:
	 				rewrite.insertLast(child, null);
	 				break;
	 		}
		} else {
			rewriter.set(parent, propertyDescriptor, child, null);
		}
 	}
	protected JavaScriptUnit parse(IJavaScriptUnit cu) throws JavaScriptModelException {
		// ensure cu is consistent (noop if already consistent)
		cu.makeConsistent(this.progressMonitor);
		// create an AST for the compilation unit
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setSource(cu);
		return (JavaScriptUnit) parser.createAST(this.progressMonitor);
	}
	/**
	 * Sets the name of the <code>DOMNode</code> that will be used to
	 * create this new element.
	 * Used by the <code>CopyElementsOperation</code> for renaming.
	 * Only used for <code>CreateTypeMemberOperation</code>
	 */
	protected void setAlteredName(String newName) {
		// implementation in CreateTypeMemberOperation
	}
	/**
	 * Instructs this operation to position the new element relative
	 * to the given sibling, or to add the new element as the last child
	 * of its parent if <code>null</code>. The <code>position</code>
	 * must be one of the position constants.
	 */
	protected void setRelativePosition(IJavaScriptElement sibling, int policy) throws IllegalArgumentException {
		if (sibling == null) {
			this.anchorElement = null;
			this.insertionPolicy = INSERT_LAST;
		} else {
			this.anchorElement = sibling;
			this.insertionPolicy = policy;
		}
	}
	/**
	 * Possible failures: <ul>
	 *  <li>NO_ELEMENTS_TO_PROCESS - the compilation unit supplied to the operation is
	 * 		<code>null</code>.
	 *  <li>INVALID_NAME - no name, a name was null or not a valid
	 * 		import declaration name.
	 *  <li>INVALID_SIBLING - the sibling provided for positioning is not valid.
	 * </ul>
	 * @see IJavaScriptModelStatus
	 * @see org.eclipse.wst.jsdt.core.JavaScriptConventions
	 */
	public IJavaScriptModelStatus verify() {
		if (getParentElement() == null) {
			return new JavaModelStatus(IJavaScriptModelStatusConstants.NO_ELEMENTS_TO_PROCESS);
		}
		if (this.anchorElement != null) {
			IJavaScriptElement domPresentParent = this.anchorElement.getParent();
			if (domPresentParent.getElementType() == IJavaScriptElement.IMPORT_CONTAINER) {
				domPresentParent = domPresentParent.getParent();
			}
			if (!domPresentParent.equals(getParentElement())) {
				return new JavaModelStatus(IJavaScriptModelStatusConstants.INVALID_SIBLING, this.anchorElement);
			}
		}
		return JavaModelStatus.VERIFIED_OK;
	}
}
