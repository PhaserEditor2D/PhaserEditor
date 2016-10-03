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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.wst.jsdt.core.Flags;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.ISourceRange;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.WorkingCopyOwner;
import org.eclipse.wst.jsdt.core.compiler.IProblem;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ASTParser;
import org.eclipse.wst.jsdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.wst.jsdt.core.dom.ArrayType;
import org.eclipse.wst.jsdt.core.dom.Block;
import org.eclipse.wst.jsdt.core.dom.BodyDeclaration;
import org.eclipse.wst.jsdt.core.dom.ClassInstanceCreation;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.IExtendedModifier;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.ImportDeclaration;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.Modifier;
import org.eclipse.wst.jsdt.core.dom.Name;
import org.eclipse.wst.jsdt.core.dom.PackageDeclaration;
import org.eclipse.wst.jsdt.core.dom.PrimitiveType;
import org.eclipse.wst.jsdt.core.dom.QualifiedName;
import org.eclipse.wst.jsdt.core.dom.QualifiedType;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.SingleVariableDeclaration;
import org.eclipse.wst.jsdt.core.dom.Type;
import org.eclipse.wst.jsdt.core.dom.TypeDeclaration;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchConstants;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchScope;
import org.eclipse.wst.jsdt.core.search.SearchEngine;
import org.eclipse.wst.jsdt.core.search.SearchPattern;
import org.eclipse.wst.jsdt.core.search.TypeNameMatch;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTFlattener;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.corext.dom.HierarchicalASTVisitor;
import org.eclipse.wst.jsdt.internal.corext.dom.NodeFinder;
import org.eclipse.wst.jsdt.internal.corext.dom.Selection;
import org.eclipse.wst.jsdt.internal.corext.dom.SelectionAnalyzer;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.RefactoringFileBuffers;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.corext.util.TypeNameMatchCollector;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.refactoring.contentassist.JavaTypeCompletionProcessor;

public class TypeContextChecker {

	public interface IProblemVerifier {

		boolean isError(IProblem problem, ASTNode node);

	}

	private static class MethodTypesChecker {

		private static final String METHOD_NAME= "__$$__"; //$NON-NLS-1$

		private final IFunction fMethod;
		private final StubTypeContext fStubTypeContext;
		private final List/*<ParameterInfo>*/ fParameterInfos;
		private final ReturnTypeInfo fReturnTypeInfo;

		private final IProblemVerifier fProblemVerifier;

		public MethodTypesChecker(IFunction method, StubTypeContext stubTypeContext, List/*<ParameterInfo>*/ parameterInfos, ReturnTypeInfo returnTypeInfo, IProblemVerifier problemVerifier) {
			fMethod= method;
			fStubTypeContext= stubTypeContext;
			fParameterInfos= parameterInfos;
			fReturnTypeInfo= returnTypeInfo;
			fProblemVerifier= problemVerifier;
		}
		
