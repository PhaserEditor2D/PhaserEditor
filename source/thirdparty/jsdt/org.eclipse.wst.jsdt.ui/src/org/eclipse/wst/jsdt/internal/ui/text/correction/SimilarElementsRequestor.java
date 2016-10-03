/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.text.correction;

import java.util.HashSet;

import org.eclipse.wst.jsdt.core.CompletionProposal;
import org.eclipse.wst.jsdt.core.CompletionRequestor;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.Signature;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.JSdoc;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.Name;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.corext.util.TypeFilter;

public class SimilarElementsRequestor extends CompletionRequestor {

	public static final int CLASSES= 1 << 1;
	public static final int INTERFACES= 1 << 2;
	public static final int ANNOTATIONS= 1 << 3;
	public static final int ENUMS= 1 << 4;
	public static final int VARIABLES= 1 << 5;
	public static final int PRIMITIVETYPES= 1 << 6;
	public static final int VOIDTYPE= 1 << 7;
	public static final int REF_TYPES= CLASSES | INTERFACES | ENUMS | ANNOTATIONS;
	public static final int REF_TYPES_AND_VAR= REF_TYPES | VARIABLES;
	public static final int ALL_TYPES= PRIMITIVETYPES | REF_TYPES_AND_VAR;

	private static final String[] PRIM_TYPES= { "boolean", "byte", "char", "short", "int", "long", "float", "double" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$

	private int fKind;
	private String fName;

	private HashSet fResult;

	public static SimilarElement[] findSimilarElement(IJavaScriptUnit cu, Name name, int kind) throws JavaScriptModelException {
		int pos= name.getStartPosition();
		int nArguments= -1;

		String identifier= ASTNodes.getSimpleNameIdentifier(name);
		String returnType= null;
		IJavaScriptUnit preparedCU= null;

		try {
//			if (name.isQualifiedName()) {
//				pos= ((QualifiedName) name).getName().getStartPosition();
//			} else {
				pos= name.getStartPosition() + 1; // first letter must be included, other
		//	}
			JSdoc javadoc=  (JSdoc) ASTNodes.getParent(name, ASTNode.JSDOC);
			if (javadoc != null) {
				preparedCU= createPreparedCU(cu, javadoc, name.getStartPosition());
				cu= preparedCU;
			}

			SimilarElementsRequestor requestor= new SimilarElementsRequestor(identifier, kind, nArguments, returnType);
			/* ORIGINAL -------- BC */
			requestor.setIgnored(CompletionProposal.ANONYMOUS_CLASS_DECLARATION, true);
			requestor.setIgnored(CompletionProposal.KEYWORD, true);
			requestor.setIgnored(CompletionProposal.LABEL_REF, true);
			requestor.setIgnored(CompletionProposal.METHOD_DECLARATION, true);
			requestor.setIgnored(CompletionProposal.PACKAGE_REF, true);
			requestor.setIgnored(CompletionProposal.VARIABLE_DECLARATION, true);
			requestor.setIgnored(CompletionProposal.METHOD_REF, true);
			requestor.setIgnored(CompletionProposal.FIELD_REF, true);
			requestor.setIgnored(CompletionProposal.LOCAL_VARIABLE_REF, true);
			requestor.setIgnored(CompletionProposal.VARIABLE_DECLARATION, true);
			requestor.setIgnored(CompletionProposal.VARIABLE_DECLARATION, true);
			requestor.setIgnored(CompletionProposal.POTENTIAL_METHOD_DECLARATION, true);
			requestor.setIgnored(CompletionProposal.METHOD_NAME_REFERENCE, true);
			
//			
//
//			requestor.setIgnored(CompletionProposal.ANNOTATION_ATTRIBUTE_REF, false);
//			requestor.setIgnored(CompletionProposal.ANONYMOUS_CLASS_DECLARATION, false);
//			requestor.setIgnored(CompletionProposal.FIELD_REF, false);
//			requestor.setIgnored(CompletionProposal.KEYWORD, false);
//			requestor.setIgnored(CompletionProposal.LABEL_REF, false);
//			requestor.setIgnored(CompletionProposal.LOCAL_VARIABLE_REF, false);
//			requestor.setIgnored(CompletionProposal.METHOD_DECLARATION, false);
//			requestor.setIgnored(CompletionProposal.METHOD_NAME_REFERENCE, false);
//			requestor.setIgnored(CompletionProposal.METHOD_REF, false);
//			requestor.setIgnored(CompletionProposal.PACKAGE_REF, false);
//			requestor.setIgnored(CompletionProposal.POTENTIAL_METHOD_DECLARATION, false);
//			requestor.setIgnored(CompletionProposal.VARIABLE_DECLARATION, false);
//			
//			requestor.setIgnored(CompletionProposal.JSDOC_BLOCK_TAG, true);
//			requestor.setIgnored(CompletionProposal.JSDOC_FIELD_REF, true);
//			requestor.setIgnored(CompletionProposal.JSDOC_INLINE_TAG, true);
//			requestor.setIgnored(CompletionProposal.JSDOC_METHOD_REF, true);
//			requestor.setIgnored(CompletionProposal.JSDOC_PARAM_REF, true);
//			requestor.setIgnored(CompletionProposal.JSDOC_TYPE_REF, true);
//			requestor.setIgnored(CompletionProposal.JSDOC_VALUE_REF, true);
//			
//			requestor.setIgnored(CompletionProposal.TYPE_REF, true);
//			
			
			
			
			
			return requestor.process(cu, pos);
		} finally {
			if (preparedCU != null) {
				preparedCU.discardWorkingCopy();
			}
		}
	}

	private static IJavaScriptUnit createPreparedCU(IJavaScriptUnit cu, JSdoc comment, int wordStart) throws JavaScriptModelException {
		int startpos= comment.getStartPosition();
		boolean isTopLevel= comment.getParent().getParent() instanceof JavaScriptUnit;
		char[] content= (char[]) cu.getBuffer().getCharacters().clone();
		if (isTopLevel && (wordStart + 6 < content.length)) {
			content[startpos++]= 'i'; content[startpos++]= 'm'; content[startpos++]= 'p';
			content[startpos++]= 'o'; content[startpos++]= 'r'; content[startpos++]= 't';
		}
		if (wordStart < content.length) {
			for (int i= startpos; i < wordStart; i++) {
				content[i]= ' ';
			}
		}

		/*
		 * Explicitly create a new non-shared working copy.
		 */
		IJavaScriptUnit newCU= cu.getWorkingCopy(null);
		newCU.getBuffer().setContents(content);
		return newCU;
	}


	/**
	 * Constructor for SimilarElementsRequestor.
	 */
	private SimilarElementsRequestor(String name, int kind, int nArguments, String preferredType) {
		super();
		fName= name;
		fKind= kind;

		fResult= new HashSet();
	}

	private void addResult(SimilarElement elem) {
		fResult.add(elem);
	}

	private SimilarElement[] process(IJavaScriptUnit cu, int pos) throws JavaScriptModelException {
		try {
			cu.codeComplete(pos, this);
			processKeywords();
			return (SimilarElement[]) fResult.toArray(new SimilarElement[fResult.size()]);
		} finally {
			fResult.clear();
		}
	}

	private boolean isKind(int kind) {
		return (fKind & kind) != 0;
	}

	/**
	 * Method addPrimitiveTypes.
	 */
	private void processKeywords() {
		if (isKind(PRIMITIVETYPES)) {
			for (int i= 0; i < PRIM_TYPES.length; i++) {
				if (NameMatcher.isSimilarName(fName, PRIM_TYPES[i])) {
					addResult(new SimilarElement(PRIMITIVETYPES, PRIM_TYPES[i], 50));
				}
			}
		}
		if (isKind(VOIDTYPE)) {
			String voidType= "void"; //$NON-NLS-1$
			if (NameMatcher.isSimilarName(fName, voidType)) {
				addResult(new SimilarElement(PRIMITIVETYPES, voidType, 50));
			}
		}
	}

	private static final int getKind(int flags, char[] typeNameSig) {
		return CLASSES;
	}


	private void addType(char[] typeNameSig, int flags, int relevance) {
		int kind= getKind(flags, typeNameSig);
		if (!isKind(kind)) {
			return;
		}
		String fullName= new String(Signature.toCharArray(typeNameSig));
		if (TypeFilter.isFiltered(fullName)) {
			return;
		}
		if (NameMatcher.isSimilarName(fName, Signature.getSimpleName(fullName))) {
			addResult(new SimilarElement(kind, fullName, relevance));
		}
	}


	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.CompletionRequestor#accept(org.eclipse.wst.jsdt.core.CompletionProposal)
	 */
	public void accept(CompletionProposal proposal) {
		if (proposal.getKind() == CompletionProposal.TYPE_REF) {
			addType(proposal.getSignature(), proposal.getFlags(), proposal.getRelevance());
		}
	}
}
