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
package org.eclipse.wst.jsdt.internal.ui.text.java;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.commands.IParameterValues;

/**
 * Map of parameters for the specific content assist command.
 * 
 * 
 */
public final class ContentAssistComputerParameter implements IParameterValues {
	/*
	 * @see org.eclipse.core.commands.IParameterValues#getParameterValues()
	 */
	public Map getParameterValues() {
		Collection descriptors= CompletionProposalComputerRegistry.getDefault().getProposalCategories();
		Map map= new HashMap(descriptors.size());
		for (Iterator it= descriptors.iterator(); it.hasNext();) {
			CompletionProposalCategory category= (CompletionProposalCategory) it.next();
			map.put(category.getDisplayName(), category.getId());
		}
		return map;
	}
}
