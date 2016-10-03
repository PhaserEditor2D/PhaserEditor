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
package org.eclipse.wst.jsdt.internal.ui.search;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.StringTokenizer;

import org.eclipse.search.ui.text.Match;
import org.eclipse.search.ui.text.MatchFilter;
import org.eclipse.wst.jsdt.core.IField;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IImportDeclaration;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.ILocalVariable;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchConstants;
import org.eclipse.wst.jsdt.core.search.SearchMatch;
import org.eclipse.wst.jsdt.core.search.SearchPattern;
import org.eclipse.wst.jsdt.internal.corext.util.JdtFlags;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.ui.search.ElementQuerySpecification;
import org.eclipse.wst.jsdt.ui.search.PatternQuerySpecification;
import org.eclipse.wst.jsdt.ui.search.QuerySpecification;

abstract class JavaMatchFilter extends MatchFilter {
	
	public abstract boolean filters(JavaElementMatch match);
	
	/**
	 * Returns whether this filter is applicable for this query
	 * @param query 
	 * @return returns <code>true</code> if this match filter is applicable for the given query
	 */
	public abstract boolean isApplicable(JavaSearchQuery query);
	
	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.text.MatchFilter#filters(org.eclipse.search.ui.text.Match)
	 */
	public boolean filters(Match match) {
		if (match instanceof JavaElementMatch) {
			return filters((JavaElementMatch) match);
		}
		return false;
	}
	
	private static final String SETTINGS_LAST_USED_FILTERS= "filters_last_used";  //$NON-NLS-1$
	
	public static MatchFilter[] getLastUsedFilters() {
		String string= JavaScriptPlugin.getDefault().getDialogSettings().get(SETTINGS_LAST_USED_FILTERS);
		if (string != null && string.length() > 0) {
			return decodeFiltersString(string);
		}
		return getDefaultFilters();
	}
	
	public static void setLastUsedFilters(MatchFilter[] filters) {
		String encoded= encodeFilters(filters);
		JavaScriptPlugin.getDefault().getDialogSettings().put(SETTINGS_LAST_USED_FILTERS, encoded);
	}
	
	public static MatchFilter[] getDefaultFilters() {
		return new MatchFilter[] { IMPORT_FILTER };
	}
	
	private static String encodeFilters(MatchFilter[] enabledFilters) {
		StringBuffer buf= new StringBuffer();
		for (int i= 0; i < enabledFilters.length; i++) {
			MatchFilter matchFilter= enabledFilters[i];
			buf.append(matchFilter.getID());
			buf.append(';');
		}
		return buf.toString();
	}
	
	private static JavaMatchFilter[] decodeFiltersString(String encodedString) {
		StringTokenizer tokenizer= new StringTokenizer(encodedString, String.valueOf(';'));
		HashSet result= new HashSet();
		while (tokenizer.hasMoreTokens()) {
			JavaMatchFilter curr= findMatchFilter(tokenizer.nextToken());
			if (curr != null) {
				result.add(curr);
			}
		}
		return (JavaMatchFilter[]) result.toArray(new JavaMatchFilter[result.size()]);
	}
		
	private static final JavaMatchFilter POTENTIAL_FILTER= new PotentialFilter(); 
	private static final JavaMatchFilter IMPORT_FILTER= new ImportFilter(); 
	private static final JavaMatchFilter JAVADOC_FILTER= new JavadocFilter(); 
	private static final JavaMatchFilter READ_FILTER= new ReadFilter(); 
	private static final JavaMatchFilter WRITE_FILTER= new WriteFilter(); 
	
	//private static final JavaMatchFilter POLYMORPHIC_FILTER= new PolymorphicFilter(); 
	private static final JavaMatchFilter INEXACT_FILTER= new InexactMatchFilter(); 
	private static final JavaMatchFilter ERASURE_FILTER= new ErasureMatchFilter(); 
	
	//private static final JavaMatchFilter NON_PUBLIC_FILTER= new NonPublicFilter();
	private static final JavaMatchFilter STATIC_FILTER= new StaticFilter();
	private static final JavaMatchFilter NON_STATIC_FILTER= new NonStaticFilter();
	private static final JavaMatchFilter DEPRECATED_FILTER= new DeprecatedFilter();
	private static final JavaMatchFilter NON_DEPRECATED_FILTER= new NonDeprecatedFilter();
	
