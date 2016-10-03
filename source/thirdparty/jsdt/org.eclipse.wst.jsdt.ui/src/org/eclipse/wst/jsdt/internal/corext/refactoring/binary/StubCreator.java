/*******************************************************************************
 * Copyright (c) 2006, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.corext.refactoring.binary;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.wst.jsdt.core.Flags;
import org.eclipse.wst.jsdt.core.IField;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.Signature;
import org.eclipse.wst.jsdt.internal.corext.refactoring.Checks;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;

public class StubCreator {

	/** The internal string buffer */
	protected StringBuffer fBuffer;

	/** Should stubs for private member be generated as well? */
	protected final boolean fStubInvisible;

	public StubCreator(final boolean stubInvisible) {
		fStubInvisible= stubInvisible;
	}

	protected void appendEnumConstants(final IType type) throws JavaScriptModelException {
		final IField[] fields= type.getFields();
		final List list= new ArrayList(fields.length);
		
		for (int index= 0; index < list.size(); index++) {
			if (index > 0)
				fBuffer.append(","); //$NON-NLS-1$
			fBuffer.append(((IField) list.get(index)).getElementName());
		}
		fBuffer.append(";"); //$NON-NLS-1$
	}

	protected void appendExpression(final String signature) {
		fBuffer.append("("); //$NON-NLS-1$
		fBuffer.append(Signature.toString(signature));
		fBuffer.append(")"); //$NON-NLS-1$
		fBuffer.append("null"); //$NON-NLS-1$
	}

	protected void appendFieldDeclaration(final IField field) throws JavaScriptModelException {
		appendFlags(field);
		fBuffer.append(" "); //$NON-NLS-1$
		final String signature= field.getTypeSignature();
		fBuffer.append(Signature.toString(signature));
		fBuffer.append(" "); //$NON-NLS-1$
		fBuffer.append(field.getElementName());
		fBuffer.append(";"); //$NON-NLS-1$
	}

	protected void appendFlags(final IMember member) throws JavaScriptModelException {
		int flags= member.getFlags();
		final int kind= member.getElementType();
		if (kind == IJavaScriptElement.TYPE) {
			flags&= ~Flags.AccSuper;
			final IType type= (IType) member;
			if (!type.isMember())
				flags&= ~Flags.AccPrivate;
		}
		if (kind == IJavaScriptElement.METHOD) {
			flags&= ~Flags.AccVarargs;
		}
		if (flags != 0)
			fBuffer.append(Flags.toString(flags));
	}

	protected void appendMembers(final IType type, final IProgressMonitor monitor) throws JavaScriptModelException {
		try {
			monitor.beginTask(RefactoringCoreMessages.StubCreationOperation_creating_type_stubs, 1);
			final IJavaScriptElement[] children= type.getChildren();
			for (int index= 0; index < children.length; index++) {
				final IMember child= (IMember) children[index];
				final int flags= child.getFlags();
				final boolean isPrivate= Flags.isPrivate(flags);
				final boolean isDefault= !Flags.isPublic(flags) && !isPrivate;
				final boolean stub= fStubInvisible || (!isPrivate && !isDefault);
				if (child instanceof IType) {
					if (stub)
						appendTypeDeclaration((IType) child, new SubProgressMonitor(monitor, 1));
				} else if (child instanceof IField) {
					if (stub)
						appendFieldDeclaration((IField) child);
				} else if (child instanceof IFunction) {
					final IFunction method= (IFunction) child;
					final String name= method.getElementName();
					
					boolean skip= !stub || name.equals("<clinit>"); //$NON-NLS-1$
					if (method.isConstructor())
						skip= false;
					if (!skip)
						appendMethodDeclaration(method);
				}
				fBuffer.append("\n"); //$NON-NLS-1$
			}
		} finally {
			monitor.done();
		}
	}

	protected void appendMethodBody(final IFunction method) throws JavaScriptModelException {
		if (method.isConstructor()) {
			final IType declaringType= method.getDeclaringType();
			String superSignature= declaringType.getSuperclassTypeSignature();
			if (superSignature != null) {
				final IType superclass= declaringType.getJavaScriptProject().findType(Signature.getSignatureQualifier(superSignature), Signature.getSignatureSimpleName(superSignature));
				if (superclass != null) {
					final IFunction[] superMethods= superclass.getFunctions();
					IFunction superConstructor= null;
					final int length= superMethods.length;
					for (int index= 0; index < length; index++) {
						final IFunction superMethod= superMethods[index];
						if (superMethod.isConstructor() && !Flags.isPrivate(superMethod.getFlags())) {
							superConstructor= superMethod;
							break;
						}
					}
					if (superConstructor != null) {
						final String[] superParameters= superConstructor.getParameterTypes();
						final int paramLength= superParameters.length;
						if (paramLength != 0) {
							fBuffer.append("super("); //$NON-NLS-1$
							for (int index= 0; index < paramLength; index++) {
								if (index > 0)
									fBuffer.append(","); //$NON-NLS-1$
								appendExpression(superParameters[index]);
							}
							fBuffer.append(");"); //$NON-NLS-1$
						}
					}
				}
			}
		} else {
			String returnType= method.getReturnType();
			if (!Signature.SIG_VOID.equals(returnType)) {
				fBuffer.append("return "); //$NON-NLS-1$
				appendExpression(returnType);
				fBuffer.append(";"); //$NON-NLS-1$
			}
		}
	}

	protected void appendMethodDeclaration(final IFunction method) throws JavaScriptModelException {
		appendFlags(method);
		fBuffer.append(" "); //$NON-NLS-1$
		final String returnType= method.getReturnType();
		if (!method.isConstructor()) {
			fBuffer.append(Signature.toString(returnType));
			fBuffer.append(" "); //$NON-NLS-1$
		}
		fBuffer.append(method.getElementName());
		fBuffer.append("("); //$NON-NLS-1$
		final String[] parameterTypes= method.getParameterTypes();
		final int flags= method.getFlags();
		final boolean varargs= Flags.isVarargs(flags);
		final int parameterLength= parameterTypes.length;
		for (int index= 0; index < parameterLength; index++) {
			if (index > 0)
				fBuffer.append(","); //$NON-NLS-1$
			fBuffer.append(Signature.toString(parameterTypes[index]));
			if (varargs && index == parameterLength - 1) {
				final int length= fBuffer.length();
				if (length >= 2 && fBuffer.indexOf("[]", length - 2) >= 0) //$NON-NLS-1$
					fBuffer.setLength(length - 2);
				fBuffer.append("..."); //$NON-NLS-1$
			}
			fBuffer.append(" "); //$NON-NLS-1$
			appendMethodParameterName(method, index);
		}
		fBuffer.append(")"); //$NON-NLS-1$
		if (Flags.isAbstract(flags))
			fBuffer.append(";"); //$NON-NLS-1$
		else {
			fBuffer.append("{\n"); //$NON-NLS-1$
			appendMethodBody(method);
			fBuffer.append("}"); //$NON-NLS-1$
		}
	}

	protected void appendMethodParameterName(IFunction method, int index) {
		fBuffer.append("a"); //$NON-NLS-1$
		fBuffer.append(index);
	}

	protected void appendTopLevelType(final IType type, IProgressMonitor subProgressMonitor) throws JavaScriptModelException {
		String packageName= type.getPackageFragment().getElementName();
		if (packageName.length() > 0) {
			fBuffer.append("package "); //$NON-NLS-1$
			fBuffer.append(packageName);
			fBuffer.append(";\n"); //$NON-NLS-1$
		}
		appendTypeDeclaration(type, subProgressMonitor);
	}

	protected void appendTypeDeclaration(final IType type, final IProgressMonitor monitor) throws JavaScriptModelException {
		try {
			monitor.beginTask(RefactoringCoreMessages.StubCreationOperation_creating_type_stubs, 1);
			if (type.isClass()) {
				appendFlags(type);
				fBuffer.append(" class "); //$NON-NLS-1$
				fBuffer.append(type.getElementName());
				final String signature= type.getSuperclassTypeSignature();
				if (signature != null) {
					fBuffer.append(" extends "); //$NON-NLS-1$
					fBuffer.append(Signature.toString(signature));
				}
				fBuffer.append("{\n"); //$NON-NLS-1$
				appendMembers(type, new SubProgressMonitor(monitor, 1));
				fBuffer.append("}"); //$NON-NLS-1$
			}
		} finally {
			monitor.done();
		}
	}

	/**
	 * @param topLevelType
	 * @param monitor
	 *            progress monitor, can be <code>null</code>
	 * @return source stub
	 * @throws JavaScriptModelException
	 */
	public String createStub(IType topLevelType, IProgressMonitor monitor) throws JavaScriptModelException {
		Assert.isTrue(Checks.isTopLevel(topLevelType));
		if (monitor == null)
			monitor= new NullProgressMonitor();

		fBuffer= new StringBuffer(2046);
		appendTopLevelType(topLevelType, monitor);
		String result= fBuffer.toString();
		fBuffer= null;
		return result;
	}

}
