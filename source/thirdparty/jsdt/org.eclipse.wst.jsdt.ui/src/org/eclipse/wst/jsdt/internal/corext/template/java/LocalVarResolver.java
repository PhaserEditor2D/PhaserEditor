/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
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

/**
 * Resolves template variables to a local variable that is assignment-compatible with the variable
 * instance' class parameter.
 * 
 * 
 */
public class LocalVarResolver extends TemplateVariableResolver {
	
	private final String fDefaultType;
	private String fType;

	/**
	 * Default ctor for instantiation by the extension point.
	 */
	public LocalVarResolver() {
		this("java.lang.Object"); //$NON-NLS-1$
	}
	
	LocalVarResolver(String defaultType) {
		fDefaultType= defaultType;
	}

	/*
	 * @see org.eclipse.jface.text.templates.TemplateVariableResolver#resolve(org.eclipse.jface.text.templates.TemplateVariable, org.eclipse.jface.text.templates.TemplateContext)
	 */
	public void resolve(TemplateVariable variable, TemplateContext context) {
		List params= variable.getVariableType().getParams();
		if (params.size() == 0)
			fType= fDefaultType;
		else
			fType= (String) params.get(0);
		
		if (variable instanceof JavaVariable) {
			JavaContext jc= (JavaContext) context;
			JavaVariable jv= (JavaVariable) variable;
			jv.setParamType(fType);
	        Variable[] variables= jc.getLocalVariables(fType);
			if (variables.length > 0) {
				jv.setChoices(variables);
				jc.markAsUsed(jv.getDefaultValue());
			} else {
				super.resolve(variable, context);
				return;
			}
			if (variables.length > 1)
				variable.setUnambiguous(false);
			else
				variable.setUnambiguous(isUnambiguous(context));
		} else
			super.resolve(variable, context);
	}
	
	/*
	 * @see org.eclipse.jface.text.templates.TemplateVariableResolver#resolveAll(org.eclipse.jface.text.templates.TemplateContext)
	 */
	protected String[] resolveAll(TemplateContext context) {
        JavaContext jc= (JavaContext) context;
        Variable[] iterables= jc.getLocalVariables(fType);
        String[] names= new String[iterables.length];
        for (int i= 0; i < iterables.length; i++)
			names[i]= iterables[i].getName();
        if (names.length > 0)
        	jc.markAsUsed(names[0]);
		return names;
	}
}
