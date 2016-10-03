/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.compiler.env;

import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.core.compiler.IProblem;

/**
 * Definition of a set of access rules used to flag forbidden references to non API code.
 */
public class AccessRuleSet {

	private AccessRule[] accessRules;
	public String[] messageTemplates;
	public static final int MESSAGE_TEMPLATES_LENGTH = 4;

	/**
	 * Make a new set of access rules.
	 * @param accessRules the access rules to be contained by the new set
	 * @param messageTemplates a Sting[4] array specifying the messages for type,
	 * constructor, method and field access violation; each should contain as many
	 * placeholders as expected by the respective access violation message (that is,
	 * one for type and constructor, two for method and field); replaced by a
	 * default value if null.
	 */
	public AccessRuleSet(AccessRule[] accessRules, String[] messageTemplates) {
		this.accessRules = accessRules;
		if (messageTemplates != null && messageTemplates.length == MESSAGE_TEMPLATES_LENGTH)
			this.messageTemplates = messageTemplates;
		else
			this.messageTemplates = new String[] {"{0}", "{0}", "{0} {1}", "{0} {1}"};  //$NON-NLS-1$ //$NON-NLS-2$//$NON-NLS-3$ //$NON-NLS-4$
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object object) {
		if (this == object)
			return true;
		if (!(object instanceof AccessRuleSet))
			return false;
		AccessRuleSet otherRuleSet = (AccessRuleSet) object;
		if (this.messageTemplates.length != MESSAGE_TEMPLATES_LENGTH ||
				otherRuleSet.messageTemplates.length != MESSAGE_TEMPLATES_LENGTH)
			return false; // guard
		for (int i = 0; i < MESSAGE_TEMPLATES_LENGTH; i++)
			if (!this.messageTemplates[i].equals(otherRuleSet.messageTemplates[i]))
				return false;
		int rulesLength = this.accessRules.length;
		if (rulesLength != otherRuleSet.accessRules.length) return false;
		for (int i = 0; i < rulesLength; i++)
			if (!this.accessRules[i].equals(otherRuleSet.accessRules[i]))
				return false;
		return true;
	}

	public AccessRule[] getAccessRules() {
		return this.accessRules;
	}

/**
 * Select the first access rule which is violated when accessing a given type,
 * or null if no 'non accessible' access rule applies.
 * @param targetTypeFilePath the target type file path, formed as:
 * "org/eclipse/jdt/core/JavaScriptCore"
 * @return the first access restriction that applies if any, null else
 */
public AccessRestriction getViolatedRestriction(char[] targetTypeFilePath) {
	for (int i = 0, length = this.accessRules.length; i < length; i++) {
		AccessRule accessRule = this.accessRules[i];
		if (CharOperation.pathMatch(accessRule.pattern, targetTypeFilePath,
				true/*case sensitive*/, '/')) {
			switch (accessRule.getProblemId()) {
				case IProblem.ForbiddenReference:
				case IProblem.DiscouragedReference:
					return new AccessRestriction(accessRule, this.messageTemplates);
				default:
					return null;
			}
		}
	}
	return null;
}

	public String toString() {
		return toString(true/*wrap lines*/);
	}

	public String toString(boolean wrap) {
		StringBuffer buffer = new StringBuffer(200);
		buffer.append("AccessRuleSet {"); //$NON-NLS-1$
		if (wrap)
			buffer.append('\n');
		for (int i = 0, length = this.accessRules.length; i < length; i++) {
			if (wrap)
				buffer.append('\t');
			AccessRule accessRule = this.accessRules[i];
			buffer.append(accessRule);
			if (wrap)
				buffer.append('\n');
			else if (i < length-1)
				buffer.append(", "); //$NON-NLS-1$
		}
		buffer.append("} [templates:\""); //$NON-NLS-1$
		for (int i = 0; i < messageTemplates.length; i++)
			buffer.append(this.messageTemplates[i]);
		buffer.append("\"]"); //$NON-NLS-1$
		return buffer.toString();
	}
}
