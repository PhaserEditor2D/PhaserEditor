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
package org.eclipse.wst.jsdt.internal.core.builder;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.IPath;
import org.eclipse.wst.jsdt.internal.compiler.env.AccessRuleSet;
import org.eclipse.wst.jsdt.internal.compiler.env.NameEnvironmentAnswer;

public abstract class ClasspathLocation {

static ClasspathLocation forSourceFolder(IContainer sourceFolder, IContainer outputFolder, char[][] inclusionPatterns, char[][] exclusionPatterns) {
	return new ClasspathMultiDirectory(sourceFolder, outputFolder, inclusionPatterns, exclusionPatterns);
}

public static ClasspathLocation forBinaryFolder(IContainer binaryFolder, boolean isOutputFolder, AccessRuleSet accessRuleSet) {
	return new ClasspathDirectory(binaryFolder, isOutputFolder, accessRuleSet);
}

public static ClasspathLocation forLibrary(String libraryPathname, long lastModified, AccessRuleSet accessRuleSet) {
	return new ClasspathLibrary(libraryPathname, lastModified, accessRuleSet);
}

public static ClasspathLocation forLibrary(String libraryPathname, AccessRuleSet accessRuleSet) {
	return forLibrary(libraryPathname, 0, accessRuleSet);
}


public abstract NameEnvironmentAnswer findClass(String binaryFileName, String qualifiedPackageName, String qualifiedBinaryFileName);

public abstract IPath getProjectRelativePath();

public boolean isOutputFolder() {
	return false;
}

public abstract boolean isPackage(String qualifiedPackageName);

public void cleanup() {
	// free anything which is not required when the state is saved
}
public void reset() {
	// reset any internal caches before another compile loop starts
}

public abstract String debugPathString();

}
