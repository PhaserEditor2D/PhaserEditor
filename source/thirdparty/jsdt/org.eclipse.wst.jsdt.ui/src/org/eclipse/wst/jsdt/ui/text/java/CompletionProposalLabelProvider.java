/*******************************************************************************
 * Copyright (c) 2005, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.ui.text.java;

import java.util.Arrays;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.wst.jsdt.core.CompletionContext;
import org.eclipse.wst.jsdt.core.CompletionProposal;
import org.eclipse.wst.jsdt.core.Flags;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.JSDScopeUtil;
import org.eclipse.wst.jsdt.core.JsGlobalScopeContainerInitializer;
import org.eclipse.wst.jsdt.core.Signature;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.internal.core.search.indexing.IIndexConstants;
import org.eclipse.wst.jsdt.internal.core.util.Util;
import org.eclipse.wst.jsdt.internal.corext.template.java.SignatureUtil;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.JavaElementImageProvider;
import org.eclipse.wst.jsdt.ui.JavaScriptElementImageDescriptor;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabels;


/**
 * Provides labels for JavaScript content assist proposals. The functionality is
 * similar to the one provided by {@link org.eclipse.wst.jsdt.ui.JavaScriptElementLabels},
 * but based on signatures and {@link CompletionProposal}s.
 *
 * @see Signature
 *
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves. */
public class CompletionProposalLabelProvider {

	/** Used to filter out parameter type names of the ANY type */
	private static final char[] C_ANY = new char[] {Signature.C_ANY};
	
	/**
	 * The completion context.
	 */
	private CompletionContext fContext;

	/**
	 * Creates a new label provider.
	 */
	public CompletionProposalLabelProvider() {
	}

	/**
	 * Creates and returns a parameter list of the given method or type proposal
	 * suitable for display. The list does not include parentheses. The lower
	 * bound of parameter types is returned.
	 * <p>
	 * Examples:
	 * <pre>
	 *   &quot;void method(int i, Strings)&quot; -&gt; &quot;int i, String s&quot;
	 *   &quot;? extends Number method(java.lang.String s, ? super Number n)&quot; -&gt; &quot;String s, Number n&quot;
	 * </pre>
	 * </p>
	 *
	 * @param proposal the proposal to create the parameter list
	 *        for. Must be of kind {@link CompletionProposal#METHOD_REF} or
	 *        {@link CompletionProposal#TYPE_REF}.
	 * @return the list of comma-separated parameters suitable for display
	 */
	public String createParameterList(CompletionProposal proposal) {
		int kind= proposal.getKind();
		switch (kind) {
			case CompletionProposal.METHOD_REF:
				return appendUnboundedParameterList(new StringBuffer(), proposal).toString();
			default:
				Assert.isLegal(false);
				return null; // dummy
		}
	}

	/**
	 * Appends the parameter list to <code>buffer</code>.
	 *
	 * @param buffer the buffer to append to
	 * @param methodProposal the method proposal
	 * @return the modified <code>buffer</code>
	 */
	private StringBuffer appendUnboundedParameterList(StringBuffer buffer, CompletionProposal methodProposal) {
		char[] signature= methodProposal.getSignature();
		char[][] parameterNames= methodProposal.getParamaterNames();
		char[][] parameterTypes = methodProposal.getParameterTypeNames();

		/* TODO: remove this because it uses signatures
		 * if did not get parameter types attempt to use signature */
		if((parameterTypes == null || parameterTypes.length == 0) && signature != null && signature.length > 0) {
			parameterTypes = Signature.getParameterTypes(signature);
			for (int i= 0; i < parameterTypes.length; i++) {
				parameterTypes[i]= createTypeDisplayName(parameterTypes[i]);
			}
		}

		if (Flags.isVarargs(methodProposal.getFlags())) {
			int index= parameterTypes.length - 1;
			parameterTypes[index]= convertToVararg(parameterTypes[index]);
		}
		return appendParameterSignature(buffer, parameterTypes, parameterNames);
	}
	
