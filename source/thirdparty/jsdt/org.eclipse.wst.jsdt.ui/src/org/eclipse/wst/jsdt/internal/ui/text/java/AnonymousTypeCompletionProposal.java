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

import java.util.ArrayList;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension4;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.graphics.Image;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.ISourceRange;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.Signature;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTParser;
import org.eclipse.wst.jsdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.IBinding;
import org.eclipse.wst.jsdt.core.dom.IFunctionBinding;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.wst.jsdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.wst.jsdt.core.dom.rewrite.ListRewrite;
import org.eclipse.wst.jsdt.core.formatter.CodeFormatter;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.StubUtility2;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.corext.dom.NodeFinder;
import org.eclipse.wst.jsdt.internal.corext.template.java.SignatureUtil;
import org.eclipse.wst.jsdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.corext.util.Strings;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.dialogs.OverrideMethodDialog;
import org.eclipse.wst.jsdt.internal.ui.preferences.JavaPreferencesSettings;

public class AnonymousTypeCompletionProposal extends JavaTypeCompletionProposal implements ICompletionProposalExtension4 {

	private String fDeclarationSignature;
	private IType fSuperType;

	public AnonymousTypeCompletionProposal(IJavaScriptProject jproject, IJavaScriptUnit cu, int start, int length, String constructorCompletion, String displayName, String declarationSignature, int relevance) {
		super(constructorCompletion, cu, start, length, null, displayName, relevance);
		Assert.isNotNull(declarationSignature);
		Assert.isNotNull(jproject);
		Assert.isNotNull(cu);

		fDeclarationSignature= declarationSignature;
		fSuperType= getDeclaringType(jproject, SignatureUtil.stripSignatureToFQN(String.valueOf(declarationSignature)));

		setImage(getImageForType(fSuperType));
		setCursorPosition(constructorCompletion.indexOf('(') + 1);
	}

	private int createDummy(String name, StringBuffer buffer) throws JavaScriptModelException {
		String lineDelim= "\n"; // Using newline is ok since source is used in dummy compilation unit //$NON-NLS-1$
		buffer.append("class "); //$NON-NLS-1$
		buffer.append(name);
		buffer.append(" extends "); //$NON-NLS-1$
		if (fDeclarationSignature != null)
			buffer.append(Signature.toString(fDeclarationSignature));
		else
			buffer.append(fSuperType.getFullyQualifiedParameterizedName());
		int start= buffer.length();
		buffer.append("{"); //$NON-NLS-1$
		buffer.append(lineDelim);
		buffer.append(lineDelim);
		buffer.append("}"); //$NON-NLS-1$
		return start;
	}

