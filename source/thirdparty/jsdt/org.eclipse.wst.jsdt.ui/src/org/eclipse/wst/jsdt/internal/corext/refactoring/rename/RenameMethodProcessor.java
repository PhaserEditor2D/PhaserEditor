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
package org.eclipse.wst.jsdt.internal.corext.refactoring.rename;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.GroupCategorySet;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;
import org.eclipse.ltk.core.refactoring.participants.RenameArguments;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.wst.jsdt.core.Flags;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IFunctionContainer;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.ITypeHierarchy;
import org.eclipse.wst.jsdt.core.JavaScriptConventions;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.WorkingCopyOwner;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.wst.jsdt.core.refactoring.IJavaScriptRefactorings;
import org.eclipse.wst.jsdt.core.refactoring.descriptors.JavaScriptRefactoringDescriptor;
import org.eclipse.wst.jsdt.core.refactoring.descriptors.RenameJavaScriptElementDescriptor;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchConstants;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchScope;
import org.eclipse.wst.jsdt.core.search.MethodDeclarationMatch;
import org.eclipse.wst.jsdt.core.search.SearchEngine;
import org.eclipse.wst.jsdt.core.search.SearchMatch;
import org.eclipse.wst.jsdt.core.search.SearchParticipant;
import org.eclipse.wst.jsdt.core.search.SearchPattern;
import org.eclipse.wst.jsdt.core.search.SearchRequestor;
import org.eclipse.wst.jsdt.internal.core.JavaElement;
import org.eclipse.wst.jsdt.internal.corext.refactoring.Checks;
import org.eclipse.wst.jsdt.internal.corext.refactoring.JDTRefactoringDescriptor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.JDTRefactoringDescriptorComment;
import org.eclipse.wst.jsdt.internal.corext.refactoring.JavaRefactoringArguments;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringAvailabilityTester;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringScopeFactory;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringSearchEngine;
import org.eclipse.wst.jsdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.wst.jsdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.DynamicValidationRefactoringChange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.TextChangeCompatibility;
import org.eclipse.wst.jsdt.internal.corext.refactoring.code.ScriptableRefactoring;
import org.eclipse.wst.jsdt.internal.corext.refactoring.delegates.DelegateCreator;
import org.eclipse.wst.jsdt.internal.corext.refactoring.delegates.DelegateMethodCreator;
import org.eclipse.wst.jsdt.internal.corext.refactoring.participants.JavaProcessors;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.wst.jsdt.internal.corext.refactoring.tagging.IDelegateUpdating;
import org.eclipse.wst.jsdt.internal.corext.refactoring.tagging.IReferenceUpdating;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.corext.util.JdtFlags;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.corext.util.SearchUtils;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.refactoring.RefactoringSaveHelper;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabels;

public abstract class RenameMethodProcessor extends JavaRenameProcessor implements IReferenceUpdating, IDelegateUpdating {

	private static final String ATTRIBUTE_DELEGATE= "delegate"; //$NON-NLS-1$
	private static final String ATTRIBUTE_DEPRECATE= "deprecate"; //$NON-NLS-1$

	private SearchResultGroup[] fOccurrences;
	private boolean fUpdateReferences;
	private IFunction fMethod;
	private Set/*<IFunction>*/ fMethodsToRename;
	private TextChangeManager fChangeManager;
	private WorkingCopyOwner fWorkingCopyOwner;
	private boolean fIsComposite;
	private GroupCategorySet fCategorySet;
	private boolean fDelegateUpdating;
	private boolean fDelegateDeprecation;
	protected boolean fInitialized= false;

	public static final String IDENTIFIER= "org.eclipse.wst.jsdt.ui.renameMethodProcessor"; //$NON-NLS-1$
	
	/**
	 * Creates a new rename method processor.
	 * @param method the method, or <code>null</code> if invoked by scripting
	 */
	protected RenameMethodProcessor(IFunction method) {
		this(method, new TextChangeManager(true), null);
		fIsComposite= false;
	}
	
	/**
	 * Creates a new rename method processor.
	 * <p>
	 * This constructor is only invoked by <code>RenameTypeProcessor</code>.
	 * </p>
	 * 
	 * @param method the method
	 * @param manager the change manager
	 * @param categorySet the group category set
	 */
	protected RenameMethodProcessor(IFunction method, TextChangeManager manager, GroupCategorySet categorySet) {
		initialize(method);
		fChangeManager= manager;
		fCategorySet= categorySet;
		fDelegateUpdating= false;
		fDelegateDeprecation= true;
		fIsComposite= true;
	}
	
	protected void initialize(IFunction method) {
		fMethod= method;
		if (!fInitialized) {
			if (method != null)
				setNewElementName(method.getElementName());
			fUpdateReferences= true;
			initializeWorkingCopyOwner();
		}		
	}

	protected void initializeWorkingCopyOwner() {
		fWorkingCopyOwner= new WorkingCopyOwner() {/*must subclass*/};
	}
	
	protected void setData(RenameMethodProcessor other) {
		fUpdateReferences= other.fUpdateReferences;
		setNewElementName(other.getNewElementName());
	}

