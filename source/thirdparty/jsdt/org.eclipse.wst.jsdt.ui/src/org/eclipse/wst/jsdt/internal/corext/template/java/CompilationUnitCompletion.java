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
package org.eclipse.wst.jsdt.internal.corext.template.java;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.jsdt.core.CompletionProposal;
import org.eclipse.wst.jsdt.core.CompletionRequestor;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.ITypeHierarchy;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.Signature;
import org.eclipse.wst.jsdt.core.compiler.IProblem;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;

/**
 * A completion requester to collect informations on local variables.
 * This class is used for guessing variable names like arrays, collections, etc.
 */
final class CompilationUnitCompletion extends CompletionRequestor {

	/**
	 * Describes a local variable (including parameters) inside the method where
	 * code completion was invoked. Special predicates exist to query whether
	 * a variable can be iterated over. 
	 */
	public final class Variable {
		private static final int UNKNOWN= 0, NONE= 0;
		private static final int ARRAY= 1;
		private static final int COLLECTION= 2;
		private static final int ITERABLE= 4;
		
		/**
		 * The name of the local variable.
		 */
		private final String name;
		
		/**
		 * The signature of the local variable's type.
		 */
		private final String signature;
		
		/* lazily computed properties */
		private int fType= UNKNOWN;
		private int fChecked= NONE;
		private String[] fMemberTypes;
		
		private Variable(String name, String signature) {
			this.name= name;
			this.signature= signature;
		}

		/**
		 * Returns the name of the variable.
		 * 
		 * @return the name of the variable
		 */
		public String getName() {
			return name;
		}
		
		/**
		 * Returns <code>true</code> if the type of the local variable is an
		 * array type.
		 * 
		 * @return <code>true</code> if the receiver's type is an array,
		 *         <code>false</code> if not
		 */
		public boolean isArray() {
			
			try
			{
				if (fType == UNKNOWN && (fChecked & ARRAY) == 0 && Signature.getTypeSignatureKind(signature) == Signature.ARRAY_TYPE_SIGNATURE)
					fType= ARRAY;
			} catch(IllegalArgumentException iae) {/* Ignore bad signature: assume not array */}
			fChecked |= ARRAY;
			return fType == ARRAY;
		}

		/**
		 * Returns <code>true</code> if the receiver's type is a subclass of
		 * <code>java.util.Collection</code>, <code>false</code> otherwise.
		 * 
		 * @return <code>true</code> if the receiver's type is a subclass of
		 *         <code>java.util.Collection</code>, <code>false</code>
		 *         otherwise
		 */
		public boolean isCollection() {
			// Collection extends Iterable
			if ((fType == UNKNOWN || fType == ITERABLE) && (fChecked & COLLECTION) == 0 && isSubtypeOf("java.util.Collection")) //$NON-NLS-1$
				fType= COLLECTION;
			fChecked |= COLLECTION;
			return fType == COLLECTION;
		}
		
		/**
		 * Returns <code>true</code> if the receiver's type is a subclass of
		 * <code>java.lang.Iterable</code>, <code>false</code> otherwise.
		 * 
		 * @return <code>true</code> if the receiver's type is a subclass of
		 *         <code>java.lang.Iterable</code>, <code>false</code>
		 *         otherwise
		 */
		public boolean isIterable() {
			if (fType == UNKNOWN && (fChecked & ITERABLE) == 0 && isSubtypeOf("java.lang.Iterable")) //$NON-NLS-1$
				fType= ITERABLE;
			fChecked |= ITERABLE;
			return fType == ITERABLE || fType == COLLECTION; // Collection extends Iterable
		}