		public RefactoringStatus[] checkAndResolveMethodTypes() throws CoreException {
			
			/* ECMA3 no variable or return types */
			if(!JavaScriptCore.IS_ECMASCRIPT4) return new RefactoringStatus[0];
			
			RefactoringStatus[] results= new MethodTypesSyntaxChecker(fMethod, fParameterInfos, fReturnTypeInfo).checkSyntax();
			for (int i= 0; i < results.length; i++)
				if (results[i] != null && results[i].hasFatalError())
					return results;
			
			int parameterCount= fParameterInfos.size();
			String[] types= new String[parameterCount + 1];
			for (int i= 0; i < parameterCount; i++)
				types[i]= ParameterInfo.stripEllipsis(((ParameterInfo) fParameterInfos.get(i)).getNewTypeName());
			types[parameterCount]= fReturnTypeInfo.getNewTypeName();
			RefactoringStatus[] semanticsResults= new RefactoringStatus[parameterCount + 1];
			ITypeBinding[] typeBindings= resolveBindings(types, semanticsResults, true);
			
			boolean needsSecondPass= false;
			for (int i= 0; i < types.length; i++)
				if (typeBindings[i] == null || ! semanticsResults[i].isOK())
					needsSecondPass= true;
			
			RefactoringStatus[] semanticsResults2= new RefactoringStatus[parameterCount + 1];
			if (needsSecondPass)
				typeBindings= resolveBindings(types, semanticsResults2, false);
			
			for (int i= 0; i < fParameterInfos.size(); i++) {
				ParameterInfo parameterInfo= (ParameterInfo) fParameterInfos.get(i);
				if (parameterInfo.getOldTypeBinding() != null && ! parameterInfo.isTypeNameChanged()) {
					parameterInfo.setNewTypeBinding(parameterInfo.getOldTypeBinding());
				} else {
					parameterInfo.setNewTypeBinding(typeBindings[i]);
					if (typeBindings[i] == null || (needsSecondPass && ! semanticsResults2[i].isOK())) {
						if (results[i] == null)
							results[i]= semanticsResults2[i];
						else
							results[i].merge(semanticsResults2[i]);
					}
				}
			}
			fReturnTypeInfo.setNewTypeBinding(typeBindings[fParameterInfos.size()]);
			if (typeBindings[parameterCount] == null || (needsSecondPass && ! semanticsResults2[parameterCount].isOK())) {
				if (results[parameterCount] == null)
					results[parameterCount]= semanticsResults2[parameterCount];
				else
					results[parameterCount].merge(semanticsResults2[parameterCount]);
			}
			
			return results;
		}

		private ITypeBinding[] resolveBindings(String[] types, RefactoringStatus[] results, boolean firstPass) throws CoreException {
			//TODO: split types into parameterTypes and returnType
			int parameterCount= types.length - 1;
			ITypeBinding[] typeBindings= new ITypeBinding[types.length];
			
			StringBuffer cuString= new StringBuffer();
			cuString.append(fStubTypeContext.getBeforeString());
			int offsetBeforeMethodName= appendMethodDeclaration(cuString, types, parameterCount);
			cuString.append(fStubTypeContext.getAfterString());
			
			// need a working copy to tell the parser where to resolve (package visible) types
			IJavaScriptUnit wc= fMethod.getJavaScriptUnit().getWorkingCopy(new WorkingCopyOwner() {/*subclass*/}, new NullProgressMonitor());
			try {
				wc.getBuffer().setContents(cuString.toString());
				JavaScriptUnit compilationUnit= new RefactoringASTParser(AST.JLS3).parse(wc, true);
				ASTNode method= NodeFinder.perform(compilationUnit, offsetBeforeMethodName, METHOD_NAME.length()).getParent();
				Type[] typeNodes= new Type[types.length];
				if (method instanceof FunctionDeclaration) {
					FunctionDeclaration methodDeclaration= (FunctionDeclaration) method;
					typeNodes[parameterCount]= methodDeclaration.getReturnType2();
					List/*<SingleVariableDeclaration>*/ parameters= methodDeclaration.parameters();
					for (int i= 0; i < parameterCount; i++)
						typeNodes[i]= ((SingleVariableDeclaration) parameters.get(i)).getType();

				}

				for (int i= 0; i < types.length; i++) {
					Type type= typeNodes[i];
					if (type == null) {
						String msg= Messages.format(RefactoringCoreMessages.TypeContextChecker_couldNotResolveType, types[i]); 
						results[i]= RefactoringStatus.createErrorStatus(msg);
						continue;
					}
					results[i]= new RefactoringStatus();
					IProblem[] problems= ASTNodes.getProblems(type, ASTNodes.NODE_ONLY, ASTNodes.PROBLEMS);
					if (problems.length > 0) {
						for (int p= 0; p < problems.length; p++)
							if (isError(problems[p], type))
								results[i].addError(problems[p].getMessage());
					}
					typeBindings[i]= type.resolveBinding();
					typeBindings[i]= handleBug84585(typeBindings[i]);
					if (firstPass && typeBindings[i] == null)
						types[i]= qualifyTypes(type, results[i]);
				}
				return typeBindings;
			} finally {
				wc.discardWorkingCopy();
			}
		}

