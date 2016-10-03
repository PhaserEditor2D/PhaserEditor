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
package org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.wst.jsdt.core.BindingKey;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTParser;
import org.eclipse.wst.jsdt.core.dom.ASTRequestor;
import org.eclipse.wst.jsdt.core.dom.IBinding;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;



public class TypeEnvironment {
	
	private static class ProjectKeyPair {
		private final IJavaScriptProject fProject;
		private final String fBindingKey;
		
		public ProjectKeyPair(IJavaScriptProject project, String bindingKey) {
			fProject= project;
			fBindingKey= bindingKey;
		}
		
		public boolean equals(Object other) {
			if (this == other)
				return true;
			if (! (other instanceof ProjectKeyPair))
				return false;
			ProjectKeyPair otherPair= (ProjectKeyPair) other;
			return fProject.equals(otherPair.fProject) && fBindingKey.equals(otherPair.fBindingKey);
		}
		
		public int hashCode() {
			return fProject.hashCode() + fBindingKey.hashCode();
		}
	}
	
	/** Type code for the primitive type "int". */
	public final PrimitiveType INT= new PrimitiveType(this, PrimitiveType.INT, BindingKey.createTypeBindingKey("int")); //$NON-NLS-1$
	/** Type code for the primitive type "char". */
	public final PrimitiveType CHAR = new PrimitiveType(this, PrimitiveType.CHAR, BindingKey.createTypeBindingKey("char")); //$NON-NLS-1$
	/** Type code for the primitive type "boolean". */
	public final PrimitiveType BOOLEAN = new PrimitiveType(this, PrimitiveType.BOOLEAN, BindingKey.createTypeBindingKey("boolean")); //$NON-NLS-1$
	/** Type code for the primitive type "short". */
	public final PrimitiveType SHORT = new PrimitiveType(this, PrimitiveType.SHORT, BindingKey.createTypeBindingKey("short")); //$NON-NLS-1$
	/** Type code for the primitive type "long". */
	public final PrimitiveType LONG = new PrimitiveType(this, PrimitiveType.LONG, BindingKey.createTypeBindingKey("long")); //$NON-NLS-1$
	/** Type code for the primitive type "float". */
	public final PrimitiveType FLOAT = new PrimitiveType(this, PrimitiveType.FLOAT, BindingKey.createTypeBindingKey("float")); //$NON-NLS-1$
	/** Type code for the primitive type "double". */
	public final PrimitiveType DOUBLE = new PrimitiveType(this, PrimitiveType.DOUBLE, BindingKey.createTypeBindingKey("double")); //$NON-NLS-1$
	/** Type code for the primitive type "byte". */
	public final PrimitiveType BYTE = new PrimitiveType(this, PrimitiveType.BYTE, BindingKey.createTypeBindingKey("byte")); //$NON-NLS-1$
	
	/** Type code for the primitive type "null". */
	public final NullType NULL= new NullType(this);
	
	public final VoidType VOID= new VoidType(this); 
	
	final PrimitiveType[] PRIMITIVE_TYPES= {INT, CHAR, BOOLEAN, SHORT, LONG, FLOAT, DOUBLE, BYTE};
	
	private static final String[] BOXED_PRIMITIVE_NAMES= new String[] {
		"java.lang.Integer",  //$NON-NLS-1$
		"java.lang.Character",  //$NON-NLS-1$
		"java.lang.Boolean",  //$NON-NLS-1$
		"java.lang.Short",  //$NON-NLS-1$
		"java.lang.Long",  //$NON-NLS-1$
		"java.lang.Float",  //$NON-NLS-1$
		"java.lang.Double",  //$NON-NLS-1$
		"java.lang.Byte"};  //$NON-NLS-1$
	
	private TType OBJECT_TYPE= null;
	
	private Map/*<TType, ArrayType>*/[]          fArrayTypes= new Map[] { new HashMap() };
	private Map/*<IJavaScriptElement, StandardType>*/  fStandardTypes= new HashMap();
	private Map/*<IJavaScriptElement, GenericType>*/   fGenericTypes= new HashMap();
	private Map/*<ProjectKeyPair, ParameterizedType>*/ fParameterizedTypes= new HashMap();
	private Map/*<IJavaScriptElement, RawType>*/       fRawTypes= new HashMap();
	private Map/*<ProjectKeyPair, CaptureType>*/ fCaptureTypes= new HashMap();
	private UnboundWildcardType fUnboundWildcardType= null;
	
	private static final int MAX_ENTRIES= 1024;
	private Map/*<TypeTuple, Boolean>*/ fSubTypeCache= new LinkedHashMap(50, 0.75f, true) {
		private static final long serialVersionUID= 1L;
		protected boolean removeEldestEntry(Map.Entry eldest) {
			return size() > MAX_ENTRIES;
		}
	};
	
