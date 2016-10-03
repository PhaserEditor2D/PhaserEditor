/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.text.correction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.swt.graphics.Image;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;
import org.eclipse.ui.ISharedImages;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.BodyDeclaration;
import org.eclipse.wst.jsdt.core.dom.FieldDeclaration;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.IFunctionBinding;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.JSdoc;
import org.eclipse.wst.jsdt.core.dom.Name;
import org.eclipse.wst.jsdt.core.dom.PrimitiveType;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.SingleVariableDeclaration;
import org.eclipse.wst.jsdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.wst.jsdt.core.dom.TagElement;
import org.eclipse.wst.jsdt.core.dom.TextElement;
import org.eclipse.wst.jsdt.core.dom.Type;
import org.eclipse.wst.jsdt.core.dom.TypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.VariableDeclaration;
import org.eclipse.wst.jsdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.wst.jsdt.core.dom.rewrite.ListRewrite;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.corext.dom.Bindings;
import org.eclipse.wst.jsdt.internal.corext.util.Strings;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.JavaUIStatus;
import org.eclipse.wst.jsdt.ui.CodeGeneration;
import org.eclipse.wst.jsdt.ui.text.java.IInvocationContext;
import org.eclipse.wst.jsdt.ui.text.java.IProblemLocation;

/**
 *
 */
public class JavadocTagsSubProcessor {

	private static final class AddJavadocCommentProposal extends CUCorrectionProposal {

	 	private final int fInsertPosition;
		private final String fComment;

		private AddJavadocCommentProposal(String name, IJavaScriptUnit cu, int relevance, int insertPosition, String comment) {
			super(name, cu, relevance, JavaPluginImages.get(JavaPluginImages.IMG_OBJS_JAVADOCTAG));
			fInsertPosition= insertPosition;
			fComment= comment;
		}

		protected void addEdits(IDocument document, TextEdit rootEdit) throws CoreException {
			try {
				String lineDelimiter= TextUtilities.getDefaultLineDelimiter(document);
				final IJavaScriptProject project= getCompilationUnit().getJavaScriptProject();
				IRegion region= document.getLineInformationOfOffset(fInsertPosition);

				String lineContent= document.get(region.getOffset(), region.getLength());
				String indentString= Strings.getIndentString(lineContent, project);
				String str= Strings.changeIndent(fComment, 0, project, indentString, lineDelimiter);
				InsertEdit edit= new InsertEdit(fInsertPosition, str);
				rootEdit.addChild(edit);
				if (fComment.charAt(fComment.length() - 1) != '\n') {
					rootEdit.addChild(new InsertEdit(fInsertPosition, lineDelimiter));
					rootEdit.addChild(new InsertEdit(fInsertPosition, indentString));
				}
			} catch (BadLocationException e) {
				throw new CoreException(JavaUIStatus.createError(IStatus.ERROR, e));
			}
		}
	}

	private static final class AddMissingJavadocTagProposal extends LinkedCorrectionProposal {

		private final BodyDeclaration fBodyDecl; // MethodDecl or TypeDecl
		private final ASTNode fMissingNode;

		public AddMissingJavadocTagProposal(String label, IJavaScriptUnit cu, BodyDeclaration methodDecl, ASTNode missingNode, int relevance) {
			super(label, cu, null, relevance, JavaPluginImages.get(JavaPluginImages.IMG_OBJS_JAVADOCTAG));
			fBodyDecl= methodDecl;
			fMissingNode= missingNode;
		}

		protected ASTRewrite getRewrite() throws CoreException {
			AST ast= fBodyDecl.getAST();
			ASTRewrite rewrite= ASTRewrite.create(ast);
		 	insertMissingJavadocTag(rewrite, fMissingNode, fBodyDecl);
			return rewrite;
		}

