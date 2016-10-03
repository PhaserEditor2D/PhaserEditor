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
package org.eclipse.wst.jsdt.internal.ui.compare;

import java.util.Iterator;
import java.util.Stack;

import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ASTVisitor;
import org.eclipse.wst.jsdt.core.dom.FieldDeclaration;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.ImportDeclaration;
import org.eclipse.wst.jsdt.core.dom.Initializer;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.PackageDeclaration;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.SingleVariableDeclaration;
import org.eclipse.wst.jsdt.core.dom.Type;
import org.eclipse.wst.jsdt.core.dom.TypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment;


class JavaParseTreeBuilder extends ASTVisitor {

    private char[] fBuffer;
    private Stack fStack= new Stack();
    private JavaNode fImportContainer;
    private boolean fShowCU;

    /*
     * Parsing is performed on the given buffer and the resulting tree (if any)
     * hangs below the given root.
     */
    JavaParseTreeBuilder(JavaNode root, char[] buffer, boolean showCU) {
        fBuffer= buffer;
        fShowCU= showCU;
        fStack.clear();
        fStack.push(root);
    }

    public boolean visit(PackageDeclaration node) {
        new JavaNode(getCurrentContainer(), JavaNode.PACKAGE, null, node.getStartPosition(), node.getLength());
        return false;
    }

    public boolean visit(JavaScriptUnit node) {
        if (fShowCU)
            push(JavaNode.CU, null, node.getStartPosition(), node.getLength());
        return true;
    }

    public void endVisit(JavaScriptUnit node) {
        if (fShowCU)
            pop();
    }

    public boolean visit(TypeDeclaration node) {
        push(JavaNode.CLASS, node.getName().toString(), node.getStartPosition(), node.getLength());
        return true;
    }

    public void endVisit(TypeDeclaration node) {
        pop();
    }

 
    


    public boolean visit(FunctionDeclaration node) {
        String signature= getSignature(node);
        push(node.isConstructor() ? JavaNode.CONSTRUCTOR : JavaNode.METHOD, signature, node.getStartPosition(), node.getLength());
        return false;
    }

    public void endVisit(FunctionDeclaration node) {
        pop();
    }

    public boolean visit(Initializer node) {
        push(JavaNode.INIT, getCurrentContainer().getInitializerCount(), node.getStartPosition(), node.getLength());
        return false;
    }

    public void endVisit(Initializer node) {
        pop();
    }

    public boolean visit(ImportDeclaration node) {
        int s= node.getStartPosition();
        int l= node.getLength();
        int declarationEnd= s + l;
        if (fImportContainer == null)
            fImportContainer= new JavaNode(getCurrentContainer(), JavaNode.IMPORT_CONTAINER, null, s, l);
        String nm= node.getName().toString();
        if (node.isOnDemand())
            nm+= ".*"; //$NON-NLS-1$
        new JavaNode(fImportContainer, JavaNode.IMPORT, nm, s, l);
        fImportContainer.setLength(declarationEnd - fImportContainer.getRange().getOffset() + 1);
        fImportContainer.setAppendPosition(declarationEnd + 2); // FIXME
        return false;
    }

    public boolean visit(VariableDeclarationFragment node) {
        String name= getFieldName(node);
        ASTNode parent= node.getParent();
        push(JavaNode.FIELD, name, parent.getStartPosition(), parent.getLength());
        return false;
    }

    public void endVisit(VariableDeclarationFragment node) {
        pop();
    }

    // private stuff

    /**
     * Adds a new JavaNode with the given type and name to the current
     * container.
     */
    private void push(int type, String name, int declarationStart, int length) {

        while (declarationStart > 0) {
            char c= fBuffer[declarationStart - 1];
            if (c != ' ' && c != '\t')
                break;
            declarationStart--;
            length++;
        }

        JavaNode node= new JavaNode(getCurrentContainer(), type, name, declarationStart, length);
        if (type == JavaNode.CU)
            node.setAppendPosition(declarationStart + length + 1);
        else
            node.setAppendPosition(declarationStart + length);

        fStack.push(node);
    }

    /**
     * Closes the current Java node by setting its end position and pops it off
     * the stack.
     */
    private void pop() {
        fStack.pop();
    }

    private JavaNode getCurrentContainer() {
        return (JavaNode) fStack.peek();
    }

    private String getFieldName(VariableDeclarationFragment node) {
        StringBuffer buffer= new StringBuffer();
        buffer.append(node.getName().toString());
        ASTNode parent= node.getParent();
        if (parent instanceof FieldDeclaration) {
            FieldDeclaration fd= (FieldDeclaration) parent;
            buffer.append(" : "); //$NON-NLS-1$
            buffer.append(getType(fd.getType()));
        }
        return buffer.toString();
    }

    private String getSignature(FunctionDeclaration node) {
    	StringBuffer buffer= new StringBuffer();
        SimpleName name = node.getName();
        if (name!=null)
        	buffer.append(name.toString());
     
        buffer.append('(');
        boolean first= true;
        Iterator iterator= node.parameters().iterator();
        while (iterator.hasNext()) {
            Object parameterDecl= iterator.next();
            if (parameterDecl instanceof SingleVariableDeclaration) {
                SingleVariableDeclaration svd= (SingleVariableDeclaration) parameterDecl;
                if (!first)
                    buffer.append(", "); //$NON-NLS-1$
                buffer.append(getType(svd.getType()));
                if (svd.isVarargs())
                    buffer.append("..."); //$NON-NLS-1$
                first= false;
            }
        }
        buffer.append(')');
        return buffer.toString();
    }


    private String getType(Type type) {
        String name= type.toString();
        int pos= name.lastIndexOf('.');
        if (pos >= 0)
            name= name.substring(pos + 1);
        return name;
    }
}
