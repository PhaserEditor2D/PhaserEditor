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
package org.eclipse.wst.jsdt.core.infer;

import java.util.HashMap;
import java.util.Map;


/**
 * 
 * Inference Options
 * 
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public class InferOptions {

	public static final String OPTION_UseAssignments = "org.eclipse.wst.jsdt.core.infer.useAssignments"; //$NON-NLS-1$
	public static final String OPTION_UseInitMethod = "org.eclipse.wst.jsdt.core.infer.useInitMethod"; //$NON-NLS-1$
	public static final String OPTION_SaveArgumentComments = "org.eclipse.wst.jsdt.core.infer.saveArgumentComments"; //$NON-NLS-1$
	public static final String OPTION_DocLocation = "org.eclipse.wst.jsdt.core.infer.docLocation"; //$NON-NLS-1$

	
	public static final int DOC_LOCATION_BEFORE=1;
	public static final int DOC_LOCATION_AFTER=2;


	// tags used to recognize tasks in comments
	public char[][] systemClassMethod = null;

	/**
	 * Set to true var types are inferred based on assigments
	 */
	public boolean useAssignments=true;

	public boolean useInitMethod;
	public String engineClass;
    public boolean saveArgumentComments;
    public int docLocation=DOC_LOCATION_BEFORE;
    
	
	

	/**
	 * Initializing the compiler options with defaults
	 */
	public InferOptions(){
		// use default options
		setDefaultOptions();
	}

	/**
	 * Initializing the compiler options with external settings
	 * @param settings
	 */
	public InferOptions(Map settings){

		if (settings == null) return;
		set(settings);
	}

	public void setDefaultOptions()
	{
		this.useAssignments=true;
		this.useInitMethod=true;
		this.saveArgumentComments=true;
	}

	public Map getMap() {
		Map optionsMap = new HashMap(30);
		optionsMap.put(OPTION_UseAssignments, this.useAssignments ? "true":"false"); //$NON-NLS-1$ //$NON-NLS-2$
		optionsMap.put(OPTION_UseInitMethod, this.useInitMethod ? "true":"false"); //$NON-NLS-1$ //$NON-NLS-2$
		optionsMap.put(OPTION_SaveArgumentComments, this.saveArgumentComments ? "true":"false"); //$NON-NLS-1$ //$NON-NLS-2$
		optionsMap.put(OPTION_DocLocation, String.valueOf(this.docLocation)); //$NON-NLS-1$ //$NON-NLS-2$
		return optionsMap;
	}


	public void set(Map optionsMap) {

		Object optionValue;
		if ((optionValue = optionsMap.get(OPTION_UseAssignments)) != null) {
			this.useAssignments="true".equals(optionValue) ; //$NON-NLS-1$
		}
		if ((optionValue = optionsMap.get(OPTION_UseInitMethod)) != null) {
			this.useInitMethod="true".equals(optionValue) ; //$NON-NLS-1$
		}	
		if ((optionValue = optionsMap.get(OPTION_SaveArgumentComments)) != null) {
			this.saveArgumentComments="true".equals(optionValue) ; //$NON-NLS-1$
		}
		if ((optionValue = optionsMap.get(OPTION_DocLocation)) != null) {
			this.docLocation=   Integer.parseInt((String)optionValue) ; //$NON-NLS-1$
		}
	}

	public String toString() {

		StringBuffer buf = new StringBuffer("InferOptions:"); //$NON-NLS-1$
		buf.append("\n\t- use assignments: ").append( this.useAssignments ? "ON" : " OFF"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		buf.append("\n\t- use initialization method : ").append( this.useInitMethod ? "ON" : " OFF"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return buf.toString();
	}


	public InferEngine createEngine()
	{
		if (engineClass!=null)
		{
			try {
				InferEngine engine= (InferEngine) Class.forName(engineClass).newInstance();
				engine.inferOptions=this;
				return engine;
			} catch (Exception ex)
			{
				ex.printStackTrace();
				//TODO: implement something
			}
		}
		return new InferEngine(this);
	}
}