		/**
		 * Returns <code>true</code> if the receiver's type is an implementor
		 * of <code>interfaceName</code>.
		 * 
		 * @param supertype the fully qualified name of the interface
		 * @return <code>true</code> if the receiver's type implements the
		 *         type named <code>interfaceName</code>
		 */
		private boolean isSubtypeOf(String supertype) {
			String implementorName= SignatureUtil.stripSignatureToFQN(signature);
			if (implementorName.length() == 0)
				return false;
			
			boolean qualified= supertype.indexOf('.') != -1;
			
			// try cheap test first
			if (implementorName.equals(supertype) || !qualified && Signature.getSimpleName(implementorName).equals(supertype))
				return true;

			if (fUnit == null)
				return false;

			IJavaScriptProject project= fUnit.getJavaScriptProject();

			try {
				IType sub= project.findType(implementorName);
				if (sub == null)
					return false;
				
				if (qualified) {
					IType sup= project.findType(supertype);
					if (sup == null)
						return false;
					ITypeHierarchy hierarchy= sub.newSupertypeHierarchy(null);
					return hierarchy.contains(sup);
				} else {
					ITypeHierarchy hierarchy= sub.newSupertypeHierarchy(null);
					IType[] allTypes= hierarchy.getAllClasses();
					for (int i= 0; i < allTypes.length; i++) {
						IType type= allTypes[i];
						if (type.getElementName().equals(supertype))
							return true;
					}
				}

			} catch (JavaScriptModelException e) {
				// ignore and return false
			}			
			
			return false;
		}
		
		private IType[] getSupertypes(String supertype) {
			IType[] empty= new IType[0];
			String implementorName= SignatureUtil.stripSignatureToFQN(signature);
			if (implementorName.length() == 0)
				return empty;
			
			boolean qualified= supertype.indexOf('.') != -1;

			if (fUnit == null)
				return empty;
			
			IJavaScriptProject project= fUnit.getJavaScriptProject();
			
			try {
				IType sub= project.findType(implementorName);
				if (sub == null)
					return empty;
				
				if (qualified) {
					IType sup= project.findType(supertype);
					if (sup == null)
						return empty;
					return new IType[] {sup};
				} else {
					ITypeHierarchy hierarchy= sub.newSupertypeHierarchy(null);
					IType[] allTypes= hierarchy.getAllClasses();
					List matches= new ArrayList();
					for (int i= 0; i < allTypes.length; i++) {
						IType type= allTypes[i];
						if (type.getElementName().equals(supertype))
							matches.add(type);
					}
					return (IType[]) matches.toArray(new IType[matches.size()]);
				}
				
			} catch (JavaScriptModelException e) {
				// ignore and return false
			}			
			
			return empty;
		}

		/**
		 * Returns the signature of the member type.
		 * 
		 * @return the signature of the member type
		 */
		public String getMemberTypeSignature() {
			return getMemberTypeSignatures()[0];
		}
		
		/**
		 * Returns the signatures of all member type bounds.
		 * 
		 * @return the signatures of all member type bounds
		 */
		public String[] getMemberTypeSignatures() {
			if (isArray()) {
				return new String[] {Signature.createArraySignature(Signature.getElementType(signature), Signature.getArrayCount(signature) - 1)};
			} else if (fUnit != null && (isIterable() || isCollection())) {
				if (fMemberTypes == null) {
					try {
						try {
							TypeParameterResolver util= new TypeParameterResolver(this);
							fMemberTypes= util.computeBinding("java.lang.Iterable", 0); //$NON-NLS-1$
						} catch (JavaScriptModelException e) {
							try {
								TypeParameterResolver util= new TypeParameterResolver(this);
								fMemberTypes= util.computeBinding("java.util.Collection", 0); //$NON-NLS-1$
							} catch (JavaScriptModelException x) {
								fMemberTypes= new String[0];
							}
						}
					} catch (IndexOutOfBoundsException e) {
						fMemberTypes= new String[0];
					}
				}
				if (fMemberTypes.length > 0)
					return fMemberTypes;
			}
			return new String[] {Signature.createTypeSignature("java.lang.Object", true)}; //$NON-NLS-1$
		}
		