	public String getIdentifier() {
		return IDENTIFIER;
	}

	public boolean isApplicable() throws CoreException {
		return RefactoringAvailabilityTester.isRenameAvailable(fMethod);
	}

	public String getProcessorName() {
		return RefactoringCoreMessages.RenameMethodRefactoring_name;
	}
	
	protected String[] getAffectedProjectNatures() throws CoreException {
		return JavaProcessors.computeAffectedNatures(fMethod);
	}

	public Object[] getElements() {
		return new Object[] {fMethod};
	}

	protected RenameModifications computeRenameModifications() throws CoreException {
		RenameModifications result= new RenameModifications();
		RenameArguments args= new RenameArguments(getNewElementName(), getUpdateReferences());
		for (Iterator iter= fMethodsToRename.iterator(); iter.hasNext();) {
			IFunction method= (IFunction) iter.next();
			result.rename(method, args);
		}
		return result;
	}
	
	protected IFile[] getChangedFiles() throws CoreException {
		return ResourceUtil.getFiles(fChangeManager.getAllCompilationUnits());
	}
	
	public int getSaveMode() {
		return RefactoringSaveHelper.SAVE_NON_JAVA_UPDATES;
	}
	
	//---- INameUpdating -------------------------------------
	
	public final String getCurrentElementName(){
		return fMethod.getElementName();
	}
		
	public final RefactoringStatus checkNewElementName(String newName) {
		Assert.isNotNull(newName, "new name"); //$NON-NLS-1$
				
		RefactoringStatus status= Checks.checkName(newName, JavaScriptConventions.validateFunctionName(newName));
		if (status.isOK() && Checks.startsWithUpperCase(newName))
			status= RefactoringStatus.createWarningStatus(fIsComposite 
					? Messages.format(RefactoringCoreMessages.Checks_method_names_lowercase2, new String[] { newName, fMethod.getDeclaringType().getElementName()})
					: RefactoringCoreMessages.Checks_method_names_lowercase);
		
		if (Checks.isAlreadyNamed(fMethod, newName))
			status.addFatalError(fIsComposite 
					? Messages.format(RefactoringCoreMessages.RenameMethodRefactoring_same_name2, new String[] { newName, fMethod.getDeclaringType().getElementName() } ) 
					: RefactoringCoreMessages.RenameMethodRefactoring_same_name,
					JavaStatusContext.create(fMethod)); 
		return status;
	}
	
	public Object getNewElement() {
		if (fMethod.getDeclaringType()!=null)
			return fMethod.getDeclaringType().getFunction(getNewElementName(), fMethod.getParameterTypes());
		return fMethod.getJavaScriptUnit().getFunction(getNewElementName(), fMethod.getParameterTypes());
	}
	
	public final IFunction getMethod() {
		return fMethod;
	}
	
	private void initializeMethodsToRename(IProgressMonitor pm) throws CoreException {
		if (fMethodsToRename == null)
			fMethodsToRename= new HashSet(Arrays.asList(MethodChecks.getOverriddenMethods(getMethod(), pm)));
	}
	
	protected void setMethodsToRename(IFunction[] methods) {
		fMethodsToRename= new HashSet(Arrays.asList(methods));
	}
	
	protected Set getMethodsToRename() {
		return fMethodsToRename;
	}
	
	//---- IReferenceUpdating -----------------------------------

	public boolean canEnableUpdateReferences() {
		return true;
	}

	public final void setUpdateReferences(boolean update) {
		fUpdateReferences= update;
	}	
	
	public boolean getUpdateReferences() {
		return fUpdateReferences;
	}	
	
	//------------------- IDelegateUpdating ----------------------
		
	public boolean canEnableDelegateUpdating() {
		return true;
	}

	public boolean getDelegateUpdating() {
		return fDelegateUpdating;
	}

	public void setDelegateUpdating(boolean updating) {
		fDelegateUpdating= updating;
	}

	public boolean getDeprecateDelegates() {
		return fDelegateDeprecation;
	}

	public void setDeprecateDelegates(boolean deprecate) {
		fDelegateDeprecation= deprecate;
	}

	//----------- preconditions ------------------

	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
		if (! fMethod.exists()){
			String message= Messages.format(RefactoringCoreMessages.RenameMethodRefactoring_deleted, 
								fMethod.getJavaScriptUnit().getElementName());
			return RefactoringStatus.createFatalErrorStatus(message);
		}	
		