	/**
	 * Map from TType to its known subtypes, or <code>null</code> iff subtype
	 * information was not requested in the constructor.
	 */
	private Map/*<TType, List<TType>>*/ fSubTypes;
	
	public static ITypeBinding[] createTypeBindings(TType[] types, IJavaScriptProject project) {
		final Map mapping= new HashMap();
		List keys= new ArrayList();
		for (int i= 0; i < types.length; i++) {
			TType type= types[i];
			String bindingKey= type.getBindingKey();
			mapping.put(bindingKey, type);
			keys.add(bindingKey);
		}
		ASTParser parser= ASTParser.newParser(AST.JLS3);
		parser.setProject(project);
		parser.setResolveBindings(true);
		parser.createASTs(new IJavaScriptUnit[0], (String[])keys.toArray(new String[keys.size()]), 
			new ASTRequestor() {
				public void acceptBinding(String bindingKey, IBinding binding) {
					mapping.put(bindingKey, binding);
				}
			}, null);
		ITypeBinding[] result= new ITypeBinding[types.length];
		for (int i= 0; i < types.length; i++) {
			TType type= types[i];
			String bindingKey= type.getBindingKey();
			Object value= mapping.get(bindingKey);
			if (value instanceof ITypeBinding) {
				result[i]= (ITypeBinding)value;
			}
		}
		return result;
	}
	
	public TypeEnvironment() {
		this(false);
	}
	
	public TypeEnvironment(boolean rememberSubtypes) {
		if (rememberSubtypes) {
			fSubTypes= new HashMap();
		}
	}
	
	Map/*<TypeTuple, Boolean>*/ getSubTypeCache() {
		return fSubTypeCache;
	}
	
	public TType create(ITypeBinding binding) {
		if (binding.isPrimitive()) {
			return createPrimitiveType(binding);
		} else if (binding.isArray()) {
			return createArrayType(binding);
		}
		if ("null".equals(binding.getName())) //$NON-NLS-1$
			return NULL;
		return createStandardType(binding);
	}
	
	public TType[] create(ITypeBinding[] bindings) {
		TType[] result= new TType[bindings.length];
		for (int i= 0; i < bindings.length; i++) {
			result[i]= create(bindings[i]);
		}
		return result;
	}
	
	/**
	 * Returns the TType for java.lang.Object.
	 * <p>
	 * Warning: currently returns <code>null</code> unless this type environment
	 * has already created its first hierarchy type.
	 * 
	 * @return the TType for java.lang.Object
	 */
	public TType getJavaLangObject() {
		return OBJECT_TYPE;
	}
	
	void initializeJavaLangObject(ITypeBinding object) {
		if (OBJECT_TYPE != null)
			return;
		
		TType objectType= createStandardType(object);
		Assert.isTrue(objectType.isJavaLangObject());
	}
	
	PrimitiveType createUnBoxed(StandardType type) {
		String name= type.getPlainPrettySignature();
		for (int i= 0; i < BOXED_PRIMITIVE_NAMES.length; i++) {
			if (BOXED_PRIMITIVE_NAMES[i].equals(name))
				return PRIMITIVE_TYPES[i];
		}
		return null;
	}
	
	StandardType createBoxed(PrimitiveType type, IJavaScriptProject focus) {
		String fullyQualifiedName= BOXED_PRIMITIVE_NAMES[type.getId()];
		try {
			IType javaElementType= focus.findType(fullyQualifiedName);
			StandardType result= (StandardType)fStandardTypes.get(javaElementType);
			if (result != null)
				return result;
			ASTParser parser= ASTParser.newParser(AST.JLS3);
			parser.setProject(focus);
			IBinding[] bindings= parser.createBindings(new IJavaScriptElement[] {javaElementType} , null);
			return createStandardType((ITypeBinding)bindings[0]);
		} catch (JavaScriptModelException e) {
			// fall through
		}
		return null;
	}
	
	Map/*<TType, List<TType>>*/ getSubTypes() {
		return fSubTypes;
	}
	
	private void cacheSubType(TType supertype, TType result) {
		if (fSubTypes == null)
			return;
		if (supertype == null)
			supertype= OBJECT_TYPE;
		
		ArrayList subtypes= (ArrayList) fSubTypes.get(supertype);
		if (subtypes == null) {
			subtypes= new ArrayList(5);
			fSubTypes.put(supertype, subtypes);
		} else {
			Assert.isTrue(! subtypes.contains(result));
		}
		subtypes.add(result);
	}

