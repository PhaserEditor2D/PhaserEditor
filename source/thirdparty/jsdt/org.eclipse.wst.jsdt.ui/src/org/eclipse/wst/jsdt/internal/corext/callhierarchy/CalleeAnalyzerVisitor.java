/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Jesper Kamstrup Linnet (eclipse@kamstrup-linnet.dk) - initial API and implementation 
 *          (report 36180: Callers/Callees view)
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.corext.callhierarchy;

import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.ISourceRange;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ASTVisitor;
import org.eclipse.wst.jsdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.wst.jsdt.core.dom.ClassInstanceCreation;
import org.eclipse.wst.jsdt.core.dom.ConstructorInvocation;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.FunctionInvocation;
import org.eclipse.wst.jsdt.core.dom.IFunctionBinding;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.SuperConstructorInvocation;
import org.eclipse.wst.jsdt.core.dom.SuperMethodInvocation;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchScope;
import org.eclipse.wst.jsdt.internal.corext.dom.Bindings;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
/**
*
* Provisional API: This class/interface is part of an interim API that is still under development and expected to
* change significantly before reaching stability. It is being made available at this early stage to solicit feedback
* from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
* (repeatedly) as the API evolves.
*/
class CalleeAnalyzerVisitor extends ASTVisitor {
    private CallSearchResultCollector fSearchResults;
    private IFunction fMethod;
    private JavaScriptUnit fCompilationUnit;
    private IProgressMonitor fProgressMonitor;
    private int fMethodEndPosition;
    private int fMethodStartPosition;

    CalleeAnalyzerVisitor(IFunction method, JavaScriptUnit compilationUnit, IProgressMonitor progressMonitor) {
        fSearchResults = new CallSearchResultCollector();
        this.fMethod = method;
        this.fCompilationUnit= compilationUnit;
        this.fProgressMonitor = progressMonitor;

        try {
            ISourceRange sourceRange = method.getSourceRange();
            this.fMethodStartPosition = sourceRange.getOffset();
            this.fMethodEndPosition = fMethodStartPosition + sourceRange.getLength();
        } catch (JavaScriptModelException jme) {
            JavaScriptPlugin.log(jme);
        }
    }

    /**
     * Method getCallees.
     *
     * @return CallerElement
     */
    public Map getCallees() {
        return fSearchResults.getCallers();
    }

    /* (non-Javadoc)
     * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.ClassInstanceCreation)
     */
    public boolean visit(ClassInstanceCreation node) {
        progressMonitorWorked(1);
        if (!isFurtherTraversalNecessary(node)) {
            return false;
        }

        if (isNodeWithinMethod(node)) {
            addMethodCall(node.resolveConstructorBinding(), node);
        }

        return true;
    }

    /**
     * Find all constructor invocations (<code>this(...)</code>) from the called method.
     * Since we only traverse into the AST on the wanted method declaration, this method
     * should not hit on more constructor invocations than those in the wanted method.
     *
     * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.ConstructorInvocation)
     */
    public boolean visit(ConstructorInvocation node) {
        progressMonitorWorked(1);
        if (!isFurtherTraversalNecessary(node)) {
            return false;
        }

        if (isNodeWithinMethod(node)) {
            addMethodCall(node.resolveConstructorBinding(), node);
        }

        return true;
    }

    /**
     * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.FunctionDeclaration)
     */
    public boolean visit(FunctionDeclaration node) {
        progressMonitorWorked(1);
        return isFurtherTraversalNecessary(node);
    }

    /**
     * Find all method invocations from the called method. Since we only traverse into
     * the AST on the wanted method declaration, this method should not hit on more
     * method invocations than those in the wanted method.
     *
     * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.FunctionInvocation)
     */
    public boolean visit(FunctionInvocation node) {
        progressMonitorWorked(1);
        if (!isFurtherTraversalNecessary(node)) {
            return false;
        }

        if (isNodeWithinMethod(node)) {
            addMethodCall(node.resolveMethodBinding(), node);
        }

        return true;
    }

    /**
     * Find invocations of the supertype's constructor from the called method
     * (=constructor). Since we only traverse into the AST on the wanted method
     * declaration, this method should not hit on more method invocations than those in
     * the wanted method.
     *
     * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.SuperConstructorInvocation)
     */
    public boolean visit(SuperConstructorInvocation node) {
        progressMonitorWorked(1);
        if (!isFurtherTraversalNecessary(node)) {
            return false;
        }

        if (isNodeWithinMethod(node)) {
            addMethodCall(node.resolveConstructorBinding(), node);
        }
        
        return true;
    }

