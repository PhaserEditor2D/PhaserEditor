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

package org.eclipse.wst.jsdt.internal.ui.text.javadoc;


import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultIndentLineAutoEditStrategy;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditorExtension3;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.ISourceRange;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.corext.util.MethodOverrideTester;
import org.eclipse.wst.jsdt.internal.corext.util.Strings;
import org.eclipse.wst.jsdt.internal.corext.util.SuperTypeHierarchyCache;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.ui.CodeGeneration;
import org.eclipse.wst.jsdt.ui.IWorkingCopyManager;
import org.eclipse.wst.jsdt.ui.PreferenceConstants;


/**
 * Auto indent strategy for Javadoc comments.
 */
public class JavaDocAutoIndentStrategy extends DefaultIndentLineAutoEditStrategy {

	/** The partitioning that this strategy operates on. */
	private final String fPartitioning;

	/**
	 * Creates a new Javadoc auto indent strategy for the given document partitioning.
	 *
	 * @param partitioning the document partitioning
	 */
	public JavaDocAutoIndentStrategy(String partitioning) {
		fPartitioning= partitioning;
	}

	/**
	 * Copies the indentation of the previous line and adds a star.
	 * If the javadoc just started on this line add standard method tags
	 * and close the javadoc.
	 *
	 * @param d the document to work on
	 * @param c the command to deal with
	 */
	private void indentAfterNewLine(IDocument d, DocumentCommand c) {

		int offset= c.offset;
		if (offset == -1 || d.getLength() == 0)
			return;

		try {
			int p= (offset == d.getLength() ? offset - 1 : offset);
			IRegion line= d.getLineInformationOfOffset(p);

			int lineOffset= line.getOffset();
			int firstNonWS= findEndOfWhiteSpace(d, lineOffset, offset);
			Assert.isTrue(firstNonWS >= lineOffset, "indentation must not be negative"); //$NON-NLS-1$

			StringBuffer buf= new StringBuffer(c.text);
			IRegion prefix= findPrefixRange(d, line);
			String indentation= d.get(prefix.getOffset(), prefix.getLength());
			int lengthToAdd= Math.min(offset - prefix.getOffset(), prefix.getLength());

			buf.append(indentation.substring(0, lengthToAdd));

			if (firstNonWS < offset) {
				/* ensure this isn't a double slash comment */
				
				if (d.getChar(firstNonWS) == '/' && d.getChar(firstNonWS+1)!='/') {
					// javadoc started on this line
					buf.append(" * "); //$NON-NLS-1$

					if (isPreferenceTrue(PreferenceConstants.EDITOR_CLOSE_JAVADOCS) && isNewComment(d, offset)) {
						c.shiftsCaret= false;
						c.caretOffset= c.offset + buf.length();
						String lineDelimiter= TextUtilities.getDefaultLineDelimiter(d);

						String endTag= lineDelimiter + indentation + " */"; //$NON-NLS-1$

						if (isPreferenceTrue(PreferenceConstants.EDITOR_ADD_JAVADOC_TAGS)) {
							// we need to close the comment before computing
							// the correct tags in order to get the method
							d.replace(offset, 0, endTag);

							// evaluate method signature
							IJavaScriptUnit unit= getCompilationUnit();

							if (unit != null) {
								try {
									JavaModelUtil.reconcile(unit);
									String string= createJavaDocTags(d, c, indentation, lineDelimiter, unit);
									// only add tags if they are non-empty - the empty line has already been added above.
									if (string != null && !string.trim().equals("*")) //$NON-NLS-1$
										buf.append(string);
								} catch (CoreException e) {
									// ignore
								}
							}
						} else {
							buf.append(endTag);
						}
					}

				}
			}

			// move the caret behind the prefix, even if we do not have to insert it.
			if (lengthToAdd < prefix.getLength())
				c.caretOffset= offset + prefix.getLength() - lengthToAdd;
			c.text= buf.toString();

		} catch (BadLocationException excp) {
			// stop work
		}
	}

	/**
	 * Returns the value of the given boolean-typed preference.
	 *
	 * @param preference the preference to look up
	 * @return the value of the given preference in the Java plug-in's default preference store
	 */
	private boolean isPreferenceTrue(String preference) {
		return JavaScriptPlugin.getDefault().getPreferenceStore().getBoolean(preference);
	}

	/**
	 * Returns the range of the Javadoc prefix on the given line in
	 * <code>document</code>. The prefix greedily matches the following regex
	 * pattern: <code>\w*\*\w*</code>, that is, any number of whitespace
	 * characters, followed by an asterix ('*'), followed by any number of
	 * whitespace characters.
	 *
	 * @param document the document to which <code>line</code> refers
	 * @param line the line from which to extract the prefix range
	 * @return an <code>IRegion</code> describing the range of the prefix on
	 *         the given line
	 * @throws BadLocationException if accessing the document fails
	 */
	private IRegion findPrefixRange(IDocument document, IRegion line) throws BadLocationException {
		int lineOffset= line.getOffset();
		int lineEnd= lineOffset + line.getLength();
		int indentEnd= findEndOfWhiteSpace(document, lineOffset, lineEnd);
		if (indentEnd < lineEnd && document.getChar(indentEnd) == '*') {
			indentEnd++;
			while (indentEnd < lineEnd && document.getChar(indentEnd) == ' ')
				indentEnd++;
		}
		return new Region(lineOffset, indentEnd - lineOffset);
	}

