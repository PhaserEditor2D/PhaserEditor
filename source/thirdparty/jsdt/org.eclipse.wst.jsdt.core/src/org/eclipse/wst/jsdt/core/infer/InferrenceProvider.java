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
 * Implemented by contributors to the org.eclipse.wst.jsdt.core.infer.inferrenceSupport extension point
 * 
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public interface InferrenceProvider {
	
	/**
	 * <p>Indicates that this InferrenceProvider, and its engine, are the only ones that should apply.</p>
	 * <p>Its use is discouraged.</p>
	 */
	public static final int ONLY_THIS = 1;

	/**
	 * <p>Indicates that this InferrenceProvider, and its engine, do not apply.</p>
	 */
	public static final int NOT_THIS = 2;
	
	/**
	 * <p>Indicates that this InferrenceProvider, and its engine, should apply to a script file.</p>
	 */
	public static final int MAYBE_THIS = 3;
	
	/**
	 * Get the inference engine for this inference provider, or null if one will not be provided.  Implementors returning null are expected to return {@link #NOT_THIS} for all calls to {@link #getInferEngine()}
	 * @return an inference engine
	 */
	public IInferEngine getInferEngine();
	
	
	/**
	 * Determine if this inference provider applies to a script
	 * @param scriptFile The script that the inferencing will be done for
	 * @return  {@link #ONLY_THIS}, {@link #NOT_THIS}, {@link #MAYBE_THIS} depending on how much
	 * this inference provider applies to the specified script.
	 */
	public int applysTo(IInferenceFile scriptFile);


	/**
	 * Get the inference provider ID
	 * @return the id of this inference provider
	 */
	public String getID();
	

	/**
	 * @return the ResolutionConfiguration used to resolve the inferred classes
	 */
	public ResolutionConfiguration getResolutionConfiguration();

	/**
	 * @return the RefactoringSupport used to provide refactoring for inferred
	 *         types, or null if it is not offered.
	 */
	public RefactoringSupport getRefactoringSupport();
}