		private boolean isError(IProblem problem, Type type) {
			if (fProblemVerifier != null)
				return fProblemVerifier.isError(problem, type);
			return true;
		}

		private int appendMethodDeclaration(StringBuffer cuString, String[] types, int parameterCount) throws JavaScriptModelException {
			if (Flags.isStatic(fMethod.getFlags()))
				cuString.append("static "); //$NON-NLS-1$
			
			cuString.append(types[parameterCount]).append(' ');
			int offsetBeforeMethodName= cuString.length();
			cuString.append(METHOD_NAME).append('('); 
			for (int i= 0; i < parameterCount; i++) {
				if (i > 0)
					cuString.append(',');
				cuString.append(types[i]).append(" p").append(i); //$NON-NLS-1$
			}
			cuString.append(");"); //$NON-NLS-1$

			return offsetBeforeMethodName;
		}

		private String qualifyTypes(Type type, final RefactoringStatus result) throws CoreException {
			class NestedException extends RuntimeException {
				private static final long serialVersionUID= 1L;
				NestedException(CoreException e) {
					super(e);
				}
			}
			ASTFlattener flattener= new ASTFlattener() {
				public boolean visit(SimpleName node) {
					appendResolved(node.getIdentifier());
					return false;
				}
				public boolean visit(QualifiedName node) {
					appendResolved(node.getFullyQualifiedName());
					return false;
				}
				public boolean visit(QualifiedType node) {
					appendResolved(ASTNodes.asString(node));
					return false;
				}
				private void appendResolved(String typeName) {
					String resolvedType;
					try {
						resolvedType= resolveType(typeName, result, fMethod.getDeclaringType(), null);
					} catch (CoreException e) {
						throw new NestedException(e);
					}
					this.fBuffer.append(resolvedType);
				}
			};
			try {
				type.accept(flattener);
			} catch (NestedException e) {
				throw ((CoreException) e.getCause());
			}
			return flattener.getResult();
		}

		private static String resolveType(String elementTypeName, RefactoringStatus status, IType declaringType, IProgressMonitor pm) throws CoreException {
			String[][] fqns= declaringType.resolveType(elementTypeName);
			if (fqns != null) {
				if (fqns.length == 1) {
					return JavaModelUtil.concatenateName(fqns[0][0], fqns[0][1]);
				} else if (fqns.length > 1){
					String[] keys= {elementTypeName, String.valueOf(fqns.length)};
					String msg= Messages.format(RefactoringCoreMessages.TypeContextChecker_ambiguous, keys); 
					status.addError(msg);
					return elementTypeName;
				}
			}
			
			List typeRefsFound= findTypeInfos(elementTypeName, declaringType, pm);
			if (typeRefsFound.size() == 0){
				String[] keys= {elementTypeName};
				String msg= Messages.format(RefactoringCoreMessages.TypeContextChecker_not_unique, keys); 
				status.addError(msg);
				return elementTypeName;
			} else if (typeRefsFound.size() == 1){
				TypeNameMatch typeInfo= (TypeNameMatch) typeRefsFound.get(0);
				return typeInfo.getFullyQualifiedName();
			} else {
				Assert.isTrue(typeRefsFound.size() > 1);
				String[] keys= {elementTypeName, String.valueOf(typeRefsFound.size())};
				String msg= Messages.format(RefactoringCoreMessages.TypeContextChecker_ambiguous, keys); 
				status.addError(msg);
				return elementTypeName;
			}
		}