		/**
		 * Returns the type names of all member type bounds, as they would be 
		 * appear when referenced in the current compilation unit.
		 * 
		 * @return type names of all member type bounds
		 */
		public String[] getMemberTypeNames() {
			String[] signatures= getMemberTypeSignatures();
			String[] names= new String[signatures.length];
			
			for (int i= 0; i < signatures.length; i++) {
				String sig= signatures[i];
				String local= (String) fLocalTypes.get(Signature.getElementType(sig));
				int dim= Signature.getArrayCount(sig);
				if (local != null && dim > 0) {
					StringBuffer array= new StringBuffer(local);
					for (int j= 0; j < dim; j++)
						array.append("[]"); //$NON-NLS-1$
					local= array.toString();
				}
				if (local != null)
					names[i]= local;
				else
					names[i]= Signature.getSimpleName(Signature.getSignatureSimpleName(sig));
			}
			return names;
		}

		/**
		 * Returns the type arguments of the declared type of the variable. Returns
		 * an empty array if it is not a parameterized type.
		 * 
		 * @param type the fully qualified type name of which to match a type argument  
		 * @param index the index of the type parameter in the type
		 * @return the type bounds for the specified type argument in this local variable
		 * 
		 */
		public String[] getTypeArgumentBoundSignatures(String type, int index) {
			List all= new ArrayList();
			IType[] supertypes= getSupertypes(type);
			if (fUnit != null) {
				for (int i= 0; i < supertypes.length; i++) {
					try {
						TypeParameterResolver util= new TypeParameterResolver(this);
						String[] result= util.computeBinding(supertypes[i].getFullyQualifiedName(), index);
						all.addAll(Arrays.asList(result));
					} catch (JavaScriptModelException e) {
					} catch (IndexOutOfBoundsException e) {
					}
				}
			}
			if (all.isEmpty())
				return new String[] {Signature.createTypeSignature("java.lang.Object", true)}; //$NON-NLS-1$
			return (String[]) all.toArray(new String[all.size()]);
		}

		/*
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			String type;
			switch (fType) {
				case ITERABLE:
					type= "ITERABLE"; //$NON-NLS-1$
					break;
				case COLLECTION:
					type= "COLLECTION"; //$NON-NLS-1$
					break;
				case ARRAY:
					type= "ARRAY"; //$NON-NLS-1$
					break;
				default:
					type= "UNKNOWN"; //$NON-NLS-1$
					break;
			}
			return "LocalVariable [name=\"" + name + "\" signature=\"" + signature + "\" type=\"" + type + "\" member=\"" + getMemberTypeSignature() + "\"]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		}
	}
	
	/**
	 * Given a java type, a resolver computes the bounds of type variables
	 * declared in a super type, considering any type constraints along the
	 * inheritance path.
	 */
	private final class TypeParameterResolver {
		private static final String OBJECT_SIGNATURE= "Ljava.lang.Object;"; //$NON-NLS-1$
		
		private final ITypeHierarchy fHierarchy;
		private final Variable fVariable;
		private final IType fType;
		private final List fBounds= new ArrayList();

		/**
		 * Creates a new type parameter resolver to compute the bindings of type
		 * parameters for the declared type of <code>variable</code>. For any
		 * super type of the type of <code>variable</code>, calling
		 * {@link #computeBinding(IType, int) computeBinding} will find the type
		 * bounds of type variables in the super type, considering any type
		 * constraints along the inheritance path.
		 * 
		 * @param variable the local variable under investigation
		 * @throws JavaScriptModelException if the type of <code>variable</code>
		 *         cannot be found
		 */
		public TypeParameterResolver(Variable variable) throws JavaScriptModelException {
			String typeName= SignatureUtil.stripSignatureToFQN(variable.signature);
			IJavaScriptProject project= fUnit.getJavaScriptProject();
			fType= project.findType(typeName);
			fHierarchy= fType.newSupertypeHierarchy(null);
			fVariable= variable;
		}
		