	/**
	 * Converts the display name for an array type into a variable arity display name.
	 * <p>
	 * Examples:
	 * <ul>
	 * <li> "int[]" -> "int..."</li>
	 * <li> "Object[][]" -> "Object[]..."</li>
	 * <li> "String" -> "String"</li>
	 * </ul>
	 * </p>
	 * <p>
	 * If <code>typeName</code> does not include the substring "[]", it is returned unchanged.
	 * </p>
	 * 
	 * @param typeName the type name to convert
	 * @return the converted type name
	 * 
	 */
    private char[] convertToVararg(char[] typeName) {
    	if (typeName == null)
    		return typeName;
    	final int len= typeName.length;
		if (len < 2)
    		return typeName;
    	
    	if (typeName[len - 1] != ']')
    		return typeName;
    	if (typeName[len - 2] != '[')
    		return typeName;
    	
		char[] vararg= new char[len + 1];
		System.arraycopy(typeName, 0, vararg, 0, len - 2);
		vararg[len - 2]= '.';
		vararg[len - 1]= '.';
		vararg[len]= '.';
	    return vararg;
    }

	/**
	 * Returns the display string for a JavaScript type signature.
	 *
	 * @param typeSignature the type signature to create a display name for
	 * @return the display name for <code>typeSignature</code>
	 * @throws IllegalArgumentException if <code>typeSignature</code> is not a
	 *         valid signature
	 * @see Signature#toCharArray(char[])
	 * @see Signature#getSimpleName(char[])
	 */
	private char[] createTypeDisplayName(char[] typeSignature) throws IllegalArgumentException {
		char[] displayName= Signature.getSimpleName(Signature.toCharArray(typeSignature));

		// XXX see https://bugs.eclipse.org/bugs/show_bug.cgi?id=84675
		boolean useShortGenerics= false;
		if (useShortGenerics) {
			StringBuffer buf= new StringBuffer();
			buf.append(displayName);
			int pos;
			do {
				pos= buf.indexOf("? extends "); //$NON-NLS-1$
				if (pos >= 0) {
					buf.replace(pos, pos + 10, "+"); //$NON-NLS-1$
				} else {
					pos= buf.indexOf("? super "); //$NON-NLS-1$
					if (pos >= 0)
						buf.replace(pos, pos + 8, "-"); //$NON-NLS-1$
				}
			} while (pos >= 0);
			return buf.toString().toCharArray();
		}
		return displayName;
	}

	/**
	 * Creates a display string of a parameter list (without the parentheses)
	 * for the given parameter types and names.
	 *
	 * @param buffer the string buffer 
	 * @param parameterTypes the parameter types
	 * @param parameterNames the parameter names
	 * @return the display string of the parameter list defined by the passed arguments
	 */
	private final StringBuffer appendParameterSignature(StringBuffer buffer, char[][] parameterTypes, char[][] parameterNames) {
		if (parameterNames != null) {
			for (int i = 0; i < parameterNames.length; i++) {
				if (i > 0) {
					buffer.append(',');
					buffer.append(' ');
				}
				//do not display the ANY type
				if (parameterTypes != null && parameterTypes.length > i &&
						parameterTypes[i] != null && parameterTypes[i].length > 0 &&
						!Arrays.equals(Signature.ANY, parameterTypes[i]) &&
						!Arrays.equals(C_ANY, parameterTypes[i])) {
					
					Util.insertTypeLabel(parameterTypes[i], buffer);
					buffer.append(' ');
				}
					
				if (parameterNames[i] != null) {
					buffer.append(parameterNames[i]);
				}
			}
		}
		return buffer;
	}

