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
package org.eclipse.wst.jsdt.internal.corext.refactoring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.jsdt.core.IBuffer;
import org.eclipse.wst.jsdt.core.IField;
import org.eclipse.wst.jsdt.core.IImportContainer;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.ISourceReference;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ASTParser;
import org.eclipse.wst.jsdt.core.dom.FieldDeclaration;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment;
import org.eclipse.wst.jsdt.internal.corext.refactoring.reorg.ReorgUtils;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;
import org.eclipse.wst.jsdt.internal.corext.util.Strings;

/**
 * A tuple used to keep source of an element and its type.
 * @see IJavaScriptElement
 * @see org.eclipse.wst.jsdt.core.ISourceReference
 */
public class TypedSource {

	private static class SourceTuple {

		private SourceTuple(IJavaScriptUnit unit) {
			this.unit= unit;
		}
		private IJavaScriptUnit unit;
		private JavaScriptUnit node;
	}

	private final String fSource;
	private final int fType;

	private TypedSource(String source, int type){
		Assert.isNotNull(source);
		Assert.isTrue(canCreateForType(type));
		fSource= source;
		fType= type;				  
	}
	
	public static TypedSource create(String source, int type) {
		if (source == null || ! canCreateForType(type))
			return null;
		return new TypedSource(source, type);
	}

	public String getSource() {
		return fSource;
	}

	public int getType() {
		return fType;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object other) {
		if (! (other instanceof TypedSource))
			return false;
		
		TypedSource ts= (TypedSource)other;
		return ts.getSource().equals(getSource()) && ts.getType() == getType();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return getSource().hashCode() ^ (97 * getType());
	}

	private static boolean canCreateForType(int type){
		return 		type == IJavaScriptElement.FIELD 
				|| 	type == IJavaScriptElement.TYPE
				|| 	type == IJavaScriptElement.IMPORT_CONTAINER
				|| 	type == IJavaScriptElement.IMPORT_DECLARATION
				|| 	type == IJavaScriptElement.INITIALIZER
				|| 	type == IJavaScriptElement.METHOD;
	}
	
	
	public static void sortByType(TypedSource[] typedSources){
		Arrays.sort(typedSources, createTypeComparator());
	}

	public static Comparator createTypeComparator() {
		return new Comparator(){
			public int compare(Object arg0, Object arg1) {
				return ((TypedSource)arg0).getType() - ((TypedSource)arg1).getType();
			}
		};
	}
	public static TypedSource[] createTypedSources(IJavaScriptElement[] javaElements) throws CoreException {
		//Map<IJavaScriptUnit, List<IJavaScriptElement>>
		Map grouped= ReorgUtils.groupByCompilationUnit(Arrays.asList(javaElements));
		List result= new ArrayList(javaElements.length);
		for (Iterator iter= grouped.keySet().iterator(); iter.hasNext();) {
			IJavaScriptUnit cu= (IJavaScriptUnit) iter.next();
			for (Iterator iterator= ((List) grouped.get(cu)).iterator(); iterator.hasNext();) {
				SourceTuple tuple= new SourceTuple(cu);
				TypedSource[] ts= createTypedSources((IJavaScriptElement) iterator.next(), tuple);
				if (ts != null)
					result.addAll(Arrays.asList(ts));				
			}
		}
		return (TypedSource[]) result.toArray(new TypedSource[result.size()]);		
	}

	private static TypedSource[] createTypedSources(IJavaScriptElement elem, SourceTuple tuple) throws CoreException {
		if (! ReorgUtils.isInsideCompilationUnit(elem))
			return null;
		if (elem.getElementType() == IJavaScriptElement.IMPORT_CONTAINER) 
			return createTypedSourcesForImportContainer(tuple, (IImportContainer)elem);
		else if (elem.getElementType() == IJavaScriptElement.FIELD) 
			return new TypedSource[] {create(getFieldSource((IField)elem, tuple), elem.getElementType())};
		return new TypedSource[] {create(getSourceOfDeclararationNode(elem, tuple.unit), elem.getElementType())};
	}

	private static TypedSource[] createTypedSourcesForImportContainer(SourceTuple tuple, IImportContainer container) throws JavaScriptModelException, CoreException {
		IJavaScriptElement[] imports= container.getChildren();
		List result= new ArrayList(imports.length);
		for (int i= 0; i < imports.length; i++) {
			result.addAll(Arrays.asList(createTypedSources(imports[i], tuple)));
		}
		return (TypedSource[]) result.toArray(new TypedSource[result.size()]);
	}

	private static String getFieldSource(IField field, SourceTuple tuple) throws CoreException {
		if (tuple.node == null) {
			ASTParser parser= ASTParser.newParser(AST.JLS3);
			parser.setSource(tuple.unit);
			tuple.node= (JavaScriptUnit) parser.createAST(null);
		}
		FieldDeclaration declaration= ASTNodeSearchUtil.getFieldDeclarationNode(field, tuple.node);
		if (declaration.fragments().size() == 1)
			return getSourceOfDeclararationNode(field, tuple.unit);
		VariableDeclarationFragment declarationFragment= ASTNodeSearchUtil.getFieldDeclarationFragmentNode(field, tuple.node);
		IBuffer buffer= tuple.unit.getBuffer();
		StringBuffer buff= new StringBuffer();
		buff.append(buffer.getText(declaration.getStartPosition(), ((ASTNode) declaration.fragments().get(0)).getStartPosition() - declaration.getStartPosition()));
		buff.append(buffer.getText(declarationFragment.getStartPosition(), declarationFragment.getLength()));
		buff.append(";"); //$NON-NLS-1$
		return buff.toString();
	}

	private static String getSourceOfDeclararationNode(IJavaScriptElement elem, IJavaScriptUnit cu) throws JavaScriptModelException, CoreException {
		Assert.isTrue(elem.getElementType() != IJavaScriptElement.IMPORT_CONTAINER);
		if (elem instanceof ISourceReference) {
			ISourceReference reference= (ISourceReference) elem;
			String source= reference.getSource();
			if (source != null)
				return Strings.trimIndentation(source, cu.getJavaScriptProject(), false);
		}
		return ""; //$NON-NLS-1$
	}
}
