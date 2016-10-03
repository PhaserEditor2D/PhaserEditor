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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptModelStatusConstants;
import org.eclipse.wst.jsdt.core.IRegion;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ASTParser;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.wst.jsdt.internal.core.util.Messages;

/**
 * This operation deletes a collection of elements (and
 * all of their children).
 * If an element does not exist, it is ignored.
 *
 * <p>NOTE: This operation only deletes elements contained within leaf resources -
 * that is, elements within compilation units. To delete a compilation unit or
 * a package, etc (which have an actual resource), a DeleteResourcesOperation
 * should be used.
 */
public class DeleteElementsOperation extends MultiOperation {
	/**
	 * The elements this operation processes grouped by compilation unit
	 * @see #processElements() Keys are compilation units,
	 * values are <code>IRegion</code>s of elements to be processed in each
	 * compilation unit.
	 */
	protected Map childrenToRemove;
	/**
	 * The <code>ASTParser</code> used to manipulate the source code of
	 * <code>IJavaScriptUnit</code>.
	 */
	protected ASTParser parser;
	/**
	 * When executed, this operation will delete the given elements. The elements
	 * to delete cannot be <code>null</code> or empty, and must be contained within a
	 * compilation unit.
	 */
	public DeleteElementsOperation(IJavaScriptElement[] elementsToDelete, boolean force) {
		super(elementsToDelete, force);
		initASTParser();
	}

	private void deleteElement(IJavaScriptElement elementToRemove, IJavaScriptUnit cu) throws JavaScriptModelException {
		// ensure cu is consistent (noop if already consistent)
		cu.makeConsistent(this.progressMonitor);
		this.parser.setSource(cu);
		JavaScriptUnit astCU = (JavaScriptUnit) this.parser.createAST(this.progressMonitor);
		ASTNode node = ((JavaElement) elementToRemove).findNode(astCU);
		if (node == null)
			Assert.isTrue(false, "Failed to locate " + elementToRemove.getElementName() + " in " + cu.getElementName()); //$NON-NLS-1$//$NON-NLS-2$
		IDocument document = getDocument(cu);
		AST ast = astCU.getAST();
		ASTRewrite rewriter = ASTRewrite.create(ast);
		rewriter.remove(node, null);
 		TextEdit edits = rewriter.rewriteAST(document, null);
 		try {
	 		edits.apply(document);
 		} catch (BadLocationException e) {
 			throw new JavaScriptModelException(e, IJavaScriptModelStatusConstants.INVALID_CONTENTS);
 		}
	}

	private void initASTParser() {
		this.parser = ASTParser.newParser(AST.JLS3);
	}

	/**
	 * @see MultiOperation
	 */
	protected String getMainTaskName() {
		return Messages.operation_deleteElementProgress;
	}
	protected ISchedulingRule getSchedulingRule() {
		if (this.elementsToProcess != null && this.elementsToProcess.length == 1) {
			IResource resource = this.elementsToProcess[0].getResource();
			if (resource != null)
				return ResourcesPlugin.getWorkspace().getRuleFactory().modifyRule(resource);
		}
		return super.getSchedulingRule();
	}
	/**
	 * Groups the elements to be processed by their compilation unit.
	 * If parent/child combinations are present, children are
	 * discarded (only the parents are processed). Removes any
	 * duplicates specified in elements to be processed.
	 */
	protected void groupElements() throws JavaScriptModelException {
		childrenToRemove = new HashMap(1);
		int uniqueCUs = 0;
		for (int i = 0, length = elementsToProcess.length; i < length; i++) {
			IJavaScriptElement e = elementsToProcess[i];
			IJavaScriptUnit cu = getCompilationUnitFor(e);
			if (cu == null) {
				throw new JavaScriptModelException(new JavaModelStatus(IJavaScriptModelStatusConstants.READ_ONLY, e));
			} else {
				IRegion region = (IRegion) childrenToRemove.get(cu);
				if (region == null) {
					region = new Region();
					childrenToRemove.put(cu, region);
					uniqueCUs += 1;
				}
				region.add(e);
			}
		}
		elementsToProcess = new IJavaScriptElement[uniqueCUs];
		Iterator iter = childrenToRemove.keySet().iterator();
		int i = 0;
		while (iter.hasNext()) {
			elementsToProcess[i++] = (IJavaScriptElement) iter.next();
		}
	}
	/**
	 * Deletes this element from its compilation unit.
	 * @see MultiOperation
	 */
	protected void processElement(IJavaScriptElement element) throws JavaScriptModelException {
		IJavaScriptUnit cu = (IJavaScriptUnit) element;

		// keep track of the import statements - if all are removed, delete
		// the import container (and report it in the delta)
		int numberOfImports = cu.getImports().length;

		JavaElementDelta delta = new JavaElementDelta(cu);
		IJavaScriptElement[] cuElements = ((IRegion) childrenToRemove.get(cu)).getElements();
		for (int i = 0, length = cuElements.length; i < length; i++) {
			IJavaScriptElement e = cuElements[i];
			if (e.exists()) {
				deleteElement(e, cu);
				delta.removed(e);
				if (e.getElementType() == IJavaScriptElement.IMPORT_DECLARATION) {
					numberOfImports--;
					if (numberOfImports == 0) {
						delta.removed(cu.getImportContainer());
					}
				}
			}
		}
		if (delta.getAffectedChildren().length > 0) {
			cu.save(getSubProgressMonitor(1), force);
			if (!cu.isWorkingCopy()) { // if unit is working copy, then save will have already fired the delta
				addDelta(delta);
				setAttribute(HAS_MODIFIED_RESOURCE_ATTR, TRUE);
			}
		}
	}
	/**
	 * @see MultiOperation
	 * This method first group the elements by <code>IJavaScriptUnit</code>,
	 * and then processes the <code>IJavaScriptUnit</code>.
	 */
	protected void processElements() throws JavaScriptModelException {
		groupElements();
		super.processElements();
	}
	/**
	 * @see MultiOperation
	 */
	protected void verify(IJavaScriptElement element) throws JavaScriptModelException {
		IJavaScriptElement[] children = ((IRegion) childrenToRemove.get(element)).getElements();
		for (int i = 0; i < children.length; i++) {
			IJavaScriptElement child = children[i];
			if (child.getCorrespondingResource() != null)
				error(IJavaScriptModelStatusConstants.INVALID_ELEMENT_TYPES, child);

			if (child.isReadOnly())
				error(IJavaScriptModelStatusConstants.READ_ONLY, child);
		}
	}
}