	/**
	 * Creates the Javadoc tags for newly inserted comments.
	 *
	 * @param document the document
	 * @param command the command
	 * @param indentation the base indentation to use
	 * @param lineDelimiter the line delimiter to use
	 * @param unit the compilation unit shown in the editor
	 * @return the tags to add to the document
	 * @throws CoreException if accessing the java model fails
	 * @throws BadLocationException if accessing the document fails
	 */
	private String createJavaDocTags(IDocument document, DocumentCommand command, String indentation, String lineDelimiter, IJavaScriptUnit unit)
		throws CoreException, BadLocationException
	{
		IJavaScriptElement element= unit.getElementAt(command.offset);
		if (element == null)
			return null;

		switch (element.getElementType()) {
		case IJavaScriptElement.TYPE:
			return createTypeTags(document, command, indentation, lineDelimiter, (IType) element);

		case IJavaScriptElement.METHOD:
			return createMethodTags(document, command, indentation, lineDelimiter, (IFunction) element);

		default:
			return null;
		}
	}

	/**
	 * Removes start and end of a comment and corrects indentation and line
	 * delimiters.
	 *
	 * @param comment the computed comment
	 * @param indentation the base indentation
	 * @param project the java project for the formatter settings, or
	 *        <code>null</code> for global preferences
	 * @param lineDelimiter the line delimiter
	 * @return a trimmed version of <code>comment</code>
	 */
	private String prepareTemplateComment(String comment, String indentation, IJavaScriptProject project, String lineDelimiter) {
		//	trim comment start and end if any
		if (comment.endsWith("*/")) //$NON-NLS-1$
			comment= comment.substring(0, comment.length() - 2);
		comment= comment.trim();
		if (comment.startsWith("/*")) { //$NON-NLS-1$
			if (comment.length() > 2 && comment.charAt(2) == '*') {
				comment= comment.substring(3); // remove '/**'
			} else {
				comment= comment.substring(2); // remove '/*'
			}
		}
		// trim leading spaces, but not new lines
		int nonSpace= 0;
		int len= comment.length();
		while (nonSpace < len && Character.getType(comment.charAt(nonSpace)) == Character.SPACE_SEPARATOR)
				nonSpace++;
		comment= comment.substring(nonSpace);
		
		return Strings.changeIndent(comment, 0, project, indentation, lineDelimiter);
	}

	private String createTypeTags(IDocument document, DocumentCommand command, String indentation, String lineDelimiter, IType type)
		throws CoreException, BadLocationException
	{
		String comment= CodeGeneration.getTypeComment(type.getJavaScriptUnit(), type.getTypeQualifiedName('.'), lineDelimiter);
		if (comment != null) {
			boolean javadocComment= comment.startsWith("/**"); //$NON-NLS-1$
			if (!isFirstComment(document, command, type, javadocComment)) 
				return null;
			return prepareTemplateComment(comment.trim(), indentation, type.getJavaScriptProject(), lineDelimiter);
		}
		return null;
	}

	private String createMethodTags(IDocument document, DocumentCommand command, String indentation, String lineDelimiter, IFunction method)
		throws CoreException, BadLocationException
	{
		IRegion partition= TextUtilities.getPartition(document, fPartitioning, command.offset, false);
		IFunction inheritedMethod= getInheritedMethod(method);
		String comment= CodeGeneration.getMethodComment(method, inheritedMethod, lineDelimiter);
		if (comment != null) {
			comment= comment.trim();
			boolean javadocComment= comment.startsWith("/**"); //$NON-NLS-1$
			if (!isFirstComment(document, command, method, javadocComment))
				return null;
			boolean isJavaDoc= partition.getLength() >= 3 && document.get(partition.getOffset(), 3).equals("/**"); //$NON-NLS-1$
			if (javadocComment == isJavaDoc) {
				return prepareTemplateComment(comment, indentation, method.getJavaScriptProject(), lineDelimiter);
			}
		}
		return null;
	}

