/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.compiler.env;

public class AccessRestriction {

	private AccessRule accessRule;
	private String[] messageTemplates;
	public AccessRestriction(AccessRule accessRule, String [] messageTemplates) {
		this.accessRule = accessRule;
		this.messageTemplates = messageTemplates;
	}

	/**
	 * Returns readable description for problem reporting,
	 * message is expected to contain room for restricted type name
	 * e.g. "{0} has restricted access"
	 */
	public String getMessageTemplate() {
		return this.messageTemplates[0];
	}

	public String getConstructorAccessMessageTemplate() {
		return this.messageTemplates[1];
	}

	public String getMethodAccessMessageTemplate() {
		return this.messageTemplates[2];
	}

	public String getFieldAccessMessageTemplate() {
		return this.messageTemplates[3];
	}

	public int getProblemId() {
		return this.accessRule.getProblemId();
	}

	public boolean ignoreIfBetter() {
		return this.accessRule.ignoreIfBetter();
	}
}