	private void cacheSubTypes(TType[] interfaces, TType result) {
		for (int i= 0; i < interfaces.length; i++) {
			cacheSubType(interfaces[i], result);
		}
	}

	private TType createPrimitiveType(ITypeBinding binding) {
		String name= binding.getName();
		String[] names= PrimitiveType.NAMES;
		for (int i= 0; i < names.length; i++) {
			if (name.equals(names[i])) {
				return PRIMITIVE_TYPES[i];
			}
		}
		Assert.isTrue(false, "Primitive type " + name + "unkown");  //$NON-NLS-1$//$NON-NLS-2$
		return null;
	}

	private ArrayType createArrayType(ITypeBinding binding) {
		int index= binding.getDimensions() - 1;
		TType elementType= create(binding.getElementType());
		Map/*<TType, ArrayType>*/ arrayTypes= getArrayTypesMap(index);
		ArrayType result= (ArrayType)arrayTypes.get(elementType);
		if (result != null)
			return result;
		result= new ArrayType(this);
		arrayTypes.put(elementType, result);
		result.initialize(binding, elementType);
		return result;
	}
	
	public ArrayType createArrayType(TType elementType, int dimensions) {
		Assert.isTrue(! elementType.isArrayType());
		Assert.isTrue(! elementType.isAnonymous());
		Assert.isTrue(dimensions > 0);
		
		int index= dimensions - 1;
		Map arrayTypes= getArrayTypesMap(index);
		ArrayType result= (ArrayType)arrayTypes.get(elementType);
		if (result != null)
			return result;
		result= new ArrayType(this, BindingKey.createArrayTypeBindingKey(elementType.getBindingKey(), dimensions));
		arrayTypes.put(elementType, result);
		result.initialize(elementType, dimensions);
		return result;
	}

	private Map/*<TType, ArrayType>*/ getArrayTypesMap(int index) {
		int oldLength= fArrayTypes.length;
		if (index >= oldLength) {
			Map[] newArray= new Map[index + 1];
			System.arraycopy(fArrayTypes, 0, newArray, 0, oldLength);
			fArrayTypes= newArray;
		}
		Map arrayTypes= fArrayTypes[index];
		if (arrayTypes == null) {
			arrayTypes= new HashMap();
			fArrayTypes[index]= arrayTypes;
		}
		return arrayTypes;
	}
	
	private StandardType createStandardType(ITypeBinding binding) {
		IJavaScriptElement javaElement= binding.getJavaElement();
		StandardType result= (StandardType)fStandardTypes.get(javaElement);
		if (result != null)
			return result;
		result= new StandardType(this);
		fStandardTypes.put(javaElement, result);
		result.initialize(binding, (IType)javaElement);
		if (OBJECT_TYPE == null && result.isJavaLangObject())
			OBJECT_TYPE= result;
		return result;
	}
	
	private GenericType createGenericType(ITypeBinding binding) {
		IJavaScriptElement javaElement= binding.getJavaElement();
		GenericType result= (GenericType)fGenericTypes.get(javaElement);
		if (result != null)
			return result;
		result= new GenericType(this);
		fGenericTypes.put(javaElement, result);
		result.initialize(binding, (IType)javaElement);
		cacheSubType(result.getSuperclass(), result);
		cacheSubTypes(result.getInterfaces(), result);
		return result;
	}
	
	private RawType createRawType(ITypeBinding binding) {
		IJavaScriptElement javaElement= binding.getJavaElement();
		RawType result= (RawType)fRawTypes.get(javaElement);
		if (result != null)
			return result;
		result= new RawType(this);
		fRawTypes.put(javaElement, result);
		result.initialize(binding, (IType)javaElement);
		cacheSubType(result.getSuperclass(), result);
		cacheSubTypes(result.getInterfaces(), result);
		return result;
	}
	
	private TType createUnboundWildcardType(ITypeBinding binding) {
		if (fUnboundWildcardType == null) {
			fUnboundWildcardType= new UnboundWildcardType(this);
			fUnboundWildcardType.initialize(binding);
		}
		return fUnboundWildcardType;
	}	
	
	private CaptureType createCaptureType(ITypeBinding binding) {
		IJavaScriptProject javaProject= binding.getDeclaringClass().getJavaElement().getJavaScriptProject();
		String bindingKey= binding.getKey();
		ProjectKeyPair pair= new ProjectKeyPair(javaProject, bindingKey);
		CaptureType result= (CaptureType)fCaptureTypes.get(pair);
		if (result != null)
			return result;
		result= new CaptureType(this);
		fCaptureTypes.put(pair, result);
		result.initialize(binding, javaProject);
		return result;
	}
}
