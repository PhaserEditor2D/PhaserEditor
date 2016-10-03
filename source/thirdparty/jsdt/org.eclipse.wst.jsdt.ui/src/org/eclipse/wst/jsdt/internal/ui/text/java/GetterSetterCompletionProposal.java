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
package org.eclipse.wst.jsdt.internal.ui.text.java;

import java.util.Collection;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension4;
import org.eclipse.wst.jsdt.core.Flags;
import org.eclipse.wst.jsdt.core.IField;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.Signature;
import org.eclipse.wst.jsdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.wst.jsdt.core.formatter.CodeFormatter;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.GetterSetterUtil;
import org.eclipse.wst.jsdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.corext.util.Strings;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;
import org.eclipse.wst.jsdt.internal.ui.preferences.JavaPreferencesSettings;

public class GetterSetterCompletionProposal extends JavaTypeCompletionProposal implements ICompletionProposalExtension4 {

	public static void evaluateProposals(IType type, String prefix, int offset, int length, int relevance, Set suggestedMethods, Collection result) throws CoreException {
		if (prefix.length() == 0) {
			relevance--;
		}

		IField[] fields= type.getFields();
		IFunction[] methods= type.getFunctions();
		for (int i= 0; i < fields.length; i++) {
			IField curr= fields[i];
			String getterName= GetterSetterUtil.getGetterName(curr, null);
			if (getterName.startsWith(prefix) && !hasMethod(methods, getterName) && suggestedMethods.add(getterName)) {
				result.add(new GetterSetterCompletionProposal(curr, offset, length, true, relevance));
			}

			String setterName= GetterSetterUtil.getSetterName(curr, null);
			if (setterName.startsWith(prefix) && !hasMethod(methods, setterName) && suggestedMethods.add(setterName)) {
				result.add(new GetterSetterCompletionProposal(curr, offset, length, false, relevance));
			}
		}
	}

	private static boolean hasMethod(IFunction[] methods, String name) {
		for (int i= 0; i < methods.length; i++) {
			if (methods[i].getElementName().equals(name)) {
				return true;
			}
		}
		return false;
	}

	private final IField fField;
	private final boolean fIsGetter;

	public GetterSetterCompletionProposal(IField field, int start, int length, boolean isGetter, int relevance) throws JavaScriptModelException {
		super("", field.getJavaScriptUnit(), start, length, JavaPluginImages.get(JavaPluginImages.IMG_MISC_PUBLIC), getDisplayName(field, isGetter), relevance); //$NON-NLS-1$
		Assert.isNotNull(field);

		fField= field;
		fIsGetter= isGetter;
		setProposalInfo(new ProposalInfo(field));
	}

	private static String getDisplayName(IField field, boolean isGetter) throws JavaScriptModelException {
		StringBuffer buf= new StringBuffer();
		if (isGetter) {
			buf.append(GetterSetterUtil.getGetterName(field, null));
			buf.append("()  "); //$NON-NLS-1$
			buf.append(Signature.toString(field.getTypeSignature()));
			buf.append(" - "); //$NON-NLS-1$
			buf.append(Messages.format(JavaTextMessages.GetterSetterCompletionProposal_getter_label, field.getElementName()));
		} else {
			buf.append(GetterSetterUtil.getSetterName(field, null));
			buf.append('(').append(Signature.toString(field.getTypeSignature())).append(')');
			buf.append("  "); //$NON-NLS-1$
			buf.append(Signature.toString(Signature.SIG_VOID));
			buf.append(" - "); //$NON-NLS-1$
			buf.append(Messages.format(JavaTextMessages.GetterSetterCompletionProposal_setter_label, field.getElementName()));
		}
		return buf.toString();
	}

	/* (non-Javadoc)
	 * @see JavaTypeCompletionProposal#updateReplacementString(IDocument, char, int, ImportRewrite)
	 */
	protected boolean updateReplacementString(IDocument document, char trigger, int offset, ImportRewrite impRewrite) throws CoreException, BadLocationException {

		CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings(fField.getJavaScriptProject());
		boolean addComments= settings.createComments;
		int flags= Flags.AccPublic | (fField.getFlags() & Flags.AccStatic);

		String stub;
		if (fIsGetter) {
			String getterName= GetterSetterUtil.getGetterName(fField, null);
			stub= GetterSetterUtil.getGetterStub(fField, getterName, addComments, flags);
		} else {
			String setterName= GetterSetterUtil.getSetterName(fField, null);
			stub= GetterSetterUtil.getSetterStub(fField, setterName, addComments, flags);
		}

		// use the code formatter
		String lineDelim= TextUtilities.getDefaultLineDelimiter(document);

		IRegion region= document.getLineInformationOfOffset(getReplacementOffset());
		int lineStart= region.getOffset();
		int indent= Strings.computeIndentUnits(document.get(lineStart, getReplacementOffset() - lineStart), settings.tabWidth, settings.indentWidth);

		String replacement= CodeFormatterUtil.format(CodeFormatter.K_CLASS_BODY_DECLARATIONS, stub, indent, null, lineDelim, fField.getJavaScriptProject());

		if (replacement.endsWith(lineDelim)) {
			replacement= replacement.substring(0, replacement.length() - lineDelim.length());
		}

		setReplacementString(Strings.trimLeadingTabsAndSpaces(replacement));
		return true;
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension4#isAutoInsertable()
	 */
	public boolean isAutoInsertable() {
		return false;
	}
}

