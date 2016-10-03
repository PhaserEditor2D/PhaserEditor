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
package org.eclipse.wst.jsdt.internal.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.templates.ContextTypeRegistry;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.persistence.TemplatePersistenceData;
import org.eclipse.jface.text.templates.persistence.TemplateStore;
import org.eclipse.ui.editors.text.templates.ContributionTemplateStore;


/**
 * for compatibility only - don't use
 */
public final class CompatibilityTemplateStore extends ContributionTemplateStore {


	private org.eclipse.wst.jsdt.internal.corext.template.java.TemplateSet fLegacySet;

	public CompatibilityTemplateStore(ContextTypeRegistry registry, IPreferenceStore store, String key, org.eclipse.wst.jsdt.internal.corext.template.java.TemplateSet legacySet) {
		super(registry, store, key);
		fLegacySet= legacySet;
	}

	public void load() throws IOException {
		super.load();
		
		if (fLegacySet != null) {
			
			List legacyTemplates= new ArrayList(Arrays.asList(fLegacySet.getTemplates()));
			fLegacySet.clear();
			
			TemplatePersistenceData[] datas= getTemplateData(true);
			for (Iterator it= legacyTemplates.listIterator(); it.hasNext();) {
				Template t= (Template) it.next();
				TemplatePersistenceData orig= findSimilarTemplate(datas, t, isCodeTemplates());
				if (orig == null) { // no contributed match for the old template found
					if (!isCodeTemplates())
						add(new TemplatePersistenceData(t, true));
				} else { // a contributed template seems to be the descendant of the non-id template t
					if (!orig.getTemplate().getPattern().equals(t.getPattern()))
						// add as modified contributed template if changed compared to the original
						orig.setTemplate(t);
				}
			}
			
			save();
			fLegacySet= null;
		}
	}
	
	private static TemplatePersistenceData findSimilarTemplate(TemplatePersistenceData[] datas, Template template, boolean isCodeTemplates) {
		 for (int i= 0; i < datas.length; i++) {
			TemplatePersistenceData data= datas[i];
			Template orig= data.getTemplate();
			if (isSimilar(template, orig, isCodeTemplates))
				return data;
		 }
		 
		 return null;
	}

	private static boolean isSimilar(Template t, Template orig, boolean isCodeTemplates) {
		return orig.getName().equals(t.getName()) && orig.getContextTypeId().equals(t.getContextTypeId())
				&& (isCodeTemplates || orig.getDescription().equals(t.getDescription())); // only use description for templates (for, while...)
	}
	
	private boolean isCodeTemplates() {
		return fLegacySet instanceof org.eclipse.wst.jsdt.internal.corext.template.java.CodeTemplates;
	}

	/**
	 * Removes any duplicates from a template store. Duplicate user added templates
	 * are copied over their contributed siblings. If isCodeTemplates is true, 
	 * any user added templates are then removed.
	 * 
	 * @param store
	 * @param isCodeTemplates
	 */
	public static void pruneDuplicates(TemplateStore store, boolean isCodeTemplates) {
		TemplatePersistenceData[] datas= store.getTemplateData(true);
		for (int i= datas.length - 1; i >= 0; i--) {
			TemplatePersistenceData data= datas[i];
			if (data.isUserAdded()) {
				// find a contributed template that is similar and check it
				TemplatePersistenceData similar= findSimilarTemplate(datas, data.getTemplate(), isCodeTemplates);
				if (similar != data && !similar.isUserAdded()) {
					similar.setTemplate(data.getTemplate());
					store.delete(data);
				}
			}
		}
		
		if (isCodeTemplates) {
			datas= store.getTemplateData(true);
			for (int i= datas.length - 1; i >= 0; i--) {
				if (datas[i].isUserAdded())
					store.delete(datas[i]);
			}
		}
	}
}