		RefactoringStatus result= Checks.checkAvailability(fMethod);
		if (result.hasFatalError())
				return result;
		result.merge(Checks.checkIfCuBroken(fMethod));
		return result;
	}

	protected RefactoringStatus doCheckFinalConditions(IProgressMonitor pm, CheckConditionsContext context) throws CoreException {
		try{
			RefactoringStatus result= new RefactoringStatus();
			pm.beginTask("", 9); //$NON-NLS-1$
			// TODO workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=40367
			if (!Checks.isAvailable(fMethod)) {
				result.addFatalError(RefactoringCoreMessages.RenameMethodProcessor_is_binary, JavaStatusContext.create(fMethod)); 
				return result;
			}
			result.merge(Checks.checkIfCuBroken(fMethod));
			if (result.hasFatalError())
				return result;
			pm.setTaskName(RefactoringCoreMessages.RenameMethodRefactoring_taskName_checkingPreconditions); 
			result.merge(checkNewElementName(getNewElementName()));
			if (result.hasFatalError())
				return result;
			
			boolean mustAnalyzeShadowing;
			IFunction[] newNameMethods= searchForDeclarationsOfClashingMethods(new SubProgressMonitor(pm, 1));
			if (newNameMethods.length == 0) {
				mustAnalyzeShadowing= false;
				pm.worked(1);
			} else {
				IType[] outerTypes= searchForOuterTypesOfReferences(newNameMethods, new SubProgressMonitor(pm, 1));
				if (outerTypes.length > 0) {
					//There exists a reference to a clashing method, where the reference is in a nested type.
					//That nested type could be a type in a ripple method's hierarchy, which could
					//cause the reference to bind to the new ripple method instead of to
					//its old binding (a method of an enclosing scope).
					//-> Getting *more* references than before -> Semantics not preserved.
					//Example: RenamePrivateMethodTests#testFail6()
					//TODO: could pass declaringTypes to the RippleMethodFinder and check whether
					//a hierarchy contains one of outerTypes (or an outer type of an outerType, recursively).
					mustAnalyzeShadowing= true;
					
				} else {
					boolean hasOldRefsInInnerTypes= true;
						//TODO: to implement this optimization:
						//- move search for references to before this check.
						//- collect references in inner types.
						//- for each reference, check for all supertypes and their enclosing types
						//(recursively), whether they declare a rippleMethod
					if (hasOldRefsInInnerTypes) {
						//There exists a reference to a ripple method in a nested type
						//of a type in the hierarchy of any ripple method.
						//When that reference is renamed, and one of the supertypes of the
						//nested type declared a method matching the new name, then
						//the renamed reference will bind to the method in its supertype,
						//since inherited methods bind stronger than methods from enclosing scopes.
						//Getting *less* references than before -> Semantics not preserved.
						//Examples: RenamePrivateMethodTests#testFail2(), RenamePrivateMethodTests#testFail5()
						mustAnalyzeShadowing= true;
					} else {
						mustAnalyzeShadowing= false;
					}
				}
			}
			
			initializeMethodsToRename(new SubProgressMonitor(pm, 1));
			pm.setTaskName(RefactoringCoreMessages.RenameMethodRefactoring_taskName_searchingForReferences); 
			fOccurrences= getOccurrences(new SubProgressMonitor(pm, 3), result);	
			pm.setTaskName(RefactoringCoreMessages.RenameMethodRefactoring_taskName_checkingPreconditions); 
			
			if (fUpdateReferences)
				result.merge(checkRelatedMethods());
			
			result.merge(analyzeCompilationUnits()); //removes CUs with syntax errors
			pm.worked(1);
			
			if (result.hasFatalError())
				return result;
			
			createChanges(new SubProgressMonitor(pm, 1), result);
			if (fUpdateReferences & mustAnalyzeShadowing)
				result.merge(analyzeRenameChanges(new SubProgressMonitor(pm, 1)));
			else
				pm.worked(1);
			
			return result;
		} finally{
			pm.done();
		}	
	}
	
	private IType[] searchForOuterTypesOfReferences(IFunction[] newNameMethods, IProgressMonitor pm) throws CoreException {
		final Set outerTypesOfReferences= new HashSet();
		SearchPattern pattern= RefactoringSearchEngine.createOrPattern(newNameMethods, IJavaScriptSearchConstants.REFERENCES);
		IJavaScriptSearchScope scope= createRefactoringScope(getMethod());
		SearchRequestor requestor= new SearchRequestor() {
			public void acceptSearchMatch(SearchMatch match) throws CoreException {
				IMember member= (IMember) match.getElement();
				IType declaring= member.getDeclaringType();
				if (declaring == null)
					return;
				IType outer= declaring.getDeclaringType();
				if (outer != null)
					outerTypesOfReferences.add(declaring);
			}
		};
		new SearchEngine().search(pattern, SearchUtils.getDefaultSearchParticipants(),
				scope, requestor, pm);
		return (IType[]) outerTypesOfReferences.toArray(new IType[outerTypesOfReferences.size()]);
	}

	private IFunction[] searchForDeclarationsOfClashingMethods(IProgressMonitor pm) throws CoreException {
		final List results= new ArrayList();
		SearchPattern pattern= createNewMethodPattern();
		IJavaScriptSearchScope scope= RefactoringScopeFactory.create(getMethod().getJavaScriptProject());
		SearchRequestor requestor= new SearchRequestor() {
			public void acceptSearchMatch(SearchMatch match) throws CoreException {
				Object method= match.getElement();
				if (method instanceof IFunction) // check for bug 90138: [refactoring] [rename] Renaming method throws internal exception
					results.add(method);
				else
					JavaScriptPlugin.logErrorMessage("Unexpected element in search match: " + match.toString()); //$NON-NLS-1$
			}
		};
		new SearchEngine().search(pattern, SearchUtils.getDefaultSearchParticipants(), scope, requestor, pm);
		return (IFunction[]) results.toArray(new IFunction[results.size()]);
	}
	
	private SearchPattern createNewMethodPattern() throws JavaScriptModelException {
		StringBuffer stringPattern= new StringBuffer(getNewElementName()).append('(');
		int paramCount= getMethod().getNumberOfParameters();
		for (int i= 0; i < paramCount; i++) {
			if (i > 0)
				stringPattern.append(',');
			stringPattern.append('*');
		}
		stringPattern.append(')');
		
		return SearchPattern.createPattern(stringPattern.toString(), IJavaScriptSearchConstants.FUNCTION,
				IJavaScriptSearchConstants.DECLARATIONS, SearchUtils.GENERICS_AGNOSTIC_MATCH_RULE);
	}
	
	protected final IJavaScriptSearchScope createRefactoringScope() throws CoreException {
		return createRefactoringScope(fMethod);
	}
	//TODO: shouldn't scope take all ripple methods into account?
	protected static final IJavaScriptSearchScope createRefactoringScope(IFunction method) throws CoreException {
		JavaElement javaElement = (JavaElement) method;
		if (javaElement instanceof IMember) {
			IMember member= (IMember) javaElement;
			if (member.getParent().getElementType() == IJavaScriptElement.METHOD) {
				IJavaScriptElement toplevelFunction = getTopLevelFunction(member.getParent());
				return SearchEngine.createJavaSearchScope(new IJavaScriptElement[] {toplevelFunction});
			}
			else if (JdtFlags.isPrivate(member)) {
				if (member.getJavaScriptUnit() != null)
					return SearchEngine.createJavaSearchScope(new IJavaScriptElement[] { member.getJavaScriptUnit()});
				else 
					return SearchEngine.createJavaSearchScope(new IJavaScriptElement[] { member});
			}
		}
		return RefactoringScopeFactory.create(javaElement.getJavaScriptProject());
	}
	
	/**
	 * @param parent
	 * @return
	 */
	private static IJavaScriptElement getTopLevelFunction(IJavaScriptElement method) {
		if (method.getParent().getElementType() == IJavaScriptElement.METHOD)
			return getTopLevelFunction(method.getParent());
		
		return method;
	}

	SearchPattern createOccurrenceSearchPattern() {
		HashSet methods= new HashSet(fMethodsToRename);
		methods.add(fMethod);
		IFunction[] ms= (IFunction[]) methods.toArray(new IFunction[methods.size()]);
		return RefactoringSearchEngine.createOrPattern(ms, IJavaScriptSearchConstants.ALL_OCCURRENCES);
	}

	SearchResultGroup[] getOccurrences(){
		return fOccurrences;	
	}
	
	/*
	 * XXX made protected to allow overriding and working around bug 39700
	 */
	protected SearchResultGroup[] getOccurrences(IProgressMonitor pm, RefactoringStatus status) throws CoreException {
		SearchPattern pattern= createOccurrenceSearchPattern();
		return RefactoringSearchEngine.search(pattern, createRefactoringScope(),
			new MethodOccurenceCollector(getMethod().getElementName()), pm, status);	
	}

	private RefactoringStatus checkRelatedMethods() throws CoreException { 
		RefactoringStatus result= new RefactoringStatus();
		for (Iterator iter= fMethodsToRename.iterator(); iter.hasNext(); ) {
			IFunction method= (IFunction)iter.next();
			
			if (method.getDeclaringType()!=null)
			  result.merge(Checks.checkIfConstructorName(method, getNewElementName(), method.getDeclaringType().getElementName()));
			
			String[] msgData= new String[]{method.getElementName(), method.getJavaScriptUnit().getElementName()};
//			String[] msgData= new String[]{method.getElementName(), JavaModelUtil.getFullyQualifiedName(method.getDeclaringType())};
			if (! method.exists()){
				result.addFatalError(Messages.format(RefactoringCoreMessages.RenameMethodRefactoring_not_in_model, msgData)); 
				continue;
			}
			if (method.isBinary())
				result.addFatalError(Messages.format(RefactoringCoreMessages.RenameMethodRefactoring_no_binary, msgData)); 
			if (method.isReadOnly())
				result.addFatalError(Messages.format(RefactoringCoreMessages.RenameMethodRefactoring_no_read_only, msgData));
		}
		return result;	
	}
	
	private RefactoringStatus analyzeCompilationUnits() throws CoreException {
		if (fOccurrences.length == 0)
			return null;
			
		RefactoringStatus result= new RefactoringStatus();
		fOccurrences= Checks.excludeCompilationUnits(fOccurrences, result);
		if (result.hasFatalError())
			return result;
		
		result.merge(Checks.checkCompileErrorsInAffectedFiles(fOccurrences));	
			
		return result;
	}
	
	//-------
	
	private RefactoringStatus analyzeRenameChanges(IProgressMonitor pm) throws CoreException {
		IJavaScriptUnit[] newDeclarationWCs= null;
		try {
			pm.beginTask("", 4); //$NON-NLS-1$
			RefactoringStatus result= new RefactoringStatus();
			IJavaScriptUnit[] declarationCUs= getDeclarationCUs();
			newDeclarationWCs= RenameAnalyzeUtil.createNewWorkingCopies(declarationCUs,
					fChangeManager, fWorkingCopyOwner, new SubProgressMonitor(pm, 1));
			
			IFunction[] wcOldMethods= new IFunction[fMethodsToRename.size()];
			IFunction[] wcNewMethods= new IFunction[fMethodsToRename.size()];
			int i= 0;
			for (Iterator iter= fMethodsToRename.iterator(); iter.hasNext(); i++) {
				IFunction method= (IFunction) iter.next();
				IJavaScriptUnit newCu= RenameAnalyzeUtil.findWorkingCopyForCu(newDeclarationWCs, method.getJavaScriptUnit());
				IFunctionContainer typeWc= (IFunctionContainer) JavaModelUtil.findInCompilationUnit(newCu, method.getParent());
				if (typeWc == null)
					continue;
				wcOldMethods[i]= getMethodInWorkingCopy(method, getCurrentElementName(), typeWc);
				wcNewMethods[i]= getMethodInWorkingCopy(method, getNewElementName(), typeWc);
			}
			
//			SearchResultGroup[] newOccurrences= findNewOccurrences(newMethods, newDeclarationWCs, new SubProgressMonitor(pm, 3));
			SearchResultGroup[] newOccurrences= batchFindNewOccurrences(wcNewMethods, wcOldMethods, newDeclarationWCs, new SubProgressMonitor(pm, 3), result);
			
			result.merge(RenameAnalyzeUtil.analyzeRenameChanges2(fChangeManager, fOccurrences, newOccurrences, getNewElementName()));
			return result;
		} finally{
			pm.done();
			if (newDeclarationWCs != null){
				for (int i= 0; i < newDeclarationWCs.length; i++) {
					newDeclarationWCs[i].discardWorkingCopy();		
				}
			}	
		}
	}
	
	//Lower memory footprint than batchFindNewOccurrences. Not used because it is too slow.
	//Final solution is maybe to do searches in chunks of ~ 50 CUs.