	private static final JavaMatchFilter[] ALL_FILTERS= new JavaMatchFilter[] {
			POTENTIAL_FILTER,
			IMPORT_FILTER,
			JAVADOC_FILTER,
			READ_FILTER,
			WRITE_FILTER,
			
            /*POLYMORPHIC_FILTER,*/
			INEXACT_FILTER,
			ERASURE_FILTER,
			
		/*	NON_PUBLIC_FILTER,*/
			STATIC_FILTER,
			NON_STATIC_FILTER,
			DEPRECATED_FILTER,
			NON_DEPRECATED_FILTER
	};
		
	public static JavaMatchFilter[] allFilters() {
		return ALL_FILTERS;
	}
	
	public static JavaMatchFilter[] allFilters(JavaSearchQuery query) {
		ArrayList res= new ArrayList();
		for (int i= 0; i < ALL_FILTERS.length; i++) {
			JavaMatchFilter curr= ALL_FILTERS[i];
			if (curr.isApplicable(query)) {
				res.add(curr);
			}
		}
		return (JavaMatchFilter[]) res.toArray(new JavaMatchFilter[res.size()]);
	}
	
	private static JavaMatchFilter findMatchFilter(String id) {
		for (int i= 0; i < ALL_FILTERS.length; i++) {
			JavaMatchFilter matchFilter= ALL_FILTERS[i];
			if (matchFilter.getID().equals(id))
				return matchFilter;
		}
		return null;
	}
}

class PotentialFilter extends JavaMatchFilter {
	public boolean filters(JavaElementMatch match) {
		return match.getAccuracy() == SearchMatch.A_INACCURATE;
	}
	
	public String getName() {
		return SearchMessages.MatchFilter_PotentialFilter_name; 
	}
	
	public String getActionLabel() {
		return SearchMessages.MatchFilter_PotentialFilter_actionLabel; 
	}
	
	public String getDescription() {
		return SearchMessages.MatchFilter_PotentialFilter_description; 
	}
	
	public boolean isApplicable(JavaSearchQuery query) {
		return true;
	}
	
	public String getID() {
		return "filter_potential"; //$NON-NLS-1$
	}
}

class ImportFilter extends JavaMatchFilter {
	public boolean filters(JavaElementMatch match) {
		return match.getElement() instanceof IImportDeclaration;
	}

	public String getName() {
		return SearchMessages.MatchFilter_ImportFilter_name; 
	}

	public String getActionLabel() {
		return SearchMessages.MatchFilter_ImportFilter_actionLabel; 
	}

	public String getDescription() {
		return SearchMessages.MatchFilter_ImportFilter_description; 
	}
	
	public boolean isApplicable(JavaSearchQuery query) {
		QuerySpecification spec= query.getSpecification();
		if (spec instanceof ElementQuerySpecification) {
			ElementQuerySpecification elementSpec= (ElementQuerySpecification) spec;
			IJavaScriptElement element= elementSpec.getElement();
			return element instanceof IType || element instanceof IPackageFragment;
		} else if (spec instanceof PatternQuerySpecification) {
			PatternQuerySpecification patternSpec= (PatternQuerySpecification) spec;
			int searchFor= patternSpec.getSearchFor();
			return searchFor == IJavaScriptSearchConstants.TYPE || searchFor == IJavaScriptSearchConstants.PACKAGE;
		}
		return false;
	}

	public String getID() {
		return "filter_imports"; //$NON-NLS-1$
	}
}

abstract class VariableFilter extends JavaMatchFilter {
	public boolean isApplicable(JavaSearchQuery query) {
		QuerySpecification spec= query.getSpecification();
		if (spec instanceof ElementQuerySpecification) {
			ElementQuerySpecification elementSpec= (ElementQuerySpecification) spec;
			IJavaScriptElement element= elementSpec.getElement();
			return element instanceof IField || element instanceof ILocalVariable;
		} else if (spec instanceof PatternQuerySpecification) {
			PatternQuerySpecification patternSpec= (PatternQuerySpecification) spec;
			return patternSpec.getSearchFor() == IJavaScriptSearchConstants.FIELD;
		}
		return false;
	}

}

