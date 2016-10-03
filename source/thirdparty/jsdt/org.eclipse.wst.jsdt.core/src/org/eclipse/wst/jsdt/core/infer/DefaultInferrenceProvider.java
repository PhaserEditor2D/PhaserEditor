/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.core.infer;


/**
 * A default implementation of InferrenceProvider.  It uses the default Inference engine.
 * 
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public class DefaultInferrenceProvider implements InferrenceProvider {

	public static final String ID="org.eclipse.wst.jsdt.core.infer.DefaultInferrenceProvider";
	
	public int applysTo(IInferenceFile scriptFile) {
//		char[] fileNameChars = scriptFile.getFileName();
//		if (fileNameChars!=null) {
//
//			String fileName = new String(fileNameChars);
//			if (fileName.indexOf("org.eclipse.wst.jsdt.core/libraries")>=0) {
//				  return InferrenceProvider.ONLY_THIS;
//			}
//		}
		return InferrenceProvider.MAYBE_THIS;
	}

	public IInferEngine getInferEngine() {
		 InferEngine engine = new InferEngine();
		 engine.inferenceProvider=this;
		 return engine;
	}
 
	public String getID() {
		return ID;
	}

	public ResolutionConfiguration getResolutionConfiguration() {
		return new ResolutionConfiguration();
	}

	public RefactoringSupport getRefactoringSupport() {
		return null;
	}
}