//	private SearchResultGroup[] findNewOccurrences(IFunction[] newMethods, IJavaScriptUnit[] newDeclarationWCs, IProgressMonitor pm) throws CoreException {
//		pm.beginTask("", fOccurrences.length * 2); //$NON-NLS-1$
//		
//		SearchPattern refsPattern= RefactoringSearchEngine.createOrPattern(newMethods, IJavaScriptSearchConstants.REFERENCES);
//		SearchParticipant[] searchParticipants= SearchUtils.getDefaultSearchParticipants();
//		IJavaScriptSearchScope scope= RefactoringScopeFactory.create(newMethods);
//		MethodOccurenceCollector requestor= new MethodOccurenceCollector(getNewElementName());
//		SearchEngine searchEngine= new SearchEngine(fWorkingCopyOwner);
//		
//		//TODO: should process only references
//		for (int j= 0; j < fOccurrences.length; j++) { //should be getReferences()
//			//cut memory peak by holding only one reference CU at a time in memory
//			IJavaScriptUnit originalCu= fOccurrences[j].getCompilationUnit();
//			IJavaScriptUnit newWc= null;
//			try {
//				IJavaScriptUnit wc= RenameAnalyzeUtil.findWorkingCopyForCu(newDeclarationWCs, originalCu);
//				if (wc == null) {
//					newWc= RenameAnalyzeUtil.createNewWorkingCopy(originalCu, fChangeManager, fWorkingCopyOwner,
//							new SubProgressMonitor(pm, 1));
//				}
//				searchEngine.search(refsPattern, searchParticipants, scope,	requestor, new SubProgressMonitor(pm, 1));
//			} finally {
//				if (newWc != null)
//					newWc.discardWorkingCopy();
//			}
//		}
//		SearchResultGroup[] newResults= RefactoringSearchEngine.groupByResource(requestor.getResults());
//		pm.done();
//		return newResults;
//	}

	private SearchResultGroup[] batchFindNewOccurrences(IFunction[] wcNewMethods, final IFunction[] wcOldMethods, IJavaScriptUnit[] newDeclarationWCs, IProgressMonitor pm, RefactoringStatus status) throws CoreException {
		pm.beginTask("", 2); //$NON-NLS-1$
		
		SearchPattern refsPattern= RefactoringSearchEngine.createOrPattern(wcNewMethods, IJavaScriptSearchConstants.REFERENCES);
		SearchParticipant[] searchParticipants= SearchUtils.getDefaultSearchParticipants();
		IJavaScriptSearchScope scope= RefactoringScopeFactory.create(wcNewMethods);
		
		MethodOccurenceCollector requestor;
		if (getDelegateUpdating()) {
			// There will be two new matches inside the delegate(s) (the invocation
			// and the javadoc) which are OK and must not be reported.
			// Note that except these ocurrences, the delegate bodies are empty 
			// (as they were created this way).
			requestor= new MethodOccurenceCollector(getNewElementName()) {
				public void acceptSearchMatch(IJavaScriptUnit unit, SearchMatch match) throws CoreException {
					for (int i= 0; i < wcOldMethods.length; i++) 
						if (wcOldMethods[i].equals(match.getElement()))
							return;
					super.acceptSearchMatch(unit, match);
				}
			};
		} else
			requestor= new MethodOccurenceCollector(getNewElementName());
		
		SearchEngine searchEngine= new SearchEngine(fWorkingCopyOwner);
		
		ArrayList needWCs= new ArrayList();
		HashSet declaringCUs= new HashSet(newDeclarationWCs.length);
		for (int i= 0; i < newDeclarationWCs.length; i++)
			declaringCUs.add(newDeclarationWCs[i].getPrimary());
		for (int i= 0; i < fOccurrences.length; i++) {
			IJavaScriptUnit cu= fOccurrences[i].getCompilationUnit();
			if (! declaringCUs.contains(cu))
				needWCs.add(cu);
		}
		IJavaScriptUnit[] otherWCs= null;
		try {
			otherWCs= RenameAnalyzeUtil.createNewWorkingCopies(
					(IJavaScriptUnit[]) needWCs.toArray(new IJavaScriptUnit[needWCs.size()]),
					fChangeManager, fWorkingCopyOwner, new SubProgressMonitor(pm, 1));
			searchEngine.search(refsPattern, searchParticipants, scope,	requestor, new SubProgressMonitor(pm, 1));
		} finally {
			pm.done();
			if (otherWCs != null) {
				for (int i= 0; i < otherWCs.length; i++) {
					otherWCs[i].discardWorkingCopy();
				}
			}
		}
		SearchResultGroup[] newResults= RefactoringSearchEngine.groupByCu(requestor.getResults(), status);
		return newResults;
	}
	
	private IJavaScriptUnit[] getDeclarationCUs() {
		Set cus= new HashSet();
		for (Iterator iter= fMethodsToRename.iterator(); iter.hasNext();) {
			IFunction method= (IFunction) iter.next();
			cus.add(method.getJavaScriptUnit());
		}
		return (IJavaScriptUnit[]) cus.toArray(new IJavaScriptUnit[cus.size()]);
	}
	
	private IFunction getMethodInWorkingCopy(IFunction method, String elementName, IFunctionContainer typeWc) throws CoreException{
		String[] paramTypeSignatures= method.getParameterTypes();
		return typeWc.getFunction(elementName, paramTypeSignatures);
	}

	//-------
	private static IFunction[] classesDeclareMethodName(ITypeHierarchy hier, List classes, IFunction method, String newName)  throws CoreException {
		Set result= new HashSet();
		IType type= method.getDeclaringType();
		List subtypes= Arrays.asList(hier.getAllSubtypes(type));
		
		int parameterCount= method.getParameterTypes().length;
		boolean isMethodPrivate= JdtFlags.isPrivate(method);
		
		for (Iterator iter= classes.iterator(); iter.hasNext(); ){
			IType clazz= (IType) iter.next();
			IFunction[] methods= clazz.getFunctions();
			boolean isSubclass= subtypes.contains(clazz);
			for (int j= 0; j < methods.length; j++) {
				IFunction foundMethod= Checks.findMethod(newName, parameterCount, false, new IFunction[] {methods[j]});
				if (foundMethod == null)
					continue;
				if (isSubclass || type.equals(clazz))
					result.add(foundMethod);
				else if ((! isMethodPrivate) && (! JdtFlags.isPrivate(methods[j])))
					result.add(foundMethod);
			}
		}
		return (IFunction[]) result.toArray(new IFunction[result.size()]);
	}

	final static IFunction[] hierarchyDeclaresMethodName(IProgressMonitor pm, ITypeHierarchy hierarchy, IFunction method, String newName) throws CoreException {
		Set result= new HashSet();
		IType type= method.getDeclaringType();
		IFunction foundMethod= Checks.findMethod(newName, method.getParameterTypes().length, false, type);
		if (foundMethod != null) 
			result.add(foundMethod);

		IFunction[] foundInHierarchyClasses= classesDeclareMethodName(hierarchy, Arrays.asList(hierarchy.getAllClasses()), method, newName);
		if (foundInHierarchyClasses != null)
			result.addAll(Arrays.asList(foundInHierarchyClasses));
		
		IFunction[] foundInImplementingClasses= classesDeclareMethodName(hierarchy, Arrays.asList(new IType[0]), method, newName);
		if (foundInImplementingClasses != null)
			result.addAll(Arrays.asList(foundInImplementingClasses));
		return (IFunction[]) result.toArray(new IFunction[result.size()]);	
	}

	public Change createChange(IProgressMonitor monitor) throws CoreException {
		try {
			final TextChange[] changes= fChangeManager.getAllChanges();
			final List list= new ArrayList(changes.length);
			list.addAll(Arrays.asList(changes));
			String project= null;
			IJavaScriptProject javaProject= fMethod.getJavaScriptProject();
			if (javaProject != null)
				project= javaProject.getElementName();
			int flags= JavaScriptRefactoringDescriptor.JAR_MIGRATION | JavaScriptRefactoringDescriptor.JAR_REFACTORING | RefactoringDescriptor.STRUCTURAL_CHANGE;
			try {
				if (!Flags.isPrivate(fMethod.getFlags()))
					flags|= RefactoringDescriptor.MULTI_CHANGE;
			} catch (JavaScriptModelException exception) {
				JavaScriptPlugin.log(exception);
			}
			final IType declaring= fMethod.getDeclaringType();
			try {
				if (declaring!=null && (declaring.isAnonymous() || declaring.isLocal()))
					flags|= JavaScriptRefactoringDescriptor.JAR_SOURCE_ATTACHMENT;
			} catch (JavaScriptModelException exception) {
				JavaScriptPlugin.log(exception);
			}
			final String description= Messages.format(RefactoringCoreMessages.RenameMethodProcessor_descriptor_description_short, fMethod.getElementName());
			final String header= Messages.format(RefactoringCoreMessages.RenameMethodProcessor_descriptor_description, new String[] { JavaScriptElementLabels.getTextLabel(fMethod, JavaScriptElementLabels.ALL_FULLY_QUALIFIED), getNewElementName()});
			final String comment= new JDTRefactoringDescriptorComment(project, this, header).asString();
			final RenameJavaScriptElementDescriptor descriptor= new RenameJavaScriptElementDescriptor(IJavaScriptRefactorings.RENAME_METHOD);
			descriptor.setProject(project);
			descriptor.setDescription(description);
			descriptor.setComment(comment);
			descriptor.setFlags(flags);
			descriptor.setJavaElement(fMethod);
			descriptor.setNewName(getNewElementName());
			descriptor.setUpdateReferences(fUpdateReferences);
			descriptor.setKeepOriginal(fDelegateUpdating);
			descriptor.setDeprecateDelegate(fDelegateDeprecation);
			return new DynamicValidationRefactoringChange(descriptor, RefactoringCoreMessages.RenameMethodProcessor_change_name, (Change[]) list.toArray(new Change[list.size()]));
		} finally {
			monitor.done();
		}
	}

	private TextChangeManager createChanges(IProgressMonitor pm, RefactoringStatus status) throws CoreException {
		if (!fIsComposite)
			fChangeManager.clear();
		addOccurrences(fChangeManager, pm, status);
		return fChangeManager;
	}
	
	void addOccurrences(TextChangeManager manager, IProgressMonitor pm, RefactoringStatus status) throws CoreException/*thrown in subtype*/{
		pm.beginTask("", fOccurrences.length);				 //$NON-NLS-1$
		for (int i= 0; i < fOccurrences.length; i++){
			IJavaScriptUnit cu= fOccurrences[i].getCompilationUnit();
			if (cu == null)	
				continue;
			
			SearchMatch[] results= fOccurrences[i].getSearchResults();

			// Split matches into declaration and non-declaration matches
			
			List declarationsInThisCu= new ArrayList();
			List referencesInThisCu= new ArrayList();
			 
			for (int j= 0; j < results.length; j++) {
				if (results[j] instanceof MethodDeclarationMatch)
					declarationsInThisCu.add(results[j]);
				else
					referencesInThisCu.add(results[j]);
			}

			// First, handle the declarations
			if (declarationsInThisCu.size() > 0) {

				if (fDelegateUpdating) {
					// Update with delegates
					CompilationUnitRewrite rewrite= new CompilationUnitRewrite(cu);
					rewrite.setResolveBindings(true);

					for (Iterator iter= declarationsInThisCu.iterator(); iter.hasNext();) {
						SearchMatch element= (SearchMatch) iter.next();
						FunctionDeclaration method= ASTNodeSearchUtil.getMethodDeclarationNode((IFunction) element.getElement(), rewrite.getRoot());
						DelegateCreator creator= new DelegateMethodCreator();
						creator.setDeclareDeprecated(fDelegateDeprecation);
						creator.setDeclaration(method);
						creator.setSourceRewrite(rewrite);
						creator.setNewElementName(getNewElementName());
						creator.prepareDelegate();
						creator.createEdit();
					}
					// Need to handle all delegates first as this
					// creates a completely new change object.
					TextChange changeForThisCu= rewrite.createChange();
					changeForThisCu.setKeepPreviewEdits(true);
					manager.manage(cu, changeForThisCu);
				}

				// Update the normal methods
				for (Iterator iter= declarationsInThisCu.iterator(); iter.hasNext();) {
					SearchMatch element= (SearchMatch) iter.next();
					simpleUpdate(element, cu, manager.get(cu));
				}
			}

			// Second, handle references
			if (fUpdateReferences) {
				for (Iterator iter= referencesInThisCu.iterator(); iter.hasNext();) {
					SearchMatch element= (SearchMatch) iter.next();
					simpleUpdate(element, cu, manager.get(cu));
				}
			}
			
			pm.worked(1);
			if (pm.isCanceled())
				throw new OperationCanceledException();
		}
		pm.done();
	}

	private void simpleUpdate(SearchMatch element, IJavaScriptUnit cu, TextChange textChange) {
		String editName= RefactoringCoreMessages.RenameMethodRefactoring_update_occurrence;
		ReplaceEdit replaceEdit= createReplaceEdit(element, cu);
		addTextEdit(textChange, editName, replaceEdit);
	}

	protected final ReplaceEdit createReplaceEdit(SearchMatch searchResult, IJavaScriptUnit cu) {
		if (searchResult.isImplicit()) { // handle Annotation Element references, see bug 94062
			StringBuffer sb= new StringBuffer(getNewElementName());
			if (JavaScriptCore.INSERT.equals(cu.getJavaScriptProject().getOption(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_ASSIGNMENT_OPERATOR, true)))
				sb.append(' ');
			sb.append('=');
			if (JavaScriptCore.INSERT.equals(cu.getJavaScriptProject().getOption(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_ASSIGNMENT_OPERATOR, true)))
				sb.append(' ');
			return new ReplaceEdit(searchResult.getOffset(), 0, sb.toString());
		} else {
			return new ReplaceEdit(searchResult.getOffset(), searchResult.getLength(), getNewElementName());
		}
	}

	public RefactoringStatus initialize(RefactoringArguments arguments) {
		if (arguments instanceof JavaRefactoringArguments) {
			fInitialized= true;
			final JavaRefactoringArguments extended= (JavaRefactoringArguments) arguments;
			final String handle= extended.getAttribute(JDTRefactoringDescriptor.ATTRIBUTE_INPUT);
			if (handle != null) {
				final IJavaScriptElement element= JDTRefactoringDescriptor.handleToElement(extended.getProject(), handle, false);
				final String refactoring= getRefactoring().getName();
				if (element instanceof IFunction) {
					final IFunction method= (IFunction) element;
					final IType declaring= method.getDeclaringType();
					if (declaring != null && declaring.exists()) {
						final IFunction[] methods= declaring.findMethods(method);
						if (methods != null && methods.length == 1 && methods[0] != null) {
							if (!methods[0].exists())
								return ScriptableRefactoring.createInputFatalStatus(methods[0], refactoring, IJavaScriptRefactorings.RENAME_METHOD);
							fMethod= methods[0];
							initializeWorkingCopyOwner();
						} else
							return ScriptableRefactoring.createInputFatalStatus(null, refactoring, IJavaScriptRefactorings.RENAME_METHOD);
					} else
					{
						final IJavaScriptUnit unit= method.getJavaScriptUnit();
						if (unit != null && unit.exists()) {
							final IFunction[] methods= unit.findFunctions(method);
							if (methods != null && methods.length == 1 && methods[0] != null) {
								if (!methods[0].exists())
									return ScriptableRefactoring.createInputFatalStatus(methods[0], refactoring, IJavaScriptRefactorings.RENAME_METHOD);
								fMethod= methods[0];
								initializeWorkingCopyOwner();
							} else
								return ScriptableRefactoring.createInputFatalStatus(null, refactoring, IJavaScriptRefactorings.RENAME_METHOD);
						} else
							return ScriptableRefactoring.createInputFatalStatus(element, refactoring, IJavaScriptRefactorings.RENAME_METHOD);
					}
				} else
					return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, JDTRefactoringDescriptor.ATTRIBUTE_INPUT));
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, JDTRefactoringDescriptor.ATTRIBUTE_INPUT));
			final String name= extended.getAttribute(JDTRefactoringDescriptor.ATTRIBUTE_NAME);
			if (name != null && !"".equals(name)) //$NON-NLS-1$
				setNewElementName(name);
			else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, JDTRefactoringDescriptor.ATTRIBUTE_NAME));
			final String references= extended.getAttribute(JDTRefactoringDescriptor.ATTRIBUTE_REFERENCES);
			if (references != null) {
				fUpdateReferences= Boolean.valueOf(references).booleanValue();
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, JDTRefactoringDescriptor.ATTRIBUTE_REFERENCES));
			final String delegate= extended.getAttribute(ATTRIBUTE_DELEGATE);
			if (delegate != null) {
				fDelegateUpdating= Boolean.valueOf(delegate).booleanValue();
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_DELEGATE));
			final String deprecate= extended.getAttribute(ATTRIBUTE_DEPRECATE);
			if (deprecate != null) {
				fDelegateDeprecation= Boolean.valueOf(deprecate).booleanValue();
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_DEPRECATE));
		} else
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InitializableRefactoring_inacceptable_arguments);
		return new RefactoringStatus();
	}

	protected void addTextEdit(TextChange change, String editName, ReplaceEdit replaceEdit) {
		if (fIsComposite)
			TextChangeCompatibility.addTextEdit(change, editName, replaceEdit, fCategorySet);
		else
			TextChangeCompatibility.addTextEdit(change, editName, replaceEdit);

	}
}
