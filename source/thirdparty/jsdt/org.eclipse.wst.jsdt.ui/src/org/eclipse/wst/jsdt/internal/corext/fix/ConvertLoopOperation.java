/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/**
 *
 **/
package org.eclipse.wst.jsdt.internal.corext.fix;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.text.edits.TextEditGroup;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.ForStatement;
import org.eclipse.wst.jsdt.core.dom.SingleVariableDeclaration;
import org.eclipse.wst.jsdt.core.dom.Statement;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment;
import org.eclipse.wst.jsdt.internal.corext.dom.GenericVisitor;
import org.eclipse.wst.jsdt.internal.corext.dom.ScopeAnalyzer;
import org.eclipse.wst.jsdt.internal.corext.fix.LinkedFix.AbstractLinkedFixRewriteOperation;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;

public abstract class ConvertLoopOperation extends AbstractLinkedFixRewriteOperation {
	
	protected static final String FOR_LOOP_ELEMENT_IDENTIFIER= "element"; //$NON-NLS-1$
	
	protected static final IStatus ERROR_STATUS= new Status(IStatus.ERROR, JavaScriptPlugin.getPluginId(), ""); //$NON-NLS-1$
	
	private final ForStatement fStatement;
	private ConvertLoopOperation fOperation;
	private final String[] fUsedNames;
	
	public ConvertLoopOperation(ForStatement statement, String[] usedNames) {
		fStatement= statement;
		fUsedNames= usedNames;
	}
	
	public void setBodyConverter(ConvertLoopOperation operation) {
		fOperation= operation;
	}
	
	public abstract String getIntroducedVariableName();
	
	public abstract IStatus satisfiesPreconditions();
	
	protected abstract Statement convert(CompilationUnitRewrite cuRewrite, TextEditGroup group, LinkedProposalModel positionGroups) throws CoreException;
	
	protected ForStatement getForStatement() {
		return fStatement;
	}
	
	protected Statement getBody(CompilationUnitRewrite cuRewrite, TextEditGroup group, LinkedProposalModel positionGroups) throws CoreException {
		if (fOperation != null) {
			return fOperation.convert(cuRewrite, group, positionGroups);
		} else {
			return (Statement)cuRewrite.getASTRewrite().createMoveTarget(getForStatement().getBody());
		}
	}
	
	protected String[] getUsedVariableNames() {
		final List results= new ArrayList();
		
		ForStatement forStatement= getForStatement();
		JavaScriptUnit root= (JavaScriptUnit)forStatement.getRoot();
		
		Collection variableNames= new ScopeAnalyzer(root).getUsedVariableNames(forStatement.getStartPosition(), forStatement.getLength());
		results.addAll(variableNames);
		
		forStatement.accept(new GenericVisitor() {
			public boolean visit(SingleVariableDeclaration node) {
				results.add(node.getName().getIdentifier());
				return super.visit(node);
			}
			
			public boolean visit(VariableDeclarationFragment fragment) {
				results.add(fragment.getName().getIdentifier());
				return super.visit(fragment);
			}
		});
		
		results.addAll(Arrays.asList(fUsedNames));
		
		return (String[])results.toArray(new String[results.size()]);
	}
	
}