	/**
	 * Creates a display label for the given method proposal. The display label
	 * consists of:
	 * <ul>
	 *   <li>the method name</li>
	 *   <li>the parameter list (see {@link #createParameterList(CompletionProposal)})</li>
	 *   <li>the upper bound of the return type (see {@link SignatureUtil#getUpperBound(String)})</li>
	 *   <li>the raw simple name of the declaring type</li>
	 * </ul>
	 * <p>
	 * Examples:
	 * For the <code>get(int)</code> method of a variable of type <code>List<? extends Number></code>, the following
	 * display name is returned: <code>get(int index)  Number - List</code>.<br>
	 * For the <code>add(E)</code> method of a variable of type <code>List<? super Number></code>, the following
	 * display name is returned: <code>add(Number o)  void - List</code>.<br>
	 * </p>
	 *
	 * @param methodProposal the method proposal to display
	 * @return the display label for the given method proposal
	 */
	String createMethodProposalLabel(CompletionProposal methodProposal) {
		StringBuffer nameBuffer= new StringBuffer();

		// method name
		nameBuffer.append(methodProposal.getName());

		// parameters
		nameBuffer.append('(');
		appendUnboundedParameterList(nameBuffer, methodProposal);
		nameBuffer.append(')');

		// return type
		if (!methodProposal.isConstructor()) {
			char[] returnType= methodProposal.getReturnType();
			if (returnType != null && returnType.length > 0 &&  !Arrays.equals(Signature.ANY,returnType) && !Arrays.equals(Signature.VOID,returnType)) {
				nameBuffer.append(" : "); //$NON-NLS-1$
				//@GINO: Anonymous UI Label
				org.eclipse.wst.jsdt.internal.core.util.Util.insertTypeLabel( returnType, nameBuffer );
			}
		}

		// declaring type
		String declaringType= extractDeclaringTypeFQN(methodProposal);
		if(declaringType != null) {
			nameBuffer.append(" - "); //$NON-NLS-1$
			if(CharOperation.equals(declaringType.toCharArray(), IIndexConstants.GLOBAL_SYMBOL)) {
				declaringType = JavaTextMessages.Global;
			}
			org.eclipse.wst.jsdt.internal.core.util.Util.insertTypeLabel( declaringType, nameBuffer );
		}

		return nameBuffer.toString();
	}
	
	/**
	 * Creates a display label for the given method proposal. The display label consists of:
	 * <ul>
	 * <li>the method name</li>
	 * <li>the raw simple name of the declaring type</li>
	 * </ul>
	 * <p>
	 * Examples: For the <code>get(int)</code> method of a variable of type
	 * <code>List<? extends Number></code>, the following display name is returned <code>get(int) - List</code>.<br>
	 * For the <code>add(E)</code> method of a variable of type <code>List</code>, the
	 * following display name is returned:
	 * <code>add(Object) - List</code>.<br>
	 * </p>
	 * 
	 * @param methodProposal the method proposal to display
	 * @return the display label for the given method proposal
	 * 
	 */
	String createJavadocMethodProposalLabel(CompletionProposal methodProposal) {
		StringBuffer nameBuffer= new StringBuffer();
		
		// method name
		nameBuffer.append(methodProposal.getCompletion());
		
		// declaring type
		nameBuffer.append(" - "); //$NON-NLS-1$
		String declaringType= extractDeclaringTypeFQN(methodProposal);
		declaringType= Signature.getSimpleName(declaringType);
		nameBuffer.append(declaringType);
		
		return nameBuffer.toString();
	}
	
	String createOverrideMethodProposalLabel(CompletionProposal methodProposal) {
		StringBuffer nameBuffer= new StringBuffer();

		// method name
		nameBuffer.append(methodProposal.getName());

		// parameters
		nameBuffer.append('(');
		appendUnboundedParameterList(nameBuffer, methodProposal);
		nameBuffer.append(")  "); //$NON-NLS-1$

		// return type
		// TODO remove SignatureUtil.fix83600 call when bugs are fixed
		char[] returnType= createTypeDisplayName(Signature.getReturnType(methodProposal.getSignature()));
		nameBuffer.append(returnType);

		// declaring type
		nameBuffer.append(" - "); //$NON-NLS-1$

		String declaringType= extractDeclaringTypeFQN(methodProposal);
		declaringType= Signature.getSimpleName(declaringType);
		nameBuffer.append(Messages.format(JavaTextMessages.ResultCollector_overridingmethod, new String(declaringType)));

		return nameBuffer.toString();
	}