		private static List findTypeInfos(String typeName, IType contextType, IProgressMonitor pm) throws JavaScriptModelException {
			IJavaScriptSearchScope scope= SearchEngine.createJavaSearchScope(new IJavaScriptProject[]{contextType.getJavaScriptProject()}, true);
			IPackageFragment currPackage= contextType.getPackageFragment();
			ArrayList collectedInfos= new ArrayList();
			TypeNameMatchCollector requestor= new TypeNameMatchCollector(collectedInfos);
			int matchMode= SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE;
			new SearchEngine().searchAllTypeNames(null, matchMode, typeName.toCharArray(), matchMode, IJavaScriptSearchConstants.TYPE, scope, requestor, IJavaScriptSearchConstants.WAIT_UNTIL_READY_TO_SEARCH, pm);
			
			List result= new ArrayList();
			for (Iterator iter= collectedInfos.iterator(); iter.hasNext();) {
				TypeNameMatch curr= (TypeNameMatch) iter.next();
				IType type= curr.getType();
				if (type != null) {
					boolean visible=true;
					try {
						visible= JavaModelUtil.isVisible(type, currPackage);
					} catch (JavaScriptModelException e) {
						//Assume visibile if not available
					}
					if (visible) {
						result.add(curr);
					}
				}
			}
			return result;
		}

	}
	
	private static class MethodTypesSyntaxChecker {
	
		private final IFunction fMethod;
		private final List/*<ParameterInfo>*/ fParameterInfos;
		private final ReturnTypeInfo fReturnTypeInfo;
	
		public MethodTypesSyntaxChecker(IFunction method, List/*<ParameterInfo>*/ parameterInfos, ReturnTypeInfo returnTypeInfo) {
			fMethod= method;
			fParameterInfos= parameterInfos;
			fReturnTypeInfo= returnTypeInfo;
		}
		
		public RefactoringStatus[] checkSyntax() {
			/* No checks for ECMA 3 */
			if(!JavaScriptCore.IS_ECMASCRIPT4) return new RefactoringStatus[0]; 
			int parameterCount= fParameterInfos.size();
			RefactoringStatus[] results= new RefactoringStatus[parameterCount + 1];
			results[parameterCount]= checkReturnTypeSyntax();
			for (int i= 0; i < parameterCount; i++) {
				ParameterInfo info= (ParameterInfo) fParameterInfos.get(i);
				results[i]= checkParameterTypeSyntax(info);
			}
			return results;
		}
		
		private RefactoringStatus checkParameterTypeSyntax(ParameterInfo info) {
			if (! info.isAdded() && ! info.isTypeNameChanged())
				return null;
			return TypeContextChecker.checkParameterTypeSyntax(info.getNewTypeName(), fMethod.getJavaScriptProject());
		}
		
		private RefactoringStatus checkReturnTypeSyntax() {
			String newTypeName= fReturnTypeInfo.getNewTypeName();
			if ("".equals(newTypeName.trim())) { //$NON-NLS-1$
				String msg= RefactoringCoreMessages.TypeContextChecker_return_type_not_empty; 
				return RefactoringStatus.createFatalErrorStatus(msg);
			}
			List problemsCollector= new ArrayList(0);
			Type parsedType= parseType(newTypeName, fMethod.getJavaScriptProject(), problemsCollector);
			if (parsedType == null) {
				String msg= Messages.format(RefactoringCoreMessages.TypeContextChecker_invalid_return_type, new String[]{newTypeName}); 
				return RefactoringStatus.createFatalErrorStatus(msg);
			}
			if (problemsCollector.size() == 0)
				return null;
			
			RefactoringStatus result= new RefactoringStatus();
			for (Iterator iter= problemsCollector.iterator(); iter.hasNext();) {
				String msg= Messages.format(RefactoringCoreMessages.TypeContextChecker_invalid_return_type_syntax, new String[]{newTypeName, (String) iter.next()}); 
				result.addError(msg);
			}
			return result;
		}

		private static boolean isVoidArrayType(Type type){
			if (! type.isArrayType())
				return false;
			
			ArrayType arrayType= (ArrayType)type;
			if (! arrayType.getComponentType().isPrimitiveType())
				return false;
			PrimitiveType primitiveType= (PrimitiveType)arrayType.getComponentType();
			return (primitiveType.getPrimitiveTypeCode() == PrimitiveType.VOID);
		}
	
	}