class WriteFilter extends VariableFilter {
	public boolean filters(JavaElementMatch match) {
		return match.isWriteAccess() && !match.isReadAccess();
	}
	public String getName() {
		return SearchMessages.MatchFilter_WriteFilter_name; 
	}
	public String getActionLabel() {
		return SearchMessages.MatchFilter_WriteFilter_actionLabel; 
	}
	public String getDescription() {
		return SearchMessages.MatchFilter_WriteFilter_description; 
	}
	public String getID() {
		return "filter_writes"; //$NON-NLS-1$
	}
}

class ReadFilter extends VariableFilter {
	public boolean filters(JavaElementMatch match) {
		return match.isReadAccess() && !match.isWriteAccess();
	}
	public String getName() {
		return SearchMessages.MatchFilter_ReadFilter_name; 
	}
	public String getActionLabel() {
		return SearchMessages.MatchFilter_ReadFilter_actionLabel; 
	}
	public String getDescription() {
		return SearchMessages.MatchFilter_ReadFilter_description; 
	}
	public String getID() {
		return "filter_reads"; //$NON-NLS-1$
	}
}

class JavadocFilter extends JavaMatchFilter {
	public boolean filters(JavaElementMatch match) {
		return match.isJavadoc();
	}
	public String getName() {
		return SearchMessages.MatchFilter_JavadocFilter_name; 
	}
	public String getActionLabel() {
		return SearchMessages.MatchFilter_JavadocFilter_actionLabel; 
	}
	public String getDescription() {
		return SearchMessages.MatchFilter_JavadocFilter_description; 
	}
	public boolean isApplicable(JavaSearchQuery query) {
		return true;
	}
	public String getID() {
		return "filter_javadoc"; //$NON-NLS-1$
	}
}

class PolymorphicFilter extends JavaMatchFilter {
    public boolean filters(JavaElementMatch match) {
        return match.isSuperInvocation();
    }

    public String getName() {
        return SearchMessages.MatchFilter_PolymorphicFilter_name; 
    }

    public String getActionLabel() {
        return SearchMessages.MatchFilter_PolymorphicFilter_actionLabel; 
    }

    public String getDescription() {
        return SearchMessages.MatchFilter_PolymorphicFilter_description; 
    }
    
    public boolean isApplicable(JavaSearchQuery query) {
        QuerySpecification spec= query.getSpecification();
        switch (spec.getLimitTo()) {
			case IJavaScriptSearchConstants.REFERENCES:
			case IJavaScriptSearchConstants.ALL_OCCURRENCES:
                if (spec instanceof ElementQuerySpecification) {
                    ElementQuerySpecification elementSpec= (ElementQuerySpecification) spec;
                    return elementSpec.getElement() instanceof IFunction;
                } else if (spec instanceof PatternQuerySpecification) {
                    PatternQuerySpecification patternSpec= (PatternQuerySpecification) spec;
                    return patternSpec.getSearchFor() == IJavaScriptSearchConstants.METHOD;
                }
        }
        return false;
    }

    public String getID() {
        return "filter_polymorphic"; //$NON-NLS-1$
    }
}

abstract class GenericTypeFilter extends JavaMatchFilter {
	public boolean isApplicable(JavaSearchQuery query) {
		return false;
	}
}

class ErasureMatchFilter extends GenericTypeFilter {
	public boolean filters(JavaElementMatch match) {
		return (match.getMatchRule() & (SearchPattern.R_FULL_MATCH | SearchPattern.R_EQUIVALENT_MATCH)) == 0;
	}
	public String getName() {
		return SearchMessages.MatchFilter_ErasureFilter_name; 
	}
	public String getActionLabel() {
		return SearchMessages.MatchFilter_ErasureFilter_actionLabel; 
	}
	public String getDescription() {
		return SearchMessages.MatchFilter_ErasureFilter_description; 
	}
	public String getID() {
		return "filter_erasure"; //$NON-NLS-1$
	}
}

class InexactMatchFilter extends GenericTypeFilter {
	public boolean filters(JavaElementMatch match) {
		return (match.getMatchRule() & (SearchPattern.R_FULL_MATCH)) == 0;
	}
	public String getName() {
		return SearchMessages.MatchFilter_InexactFilter_name; 
	}
	public String getActionLabel() {
		return SearchMessages.MatchFilter_InexactFilter_actionLabel; 
	}
	public String getDescription() {
		return SearchMessages.MatchFilter_InexactFilter_description; 
	}
	public String getID() {
		return "filter_inexact"; //$NON-NLS-1$
	}
}