		private void insertMissingJavadocTag(ASTRewrite rewrite, ASTNode missingNode, BodyDeclaration bodyDecl) {
			AST ast= bodyDecl.getAST();
			JSdoc javadoc= bodyDecl.getJavadoc();
		 	ListRewrite tagsRewriter= rewrite.getListRewrite(javadoc, JSdoc.TAGS_PROPERTY);

		 	StructuralPropertyDescriptor location= missingNode.getLocationInParent();
		 	TagElement newTag;
		 	if (location == SingleVariableDeclaration.NAME_PROPERTY) {
		 		// normal parameter
		 		SingleVariableDeclaration decl= (SingleVariableDeclaration) missingNode.getParent();

				String name= ((SimpleName) missingNode).getIdentifier();
				newTag= ast.newTagElement();
				newTag.setTagName(TagElement.TAG_PARAM);
				List fragments= newTag.fragments();
				fragments.add(ast.newSimpleName(name));

				FunctionDeclaration methodDeclaration= (FunctionDeclaration) bodyDecl;
				List params= methodDeclaration.parameters();

				Set sameKindLeadingNames= getPreviousParamNames(params, decl);

				insertTag(tagsRewriter, newTag, sameKindLeadingNames);
		 	} else if (location == FunctionDeclaration.RETURN_TYPE2_PROPERTY) {
				newTag= ast.newTagElement();
				newTag.setTagName(TagElement.TAG_RETURN);
				insertTag(tagsRewriter, newTag, null);
		 	} else if (location == FunctionDeclaration.THROWN_EXCEPTIONS_PROPERTY) {
				newTag= ast.newTagElement();
				newTag.setTagName(TagElement.TAG_THROWS);
				TextElement excNode= ast.newTextElement();
				excNode.setText(ASTNodes.asString(missingNode));
				newTag.fragments().add(excNode);
				List exceptions= ((FunctionDeclaration) bodyDecl).thrownExceptions();
				insertTag(tagsRewriter, newTag, getPreviousExceptionNames(exceptions, missingNode));
		 	} else {
		 		Assert.isTrue(false, "AddMissingJavadocTagProposal: unexpected node location"); //$NON-NLS-1$
		 		return;
		 	}

			TextElement textElement= ast.newTextElement();
			textElement.setText(""); //$NON-NLS-1$
			newTag.fragments().add(textElement);
			addLinkedPosition(rewrite.track(textElement), false, "comment_start"); //$NON-NLS-1$
		}
	}

	private static final class AddAllMissingJavadocTagsProposal extends LinkedCorrectionProposal {

		private final BodyDeclaration fBodyDecl;

		public AddAllMissingJavadocTagsProposal(String label, IJavaScriptUnit cu, BodyDeclaration bodyDecl, int relevance) {
			super(label, cu, null, relevance, JavaPluginImages.get(JavaPluginImages.IMG_OBJS_JAVADOCTAG));
			fBodyDecl= bodyDecl;
		}

		protected ASTRewrite getRewrite() throws CoreException {
			ASTRewrite rewrite= ASTRewrite.create(fBodyDecl.getAST());
			if (fBodyDecl instanceof FunctionDeclaration) {
				insertAllMissingMethodTags(rewrite, (FunctionDeclaration) fBodyDecl);
			} else {
				insertAllMissingTypeTags(rewrite, (TypeDeclaration) fBodyDecl);
			}
			return rewrite;
		}

		private void insertAllMissingMethodTags(ASTRewrite rewriter, FunctionDeclaration methodDecl) {
		 	AST ast= methodDecl.getAST();
		 	JSdoc javadoc= methodDecl.getJavadoc();
		 	ListRewrite tagsRewriter= rewriter.getListRewrite(javadoc, JSdoc.TAGS_PROPERTY);

		 	List typeParamNames= new ArrayList();
		 	List params= methodDecl.parameters();
		 	for (int i= params.size() - 1; i >= 0 ; i--) {
		 		SingleVariableDeclaration decl= (SingleVariableDeclaration) params.get(i);
		 		String name= decl.getName().getIdentifier();
		 		if (findTag(javadoc, TagElement.TAG_PARAM, name) == null) {
		 			TagElement newTag= ast.newTagElement();
		 			newTag.setTagName(TagElement.TAG_PARAM);
		 			newTag.fragments().add(ast.newSimpleName(name));
					insertTabStop(rewriter, newTag.fragments(), "methParam" + i); //$NON-NLS-1$
		 			Set sameKindLeadingNames= getPreviousParamNames(params, decl);
		 			sameKindLeadingNames.addAll(typeParamNames);
		 			insertTag(tagsRewriter, newTag, sameKindLeadingNames);
		 		}
		 	}
		 	if (!methodDecl.isConstructor()) {
		 		Type type= methodDecl.getReturnType2();
		 		if (type != null && (!type.isPrimitiveType() || (((PrimitiveType) type).getPrimitiveTypeCode() != PrimitiveType.VOID))) {
		 			if (findTag(javadoc, TagElement.TAG_RETURN, null) == null) {
		 				TagElement newTag= ast.newTagElement();
		 				newTag.setTagName(TagElement.TAG_RETURN);
						insertTabStop(rewriter, newTag.fragments(), "return"); //$NON-NLS-1$
		 				insertTag(tagsRewriter, newTag, null);
		 			}
		 		}
		 	}
		 	List thrownExceptions= methodDecl.thrownExceptions();
		 	for (int i= thrownExceptions.size() - 1; i >= 0 ; i--) {
		 		Name exception= (Name) thrownExceptions.get(i);
		 		ITypeBinding binding= exception.resolveTypeBinding();
		 		if (binding != null) {
		 			String name= binding.getName();
		 			if (findThrowsTag(javadoc, name) == null) {
		 				TagElement newTag= ast.newTagElement();
		 				newTag.setTagName(TagElement.TAG_THROWS);
						TextElement excNode= ast.newTextElement();
						excNode.setText(ASTNodes.asString(exception));
		 				newTag.fragments().add(excNode);
						insertTabStop(rewriter, newTag.fragments(), "exception" + i); //$NON-NLS-1$
		 				insertTag(tagsRewriter, newTag, getPreviousExceptionNames(thrownExceptions, exception));
		 			}
		 		}
		 	}
		 }

