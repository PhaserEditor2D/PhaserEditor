/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.core.search.indexing;

import org.eclipse.wst.jsdt.core.search.SearchDocument;
import org.eclipse.wst.jsdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.wst.jsdt.internal.core.Logger;
import org.eclipse.wst.jsdt.internal.core.search.matching.ConstructorPattern;
import org.eclipse.wst.jsdt.internal.core.search.matching.FieldPattern;
import org.eclipse.wst.jsdt.internal.core.search.matching.MethodPattern;
import org.eclipse.wst.jsdt.internal.core.search.matching.SuperTypeReferencePattern;
import org.eclipse.wst.jsdt.internal.core.search.matching.TypeDeclarationPattern;
import org.eclipse.wst.jsdt.internal.core.search.matching.TypeSynonymsPattern;
import org.eclipse.wst.jsdt.internal.core.util.QualificationHelpers;

public abstract class AbstractIndexer implements IIndexConstants {

	SearchDocument document;

	public AbstractIndexer(SearchDocument document) {
		this.document = document;
	}
	
	public void addClassDeclaration(
			int modifiers,
			char[] packageName,
			char[] name,
			char[][] enclosingTypeNames,
			char[] superclass,
			boolean secondary,
			char[][] synonyms) {
		addTypeDeclaration(modifiers, packageName, name, superclass);

		if (superclass != null) {
			addTypeReference(superclass);
		}
		
		char[] fullyQualifiedName = QualificationHelpers.createFullyQualifiedName(packageName, name);
		
		addIndexEntry(
			SUPER_REF,
			SuperTypeReferencePattern.createIndexKey(fullyQualifiedName, superclass));
	
		// add synonyms to index
		if(synonyms != null && synonyms.length > 0) {
			this.addIndexEntry(TYPE_SYNONYMS, TypeSynonymsPattern.createIndexKey(fullyQualifiedName, synonyms));
		}
		
	}
	public void addConstructorDeclaration(char[] typeName, char[][] parameterTypes, char[][] parameterNames, int modifiers) {
		addIndexEntry(CONSTRUCTOR_DECL, ConstructorPattern.createIndexKey(typeName, parameterTypes, parameterNames, modifiers));

		if (parameterTypes != null) {
			for (int i = 0; i < parameterTypes.length; i++) {
				if(parameterTypes[i] != null) {
					addTypeReference(parameterTypes[i]);
				}
			}
		}
	}
	public void addConstructorReference(char[] typeName, int argCount) {
		addTypeReference(typeName);
		addIndexEntry(CONSTRUCTOR_REF, ConstructorPattern.createIndexKey(typeName, null, null, ClassFileConstants.AccDefault));
	}
	public void addFieldDeclaration(char[] typeName, char[] fieldName, char[] declaringType, int modifiers, 
			boolean isVar) {
		
		//only index if field has a name
		if(fieldName != null && fieldName.length > 0) {
			char [] key = isVar ? VAR_DECL:FIELD_DECL;
			addIndexEntry(key, FieldPattern.createIndexKey(fieldName, typeName, declaringType != null ? declaringType : IIndexConstants.GLOBAL_SYMBOL, modifiers));
			if (typeName!=null) {
				addTypeReference(typeName);
			}
		} else {
			//this should never happen, so log it
			String errorMsg = "JSDT AbstractIndexer attempted to index a field with no name, this should never happen.";
			if(typeName != null) {
				errorMsg += "\ntypeName: " + new String(typeName);
			}
			if(declaringType != null) {
				errorMsg += "\ndeclaringType: " + new String(declaringType);
			}
			Logger.log(Logger.WARNING, errorMsg);
		}
	}
	public void addFieldReference(char[] fieldName) {
		addNameReference(fieldName);
	}
	protected void addIndexEntry(char[] category, char[] key) {
		this.document.addIndexEntry(category, key);
	}
	public void addMethodDeclaration(char[] methodName, char[][] parameterTypes, char[][] paramaterNames,
				char[] returnType, char[] declaringType, boolean isFunction, int modifiers) {
		
		//compute key
		char[] key = MethodPattern.createIndexKey(methodName, parameterTypes, paramaterNames, 
				declaringType != null ? declaringType : IIndexConstants.GLOBAL_SYMBOL, returnType, modifiers);
		if(key != null) {
			addIndexEntry(isFunction ? FUNCTION_DECL : METHOD_DECL, key);
		}

		if (parameterTypes != null) {
			for (int i = 0; i < parameterTypes.length; i++)
				addTypeReference(parameterTypes[i]);
		}
		if (returnType != null)
			addTypeReference(returnType);
	}
	public void addMethodReference(char[] methodName) {
		char[] key = MethodPattern.createIndexKey(methodName);
		if(key != null) {
			addIndexEntry(METHOD_REF, key);
		}
	}
	public void addNameReference(char[] name) {
		addIndexEntry(REF, name);
	}
	
	/**
	 * 
	 * <p>Adds a type declaration to the index.</p>
	 * 
	 * @param modifiers of the type
	 * @param qualification qualification of the type
	 * @param simpleTypeName simple name of the type
	 * @param superTypeName fully qualified super type
	 */
	protected void addTypeDeclaration(int modifiers, char[] qualification, char[] simpleTypeName, char[] superTypeName) {
		char[] indexKey = TypeDeclarationPattern.createIndexKey(qualification, simpleTypeName, new char[][] {superTypeName}, modifiers);
		addIndexEntry(TYPE_DECL, indexKey);
	}
	public void addTypeReference(char[] typeName) {
		if (typeName!=null) {
			addNameReference(typeName);
		}
	}
	public abstract void indexDocument();
}