	private static Type parseType(String typeString, IJavaScriptProject javaProject, List/*<IProblem>*/ problemsCollector) {
		if ("".equals(typeString.trim())) //speed up for a common case //$NON-NLS-1$
			return null;
		if (! typeString.trim().equals(typeString))
			return null;
	
		StringBuffer cuBuff= new StringBuffer();
		cuBuff.append("interface A{"); //$NON-NLS-1$
		int offset= cuBuff.length();
		cuBuff.append(typeString).append(" m();}"); //$NON-NLS-1$
	
		ASTParser p= ASTParser.newParser(AST.JLS3);
		p.setSource(cuBuff.toString().toCharArray());
		p.setProject(javaProject);
		JavaScriptUnit cu= (JavaScriptUnit) p.createAST(null);
		Selection selection= Selection.createFromStartLength(offset, typeString.length());
		SelectionAnalyzer analyzer= new SelectionAnalyzer(selection, false);
		cu.accept(analyzer);
		ASTNode selected= analyzer.getFirstSelectedNode();
		if (!(selected instanceof Type))
			return null;
		Type type= (Type)selected;
		if (MethodTypesSyntaxChecker.isVoidArrayType(type))
			return null;
		IProblem[] problems= ASTNodes.getProblems(type, ASTNodes.NODE_ONLY, ASTNodes.PROBLEMS);
		if (problems.length > 0) {
			for (int i= 0; i < problems.length; i++)
				problemsCollector.add(problems[i].getMessage());
		}
		
		String typeNodeRange= cuBuff.substring(type.getStartPosition(), ASTNodes.getExclusiveEnd(type));
		if (typeString.equals(typeNodeRange))
			return type;
		else
			return null;
	}

	private static ITypeBinding handleBug84585(ITypeBinding typeBinding) {
		if (typeBinding == null)
			return null;
		else
			return typeBinding;
	}

	public static RefactoringStatus[] checkAndResolveMethodTypes(IFunction method, StubTypeContext stubTypeContext, List parameterInfos, ReturnTypeInfo returnTypeInfo, IProblemVerifier problemVerifier) throws CoreException {
		MethodTypesChecker checker= new MethodTypesChecker(method, stubTypeContext, parameterInfos, returnTypeInfo, problemVerifier);
		return checker.checkAndResolveMethodTypes();
	}

	public static RefactoringStatus[] checkMethodTypesSyntax(IFunction method, List parameterInfos, ReturnTypeInfo returnTypeInfo) {
		MethodTypesSyntaxChecker checker= new MethodTypesSyntaxChecker(method, parameterInfos, returnTypeInfo);
		return checker.checkSyntax();
	}
	
	public static RefactoringStatus checkParameterTypeSyntax(String type, IJavaScriptProject project) {
		String newTypeName= ParameterInfo.stripEllipsis(type.trim()).trim();
		
		if ("".equals(newTypeName.trim())){ //$NON-NLS-1$
			String msg= Messages.format(RefactoringCoreMessages.TypeContextChecker_parameter_type, new String[]{type}); 
			return RefactoringStatus.createFatalErrorStatus(msg);
		}
		
		if (ParameterInfo.isVarargs(type) && ! JavaModelUtil.is50OrHigher(project)) {
			String msg= Messages.format(RefactoringCoreMessages.TypeContextChecker_no_vararg_below_50, new String[]{type}); 
			return RefactoringStatus.createFatalErrorStatus(msg);
		}
		
		List problemsCollector= new ArrayList(0);
		Type parsedType= parseType(newTypeName, project, problemsCollector);
		boolean valid= parsedType != null;
		if (valid && parsedType instanceof PrimitiveType)
			valid= ! PrimitiveType.VOID.equals(((PrimitiveType) parsedType).getPrimitiveTypeCode());
		if (! valid) {
			String msg= Messages.format(RefactoringCoreMessages.TypeContextChecker_invalid_type_name, new String[]{newTypeName}); 
			return RefactoringStatus.createFatalErrorStatus(msg);
		}
		if (problemsCollector.size() == 0)
			return null;
		
		RefactoringStatus result= new RefactoringStatus();
		for (Iterator iter= problemsCollector.iterator(); iter.hasNext();) {
			String msg= Messages.format(RefactoringCoreMessages.TypeContextChecker_invalid_type_syntax, new String[]{newTypeName, (String) iter.next()}); 
			result.addError(msg);
		}
		return result;
	}
	