    /**
     * Find all method invocations from the called method. Since we only traverse into
     * the AST on the wanted method declaration, this method should not hit on more
     * method invocations than those in the wanted method.
     *
     * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.FunctionInvocation)
     */
    public boolean visit(SuperMethodInvocation node) {
        progressMonitorWorked(1);
        if (!isFurtherTraversalNecessary(node)) {
            return false;
        }

        if (isNodeWithinMethod(node)) {
            addMethodCall(node.resolveMethodBinding(), node);
        }
        
        return true;
    }
    
    /**
     * When an anonymous class declaration is reached, the traversal should not go further since it's not
     * supposed to consider calls inside the anonymous inner class as calls from the outer method.
     * 
     * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt.core.dom.AnonymousClassDeclaration)
     */
    public boolean visit(AnonymousClassDeclaration node) {
        return isNodeEnclosingMethod(node);
    }


    /**
     * Adds the specified method binding to the search results.
     *
     * @param calledMethodBinding
     * @param node
     */
    protected void addMethodCall(IFunctionBinding calledMethodBinding, ASTNode node) {
        try {
            if (calledMethodBinding != null) {
                fProgressMonitor.worked(1);

                ITypeBinding calledTypeBinding = calledMethodBinding.getDeclaringClass();
                IType calledType = null;

                if (!calledTypeBinding.isAnonymous()) {
                    calledType = (IType) calledTypeBinding.getJavaElement();
                } else {
                    if (!"java.lang.Object".equals(calledTypeBinding.getSuperclass().getQualifiedName())) { //$NON-NLS-1$
                        calledType= (IType) calledTypeBinding.getSuperclass().getJavaElement();
                    }
                }

                if (calledType == null) {
                	// No further search is possible
                	return;
                }
                
                IFunction calledMethod = findIncludingSupertypes(calledMethodBinding,
                        calledType, fProgressMonitor);

                IMember referencedMember= null;
                if (calledMethod == null) {
                    if (calledMethodBinding.isConstructor() && calledMethodBinding.getParameterTypes().length == 0) {
                        referencedMember= calledType;
                    }
                } else { 

                    if (!isIgnoredBySearchScope(calledMethod)) {
                        referencedMember= calledMethod;
                    }
                }
                final int position= node.getStartPosition();
				final int number= fCompilationUnit.getLineNumber(position);
				fSearchResults.addMember(fMethod, referencedMember, position, position + node.getLength(), number < 1 ? 1 : number);
            }
        } catch (JavaScriptModelException jme) {
            JavaScriptPlugin.log(jme);
        }
    }
    
    private static IFunction findIncludingSupertypes(IFunctionBinding method, IType type, IProgressMonitor pm) throws JavaScriptModelException {
		IFunction inThisType= Bindings.findMethod(method, type);
		if (inThisType != null)
			return inThisType;
		IType[] superTypes= JavaModelUtil.getAllSuperTypes(type, pm);
		for (int i= 0; i < superTypes.length; i++) {
			IFunction m= Bindings.findMethod(method, superTypes[i]);
			if (m != null)
				return m;
		}
		return null;
	}

    private boolean isIgnoredBySearchScope(IFunction enclosingElement) {
        if (enclosingElement != null) {
            return !getSearchScope().encloses(enclosingElement);
        } else {
            return false;
        }
    }

    private IJavaScriptSearchScope getSearchScope() {
        return CallHierarchy.getDefault().getSearchScope();
    }

    private boolean isNodeWithinMethod(ASTNode node) {
        int nodeStartPosition = node.getStartPosition();
        int nodeEndPosition = nodeStartPosition + node.getLength();

        if (nodeStartPosition < fMethodStartPosition) {
            return false;
        }

        if (nodeEndPosition > fMethodEndPosition) {
            return false;
        }

        return true;
    }

    private boolean isNodeEnclosingMethod(ASTNode node) {
        int nodeStartPosition = node.getStartPosition();
        int nodeEndPosition = nodeStartPosition + node.getLength();

        if (nodeStartPosition < fMethodStartPosition && nodeEndPosition > fMethodEndPosition) {
            // Is the method completely enclosed by the node?
            return true;
        }
        return false;
    }
    
    private boolean isFurtherTraversalNecessary(ASTNode node) {
        return isNodeWithinMethod(node) || isNodeEnclosingMethod(node);
    }

//    private IFunction findImplementingMethods(IFunction calledMethod) {
//        Collection implementingMethods = CallHierarchy.getDefault()
//                                                        .getImplementingMethods(calledMethod);
//
//        if ((implementingMethods.size() == 0) || (implementingMethods.size() > 1)) {
//            return calledMethod;
//        } else {
//            return (IFunction) implementingMethods.iterator().next();
//        }
//    }
    
    private void progressMonitorWorked(int work) {
        if (fProgressMonitor != null) {
            fProgressMonitor.worked(work);
            if (fProgressMonitor.isCanceled()) {
                throw new OperationCanceledException();
            }
        }
    }
}