abstract class ModifierFilter extends JavaMatchFilter {
	public boolean isApplicable(JavaSearchQuery query) {
		return true;
	}
}

class NonPublicFilter extends ModifierFilter {
	public boolean filters(JavaElementMatch match) {
		Object element= match.getElement();
		if (element instanceof IMember) {
			try {
				return ! JdtFlags.isPublic((IMember) element);
			} catch (JavaScriptModelException e) {
				JavaScriptPlugin.log(e);
			}
		}
		return false;
	}
	public String getName() {
		return SearchMessages.MatchFilter_NonPublicFilter_name;
	}
	public String getActionLabel() {
		return SearchMessages.MatchFilter_NonPublicFilter_actionLabel;
	}
	public String getDescription() {
		return SearchMessages.MatchFilter_NonPublicFilter_description;
	}
	public String getID() {
		return "filter_non_public"; //$NON-NLS-1$
	}
}

class StaticFilter extends ModifierFilter {
	public boolean filters(JavaElementMatch match) {
		Object element= match.getElement();
		if (element instanceof IMember) {
			try {
				return JdtFlags.isStatic((IMember) element);
			} catch (JavaScriptModelException e) {
				JavaScriptPlugin.log(e);
			}
		}
		return false;
	}
	public String getName() {
		return SearchMessages.MatchFilter_StaticFilter_name;
	}
	public String getActionLabel() {
		return SearchMessages.MatchFilter_StaticFilter_actionLabel;
	}
	public String getDescription() {
		return SearchMessages.MatchFilter_StaticFilter_description;
	}
	public String getID() {
		return 	"filter_static"; //$NON-NLS-1$
	}
}

class NonStaticFilter extends ModifierFilter {
	public boolean filters(JavaElementMatch match) {
		Object element= match.getElement();
		if (element instanceof IMember) {
			try {
				return ! JdtFlags.isStatic((IMember) element);
			} catch (JavaScriptModelException e) {
				JavaScriptPlugin.log(e);
			}
		}
		return false;
	}
	public String getName() {
		return SearchMessages.MatchFilter_NonStaticFilter_name;
	}
	public String getActionLabel() {
		return SearchMessages.MatchFilter_NonStaticFilter_actionLabel;
	}
	public String getDescription() {
		return SearchMessages.MatchFilter_NonStaticFilter_description;
	}
	public String getID() {
		return 	"filter_non_static"; //$NON-NLS-1$
	}
}

class DeprecatedFilter extends ModifierFilter {
	public boolean filters(JavaElementMatch match) {
		Object element= match.getElement();
		if (element instanceof IMember) {
			try {
				return JdtFlags.isDeprecated((IMember) element);
			} catch (JavaScriptModelException e) {
				JavaScriptPlugin.log(e);
			}
		}
		return false;
	}
	public String getName() {
		return SearchMessages.MatchFilter_DeprecatedFilter_name;
	}
	public String getActionLabel() {
		return SearchMessages.MatchFilter_DeprecatedFilter_actionLabel;
	}
	public String getDescription() {
		return SearchMessages.MatchFilter_DeprecatedFilter_description;
	}
	public String getID() {
		return 	"filter_deprecated"; //$NON-NLS-1$
	}
}

class NonDeprecatedFilter extends ModifierFilter {
	public boolean filters(JavaElementMatch match) {
		Object element= match.getElement();
		if (element instanceof IMember) {
			try {
				return !JdtFlags.isDeprecated((IMember) element);
			} catch (JavaScriptModelException e) {
				JavaScriptPlugin.log(e);
			}
		}
		return false;
	}
	public String getName() {
		return SearchMessages.MatchFilter_NonDeprecatedFilter_name;
	}
	public String getActionLabel() {
		return SearchMessages.MatchFilter_NonDeprecatedFilter_actionLabel;
	}
	public String getDescription() {
		return SearchMessages.MatchFilter_NonDeprecatedFilter_description;
	}
	public String getID() {
		return 	"filter_non_deprecated"; //$NON-NLS-1$
	}
}