		/**
		 * Given a type parameter of <code>superType</code> at position
		 * <code>index</code>, this method computes and returns the (lower)
		 * type bound(s) of that parameter for an instance of <code>fType</code>.
		 * <p>
		 * <code>superType</code> must be a super type of <code>fType</code>,
		 * and <code>superType</code> must have at least
		 * <code>index + 1</code> type parameters.
		 * </p>
		 * 
		 * @param superType the qualified type name of the super type to compute
		 *        the type parameter binding for
		 * @param index the index into the list of type parameters of
		 *        <code>superType</code>
		 * @throws JavaScriptModelException if any java model operation fails
		 * @throws IndexOutOfBoundsException if the index is not valid
		 */
		public String[] computeBinding(String superType, int index) throws JavaScriptModelException, IndexOutOfBoundsException {
			IJavaScriptProject project= fUnit.getJavaScriptProject();
			IType type= project.findType(superType);
			if (type == null)
				throw new JavaScriptModelException(new CoreException(new Status(IStatus.ERROR, JavaScriptPlugin.getPluginId(), IStatus.OK, "No such type", null))); //$NON-NLS-1$
			return computeBinding(type, index);
		}

		/**
		 * Given a type parameter of <code>superType</code> at position
		 * <code>index</code>, this method computes and returns the (lower)
		 * type bound(s) of that parameter for an instance of <code>fType</code>.
		 * <p>
		 * <code>superType</code> must be a super type of <code>fType</code>,
		 * and <code>superType</code> must have at least
		 * <code>index + 1</code> type parameters.
		 * </p>
		 * 
		 * @param superType the super type to compute the type parameter binding
		 *        for
		 * @param index the index into the list of type parameters of
		 *        <code>superType</code>
		 * @throws JavaScriptModelException if any java model operation fails
		 * @throws IndexOutOfBoundsException if the index is not valid
		 */
		public String[] computeBinding(IType superType, int index) throws JavaScriptModelException, IndexOutOfBoundsException {
			initBounds();
			return (String[]) fBounds.toArray(new String[fBounds.size()]);
		}
		
		/**
		 * Clears the collected type bounds and initializes it with
		 * <code>java.lang.Object</code>.
		 */
		private void initBounds() {
			fBounds.clear();
			fBounds.add(OBJECT_SIGNATURE); 
		}

		/**
		 * Returns <code>true</code> if <code>subTypeSignature</code>
		 * describes a type which is a true sub type of the type described by
		 * <code>superTypeSignature</code>.
		 * 
		 * @param subTypeSignature the potential subtype's signature
		 * @param superTypeSignature the potential supertype's signature
		 * @return <code>true</code> if the inheritance relationship holds
		 */
		private boolean isTrueSubtypeOf(String subTypeSignature, String superTypeSignature) {
			// try cheap test first
			if (subTypeSignature.equals(superTypeSignature))
				return true;
			
			if (SignatureUtil.isJavaLangObject(subTypeSignature))
				return false; // Object has no super types
			
			if (Signature.getTypeSignatureKind(subTypeSignature) != Signature.BASE_TYPE_SIGNATURE && SignatureUtil.isJavaLangObject(superTypeSignature)) 
				return true;
			
			IJavaScriptProject project= fUnit.getJavaScriptProject();
			
			try {
				
				if ((Signature.getTypeSignatureKind(subTypeSignature) & Signature.CLASS_TYPE_SIGNATURE) == 0)
					return false;
				IType subType= project.findType(SignatureUtil.stripSignatureToFQN(subTypeSignature));
				if (subType == null)
					return false;
				
				if ((Signature.getTypeSignatureKind(superTypeSignature) & Signature.CLASS_TYPE_SIGNATURE) == 0)
					return false;
				IType superType= project.findType(SignatureUtil.stripSignatureToFQN(superTypeSignature));
				if (superType == null)
					return false;
				
				ITypeHierarchy hierarchy= subType.newSupertypeHierarchy(null);
				IType[] types= hierarchy.getAllSuperclasses(subType);
				
				for (int i= 0; i < types.length; i++)
					if (types[i].equals(superType))
						return true;
			} catch (JavaScriptModelException e) {
				// ignore and return false
			}			
			
			return false;
		}
		
