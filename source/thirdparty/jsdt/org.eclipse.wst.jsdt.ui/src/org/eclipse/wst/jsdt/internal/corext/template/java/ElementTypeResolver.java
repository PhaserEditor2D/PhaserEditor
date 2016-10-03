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
package org.eclipse.wst.jsdt.internal.corext.template.java;

import java.util.List;

import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateVariable;
import org.eclipse.jface.text.templates.TemplateVariableResolver;
import org.eclipse.wst.jsdt.internal.corext.template.java.CompilationUnitCompletion.Variable;
import org.eclipse.wst.jsdt.internal.ui.text.template.contentassist.MultiVariable;


public class ElementTypeResolver extends TemplateVariableResolver {

	public ElementTypeResolver() {
	}
	
	/*
	 * @see org.eclipse.jface.text.templates.TemplateVariableResolver#resolve(org.eclipse.jface.text.templates.TemplateVariable, org.eclipse.jface.text.templates.TemplateContext)
	 * 
	 */
	public void resolve(TemplateVariable variable, TemplateContext context) {
		if (!(variable instanceof MultiVariable)) {
			super.resolve(variable, context);
			return;
		}
		MultiVariable mv= (MultiVariable) variable;
		List params= variable.getVariableType().getParams();
		if (params.isEmpty()) {
			super.resolve(variable, context);
			return;
		}
		
		JavaContext jc= (JavaContext) context;
		String reference= (String) params.get(0);
		TemplateVariable refVar= jc.getTemplateVariable(reference);
		if (refVar instanceof JavaVariable) {
			JavaVariable jvar= (JavaVariable) refVar;
			resolve(mv, jvar, jc);
			return;
		}
		
		super.resolve(variable, context);
	}

	private void resolve(MultiVariable variable, JavaVariable master, JavaContext context) {
		Object[] choices= master.getChoices();
		if (choices instanceof Variable[]) {
			Variable[] variables= (Variable[]) choices;

			for (int i= 0; i < variables.length; i++)
				variable.setChoices(variables[i], variables[i].getMemberTypeNames());

			context.addDependency(master, variable);
			variable.setKey(master.getCurrentChoice());
		} else {
			super.resolve(variable, context);
		}
	}
}