	public static StubTypeContext createStubTypeContext(IJavaScriptUnit cu, JavaScriptUnit root, int focalPosition) throws CoreException {
		IDocument document= RefactoringFileBuffers.acquire(cu).getDocument();
		try {
			StringBuffer bufBefore= new StringBuffer();
			StringBuffer bufAfter= new StringBuffer();
			
			int introEnd= 0;
			PackageDeclaration pack= root.getPackage();
			if (pack != null)
				introEnd= pack.getStartPosition() + pack.getLength();
			List imports= root.imports();
			if (imports.size() > 0) {
				ImportDeclaration lastImport= (ImportDeclaration) imports.get(imports.size() - 1);
				introEnd= lastImport.getStartPosition() + lastImport.getLength();
			}
			try {
				bufBefore.append(document.get(0, introEnd));
			} catch (BadLocationException e) {
				throw new RuntimeException(e); // doesn't happen
			}
			
			fillWithTypeStubs(bufBefore, bufAfter, focalPosition, root.types());
			bufBefore.append(' ');
			bufAfter.insert(0, ' ');
			return new StubTypeContext(cu, bufBefore.toString(), bufAfter.toString());
			
		} finally {
			RefactoringFileBuffers.release(cu);
		}
	}

	private static void fillWithTypeStubs(final StringBuffer bufBefore, final StringBuffer bufAfter, final int focalPosition, List/*<? extends BodyDeclaration>*/ types) {
		StringBuffer buf;
		for (Iterator iter= types.iterator(); iter.hasNext();) {
			BodyDeclaration bodyDeclaration= (BodyDeclaration) iter.next();
			if (! (bodyDeclaration instanceof AbstractTypeDeclaration)) {
				//account for local classes:
				if (! (bodyDeclaration instanceof FunctionDeclaration))
					continue;
				int bodyStart= bodyDeclaration.getStartPosition();
				int bodyEnd= bodyDeclaration.getStartPosition() + bodyDeclaration.getLength();
				if (! (bodyStart < focalPosition && focalPosition < bodyEnd))
					continue;
				FunctionDeclaration methodDeclaration= (FunctionDeclaration) bodyDeclaration;
				buf= bufBefore;
				appendModifiers(buf, methodDeclaration.modifiers());
				buf.append(" void "); //$NON-NLS-1$
				buf.append(methodDeclaration.getName().getIdentifier());
				buf.append("(){\n"); //$NON-NLS-1$
				Block body= methodDeclaration.getBody();
				body.accept(new HierarchicalASTVisitor() {
					public boolean visit(AbstractTypeDeclaration node) {
						fillWithTypeStubs(bufBefore, bufAfter, focalPosition, Collections.singletonList(node));
						return false;
					}
					public boolean visit(ClassInstanceCreation node) {
						AnonymousClassDeclaration anonDecl= node.getAnonymousClassDeclaration();
						if (anonDecl == null)
							return false;
						int anonStart= anonDecl.getStartPosition();
						int anonEnd= anonDecl.getStartPosition() + anonDecl.getLength();
						if (! (anonStart < focalPosition && focalPosition < anonEnd))
							return false;
						bufBefore.append(" new "); //$NON-NLS-1$
						bufBefore.append(node.getType().toString());
						bufBefore.append("(){\n"); //$NON-NLS-1$
						fillWithTypeStubs(bufBefore, bufAfter, focalPosition, anonDecl.bodyDeclarations());
						bufAfter.insert(0, "};\n"); //$NON-NLS-1$
						return false;
					}
				});
				buf= bufAfter;
				buf.append("}\n"); //$NON-NLS-1$
				continue;
			}
			
			AbstractTypeDeclaration decl= (AbstractTypeDeclaration) bodyDeclaration;
			buf= decl.getStartPosition() < focalPosition ? bufBefore : bufAfter;
			appendModifiers(buf, decl.modifiers());
			
			if (decl instanceof TypeDeclaration) {
				TypeDeclaration type= (TypeDeclaration) decl;
				buf.append("class "); //$NON-NLS-1$
				buf.append(type.getName().getIdentifier());
				if (type.getSuperclassType() != null) {
					buf.append(" extends "); //$NON-NLS-1$
					buf.append(ASTNodes.asString(type.getSuperclassType()));
				}
			}
			
			buf.append("{\n"); //$NON-NLS-1$
			fillWithTypeStubs(bufBefore, bufAfter, focalPosition, decl.bodyDeclarations());
			buf= decl.getStartPosition() + decl.getLength() < focalPosition ? bufBefore : bufAfter;
			buf.append("}\n"); //$NON-NLS-1$
		}
	}