	/**
	 * Extracts the fully qualified name of the declaring type of a method
	 * reference.
	 *
	 * @param methodProposal a proposed method
	 * @return the qualified name of the declaring type
	 */
	private String extractDeclaringTypeFQN(CompletionProposal methodProposal) {
		String qualifedName = null;
		
		char[] compUnit = methodProposal.getDeclarationTypeName();
		if(compUnit != null) {
			IJavaScriptProject project = methodProposal.getJavaProject();
			JsGlobalScopeContainerInitializer init = JSDScopeUtil.findLibraryInitializer(new Path(new String(compUnit)),project);
			if(init!=null) {
				String description = init.getDescription(new Path(new String(compUnit)),project);
				if( description!=null) return  "[" +  description + "]"; //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		
		if(methodProposal.getDeclarationTypeName() != null){
			qualifedName = new String(methodProposal.getDeclarationTypeName());
		}
		
		return qualifedName;
	}

	/**
	 * Creates a display label for a given type proposal. The display label
	 * consists of:
	 * <ul>
	 *   <li>the simple type name (erased when the context is in javadoc)</li>
	 *   <li>the package name</li>
	 * </ul>
	 * <p>
	 * Examples:
	 * A proposal for the generic type <code>java.util.List&lt;E&gt;</code>, the display label
	 * is: <code>List<E> - java.util</code>.
	 * </p>
	 *
	 * @param typeProposal the method proposal to display
	 * @return the display label for the given type proposal
	 */
	String createTypeProposalLabel(CompletionProposal typeProposal) {
		StringBuffer buf= new StringBuffer();
		buf.append(typeProposal.getCompletion());
		char[] declarationSignature = typeProposal.getDeclarationSignature();
		if (declarationSignature!=null && declarationSignature.length>0)
		{
			buf.append(JavaScriptElementLabels.CONCAT_STRING);
			buf.append(declarationSignature);
		}
		return buf.toString();
	}
	
	String createJavadocTypeProposalLabel(CompletionProposal typeProposal) {
		char[] fullName= Signature.toCharArray(typeProposal.getSignature());
		return createJavadocTypeProposalLabel(fullName);
	}
	
	String createJavadocTypeProposalLabel(char[] fullName) {
		// only display innermost type name as type name, using any
		// enclosing types as qualification
		int qIndex= findSimpleNameStart(fullName);
		
		StringBuffer buf= new StringBuffer("{@link "); //$NON-NLS-1$
		buf.append(fullName, qIndex, fullName.length - qIndex);
		buf.append('}');
		if (qIndex > 0) {
			buf.append(JavaScriptElementLabels.CONCAT_STRING);
			buf.append(fullName, 0, qIndex - 1);
		}
		return buf.toString();
	}
	
	private int findSimpleNameStart(char[] array) {
		int lastDot= 0;
		for (int i= 0, len= array.length; i < len; i++) {
			char ch= array[i];
			if (ch == '<') {
				return lastDot;
			} else if (ch == '.') {
				lastDot= i + 1;
			}
		}
		return lastDot;
	}

	String createSimpleLabelWithType(CompletionProposal proposal) {
		StringBuffer buf= new StringBuffer();
		buf.append(proposal.getCompletion());
		char[] typeName= Signature.getSignatureSimpleName(proposal.getSignature());
		
		if (typeName.length > 0) {
			buf.append(" : "); //$NON-NLS-1$
			
			//@GINO: Anonymous UI Label
			org.eclipse.wst.jsdt.internal.core.util.Util.insertTypeLabel( typeName, buf );
		}
		return buf.toString();
	}

	/**
	 * Returns whether the given string starts with "this.".
	 * 
	 * @param string 
	 * @return <code>true</code> if the given string starts with "this."
	 * 
	 */
	private boolean isThisPrefix(char[] string) {
		if (string == null || string.length < 5)
			return false;
		return string[0] == 't' && string[1] == 'h' && string[2] == 'i' && string[3] == 's' && string[4] == '.'; 		
	}
	String createLabelWithTypeAndDeclaration(CompletionProposal proposal) {
		char[] name= proposal.getCompletion(); 
		if (!isThisPrefix(name))
			name= proposal.getName(); 
		
		StringBuffer buf= new StringBuffer();
		buf.append(name);
		char[] returnType = proposal.getReturnType();
		if (returnType != null && returnType.length > 0&& !(Arrays.equals(Signature.ANY, returnType))) {
			buf.append(" : "); //$NON-NLS-1$
			
			//@GINO: Anonymous UI Label
			org.eclipse.wst.jsdt.internal.core.util.Util.insertTypeLabel( returnType, buf );
		}

		//get the declaration type
		char[] declarationType = null;
		if(proposal.getDeclarationTypeName() != null) {
			declarationType = proposal.getDeclarationTypeName();
		} else if (proposal.getDeclarationSignature() != null) {
			declarationType = Signature.getSignatureSimpleName(proposal.getDeclarationSignature());
		}
		
		//deal with if global type
		if(CharOperation.equals(declarationType, IIndexConstants.GLOBAL_SYMBOL)) {
			declarationType = JavaTextMessages.Global.toCharArray();
		}
		
		//append declaration type
		if (declarationType != null && declarationType.length > 0) {
			buf.append(" - "); //$NON-NLS-1$
			
			//@GINO: Anonymous UI Label
			org.eclipse.wst.jsdt.internal.core.util.Util.insertTypeLabel( declarationType, buf );
		}

		return buf.toString();
	}

	String createPackageProposalLabel(CompletionProposal proposal) {
		Assert.isTrue(proposal.getKind() == CompletionProposal.PACKAGE_REF);
		return String.valueOf(proposal.getDeclarationSignature());
	}

	String createSimpleLabel(CompletionProposal proposal) {
		return String.valueOf(proposal.getCompletion());
	}

	String createAnonymousTypeLabel(CompletionProposal proposal) {
		char[] declaringTypeSignature= proposal.getDeclarationSignature();

		StringBuffer buffer= new StringBuffer();
		buffer.append(Signature.getSignatureSimpleName(declaringTypeSignature));
		buffer.append('(');
		appendUnboundedParameterList(buffer, proposal);
		buffer.append(')');
		buffer.append("  "); //$NON-NLS-1$
		buffer.append(JavaTextMessages.ResultCollector_anonymous_type);

		return buffer.toString();
	}

	/**
	 * Creates the display label for a given <code>CompletionProposal</code>.
	 *
	 * @param proposal the completion proposal to create the display label for
	 * @return the display label for <code>proposal</code>
	 */
	public String createLabel(CompletionProposal proposal) {
		switch (proposal.getKind()) {
			case CompletionProposal.CONSTRUCTOR_INVOCATION:
			case CompletionProposal.METHOD_NAME_REFERENCE:
			case CompletionProposal.METHOD_REF:
			case CompletionProposal.POTENTIAL_METHOD_DECLARATION:
				if (fContext != null && fContext.isInJsdoc())
					return createJavadocMethodProposalLabel(proposal);
				return createMethodProposalLabel(proposal);
			case CompletionProposal.METHOD_DECLARATION:
				return createOverrideMethodProposalLabel(proposal);
			case CompletionProposal.ANONYMOUS_CLASS_DECLARATION:
				return createAnonymousTypeLabel(proposal);
			case CompletionProposal.TYPE_REF:
				return createTypeProposalLabel(proposal);
			case CompletionProposal.JSDOC_TYPE_REF:
				return createJavadocTypeProposalLabel(proposal);
			case CompletionProposal.JSDOC_FIELD_REF:
			case CompletionProposal.JSDOC_BLOCK_TAG:
			case CompletionProposal.JSDOC_INLINE_TAG:
			case CompletionProposal.JSDOC_PARAM_REF:
				return createSimpleLabel(proposal);
			case CompletionProposal.JSDOC_METHOD_REF:
				return createJavadocMethodProposalLabel(proposal);
			case CompletionProposal.PACKAGE_REF:
				return createPackageProposalLabel(proposal);
			case CompletionProposal.FIELD_REF:
				return createLabelWithTypeAndDeclaration(proposal);
			case CompletionProposal.LOCAL_VARIABLE_REF:
			case CompletionProposal.VARIABLE_DECLARATION:
				return createSimpleLabelWithType(proposal);
			case CompletionProposal.KEYWORD:
			case CompletionProposal.LABEL_REF:
				return createSimpleLabel(proposal);
			default:
				Assert.isTrue(false);
				return null;
		}
	}

	/**
	 * Creates and returns a decorated image descriptor for a completion proposal.
	 *
	 * @param proposal the proposal for which to create an image descriptor
	 * @return the created image descriptor, or <code>null</code> if no image is available
	 */
	public ImageDescriptor createImageDescriptor(CompletionProposal proposal) {
		final int flags= proposal.getFlags();

		ImageDescriptor descriptor;
		switch (proposal.getKind()) {
			case CompletionProposal.CONSTRUCTOR_INVOCATION:
			case CompletionProposal.METHOD_DECLARATION:
			case CompletionProposal.METHOD_NAME_REFERENCE:
			case CompletionProposal.METHOD_REF:
			case CompletionProposal.POTENTIAL_METHOD_DECLARATION:
				descriptor= JavaElementImageProvider.getMethodImageDescriptor(false, flags);
				break;
			case CompletionProposal.ANONYMOUS_CLASS_DECLARATION:
			case CompletionProposal.TYPE_REF:
				switch (Signature.getTypeSignatureKind(proposal.getSignature())) {
					case Signature.CLASS_TYPE_SIGNATURE:
						descriptor= JavaElementImageProvider.getTypeImageDescriptor(false, false, flags, false);
						break;
					default:
						descriptor= null;
				}
				break;
			case CompletionProposal.FIELD_REF:
				descriptor= JavaElementImageProvider.getFieldImageDescriptor(false, flags);
				break;
			case CompletionProposal.LOCAL_VARIABLE_REF:
			case CompletionProposal.VARIABLE_DECLARATION:
				descriptor= JavaPluginImages.DESC_OBJS_LOCAL_VARIABLE;
				break;
			case CompletionProposal.PACKAGE_REF:
				descriptor= JavaPluginImages.DESC_OBJS_PACKAGE;
				break;
			case CompletionProposal.KEYWORD:
			case CompletionProposal.LABEL_REF:
				descriptor= null;
				break;
			case CompletionProposal.JSDOC_METHOD_REF:
			case CompletionProposal.JSDOC_TYPE_REF:
			case CompletionProposal.JSDOC_FIELD_REF:
			case CompletionProposal.JSDOC_BLOCK_TAG:
			case CompletionProposal.JSDOC_INLINE_TAG:
			case CompletionProposal.JSDOC_PARAM_REF:
				descriptor = JavaPluginImages.DESC_OBJS_JAVADOCTAG;
				break;
			default:
				descriptor= null;
				Assert.isTrue(false);
		}

		if (descriptor == null)
			return null;
		return decorateImageDescriptor(descriptor, proposal);
	}

	ImageDescriptor createMethodImageDescriptor(CompletionProposal proposal) {
//		char[] compUnit = proposal.getDeclarationTypeName();
//		char[] propType = proposal.getName();
//		IJavaScriptProject project = proposal.getJavaProject();
		//if (compUnit!=null && compUnit.length>0)
//		{
//			IJsGlobalScopeContainerInitializerExtension init = JSDScopeUiUtil.findLibraryUiInitializer(new Path(new String(compUnit)),project);
//			if(init!=null) {
//				ImageDescriptor description = init.getImage(new Path(new String(compUnit)),new String(propType), project);
//				if( description!=null) return description;
//			}
//		}
		final int flags= proposal.getFlags();
		return decorateImageDescriptor(JavaElementImageProvider.getMethodImageDescriptor(false, flags), proposal);
	}

	ImageDescriptor createTypeImageDescriptor(CompletionProposal proposal) {
		final int flags= proposal.getFlags();
		return decorateImageDescriptor(JavaElementImageProvider.getTypeImageDescriptor(true /* in order to get all visibility decorations */, false, flags, false), proposal);
	}

	ImageDescriptor createFieldImageDescriptor(CompletionProposal proposal) {
//		char[] compUnit = proposal.getDeclarationTypeName();
//		char[] propType = proposal.getName();
//		IJavaScriptProject project = proposal.getJavaProject();
//		NameLookup lookup = proposal.getNameLookup();
//		IJavaScriptUnit[] sources = lookup.findTypeSources(new String(propType),true);
		
		
//		if (compUnit!=null && compUnit.length>0)
//		{
//			IJsGlobalScopeContainerInitializerExtension init = JSDScopeUiUtil.findLibraryUiInitializer(new Path(new String(compUnit)),project);
//			if(init!=null) {
//				ImageDescriptor description = init.getImage(new Path(new String(compUnit)),new String(propType), project);
//				if( description!=null) return description;
//			}
//		}
//		
		final int flags= proposal.getFlags();
		return decorateImageDescriptor(JavaElementImageProvider.getFieldImageDescriptor(false, flags), proposal);
	}

	ImageDescriptor createLocalImageDescriptor(CompletionProposal proposal) {
		return decorateImageDescriptor(JavaPluginImages.DESC_OBJS_LOCAL_VARIABLE, proposal);
	}

	ImageDescriptor createPackageImageDescriptor(CompletionProposal proposal) {
		return decorateImageDescriptor(JavaPluginImages.DESC_OBJS_PACKAGE, proposal);
	}

	/**
	 * Returns a version of <code>descriptor</code> decorated according to
	 * the passed <code>modifier</code> flags.
	 *
	 * @param descriptor the image descriptor to decorate
	 * @param proposal the proposal
	 * @return an image descriptor for a method proposal
	 * @see Flags
	 */
	private ImageDescriptor decorateImageDescriptor(ImageDescriptor descriptor, CompletionProposal proposal) {
		int adornments= 0;
		int flags= proposal.getFlags();
		int kind= proposal.getKind();

		if (Flags.isDeprecated(flags))
			adornments |= JavaScriptElementImageDescriptor.DEPRECATED;

		if (kind == CompletionProposal.FIELD_REF || kind == CompletionProposal.METHOD_DECLARATION || kind == CompletionProposal.METHOD_DECLARATION || kind == CompletionProposal.METHOD_NAME_REFERENCE || kind == CompletionProposal.METHOD_REF)
			if (Flags.isStatic(flags))
				adornments |= JavaScriptElementImageDescriptor.STATIC;

		if (kind == CompletionProposal.TYPE_REF && Flags.isAbstract(flags))
			adornments |= JavaScriptElementImageDescriptor.ABSTRACT;

		return new JavaScriptElementImageDescriptor(descriptor, adornments, JavaElementImageProvider.SMALL_SIZE);
	}

	/**
	 * Sets the completion context.
	 * 
	 * @param context the completion context
	 * 
	 */
	void setContext(CompletionContext context) {
		fContext= context;
	}

}
