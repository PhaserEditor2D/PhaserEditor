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
package org.eclipse.wst.jsdt.internal.core.search.matching;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.infer.InferredAttribute;
import org.eclipse.wst.jsdt.core.infer.InferredMethod;
import org.eclipse.wst.jsdt.core.infer.InferredType;
import org.eclipse.wst.jsdt.core.search.SearchMatch;
import org.eclipse.wst.jsdt.core.search.SearchPattern;
import org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode;
import org.eclipse.wst.jsdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.Expression;
import org.eclipse.wst.jsdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.ImportReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.MessageSend;
import org.eclipse.wst.jsdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.Reference;
import org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.TypeReference;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Binding;

public class OrLocator extends PatternLocator {

protected PatternLocator[] patternLocators;

public OrLocator(OrPattern pattern) {
	super(pattern);

	SearchPattern[] patterns = pattern.patterns;
	int length = patterns.length;
	this.patternLocators = new PatternLocator[length];
	for (int i = 0; i < length; i++)
		this.patternLocators[i] = PatternLocator.patternLocator(patterns[i]);
}
public void initializePolymorphicSearch(MatchLocator locator) {
	for (int i = 0, length = this.patternLocators.length; i < length; i++)
		this.patternLocators[i].initializePolymorphicSearch(locator);
}
public int match(ASTNode node, MatchingNodeSet nodeSet) {
	int level = IMPOSSIBLE_MATCH;
	for (int i = 0, length = this.patternLocators.length; i < length; i++) {
		int newLevel = this.patternLocators[i].match(node, nodeSet);
		if (newLevel > level) {
			if (newLevel == ACCURATE_MATCH) return ACCURATE_MATCH;
			level = newLevel;
		}
	}
	return level;
}
public int match(ConstructorDeclaration node, MatchingNodeSet nodeSet) {
	int level = IMPOSSIBLE_MATCH;
	for (int i = 0, length = this.patternLocators.length; i < length; i++) {
		int newLevel = this.patternLocators[i].match(node, nodeSet);
		if (newLevel > level) {
			if (newLevel == ACCURATE_MATCH) return ACCURATE_MATCH;
			level = newLevel;
		}
	}
	return level;
}
public int match(Expression node, MatchingNodeSet nodeSet) {
	int level = IMPOSSIBLE_MATCH;
	for (int i = 0, length = this.patternLocators.length; i < length; i++) {
		int newLevel = this.patternLocators[i].match(node, nodeSet);
		if (newLevel > level) {
			if (newLevel == ACCURATE_MATCH) return ACCURATE_MATCH;
			level = newLevel;
		}
	}
	return level;
}
public int match(FieldDeclaration node, MatchingNodeSet nodeSet) {
	int level = IMPOSSIBLE_MATCH;
	for (int i = 0, length = this.patternLocators.length; i < length; i++) {
		int newLevel = this.patternLocators[i].match(node, nodeSet);
		if (newLevel > level) {
			if (newLevel == ACCURATE_MATCH) return ACCURATE_MATCH;
			level = newLevel;
		}
	}
	return level;
}

public int match(InferredAttribute node, MatchingNodeSet nodeSet) {
	int level = IMPOSSIBLE_MATCH;
	for (int i = 0, length = this.patternLocators.length; i < length; i++) {
		int newLevel = this.patternLocators[i].match(node, nodeSet);
		if (newLevel > level) {
			if (newLevel == ACCURATE_MATCH) return ACCURATE_MATCH;
			level = newLevel;
		}
	}
	return level;
}

public int match(LocalDeclaration node, MatchingNodeSet nodeSet) {
	int level = IMPOSSIBLE_MATCH;
	for (int i = 0, length = this.patternLocators.length; i < length; i++) {
		int newLevel = this.patternLocators[i].match(node, nodeSet);
		if (newLevel > level) {
			if (newLevel == ACCURATE_MATCH) return ACCURATE_MATCH;
			level = newLevel;
		}
	}
	return level;
}
public int match(MethodDeclaration node, MatchingNodeSet nodeSet) {
	int level = IMPOSSIBLE_MATCH;
	for (int i = 0, length = this.patternLocators.length; i < length; i++) {
		int newLevel = this.patternLocators[i].match(node, nodeSet);
		if (newLevel > level) {
			if (newLevel == ACCURATE_MATCH) return ACCURATE_MATCH;
			level = newLevel;
		}
	}
	return level;
}

public int match(InferredMethod node, MatchingNodeSet nodeSet) {
	int level = IMPOSSIBLE_MATCH;
	for (int i = 0, length = this.patternLocators.length; i < length; i++) {
		int newLevel = this.patternLocators[i].match(node, nodeSet);
		if (newLevel > level) {
			if (newLevel == ACCURATE_MATCH) return ACCURATE_MATCH;
			level = newLevel;
		}
	}
	return level;
}

public int match(MessageSend node, MatchingNodeSet nodeSet) {
	int level = IMPOSSIBLE_MATCH;
	for (int i = 0, length = this.patternLocators.length; i < length; i++) {
		int newLevel = this.patternLocators[i].match(node, nodeSet);
		if (newLevel > level) {
			if (newLevel == ACCURATE_MATCH) return ACCURATE_MATCH;
			level = newLevel;
		}
	}
	return level;
}
public int match(Reference node, MatchingNodeSet nodeSet) {
	int level = IMPOSSIBLE_MATCH;
	for (int i = 0, length = this.patternLocators.length; i < length; i++) {
		int newLevel = this.patternLocators[i].match(node, nodeSet);
		if (newLevel > level) {
			if (newLevel == ACCURATE_MATCH) return ACCURATE_MATCH;
			level = newLevel;
		}
	}
	return level;
}
public int match(TypeDeclaration node, MatchingNodeSet nodeSet) {
	int level = IMPOSSIBLE_MATCH;
	for (int i = 0, length = this.patternLocators.length; i < length; i++) {
		int newLevel = this.patternLocators[i].match(node, nodeSet);
		if (newLevel > level) {
			if (newLevel == ACCURATE_MATCH) return ACCURATE_MATCH;
			level = newLevel;
		}
	}
	return level;
}

public int match(InferredType node, MatchingNodeSet nodeSet) {
	int level = IMPOSSIBLE_MATCH;
	for (int i = 0, length = this.patternLocators.length; i < length; i++) {
		int newLevel = this.patternLocators[i].match(node, nodeSet);
		if (newLevel > level) {
			if (newLevel == ACCURATE_MATCH) return ACCURATE_MATCH;
			level = newLevel;
		}
	}
	return level;
}

public int match(TypeReference node, MatchingNodeSet nodeSet) {
	int level = IMPOSSIBLE_MATCH;
	for (int i = 0, length = this.patternLocators.length; i < length; i++) {
		int newLevel = this.patternLocators[i].match(node, nodeSet);
		if (newLevel > level) {
			if (newLevel == ACCURATE_MATCH) return ACCURATE_MATCH;
			level = newLevel;
		}
	}
	return level;
}
protected int matchContainer() {
	int result = 0;
	for (int i = 0, length = this.patternLocators.length; i < length; i++)
		result |= this.patternLocators[i].matchContainer();
	return result;
}
protected void matchLevelAndReportImportRef(ImportReference importRef, Binding binding, MatchLocator locator) throws CoreException {
	Binding refBinding = binding;

	// Look for closest pattern
	PatternLocator closestPattern = null;
	int level = IMPOSSIBLE_MATCH;
	for (int i = 0, length = this.patternLocators.length; i < length; i++) {
		PatternLocator patternLocator = this.patternLocators[i];
		int newLevel = patternLocator.referenceType() == 0 ? IMPOSSIBLE_MATCH : patternLocator.resolveLevel(refBinding);
		if (newLevel > level) {
			closestPattern = patternLocator;
			if (newLevel == ACCURATE_MATCH) break;
			level = newLevel;
		}
	}
	if (closestPattern != null) {
		closestPattern.matchLevelAndReportImportRef(importRef, binding, locator);
	}
}
protected void matchReportImportRef(ImportReference importRef, Binding binding, IJavaScriptElement element, int accuracy, MatchLocator locator) throws CoreException {
	PatternLocator closestPattern = null;
	int level = IMPOSSIBLE_MATCH;
	for (int i = 0, length = this.patternLocators.length; i < length; i++) {
		int newLevel = this.patternLocators[i].matchLevel(importRef);
		if (newLevel > level) {
			closestPattern = this.patternLocators[i];
			if (newLevel == ACCURATE_MATCH) break;
			level = newLevel;
		}
	}
	if (closestPattern != null)
		closestPattern.matchReportImportRef(importRef, binding, element, accuracy, locator);
}
protected void matchReportReference(ASTNode reference, IJavaScriptElement element, Binding elementBinding, int accuracy, MatchLocator locator) throws CoreException {
	PatternLocator closestPattern = null;
	int level = IMPOSSIBLE_MATCH;
	for (int i = 0, length = this.patternLocators.length; i < length; i++) {
		PatternLocator patternLocator = this.patternLocators[i];
		int newLevel = patternLocator.referenceType() == 0 ? IMPOSSIBLE_MATCH : patternLocator.resolveLevel(reference);
		if (newLevel > level) {
			closestPattern = patternLocator;
			if (newLevel == ACCURATE_MATCH) break;
			level = newLevel;
		}
	}
	if (closestPattern != null)
		closestPattern.matchReportReference(reference, element, elementBinding, accuracy, locator);
}
public SearchMatch newDeclarationMatch(ASTNode reference, IJavaScriptElement element, Binding elementBinding, int accuracy, int length, MatchLocator locator) {
	PatternLocator closestPattern = null;
	int level = IMPOSSIBLE_MATCH;
	for (int i = 0, pl = this.patternLocators.length; i < pl; i++) {
		PatternLocator patternLocator = this.patternLocators[i];
		int newLevel = patternLocator.referenceType() == 0 ? IMPOSSIBLE_MATCH : patternLocator.resolveLevel(reference);
		if (newLevel > level) {
			closestPattern = patternLocator;
			if (newLevel == ACCURATE_MATCH) break;
			level = newLevel;
		}
	}
	if (closestPattern != null) {
	    return closestPattern.newDeclarationMatch(reference, element, elementBinding, accuracy, length, locator);
	}
	// super implementation...
    return locator.newDeclarationMatch(element, elementBinding, accuracy, reference.sourceStart, length);
}
public int resolveLevel(ASTNode node) {
	int level = IMPOSSIBLE_MATCH;
	for (int i = 0, length = this.patternLocators.length; i < length; i++) {
		int newLevel = this.patternLocators[i].resolveLevel(node);
		if (newLevel > level) {
			if (newLevel == ACCURATE_MATCH) return ACCURATE_MATCH;
			level = newLevel; // want to answer the stronger match
		}
	}
	return level;
}
public int resolveLevel(Binding binding) {
	int level = IMPOSSIBLE_MATCH;
	for (int i = 0, length = this.patternLocators.length; i < length; i++) {
		int newLevel = this.patternLocators[i].resolveLevel(binding);
		if (newLevel > level) {
			if (newLevel == ACCURATE_MATCH) return ACCURATE_MATCH;
			level = newLevel; // want to answer the stronger match
		}
	}
	return level;
}
}