	private static void appendModifiers(StringBuffer buf, List modifiers) {
		for (Iterator iterator= modifiers.iterator(); iterator.hasNext();) {
			IExtendedModifier extendedModifier= (IExtendedModifier) iterator.next();
			if (extendedModifier.isModifier()) {
				Modifier modifier= (Modifier) extendedModifier;
				buf.append(modifier.getKeyword().toString()).append(' ');
			}
		}
	}

	public static StubTypeContext createSuperInterfaceStubTypeContext(String typeName, IType enclosingType, IPackageFragment packageFragment) {
		return createSupertypeStubTypeContext(typeName, true, enclosingType, packageFragment);
	}
	
	public static StubTypeContext createSuperClassStubTypeContext(String typeName, IType enclosingType, IPackageFragment packageFragment) {
		return createSupertypeStubTypeContext(typeName, false, enclosingType, packageFragment);
	}
	
	private static StubTypeContext createSupertypeStubTypeContext(String typeName, boolean isInterface, IType enclosingType, IPackageFragment packageFragment) {
		StubTypeContext stubTypeContext;
		String prolog= "class " + typeName + (isInterface ? " implements " : " extends "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		String epilog= " {} "; //$NON-NLS-1$
		if (enclosingType != null) {
			try {
				IJavaScriptUnit cu= enclosingType.getJavaScriptUnit();
				ISourceRange typeSourceRange= enclosingType.getSourceRange();
				int focalPosition= typeSourceRange.getOffset() + typeSourceRange.getLength() - 1; // before closing brace
	
				ASTParser parser= ASTParser.newParser(AST.JLS3);
				parser.setSource(cu);
				parser.setFocalPosition(focalPosition);
				JavaScriptUnit compilationUnit= (JavaScriptUnit) parser.createAST(null);
	
				stubTypeContext= createStubTypeContext(cu, compilationUnit, focalPosition);
				stubTypeContext= new StubTypeContext(stubTypeContext.getCuHandle(),
						stubTypeContext.getBeforeString() + prolog,
						epilog + stubTypeContext.getAfterString());
			} catch (CoreException e) {
				JavaScriptPlugin.log(e);
				stubTypeContext= new StubTypeContext(null, null, null);
			}
			
		} else if (packageFragment != null) {
			IJavaScriptUnit cu= packageFragment.getJavaScriptUnit(JavaTypeCompletionProcessor.DUMMY_CU_NAME);
			stubTypeContext= new StubTypeContext(cu, "package " + packageFragment.getElementName() + ";" + prolog, epilog);  //$NON-NLS-1$//$NON-NLS-2$
			
		} else {
			stubTypeContext= new StubTypeContext(null, null, null);
		}
		return stubTypeContext;
	}

	public static Type parseSuperClass(String superClass) {
		return parseSuperType(superClass, false);
	}

	public static Type parseSuperInterface(String superInterface) {
		return parseSuperType(superInterface, true);
	}

	private static Type parseSuperType(String superType, boolean isInterface) {
		if (! superType.trim().equals(superType)) {
			return null;
		}
	
		StringBuffer cuBuff= new StringBuffer();
		if (isInterface)
			cuBuff.append("class __X__ implements "); //$NON-NLS-1$
		else
			cuBuff.append("class __X__ extends "); //$NON-NLS-1$
		int offset= cuBuff.length();
		cuBuff.append(superType).append(" {}"); //$NON-NLS-1$
	
		ASTParser p= ASTParser.newParser(AST.JLS3);
		p.setSource(cuBuff.toString().toCharArray());
		Map options= new HashMap();
		JavaModelUtil.set50CompilanceOptions(options);
		p.setCompilerOptions(options);
		JavaScriptUnit cu= (JavaScriptUnit) p.createAST(null);
		ASTNode selected= NodeFinder.perform(cu, offset, superType.length());
		if (selected instanceof Name)
			selected= selected.getParent();
		if (selected.getStartPosition() != offset
				|| selected.getLength() != superType.length()
				|| ! (selected instanceof Type)
				|| selected instanceof PrimitiveType) {
			return null;
		}
		Type type= (Type) selected;
		
		String typeNodeRange= cuBuff.substring(type.getStartPosition(), ASTNodes.getExclusiveEnd(type));
		if (! superType.equals(typeNodeRange)){
			return null;
		}
		return type;
	}

	public static ITypeBinding resolveSuperClass(String superclass, IType typeHandle, StubTypeContext superClassContext) {
		StringBuffer cuString= new StringBuffer();
		cuString.append(superClassContext.getBeforeString());
		cuString.append(superclass);
		cuString.append(superClassContext.getAfterString());
		
		try {
			IJavaScriptUnit wc= typeHandle.getJavaScriptUnit().getWorkingCopy(new WorkingCopyOwner() {/*subclass*/}, new NullProgressMonitor());
			try {
				wc.getBuffer().setContents(cuString.toString());
				JavaScriptUnit compilationUnit= new RefactoringASTParser(AST.JLS3).parse(wc, true);
				ASTNode type= NodeFinder.perform(compilationUnit, superClassContext.getBeforeString().length(),
						superclass.length());
				if (type instanceof Type) {
					return handleBug84585(((Type) type).resolveBinding());
				} else if (type instanceof Name) {
					ASTNode parent= type.getParent();
					if (parent instanceof Type)
						return handleBug84585(((Type) parent).resolveBinding());
				}
				throw new IllegalStateException();
			} finally {
				wc.discardWorkingCopy();
			}
		} catch (JavaScriptModelException e) {
			return null;
		}
	}

	public static ITypeBinding[] resolveSuperInterfaces(String[] interfaces, IType typeHandle, StubTypeContext superInterfaceContext) {
		ITypeBinding[] result= new ITypeBinding[interfaces.length];
		
		int[] interfaceOffsets= new int[interfaces.length];
		StringBuffer cuString= new StringBuffer();
		cuString.append(superInterfaceContext.getBeforeString());
		int last= interfaces.length - 1;
		for (int i= 0; i <= last; i++) {
			interfaceOffsets[i]= cuString.length();
			cuString.append(interfaces[i]);
			if (i != last)
				cuString.append(", "); //$NON-NLS-1$
		}
		cuString.append(superInterfaceContext.getAfterString());
		
		try {
			IJavaScriptUnit wc= typeHandle.getJavaScriptUnit().getWorkingCopy(new WorkingCopyOwner() {/*subclass*/}, new NullProgressMonitor());
			try {
				wc.getBuffer().setContents(cuString.toString());
				JavaScriptUnit compilationUnit= new RefactoringASTParser(AST.JLS3).parse(wc, true);
				for (int i= 0; i <= last; i++) {
					ASTNode type= NodeFinder.perform(compilationUnit, interfaceOffsets[i], interfaces[i].length());
					if (type instanceof Type) {
						result[i]= handleBug84585(((Type) type).resolveBinding());
					} else if (type instanceof Name) {
						ASTNode parent= type.getParent();
						if (parent instanceof Type) {
							result[i]= handleBug84585(((Type) parent).resolveBinding());
						} else {
							throw new IllegalStateException();
						}
					} else {
						throw new IllegalStateException();
					}
				}
			} finally {
				wc.discardWorkingCopy();
			}
		} catch (JavaScriptModelException e) {
			// won't happen
		}
		return result;
	}
}