	/**
	 * Returns <code>true</code> if the comment being inserted at
	 * <code>command.offset</code> is the first comment (the first
	 * javadoc comment if <code>ignoreJavadoc</code> is
	 * <code>true</code>) of the given member.
	 * <p>
	 * see also https://bugs.eclipse.org/bugs/show_bug.cgi?id=55325 (don't add parameters if the member already has a comment)
	 * </p>
	 */
	private boolean isFirstComment(IDocument document, DocumentCommand command, IMember member, boolean ignoreNonJavadoc) throws BadLocationException, JavaScriptModelException {
		IRegion partition= TextUtilities.getPartition(document, fPartitioning, command.offset, false);
		ISourceRange sourceRange= member.getSourceRange();
		if (sourceRange == null || sourceRange.getOffset() != partition.getOffset())
			return false;
		int srcOffset= sourceRange.getOffset();
		int srcLength= sourceRange.getLength();
		int nameRelativeOffset= member.getNameRange().getOffset() - srcOffset;
		int partitionRelativeOffset= partition.getOffset() - srcOffset;
		String token= ignoreNonJavadoc ? "/**" :  "/*"; //$NON-NLS-1$ //$NON-NLS-2$
		return document.get(srcOffset, srcLength).lastIndexOf(token, nameRelativeOffset) == partitionRelativeOffset; 
	}

	/**
	 * Unindents a typed slash ('/') if it forms the end of a comment.
	 *
	 * @param d the document
	 * @param c the command
	 */
	private void indentAfterCommentEnd(IDocument d, DocumentCommand c) {
		if (c.offset < 2 || d.getLength() == 0) {
			return;
		}
		try {
			if ("* ".equals(d.get(c.offset - 2, 2))) { //$NON-NLS-1$
				// modify document command
				c.length++;
				c.offset--;
			}
		} catch (BadLocationException excp) {
			// stop work
		}
	}

	/**
	 * Guesses if the command operates within a newly created javadoc comment or not.
	 * If in doubt, it will assume that the javadoc is new.
	 *
	 * @param document the document
	 * @param commandOffset the command offset
	 * @return <code>true</code> if the comment should be closed, <code>false</code> if not
	 */
	private boolean isNewComment(IDocument document, int commandOffset) {

		try {
			int lineIndex= document.getLineOfOffset(commandOffset) + 1;
			if (lineIndex >= document.getNumberOfLines())
				return true;

			IRegion line= document.getLineInformation(lineIndex);
			ITypedRegion partition= TextUtilities.getPartition(document, fPartitioning, commandOffset, false);
			int partitionEnd= partition.getOffset() + partition.getLength();
			if (line.getOffset() >= partitionEnd)
				return false;

			if (document.getLength() == partitionEnd)
				return true; // partition goes to end of document - probably a new comment

			String comment= document.get(partition.getOffset(), partition.getLength());
			if (comment.indexOf("/*", 2) != -1) //$NON-NLS-1$
				return true; // enclosed another comment -> probably a new comment

			return false;

		} catch (BadLocationException e) {
			return false;
		}
	}

	private boolean isSmartMode() {
		IWorkbenchPage page= JavaScriptPlugin.getActivePage();
		if (page != null)  {
			IEditorPart part= page.getActiveEditor();
			if (part instanceof ITextEditorExtension3) {
				ITextEditorExtension3 extension= (ITextEditorExtension3) part;
				return extension.getInsertMode() == ITextEditorExtension3.SMART_INSERT;
			}
		}
		return false;
	}

	/*
	 * @see IAutoIndentStrategy#customizeDocumentCommand
	 */
	public void customizeDocumentCommand(IDocument document, DocumentCommand command) {

		if (!isSmartMode())
			return;

		if (command.text != null) {
			if (command.length == 0) {
				String[] lineDelimiters= document.getLegalLineDelimiters();
				int index= TextUtilities.endsWith(lineDelimiters, command.text);
				if (index > -1) {
					// ends with line delimiter
					if (lineDelimiters[index].equals(command.text))
						// just the line delimiter
						indentAfterNewLine(document, command);
					return;
				}
			}

			if (command.text.equals("/")) { //$NON-NLS-1$
				indentAfterCommentEnd(document, command);
				return;
			}
		}
	}

	/**
	 * Returns the method inherited from, <code>null</code> if method is newly defined.
	 * @param method the method being written
	 * @return the ancestor method, or <code>null</code> if none
	 * @throws JavaScriptModelException if accessing the java model fails
	 */
	private static IFunction getInheritedMethod(IFunction method) throws JavaScriptModelException {
		IType declaringType= method.getDeclaringType();
		if (declaringType==null)
			return null;
		MethodOverrideTester tester= SuperTypeHierarchyCache.getMethodOverrideTester(declaringType);
		return tester.findOverriddenMethod(method, true);
	}

	/**
	 * Returns the compilation unit of the CompilationUnitEditor invoking the AutoIndentStrategy,
	 * might return <code>null</code> on error.
	 * @return the compilation unit represented by the document
	 */
	private static IJavaScriptUnit getCompilationUnit() {

		IWorkbenchWindow window= PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window == null)
			return null;

		IWorkbenchPage page= window.getActivePage();
		if (page == null)
			return null;

		IEditorPart editor= page.getActiveEditor();
		if (editor == null)
			return null;

		IWorkingCopyManager manager= JavaScriptPlugin.getDefault().getWorkingCopyManager();
		IJavaScriptUnit unit= manager.getWorkingCopy(editor.getEditorInput());
		if (unit == null)
			return null;

		return unit;
	}

}