		private void insertAllMissingTypeTags(ASTRewrite rewriter, TypeDeclaration typeDecl) {
			AST ast= typeDecl.getAST();
			JSdoc javadoc= typeDecl.getJavadoc();
			ListRewrite tagsRewriter= rewriter.getListRewrite(javadoc, JSdoc.TAGS_PROPERTY);
		}

		private void insertTabStop(ASTRewrite rewriter, List fragments, String linkedName) {
			TextElement textElement= rewriter.getAST().newTextElement();
			textElement.setText(""); //$NON-NLS-1$
			fragments.add(textElement);
			addLinkedPosition(rewriter.track(textElement), false, linkedName);
		}

	}

	public static void getMissingJavadocTagProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) {
	 	ASTNode node= problem.getCoveringNode(context.getASTRoot());
	 	if (node == null) {
	 		return;
	 	}
	 	node= ASTNodes.getNormalizedNode(node);

	 	BodyDeclaration bodyDeclaration= ASTResolving.findParentBodyDeclaration(node);
	 	if (bodyDeclaration == null) {
	 		return;
	 	}
	 	JSdoc javadoc= bodyDeclaration.getJavadoc();
	 	if (javadoc == null) {
	 		return;
	 	}

	 	String label;
	 	StructuralPropertyDescriptor location= node.getLocationInParent();
	 	if (location == SingleVariableDeclaration.NAME_PROPERTY) {
	 		label= CorrectionMessages.JavadocTagsSubProcessor_addjavadoc_paramtag_description;
	 		if (node.getParent().getLocationInParent() != FunctionDeclaration.PARAMETERS_PROPERTY) {
	 			return; // paranoia checks
	 		}
	 	} else if (location == FunctionDeclaration.RETURN_TYPE2_PROPERTY) {
	 		label= CorrectionMessages.JavadocTagsSubProcessor_addjavadoc_returntag_description;
	 	} else if (location == FunctionDeclaration.THROWN_EXCEPTIONS_PROPERTY) {
	 		label= CorrectionMessages.JavadocTagsSubProcessor_addjavadoc_throwstag_description;
	 	} else {
	 		return;
	 	}
	 	ASTRewriteCorrectionProposal proposal= new AddMissingJavadocTagProposal(label, context.getCompilationUnit(), bodyDeclaration, node, 1); 
	 	proposals.add(proposal);

	 	String label2= CorrectionMessages.JavadocTagsSubProcessor_addjavadoc_allmissing_description;
	 	ASTRewriteCorrectionProposal addAllMissing= new AddAllMissingJavadocTagsProposal(label2, context.getCompilationUnit(), bodyDeclaration, 5); 
	 	proposals.add(addAllMissing);
	}

	public static void getMissingJavadocCommentProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) throws CoreException {
		ASTNode node= problem.getCoveringNode(context.getASTRoot());
		if (node == null) {
			return;
		}
		BodyDeclaration declaration= ASTResolving.findParentBodyDeclaration(node);
		if (declaration == null) {
			return;
		}
		IJavaScriptUnit cu= context.getCompilationUnit();
		ITypeBinding binding= Bindings.getBindingOfParentType(declaration);
		if (binding == null) {
			return;
		}

		if (declaration instanceof FunctionDeclaration) {
			FunctionDeclaration methodDecl= (FunctionDeclaration) declaration;
			IFunctionBinding methodBinding= methodDecl.resolveBinding();
			IFunctionBinding overridden= null;
			if (methodBinding != null) {
				overridden= Bindings.findOverriddenMethod(methodBinding, true);
			}

			String string= CodeGeneration.getMethodComment(cu, binding.getName(), methodDecl, overridden, String.valueOf('\n'));
			if (string != null) {
				String label= CorrectionMessages.JavadocTagsSubProcessor_addjavadoc_method_description;
				proposals.add(new AddJavadocCommentProposal(label, cu, 1, declaration.getStartPosition(), string));
			}
		} else if (declaration instanceof AbstractTypeDeclaration) {
			String typeQualifiedName= Bindings.getTypeQualifiedName(binding);
			
			String string= CodeGeneration.getTypeComment(cu, typeQualifiedName, String.valueOf('\n'));
			if (string != null) {
				String label= CorrectionMessages.JavadocTagsSubProcessor_addjavadoc_type_description;
				proposals.add(new AddJavadocCommentProposal(label, cu, 1, declaration.getStartPosition(), string));
			}
		} else if (declaration instanceof FieldDeclaration) {
			String comment= "/**\n *\n */\n"; //$NON-NLS-1$
			List fragments= ((FieldDeclaration)declaration).fragments();
			if (fragments != null && fragments.size() > 0) {
				VariableDeclaration decl= (VariableDeclaration)fragments.get(0);
				String fieldName= decl.getName().getIdentifier();
				String typeName= binding.getName();
				comment= CodeGeneration.getFieldComment(cu, typeName, fieldName, String.valueOf('\n'));
			}
			if (comment != null) {
				String label= CorrectionMessages.JavadocTagsSubProcessor_addjavadoc_field_description;
				proposals.add(new AddJavadocCommentProposal(label, cu, 1, declaration.getStartPosition(), comment));
			}
		}
	}

	private static Set getPreviousParamNames(List params, ASTNode missingNode) {
		Set previousNames=  new HashSet();
		for (int i = 0; i < params.size(); i++) {
			SingleVariableDeclaration curr= (SingleVariableDeclaration) params.get(i);
			if (curr == missingNode) {
				return previousNames;
			}
			previousNames.add(curr.getName().getIdentifier());
		}
		return previousNames;
	}

	private static Set getPreviousExceptionNames(List list, ASTNode missingNode) {
		Set previousNames=  new HashSet();
		for (int i= 0; i < list.size() && missingNode != list.get(i); i++) {
			Name curr= (Name) list.get(i);
			previousNames.add(ASTNodes.getSimpleNameIdentifier(curr));
		}
		return previousNames;
	}

	public static TagElement findTag(JSdoc javadoc, String name, String arg) {
		List tags= javadoc.tags();
		int nTags= tags.size();
		for (int i= 0; i < nTags; i++) {
			TagElement curr= (TagElement) tags.get(i);
			if (name.equals(curr.getTagName())) {
				if (arg != null) {
					String argument= getArgument(curr);
					if (arg.equals(argument)) {
						return curr;
					}
				} else {
					return curr;
				}
			}
		}
		return null;
	}

	public static TagElement findParamTag(JSdoc javadoc, String arg) {
		List tags= javadoc.tags();
		int nTags= tags.size();
		for (int i= 0; i < nTags; i++) {
			TagElement curr= (TagElement) tags.get(i);
			String currName= curr.getTagName();
			if (TagElement.TAG_PARAM.equals(currName)) {
				String argument= getArgument(curr);
				if (arg.equals(argument)) {
					return curr;
				}
			}
		}
		return null;
	}


	public static TagElement findThrowsTag(JSdoc javadoc, String arg) {
		List tags= javadoc.tags();
		int nTags= tags.size();
		for (int i= 0; i < nTags; i++) {
			TagElement curr= (TagElement) tags.get(i);
			String currName= curr.getTagName();
			if (TagElement.TAG_THROWS.equals(currName) || TagElement.TAG_EXCEPTION.equals(currName)) {  
				String argument= getArgument(curr);
				if (arg.equals(argument)) {
					return curr;
				}
			}
		}
		return null;
	}

	public static void insertTag(ListRewrite rewriter, TagElement newElement, Set sameKindLeadingNames) {
		insertTag(rewriter, newElement, sameKindLeadingNames, null);
	}

	public static void insertTag(ListRewrite rewriter, TagElement newElement, Set sameKindLeadingNames, TextEditGroup groupDescription) {
		List tags= rewriter.getRewrittenList();

		String insertedTagName= newElement.getTagName();

		ASTNode after= null;
		int tagRanking= getTagRanking(insertedTagName);
		for (int i= tags.size() - 1; i >= 0; i--) {
			TagElement curr= (TagElement) tags.get(i);
			String tagName= curr.getTagName();
			if (tagName == null || tagRanking > getTagRanking(tagName)) {
				after= curr;
				break;
			}
			if (sameKindLeadingNames != null && isSameTag(insertedTagName, tagName)) {
				String arg= getArgument(curr);
				if (arg != null && sameKindLeadingNames.contains(arg)) {
					after= curr;
					break;
				}
			}
		}
		if (after != null) {
			rewriter.insertAfter(newElement, after, groupDescription);
		} else {
			rewriter.insertFirst(newElement, groupDescription);
		}
	}

	private static boolean isSameTag(String insertedTagName, String tagName) {
		if (insertedTagName.equals(tagName)) {
			return true;
		}
		if (TagElement.TAG_EXCEPTION.equals(tagName)) {
			return TagElement.TAG_THROWS.equals(insertedTagName);
		}
		return false;
	}
	
	private static String[] TAG_ORDER= { // see http://java.sun.com/j2se/javadoc/writingdoccomments/index.html#orderoftags
		TagElement.TAG_AUTHOR,
		TagElement.TAG_VERSION,
		TagElement.TAG_PARAM,
		TagElement.TAG_RETURN,
		TagElement.TAG_THROWS, // synonym to TAG_EXCEPTION
		TagElement.TAG_SEE,
		TagElement.TAG_SINCE,
		TagElement.TAG_SERIAL,
		TagElement.TAG_DEPRECATED
	};
	
	private static int getTagRanking(String tagName) {
		if (tagName.equals(TagElement.TAG_EXCEPTION)) {
			tagName= TagElement.TAG_THROWS;
		}
		for (int i= 0; i < TAG_ORDER.length; i++) {
			if (tagName.equals(TAG_ORDER[i])) {
				return i;
			}
		}
		return TAG_ORDER.length;
	}

	private static String getArgument(TagElement curr) {
		List fragments= curr.fragments();
		if (!fragments.isEmpty()) {
			Object first= fragments.get(0);
			if (first instanceof Name) {
				return ASTNodes.getSimpleNameIdentifier((Name) first);
			} else if (first instanceof TextElement && TagElement.TAG_PARAM.equals(curr.getTagName())) {
				String text= ((TextElement) first).getText();
				if ("<".equals(text) && fragments.size() >= 3) { //$NON-NLS-1$
					Object second= fragments.get(1);
					Object third= fragments.get(2);
					if (second instanceof Name && third instanceof TextElement && ">".equals(((TextElement) third).getText())) { //$NON-NLS-1$
						return '<' + ASTNodes.getSimpleNameIdentifier((Name) second) + '>';
					}
				} else if (text.startsWith(String.valueOf('<')) && text.endsWith(String.valueOf('>')) && text.length() > 2) {
					return text.substring(1, text.length() - 1);
				}
			}
		}
		return null;
	}

	public static void getRemoveJavadocTagProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) {
		ASTNode node= problem.getCoveringNode(context.getASTRoot());
		while (node != null && !(node instanceof TagElement)) {
			node= node.getParent();
		}
		if (node == null) {
			return;
		}
		ASTRewrite rewrite= ASTRewrite.create(node.getAST());
		rewrite.remove(node, null);

		String label= CorrectionMessages.JavadocTagsSubProcessor_removetag_description;
		Image image= JavaScriptPlugin.getDefault().getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE);
		proposals.add(new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, 5, image)); 
	}
}