		/**
		 * Returns <code>true</code> if <code>signature</code> is a concrete type signature,
		 * <code>false</code> if it is a type variable.
		 * 
		 * @param signature the signature to check
		 * @param context the context inside which to resolve the type
		 * @throws JavaScriptModelException if finding the type fails
		 */
		private boolean isConcreteType(String signature, IType context) throws JavaScriptModelException {
			// try and resolve otherwise
			if (context.isBinary()) {
				return fUnit.getJavaScriptProject().findType(SignatureUtil.stripSignatureToFQN(signature)) != null;
			} else {
				return context.resolveType(SignatureUtil.stripSignatureToFQN(signature)) != null;
			}
		}
	}
	
	private IJavaScriptUnit fUnit;

	private List fLocalVariables= new ArrayList();
	private List fFields= new ArrayList();
	private Map fLocalTypes= new HashMap();

	private boolean fError;

	/**
	 * Creates a compilation unit completion.
	 * 
	 * @param unit the compilation unit, may be <code>null</code>.
	 */
	CompilationUnitCompletion(IJavaScriptUnit unit) {
		reset(unit);
		setIgnored(CompletionProposal.ANONYMOUS_CLASS_DECLARATION, true);
		setIgnored(CompletionProposal.KEYWORD, true);
		setIgnored(CompletionProposal.LABEL_REF, true);
		setIgnored(CompletionProposal.METHOD_DECLARATION, true);
		setIgnored(CompletionProposal.METHOD_NAME_REFERENCE, true);
		setIgnored(CompletionProposal.METHOD_REF, true);
		setIgnored(CompletionProposal.PACKAGE_REF, true);
		setIgnored(CompletionProposal.POTENTIAL_METHOD_DECLARATION, true);
		setIgnored(CompletionProposal.VARIABLE_DECLARATION, true);
		setIgnored(CompletionProposal.TYPE_REF, true);
	}
	
	/**
	 * Resets the completion requester.
	 * 
	 * @param unit the compilation unit, may be <code>null</code>.
	 */
	private void reset(IJavaScriptUnit unit) {
		fUnit= unit;
		fLocalVariables.clear();
		fFields.clear();
		fLocalTypes.clear();
		
		if (fUnit != null) {
			try {
				IType[] cuTypes= fUnit.getAllTypes();
				for (int i= 0; i < cuTypes.length; i++) {
					String fqn= cuTypes[i].getFullyQualifiedName();
					String sig= Signature.createTypeSignature(fqn, true);
					fLocalTypes.put(sig, cuTypes[i].getElementName());
				}
			} catch (JavaScriptModelException e) {
				// ignore
			}
		}
		fError= false;
	}

	/*
	 * @see org.eclipse.wst.jsdt.core.CompletionRequestor#accept(org.eclipse.wst.jsdt.core.CompletionProposal)
	 */
	public void accept(CompletionProposal proposal) {
		
		String name= String.valueOf(proposal.getCompletion());
		String signature = ( proposal.getSignature() != null ? String.valueOf( proposal.getSignature() ) : "" ); 
		
		switch (proposal.getKind()) {
			
			case CompletionProposal.LOCAL_VARIABLE_REF:
				// collect local variables
				fLocalVariables.add(new Variable(name, signature));
				break;
			case CompletionProposal.FIELD_REF:
				// collect local variables
				fFields.add(new Variable(name, signature));
				break;
				
			default:
				break;
		}
	}
	
	/*
	 * @see org.eclipse.wst.jsdt.core.CompletionRequestor#completionFailure(org.eclipse.wst.jsdt.core.compiler.IProblem)
	 */
	public void completionFailure(IProblem problem) {
		fError= true;
	}

