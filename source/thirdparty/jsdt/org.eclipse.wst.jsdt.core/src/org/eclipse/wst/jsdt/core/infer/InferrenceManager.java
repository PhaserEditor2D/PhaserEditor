/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.core.infer;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.wst.jsdt.internal.core.Logger;
import org.eclipse.wst.jsdt.internal.core.util.Util;


/**
 * 
 * Internal
 * 
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public class InferrenceManager {

	private static final boolean _debugInfer = "true".equalsIgnoreCase(Platform.getDebugOption("org.eclipse.wst.jsdt.core/debug/inferEngine")); //$NON-NLS-1$  //$NON-NLS-2$
	public static final String EXTENSION_POINT= "inferrenceSupport"; //$NON-NLS-1$

	protected static final String TAG_INFERENCE_PROVIDER = "inferenceProvider"; //$NON-NLS-1$
	protected static final String ATTR_INFERENGINE_CLASS = "class"; //$NON-NLS-1$


	private static InferrenceManager instance = null;


	private  InferrenceSupportExtension [] extensions;

	public static InferrenceManager getInstance(){
		if( instance == null )
			instance = new InferrenceManager();

		return instance;
	}



	public InferrenceProvider [] getInferenceProviders()
	{

		if (extensions==null)
		{
			loadInferenceExtensions();
		}
		ArrayList extProviders=new ArrayList();
		extProviders.add(new DefaultInferrenceProvider());
		for (int i = 0; i < extensions.length; i++) {
			  if (extensions[i].inferProvider!=null)
				  extProviders.add(extensions[i].inferProvider);
			}
		return (InferrenceProvider [] )extProviders.toArray(new InferrenceProvider[extProviders.size()]);
	}


	public InferrenceProvider [] getInferenceProviders(IInferenceFile script)
	{
		List<InferrenceProvider> proposedProviders = new ArrayList<InferrenceProvider>();
		InferrenceProvider[] inferenceProviders = getInferenceProviders();

		if (inferenceProviders.length==1)
			return new InferrenceProvider [] {inferenceProviders[0]};
		else if (inferenceProviders.length>1){		
			//Always add the default provider - ModuleInferrenceProvider
			proposedProviders.add(inferenceProviders[0]);
		}		

		for (int i = 1; i < inferenceProviders.length; i++) {

			if (inferenceProviders[i].getID().equals(script.getInferenceID())) {
				proposedProviders.clear();
				proposedProviders.add(inferenceProviders[0]);
				proposedProviders.add(inferenceProviders[i]);
				break;
			}

			int applies = InferrenceProvider.NOT_THIS;
			try {
				applies = inferenceProviders[i].applysTo(script);
			} catch (Exception e) {
				Util.log(e, "exception in inference provider "+inferenceProviders[i].getID());
			}
			switch (applies) {
				case InferrenceProvider.MAYBE_THIS:
					proposedProviders.add(inferenceProviders[i]);
					break;

				case InferrenceProvider.ONLY_THIS:
					proposedProviders.clear();
					proposedProviders.add(inferenceProviders[i]);
					return (InferrenceProvider [] )proposedProviders.toArray(new InferrenceProvider[proposedProviders.size()]);

				default:
					break;
			}
		}
		if (_debugInfer){
			StringBuilder sb = new StringBuilder("Proposed Inference Providers: "); //$NON-NLS-1$
			for (InferrenceProvider inferrenceProvider : inferenceProviders) {
				sb.append(inferrenceProvider.getID());
				sb.append(", ");  //$NON-NLS-1$
			}
			Logger.log(Logger.INFO_DEBUG, sb.toString());
		}
	
		return (InferrenceProvider [] )proposedProviders.toArray(new InferrenceProvider[proposedProviders.size()]);
	}
	
	/**
	 *	The base Inference Engine is always added first. This method adds 
	 *  additional inference engines.
	 * 
	 * @param script
	 * @return
	 */
	public IInferEngine [] getInferenceEngines(CompilationUnitDeclaration script)
	{
		List proposedEngines = new ArrayList();
		InferrenceProvider[] inferenceProviders = getInferenceProviders(script);
		
		for (int i = 0; i < inferenceProviders.length; i++) {
			  proposedEngines.add(inferenceProviders[i].getInferEngine()) ;
		}
		return (IInferEngine [] )proposedEngines.toArray(new IInferEngine[proposedEngines.size()]);
	}
	
	protected void loadInferenceExtensions() {
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		ArrayList extList = new ArrayList();
		if (registry != null) {
			IExtensionPoint point = registry.getExtensionPoint(
					JavaScriptCore.PLUGIN_ID, EXTENSION_POINT);

			if (point != null) {
				IExtension[] extensions = point.getExtensions();
				for (int i = 0; i < extensions.length; i++) {
					IConfigurationElement[] elements = extensions[i]
							.getConfigurationElements();
					for (int j = 0; j < elements.length; j++) {
						try {
							InferrenceProvider inferProvider = null;
							if (elements[j].getName().equals(TAG_INFERENCE_PROVIDER)) {
								inferProvider = (InferrenceProvider) elements[j]
										.createExecutableExtension(ATTR_INFERENGINE_CLASS);
							}
							InferrenceSupportExtension inferenceSupport = new InferrenceSupportExtension();
							inferenceSupport.inferProvider = inferProvider;

							extList.add(inferenceSupport);
						} catch (CoreException e) {
							Util.log(e, "Error in loading inference extension");
						}
					}
				}
			}
		}

		this.extensions = (InferrenceSupportExtension[]) extList
				.toArray(new InferrenceSupportExtension[extList.size()]);
	}


}