	private boolean createStubs(StringBuffer buffer, ImportRewrite importRewrite) throws CoreException {
		if (importRewrite == null)
			return false;
		if (fSuperType == null)
			return true;
		IJavaScriptUnit copy= null;
		try {
			final String name= "Type" + System.currentTimeMillis(); //$NON-NLS-1$
			copy= fCompilationUnit.getPrimary().getWorkingCopy(null);
			final StringBuffer contents= new StringBuffer();
			int start= 0;
			int end= 0;
			ISourceRange range= fSuperType.getSourceRange();
			final boolean sameUnit= range != null && fCompilationUnit.equals(fSuperType.getJavaScriptUnit());
			final StringBuffer dummy= new StringBuffer();
			final int length= createDummy(name, dummy);
			contents.append(fCompilationUnit.getBuffer().getContents());
			if (sameUnit) {
				final int size= range.getOffset() + range.getLength();
				start= size + length;
				end= contents.length() - size;
				contents.insert(size, dummy.toString());
			} else {
				range= fCompilationUnit.getTypes()[0].getSourceRange();
				start= range.getOffset() + length;
				end= contents.length() - range.getOffset();
				contents.insert(range.getOffset(), dummy.toString());
			}
			copy.getBuffer().setContents(contents.toString());
			JavaModelUtil.reconcile(copy);
			final ASTParser parser= ASTParser.newParser(AST.JLS3);
			parser.setResolveBindings(true);
			parser.setSource(copy);
			final JavaScriptUnit unit= (JavaScriptUnit) parser.createAST(new NullProgressMonitor());
			IType type= null;
			IType[] types= copy.getAllTypes();
			for (int index= 0; index < types.length; index++) {
				IType result= types[index];
				if (result.getElementName().equals(name)) {
					type= result;
					break;
				}
			}
			if (type != null && type.exists()) {
				ITypeBinding binding= null;
				final AbstractTypeDeclaration declaration= (AbstractTypeDeclaration) ASTNodes.getParent(NodeFinder.perform(unit, type.getNameRange()), AbstractTypeDeclaration.class);
				if (declaration != null) {
					binding= declaration.resolveBinding();
					if (binding != null) {
						IFunctionBinding[] bindings= StubUtility2.getOverridableMethods(unit.getAST(), binding, true);
						CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings(fSuperType.getJavaScriptProject());
						String[] keys= null;
						
						OverrideMethodDialog dialog= new OverrideMethodDialog(JavaScriptPlugin.getActiveWorkbenchShell(), null, type, true);
						dialog.setGenerateComment(false);
						dialog.setElementPositionEnabled(false);
						if (dialog.open() == Window.OK) {
							Object[] selection= dialog.getResult();
							if (selection != null) {
								ArrayList result= new ArrayList(selection.length);
								for (int index= 0; index < selection.length; index++) {
									if (selection[index] instanceof IFunctionBinding)
										result.add(((IBinding) selection[index]).getKey());
								}
								keys= (String[]) result.toArray(new String[result.size()]);
								settings.createComments= dialog.getGenerateComment();
							}
						}
						
						if (keys == null) {
							setReplacementString(""); //$NON-NLS-1$
							setReplacementLength(0);
							return false;
						}
						ASTRewrite rewrite= ASTRewrite.create(unit.getAST());
						ListRewrite rewriter= rewrite.getListRewrite(declaration, declaration.getBodyDeclarationsProperty());
						String key= null;
						FunctionDeclaration stub= null;
						for (int index= 0; index < keys.length; index++) {
							key= keys[index];
							for (int offset= 0; offset < bindings.length; offset++) {
								if (key.equals(bindings[offset].getKey())) {
									stub= StubUtility2.createImplementationStub(copy, rewrite, importRewrite, bindings[offset], binding.getName(), false, settings);
									if (stub != null)
										rewriter.insertFirst(stub, null);
									break;
								}
							}
						}
						IDocument document= new Document(copy.getBuffer().getContents());
						try {
							rewrite.rewriteAST(document, fCompilationUnit.getJavaScriptProject().getOptions(true)).apply(document, TextEdit.UPDATE_REGIONS);
							buffer.append(document.get(start, document.getLength() - start - end));
						} catch (MalformedTreeException exception) {
							JavaScriptPlugin.log(exception);
						} catch (BadLocationException exception) {
							JavaScriptPlugin.log(exception);
						}
					}
				}
			}
			return true;
		} finally {
			if (copy != null)
				copy.discardWorkingCopy();
		}
	}

	private IType getDeclaringType(IJavaScriptProject project, String typeName) {
		try {
			return project.findType(typeName, (IProgressMonitor) null);
		} catch (JavaScriptModelException e) {
			JavaScriptPlugin.log(e);
		}
		return null;
	}

	private Image getImageForType(IType type) {
		String imageName= JavaPluginImages.IMG_OBJS_CLASS; // default
		return JavaPluginImages.get(imageName);
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension4#isAutoInsertable()
	 */
	public boolean isAutoInsertable() {
		return false;
	}

	protected boolean updateReplacementString(IDocument document, char trigger, int offset, ImportRewrite impRewrite) throws CoreException, BadLocationException {
		String replacementString= getReplacementString();

		// construct replacement text: an expression to be formatted
		StringBuffer buf= new StringBuffer("new A("); //$NON-NLS-1$
		buf.append(replacementString);

		if (!replacementString.endsWith(")")) { //$NON-NLS-1$
			buf.append(')');
		}

		if (!createStubs(buf, impRewrite)) {
			return false;
		}
		if (document.getChar(offset) != ')')
			buf.append(';');

		// use the code formatter
		String lineDelim= TextUtilities.getDefaultLineDelimiter(document);
		final IJavaScriptProject project= fCompilationUnit.getJavaScriptProject();
		IRegion region= document.getLineInformationOfOffset(getReplacementOffset());
		int indent= Strings.computeIndentUnits(document.get(region.getOffset(), region.getLength()), project);

		String replacement= CodeFormatterUtil.format(CodeFormatter.K_EXPRESSION, buf.toString(), 0, null, lineDelim, project);
		replacement= Strings.changeIndent(replacement, 0, project, CodeFormatterUtil.createIndentString(indent, project), lineDelim);
		setReplacementString(replacement.substring(replacement.indexOf('(') + 1));

		int pos= offset;
		while (pos < document.getLength() && Character.isWhitespace(document.getChar(pos))) {
			pos++;
		}

		if (pos < document.getLength() && document.getChar(pos) == ')') {
			setReplacementLength(pos - offset + 1);
		}
		return true;
	}
}