	/**
	 * Tests if the code completion process produced errors.
	 * 
	 * @return <code>true</code> if there are errors, <code>false</code>
	 *         otherwise
	 */
	public boolean hasErrors() {
		return fError;
	}

	/**
	 * Returns all local variable names.
	 * 
	 * @return all local variable names
	 */
	public String[] getLocalVariableNames() {
		String[] names= new String[fLocalVariables.size()];
		int i= 0;
		for (ListIterator iterator= fLocalVariables.listIterator(fLocalVariables.size()); iterator.hasPrevious();) {
			Variable localVariable= (Variable) iterator.previous();
			names[i++]= localVariable.getName();
		}		
		return names;
	}	
	
	/**
	 * Returns all field names.
	 * 
	 * @return all field names
	 * 
	 */
	public String[] getFieldNames() {
		String[] names= new String[fFields.size()];
		int i= 0;
		for (ListIterator iterator= fFields.listIterator(fFields.size()); iterator.hasPrevious();) {
			Variable field= (Variable)iterator.previous();
			names[i++]= field.getName();
		}		
		return names;
	}	

	/**
	 * Returns all local arrays in the order that they appear.
	 * 
	 * @return all local arrays
	 */
	public Variable[] findLocalArrays() {
		List arrays= new ArrayList();

		for (ListIterator iterator= fLocalVariables.listIterator(fLocalVariables.size()); iterator.hasPrevious();) {
			Variable localVariable= (Variable) iterator.previous();

			if (localVariable.isArray())
				arrays.add(localVariable);
		}

		return (Variable[]) arrays.toArray(new Variable[arrays.size()]);
	}
	
	/**
	 * Returns all local variables implementing or extending
	 * <code>clazz</code> in the order that they appear.
	 * 
	 * @param clazz the fully qualified type name of the class to match
	 * @return all local variables matching <code>clazz</code>
	 */
	public Variable[] findLocalVariables(String clazz) {
		List matches= new ArrayList();
		
		for (ListIterator iterator= fLocalVariables.listIterator(fLocalVariables.size()); iterator.hasPrevious();) {
			Variable localVariable= (Variable) iterator.previous();
			
			if (localVariable.isSubtypeOf(clazz))
				matches.add(localVariable);
		}
		
		return (Variable[]) matches.toArray(new Variable[matches.size()]);
	}
	
	/**
	 * Returns all local variables implementing or extending
	 * <code>clazz</code> in the order that they appear.
	 * 
	 * @param clazz the fully qualified type name of the class to match
	 * @return all local variables matching <code>clazz</code>
	 */
	public Variable[] findFieldVariables(String clazz) {
		List matches= new ArrayList();
		
		for (ListIterator iterator= fFields.listIterator(fFields.size()); iterator.hasPrevious();) {
			Variable localVariable= (Variable)iterator.previous();
			
			if (localVariable.isSubtypeOf(clazz))
				matches.add(localVariable);
		}
		
		return (Variable[]) matches.toArray(new Variable[matches.size()]);
	}

	/**
	 * Returns all local variables implementing <code>java.lang.Iterable</code>
	 * <em>and</em> all local arrays, in the order that they appear. That is, 
	 * the returned variables can be used within the <code>foreach</code> 
	 * language construct.
	 * 
	 * @return all local <code>Iterable</code>s and arrays
	 */
	public Variable[] findLocalIterables() {
		List iterables= new ArrayList();

		for (ListIterator iterator= fLocalVariables.listIterator(fLocalVariables.size()); iterator.hasPrevious();) {
			Variable localVariable= (Variable) iterator.previous();

			if (localVariable.isArray() || localVariable.isIterable())			
				iterables.add(localVariable);
		}

		return (Variable[]) iterables.toArray(new Variable[iterables.size()]);
	}

}

