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
package org.eclipse.wst.jsdt.internal.corext.fix;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.ltk.core.refactoring.CategorizedTextEditGroup;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.ContentStamp;
import org.eclipse.ltk.core.refactoring.GroupCategory;
import org.eclipse.ltk.core.refactoring.GroupCategorySet;
import org.eclipse.ltk.core.refactoring.NullChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringTickProvider;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.TextEditBasedChangeGroup;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;
import org.eclipse.text.edits.TextEditVisitor;
import org.eclipse.text.edits.UndoEdit;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.WorkingCopyOwner;
import org.eclipse.wst.jsdt.core.dom.ASTParser;
import org.eclipse.wst.jsdt.core.dom.ASTRequestor;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.internal.corext.refactoring.Checks;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.DynamicValidationStateChange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.MultiStateCompilationUnitChange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.TextChangeCompatibility;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.fix.CodeFormatCleanUp;
import org.eclipse.wst.jsdt.internal.ui.fix.CommentFormatCleanUp;
import org.eclipse.wst.jsdt.internal.ui.fix.ControlStatementsCleanUp;
import org.eclipse.wst.jsdt.internal.ui.fix.ExpressionsCleanUp;
import org.eclipse.wst.jsdt.internal.ui.fix.ICleanUp;
import org.eclipse.wst.jsdt.internal.ui.fix.SortMembersCleanUp;
import org.eclipse.wst.jsdt.internal.ui.fix.StringCleanUp;
import org.eclipse.wst.jsdt.internal.ui.fix.UnusedCodeCleanUp;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.ASTProvider;
import org.eclipse.wst.jsdt.internal.ui.refactoring.IScheduledRefactoring;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabels;

public class CleanUpRefactoring extends Refactoring implements IScheduledRefactoring {
	
	public static class CleanUpChange extends CompilationUnitChange {

		private UndoEdit fUndoEdit;

		public CleanUpChange(String name, IJavaScriptUnit cunit) {
	        super(name, cunit);
        }
		
		/**
		 * {@inheritDoc}
		 */
		protected Change createUndoChange(UndoEdit edit, ContentStamp stampToRestore) {
		    fUndoEdit= edit;
			return super.createUndoChange(edit, stampToRestore);
		}

		public UndoEdit getUndoEdit() {
        	return fUndoEdit;
        }	
	}
	
	private static class FixCalculationException extends RuntimeException {
		
		private static final long serialVersionUID= 3807273310144726165L;
		
		private final CoreException fException;
		
		public FixCalculationException(CoreException exception) {
			fException= exception;
		}
		
		public CoreException getException() {
			return fException;
		}
	}
	
	private static class ParseListElement {
		
		private final IJavaScriptUnit fUnit;
		private final ICleanUp[] fCleanUpsArray;
		
		public ParseListElement(IJavaScriptUnit unit, ICleanUp[] cleanUps) {
			fUnit= unit;
			fCleanUpsArray= cleanUps;
		}
		
		public IJavaScriptUnit getCompilationUnit() {
			return fUnit;
		}
		
		public ICleanUp[] getCleanUps() {
			return fCleanUpsArray;
		}
	}
	
	private final class CleanUpRefactoringProgressMonitor extends SubProgressMonitor {
		
		private double fRealWork;
		private int fFlushCount;
		private final int fSize;
		private final int fIndex;
		
		private CleanUpRefactoringProgressMonitor(IProgressMonitor monitor, int ticks, int size, int index) {
			super(monitor, ticks);
			fFlushCount= 0;
			fSize= size;
			fIndex= index;
		}
		
		/**
		 * {@inheritDoc}
		 */
		public void internalWorked(double work) {
			fRealWork+= work;
		}
		
		public void flush() {
			super.internalWorked(fRealWork);
			reset();
			fFlushCount++;
		}
		
		public void reset() {
			fRealWork= 0.0;
		}
		
		public void done() {}
		
		public int getIndex() {
			return fIndex + fFlushCount;
		}
		
		public String getSubTaskMessage(IJavaScriptUnit source) {
			String typeName= source.getElementName();
			return Messages.format(FixMessages.CleanUpRefactoring_ProcessingCompilationUnit_message, new Object[] {Integer.valueOf(getIndex()), Integer.valueOf(fSize), typeName});
		}
	}
	
	private static class CleanUpASTRequestor extends ASTRequestor {
		
		private final List/*<ParseListElement>*/fUndoneElements;
		private final Hashtable/*<IJavaScriptUnit, Change>*/fSolutions;
		private final Hashtable/*<IJavaScriptUnit, ICleanUp[]>*/fCompilationUnitCleanUpMap;
		private final CleanUpRefactoringProgressMonitor fMonitor;
		
		public CleanUpASTRequestor(List parseList, Hashtable solutions, CleanUpRefactoringProgressMonitor monitor) {
			fSolutions= solutions;
			fMonitor= monitor;
			fUndoneElements= new ArrayList();
			fCompilationUnitCleanUpMap= new Hashtable(parseList.size());
			for (Iterator iter= parseList.iterator(); iter.hasNext();) {
				ParseListElement element= (ParseListElement)iter.next();
				fCompilationUnitCleanUpMap.put(element.getCompilationUnit(), element.getCleanUps());
			}
		}
		
		/**
		 * {@inheritDoc}
		 */
		public void acceptAST(IJavaScriptUnit source, JavaScriptUnit ast) {
			
			fMonitor.subTask(fMonitor.getSubTaskMessage(source));
			
			IJavaScriptUnit primary= (IJavaScriptUnit)source.getPrimaryElement();
			ICleanUp[] cleanUps= (ICleanUp[])fCompilationUnitCleanUpMap.get(primary);
			
			ICleanUp[] rejectedCleanUps= calculateSolutions(source, ast, cleanUps);
			
			if (rejectedCleanUps.length > 0) {
				fUndoneElements.add(new ParseListElement(primary, rejectedCleanUps));
				fMonitor.reset();
			} else {
				fMonitor.flush();
			}
		}
		
		public void acceptSource(IJavaScriptUnit source) {
			acceptAST(source, null);
		}
		
		public List getUndoneElements() {
			return fUndoneElements;
		}
		
		private ICleanUp[] calculateSolutions(IJavaScriptUnit source, JavaScriptUnit ast, ICleanUp[] cleanUps) {
			List/*<ICleanUp>*/result= new ArrayList();
			CleanUpChange solution;
			try {
				solution= calculateChange(ast, source, cleanUps, result);
			} catch (CoreException e) {
				throw new FixCalculationException(e);
			}
			
			if (solution != null) {
				try {
					integrateSolution(solution, source);
				} catch (JavaScriptModelException e) {
					throw new FixCalculationException(e);
				}
			}
			
			return (ICleanUp[])result.toArray(new ICleanUp[result.size()]);
		}
		
		private void integrateSolution(CleanUpChange solution, IJavaScriptUnit source) throws JavaScriptModelException {
			IJavaScriptUnit primary= source.getPrimary();
			
			List changes= (List)fSolutions.get(primary);
			if (changes == null) {
				changes= new ArrayList();
				fSolutions.put(primary, changes);
			}
			changes.add(solution);
		}
	}
	
	private static abstract class CleanUpParser {
		
		private static final int MAX_AT_ONCE;
		static {
			long maxMemory= Runtime.getRuntime().maxMemory();
			int ratio= (int)Math.round((double)maxMemory / (64 * 0x100000));
			switch (ratio) {
			case 0:
				MAX_AT_ONCE= 25;
				break;
			case 1:
				MAX_AT_ONCE= 100;
				break;
			case 2:
				MAX_AT_ONCE= 200;
				break;
			case 3:
				MAX_AT_ONCE= 300;
				break;
			case 4:
				MAX_AT_ONCE= 400;
				break;
			default:
				MAX_AT_ONCE= 500;
				break;
			}
		}
		
		public void createASTs(IJavaScriptUnit[] units, String[] bindingKeys, CleanUpASTRequestor requestor, IProgressMonitor monitor) {
			if (monitor == null)
				monitor= new NullProgressMonitor();
			
			try {
				monitor.beginTask("", units.length); //$NON-NLS-1$
				
				List list= Arrays.asList(units);
				int end= 0;
				int cursor= 0;
				while (cursor < units.length) {
					end= Math.min(end + MAX_AT_ONCE, units.length);
					List toParse= list.subList(cursor, end);
					
					createParser().createASTs((IJavaScriptUnit[])toParse.toArray(new IJavaScriptUnit[toParse.size()]), bindingKeys, requestor, new SubProgressMonitor(monitor, toParse.size()));
					cursor= end;
				}
			} finally {
				monitor.done();
			}
		}
		
		protected abstract ASTParser createParser();
	}
	
	private class CleanUpFixpointIterator {
		
		private List/*<ParseListElement>*/fParseList;
		private final Hashtable/*<IJavaScriptUnit, List<CleanUpChange>>*/fSolutions;
		private final Hashtable/*<IJavaScriptUnit (primary), IJavaScriptUnit (working copy)>*/fWorkingCopies;
		private final IJavaScriptProject fProject;
		private final Map fCleanUpOptions;
		private final int fSize;
		private int fIndex;
		
		public CleanUpFixpointIterator(IJavaScriptProject project, IJavaScriptUnit[] units, ICleanUp[] cleanUps) {
			fProject= project;
			fSolutions= new Hashtable(units.length);
			fWorkingCopies= new Hashtable();
			
			fParseList= new ArrayList(units.length);
			for (int i= 0; i < units.length; i++) {
				fParseList.add(new ParseListElement(units[i], cleanUps));
			}
			
			fCleanUpOptions= new Hashtable();
			for (int i= 0; i < cleanUps.length; i++) {
				ICleanUp cleanUp= cleanUps[i];
				Map currentCleanUpOption= cleanUp.getRequiredOptions();
				if (currentCleanUpOption != null)
					fCleanUpOptions.putAll(currentCleanUpOption);
			}
			
			fSize= units.length;
			fIndex= 1;
		}
		
		public boolean hasNext() {
			return !fParseList.isEmpty();
		}
		
		public void next(IProgressMonitor monitor) throws CoreException {
			List parseList= new ArrayList();
			List sourceList= new ArrayList();
			
			try {
				for (Iterator iter= fParseList.iterator(); iter.hasNext();) {
					ParseListElement element= (ParseListElement)iter.next();
					
					IJavaScriptUnit compilationUnit= element.getCompilationUnit();
					if (fSolutions.containsKey(compilationUnit)) {
						if (fWorkingCopies.containsKey(compilationUnit)) {
							compilationUnit= (IJavaScriptUnit)fWorkingCopies.get(compilationUnit);
						} else {
							compilationUnit= compilationUnit.getWorkingCopy(new WorkingCopyOwner() {}, null);
							fWorkingCopies.put(compilationUnit.getPrimary(), compilationUnit);
						}
						applyChange(compilationUnit, (List)fSolutions.get(compilationUnit.getPrimary()));
					}
					
					if (requiresAST(compilationUnit, element.getCleanUps())) {
						parseList.add(compilationUnit);
					} else {
						sourceList.add(compilationUnit);
					}
				}
				
				CleanUpRefactoringProgressMonitor cuMonitor= new CleanUpRefactoringProgressMonitor(monitor, parseList.size() + sourceList.size(), fSize, fIndex);
				CleanUpASTRequestor requestor= new CleanUpASTRequestor(fParseList, fSolutions, cuMonitor);
				CleanUpParser parser= new CleanUpParser() {
					protected ASTParser createParser() {
						ASTParser result= ASTParser.newParser(ASTProvider.SHARED_AST_LEVEL);
						result.setResolveBindings(true);
						result.setProject(fProject);
						
						Map options= RefactoringASTParser.getCompilerOptions(fProject);
						options.putAll(fCleanUpOptions);
						result.setCompilerOptions(options);
						return result;
					}
				};
				try {
					IJavaScriptUnit[] units= (IJavaScriptUnit[])parseList.toArray(new IJavaScriptUnit[parseList.size()]);
					parser.createASTs(units, new String[0], requestor, cuMonitor);
				} catch (FixCalculationException e) {
					throw e.getException();
				}
				
				for (Iterator iterator= sourceList.iterator(); iterator.hasNext();) {
					IJavaScriptUnit cu= (IJavaScriptUnit)iterator.next();
					requestor.acceptSource(cu);
				}
				
				fParseList= requestor.getUndoneElements();
				fIndex= cuMonitor.getIndex();
			} finally {
			}
		}
		
		public void dispose() {
			for (Iterator iterator= fWorkingCopies.values().iterator(); iterator.hasNext();) {
				IJavaScriptUnit cu= (IJavaScriptUnit)iterator.next();
				try {
					cu.discardWorkingCopy();
				} catch (JavaScriptModelException e) {
					JavaScriptPlugin.log(e);
				}
			}
		}
		
		private boolean requiresAST(IJavaScriptUnit compilationUnit, ICleanUp[] cleanUps) throws CoreException {
			for (int i= 0; i < cleanUps.length; i++) {
				if (cleanUps[i].requireAST(compilationUnit))
					return true;
			}
			return false;
		}
		
		public Change[] getResult() {
			
			Change[] result= new Change[fSolutions.size()];
			int i=0;
			for (Iterator iterator= fSolutions.entrySet().iterator(); iterator.hasNext();) {
				Map.Entry entry= (Map.Entry)iterator.next();
				
				List changes= (List)entry.getValue();
				IJavaScriptUnit unit= (IJavaScriptUnit)entry.getKey();
				
				int saveMode;
				try {
					if (fLeaveFilesDirty || unit.getBuffer().hasUnsavedChanges()) {
						saveMode= TextFileChange.LEAVE_DIRTY;
					} else {
						saveMode= TextFileChange.FORCE_SAVE;
					}
				} catch (JavaScriptModelException e) {
					saveMode= TextFileChange.LEAVE_DIRTY;
					JavaScriptPlugin.log(e);
				}
				
				if (changes.size() == 1) {
					CleanUpChange change= (CleanUpChange)changes.get(0);
					change.setSaveMode(saveMode);
					result[i]= change;
				} else {
					MultiStateCompilationUnitChange mscuc= new MultiStateCompilationUnitChange(getChangeName(unit), unit);
					for (int j= 0; j < changes.size(); j++) {
						mscuc.addChange(createGroupFreeChange((CleanUpChange)changes.get(j)));
					}
					mscuc.setSaveMode(saveMode);
					result[i]= mscuc;
				}
				
				i++;
			}
			
			return result;
		}
		
		private TextChange createGroupFreeChange(CleanUpChange change) {
			CleanUpChange result= new CleanUpChange(change.getName(), change.getCompilationUnit());
			result.setEdit(change.getEdit());
			result.setSaveMode(change.getSaveMode());
	        return result;
        }
		
		private void applyChange(IJavaScriptUnit compilationUnit, List changes) throws JavaScriptModelException, CoreException {
			if (changes.size() == 1) {
				CleanUpChange change= (CleanUpChange)changes.get(changes.size() - 1);
				compilationUnit.getBuffer().setContents(change.getPreviewContent(null));
			} else {
				MultiStateCompilationUnitChange mscuc= new MultiStateCompilationUnitChange("", compilationUnit.getPrimary()); //$NON-NLS-1$
				for (int i= 0; i < changes.size(); i++) {
					mscuc.addChange((CleanUpChange)changes.get(i));
				}
				compilationUnit.getBuffer().setContents(mscuc.getPreviewContent(null));
			}
		}
	}
	
	private static final RefactoringTickProvider CLEAN_UP_REFACTORING_TICK_PROVIDER= new RefactoringTickProvider(0, 1, 0, 0);
	
	private final List/*<ICleanUp>*/fCleanUps;
	private final Hashtable/*<IJavaScriptProject, List<IJavaScriptUnit>*/fProjects;
	private Change fChange;
	private boolean fLeaveFilesDirty;
	private final String fName;
	
	public CleanUpRefactoring() {
		this(FixMessages.CleanUpRefactoring_Refactoring_name);
	}
	
	public CleanUpRefactoring(String name) {
		fName= name;
		fCleanUps= new ArrayList();
		fProjects= new Hashtable();
	}
	
	public void addCompilationUnit(IJavaScriptUnit unit) {
		IJavaScriptProject javaProject= unit.getJavaScriptProject();
		if (!fProjects.containsKey(javaProject))
			fProjects.put(javaProject, new ArrayList());
		
		List cus= (List)fProjects.get(javaProject);
		cus.add(unit);
	}
	
	public void clearCompilationUnits() {
		fProjects.clear();
	}
	
	public boolean hasCompilationUnits() {
		return !fProjects.isEmpty();
	}
	
	public IJavaScriptUnit[] getCompilationUnits() {
		List cus= new ArrayList();
		for (Iterator iter= fProjects.values().iterator(); iter.hasNext();) {
			List pcus= (List)iter.next();
			cus.addAll(pcus);
		}
		return (IJavaScriptUnit[])cus.toArray(new IJavaScriptUnit[cus.size()]);
	}
	
	public void addCleanUp(ICleanUp fix) {
		fCleanUps.add(fix);
	}
	
	public void clearCleanUps() {
		fCleanUps.clear();
	}
	
	public boolean hasCleanUps() {
		return !fCleanUps.isEmpty();
	}
	
	public ICleanUp[] getCleanUps() {
		return (ICleanUp[])fCleanUps.toArray(new ICleanUp[fCleanUps.size()]);
	}
	
	public IJavaScriptProject[] getProjects() {
		return (IJavaScriptProject[])fProjects.keySet().toArray(new IJavaScriptProject[fProjects.keySet().size()]);
	}
	
	public void setLeaveFilesDirty(boolean leaveFilesDirty) {
		fLeaveFilesDirty= leaveFilesDirty;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ltk.core.refactoring.Refactoring#getName()
	 */
	public String getName() {
		return fName;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ltk.core.refactoring.Refactoring#checkInitialConditions(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		if (pm != null) {
			pm.beginTask("", 1); //$NON-NLS-1$
			pm.worked(1);
			pm.done();
		}
		return new RefactoringStatus();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ltk.core.refactoring.Refactoring#createChange(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		if (pm != null) {
			pm.beginTask("", 1); //$NON-NLS-1$
			pm.worked(1);
			pm.done();
		}
		return fChange;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ltk.core.refactoring.Refactoring#checkFinalConditions(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		
		if (pm == null)
			pm= new NullProgressMonitor();
		
		if (fProjects.size() == 0 || fCleanUps.size() == 0) {
			pm.beginTask("", 1); //$NON-NLS-1$
			pm.worked(1);
			pm.done();
			fChange= new NullChange();
			
			return new RefactoringStatus();
		}
		
		int cuCount= 0;
		for (Iterator iter= fProjects.keySet().iterator(); iter.hasNext();) {
			IJavaScriptProject project= (IJavaScriptProject)iter.next();
			cuCount+= ((List)fProjects.get(project)).size();
		}
		
		RefactoringStatus result= new RefactoringStatus();
		
		pm.beginTask("", cuCount * 2 * fCleanUps.size() + 4 * fCleanUps.size()); //$NON-NLS-1$
		try {
			DynamicValidationStateChange change= new DynamicValidationStateChange(getName());
			change.setSchedulingRule(getSchedulingRule());
			for (Iterator projectIter= fProjects.keySet().iterator(); projectIter.hasNext();) {
				IJavaScriptProject project= (IJavaScriptProject)projectIter.next();
				
				List compilationUnits= (List)fProjects.get(project);
				IJavaScriptUnit[] cus= (IJavaScriptUnit[])compilationUnits.toArray(new IJavaScriptUnit[compilationUnits.size()]);
				
				ICleanUp[] cleanUps= (ICleanUp[])fCleanUps.toArray(new ICleanUp[fCleanUps.size()]);
				result.merge(initialize(project));
				if (result.hasFatalError())
					return result;
				
				result.merge(checkPreConditions(project, cus, new SubProgressMonitor(pm, 3 * cleanUps.length)));
				if (result.hasFatalError())
					return result;
				
				Change[] changes= cleanUpProject(project, cus, cleanUps, pm);
				
				result.merge(checkPostConditions(new SubProgressMonitor(pm, cleanUps.length)));
				if (result.hasFatalError())
					return result;
				
				for (int i= 0; i < changes.length; i++) {
					change.add(changes[i]);
				}
			}
			fChange= change;
			
			List files= new ArrayList();
			findFilesToBeModified(change, files);
			result.merge(Checks.validateModifiesFiles((IFile[])files.toArray(new IFile[files.size()]), getValidationContext()));
			if (result.hasFatalError())
				return result;
		} finally {
			pm.done();
		}
		
		return result;
	}
	
	private void findFilesToBeModified(CompositeChange change, List result) throws JavaScriptModelException {
		Change[] children= change.getChildren();
		for (int i= 0; i < children.length; i++) {
			Change child= children[i];
			if (child instanceof CompositeChange) {
				findFilesToBeModified((CompositeChange)child, result);
			} else if (child instanceof MultiStateCompilationUnitChange) {
				result.add(((MultiStateCompilationUnitChange)child).getCompilationUnit().getCorrespondingResource());
			} else if (child instanceof CompilationUnitChange) {
				result.add(((CompilationUnitChange)child).getCompilationUnit().getCorrespondingResource());
			}
		}
	}
	
	private Change[] cleanUpProject(IJavaScriptProject project, IJavaScriptUnit[] compilationUnits, ICleanUp[] cleanUps, IProgressMonitor monitor) throws CoreException {
		CleanUpFixpointIterator iter= new CleanUpFixpointIterator(project, compilationUnits, cleanUps);
		
		SubProgressMonitor subMonitor= new SubProgressMonitor(monitor, 2 * compilationUnits.length * cleanUps.length);
		subMonitor.beginTask("", compilationUnits.length); //$NON-NLS-1$
		subMonitor.subTask(Messages.format(FixMessages.CleanUpRefactoring_Parser_Startup_message, project.getElementName()));
		try {
			while (iter.hasNext()) {
				iter.next(subMonitor);
			}
			
			return iter.getResult();
		} finally {
			iter.dispose();
			subMonitor.done();
		}
	}
	
	private RefactoringStatus initialize(IJavaScriptProject javaProject) throws CoreException {
		Map options= CleanUpPreferenceUtil.loadOptions(new ProjectScope(javaProject.getProject()));
		if (options == null) {
			return RefactoringStatus.createFatalErrorStatus(Messages.format(FixMessages.CleanUpRefactoring_could_not_retrive_profile, javaProject.getElementName()));
		}
		
		ICleanUp[] cleanUps= getCleanUps();
		for (int j= 0; j < cleanUps.length; j++) {
			cleanUps[j].initialize(options);
		}
		
		return new RefactoringStatus();
	}
	
	private RefactoringStatus checkPreConditions(IJavaScriptProject javaProject, IJavaScriptUnit[] compilationUnits, IProgressMonitor monitor) throws CoreException {
		RefactoringStatus result= new RefactoringStatus();
		
		ICleanUp[] cleanUps= getCleanUps();
		monitor.beginTask("", compilationUnits.length * cleanUps.length); //$NON-NLS-1$
		monitor.subTask(Messages.format(FixMessages.CleanUpRefactoring_Initialize_message, javaProject.getElementName()));
		try {
			for (int j= 0; j < cleanUps.length; j++) {
				result.merge(cleanUps[j].checkPreConditions(javaProject, compilationUnits, new SubProgressMonitor(monitor, compilationUnits.length)));
				if (result.hasFatalError())
					return result;
			}
		} finally {
			monitor.done();
		}
		
		return result;
	}
	
	private RefactoringStatus checkPostConditions(SubProgressMonitor monitor) throws CoreException {
		RefactoringStatus result= new RefactoringStatus();
		
		ICleanUp[] cleanUps= getCleanUps();
		monitor.beginTask("", cleanUps.length); //$NON-NLS-1$
		monitor.subTask(FixMessages.CleanUpRefactoring_checkingPostConditions_message);
		try {
			for (int j= 0; j < cleanUps.length; j++) {
				result.merge(cleanUps[j].checkPostConditions(new SubProgressMonitor(monitor, 1)));
				if (result.hasFatalError())
					return result;
			}
		} finally {
			monitor.done();
		}
		return result;
	}
	
	private static String getChangeName(IJavaScriptUnit compilationUnit) {
		StringBuffer buf= new StringBuffer();
		JavaScriptElementLabels.getCompilationUnitLabel(compilationUnit, JavaScriptElementLabels.ALL_DEFAULT, buf);
		buf.append(JavaScriptElementLabels.CONCAT_STRING);
		
		StringBuffer buf2= new StringBuffer();
		JavaScriptElementLabels.getPackageFragmentLabel((IPackageFragment)compilationUnit.getParent(), JavaScriptElementLabels.P_QUALIFIED, buf2);
		buf.append(buf2.toString().replace('.', '/'));
		
		return buf.toString();
	}
	
	public static CleanUpChange calculateChange(JavaScriptUnit ast, IJavaScriptUnit source, ICleanUp[] cleanUps, List undoneCleanUps) throws CoreException {
		if (cleanUps.length == 0)
			return null;
		
		CleanUpChange solution= null;
		int i= 0;
		do {
			ICleanUp cleanUp= cleanUps[i];
			IFix fix;
			if (ast == null || !cleanUp.requireAST(source)) {
				fix= cleanUp.createFix(source);
			} else {
				fix= cleanUp.createFix(ast);
			}
			if (fix != null) {
				TextChange current= fix.createChange();
				TextEdit currentEdit= pack(current.getEdit());
				
				if (solution != null) {
					if (intersects(currentEdit, solution.getEdit())) {
						undoneCleanUps.add(cleanUp);
					} else {
						CleanUpChange merge= new CleanUpChange(FixMessages.CleanUpRefactoring_clean_up_multi_chang_name, source);
						merge.setEdit(merge(currentEdit, solution.getEdit()));
						
						copyChangeGroups(merge, solution);
						copyChangeGroups(merge, current);
						
						solution= merge;
					}
				} else {
					solution= new CleanUpChange(current.getName(), source);
					solution.setEdit(currentEdit);
					
					copyChangeGroups(solution, current);
				}
			}
			i++;
		} while (i < cleanUps.length && (solution == null || ast != null && !cleanUps[i].needsFreshAST(ast)));
		
		for (; i < cleanUps.length; i++) {
			undoneCleanUps.add(cleanUps[i]);
		}
		return solution;
	}
	
	private static void copyChangeGroups(CompilationUnitChange target, TextChange source) {
		TextEditBasedChangeGroup[] changeGroups= source.getChangeGroups();
		for (int i= 0; i < changeGroups.length; i++) {
			TextEditGroup textEditGroup= changeGroups[i].getTextEditGroup();
			TextEditGroup newGroup;
			if (textEditGroup instanceof CategorizedTextEditGroup) {
				String label= textEditGroup.getName();
				newGroup= new CategorizedTextEditGroup(label, new GroupCategorySet(new GroupCategory(label, label, label)));
			} else {
				newGroup= new TextEditGroup(textEditGroup.getName());
			}
			TextEdit[] textEdits= textEditGroup.getTextEdits();
			for (int j= 0; j < textEdits.length; j++) {
				newGroup.addTextEdit(textEdits[j]);
			}
			target.addTextEditGroup(newGroup);
		}
	}
	
	private static TextEdit pack(TextEdit edit) {
		final List edits= new ArrayList();
		edit.accept(new TextEditVisitor() {
			public boolean visitNode(TextEdit node) {
				if (node instanceof MultiTextEdit)
					return true;
				
				edits.add(node);
				return false;
			}
		});
		MultiTextEdit result= new MultiTextEdit();
		for (Iterator iterator= edits.iterator(); iterator.hasNext();) {
			TextEdit child= (TextEdit)iterator.next();
			child.getParent().removeChild(child);
			TextChangeCompatibility.insert(result, child);
		}
		return result;
	}
	
	private static boolean intersects(TextEdit edit1, TextEdit edit2) {
		if (edit1 instanceof MultiTextEdit && edit2 instanceof MultiTextEdit) {
			MultiTextEdit multiTextEdit1= (MultiTextEdit)edit1;
			TextEdit[] children1= multiTextEdit1.getChildren();
			
			MultiTextEdit multiTextEdit2= (MultiTextEdit)edit2;
			TextEdit[] children2= multiTextEdit2.getChildren();
			
			int i1= 0;
			int i2= 0;
			while (i1 < children1.length && i2 < children2.length) {
				while (children1[i1].getExclusiveEnd() < children2[i2].getOffset()) {
					i1++;
					if (i1 >= children1.length)
						return false;
				}
				while (children2[i2].getExclusiveEnd() < children1[i1].getOffset()) {
					i2++;
					if (i2 >= children2.length)
						return false;
				}
				if (intersects(children1[i1], children2[i2]))
					return true;
				
				if (children1[i1].getExclusiveEnd() < children2[i2].getExclusiveEnd()) {
					i1++;
				} else {
					i2++;
				}
			}
			
			return false;
			
		} else if (edit1 instanceof MultiTextEdit) {
			MultiTextEdit multiTextEdit1= (MultiTextEdit)edit1;
			TextEdit[] children= multiTextEdit1.getChildren();
			for (int i= 0; i < children.length; i++) {
				TextEdit child= children[i];
				if (intersects(child, edit2))
					return true;
			}
			return false;
			
		} else if (edit2 instanceof MultiTextEdit) {
			MultiTextEdit multiTextEdit2= (MultiTextEdit)edit2;
			TextEdit[] children= multiTextEdit2.getChildren();
			for (int i= 0; i < children.length; i++) {
				TextEdit child= children[i];
				if (intersects(child, edit1))
					return true;
			}
			return false;
			
		} else {
			int start1= edit1.getOffset();
			int end1= start1 + edit1.getLength();
			int start2= edit2.getOffset();
			int end2= start2 + edit2.getLength();
			
			if (start1 > end2)
				return false;
			
			if (start2 > end1)
				return false;
			
			return true;
		}
	}
	
	private static TextEdit merge(TextEdit edit1, TextEdit edit2) {
		MultiTextEdit result= new MultiTextEdit();
		if (edit1 instanceof MultiTextEdit && edit2 instanceof MultiTextEdit) {
			MultiTextEdit multiTextEdit1= (MultiTextEdit)edit1;
			TextEdit[] children1= multiTextEdit1.getChildren();
			
			if (children1.length == 0)
				return edit2;
			
			MultiTextEdit multiTextEdit2= (MultiTextEdit)edit2;
			TextEdit[] children2= multiTextEdit2.getChildren();
			
			if (children2.length == 0)
				return edit1;
			
			int i1= 0;
			int i2= 0;
			while (i1 < children1.length && i2 < children2.length) {
				
				while (i1 < children1.length && children1[i1].getExclusiveEnd() < children2[i2].getOffset()) {
					edit1.removeChild(0);
					result.addChild(children1[i1]);
					i1++;
				}
				if (i1 >= children1.length) {
					for (int i= i2; i < children2.length; i++) {
						edit2.removeChild(0);
						result.addChild(children2[i]);
					}
					return result;
				}
				while (i2 < children2.length && children2[i2].getExclusiveEnd() < children1[i1].getOffset()) {
					edit2.removeChild(0);
					result.addChild(children2[i2]);
					i2++;
				}
				if (i2 >= children2.length) {
					for (int i= i1; i < children1.length; i++) {
						edit1.removeChild(0);
						result.addChild(children1[i]);
					}
					return result;
				}
				
				if (!(children1[i1].getExclusiveEnd() < children2[i2].getOffset())) {
					edit1.removeChild(0);
					edit2.removeChild(0);
					result.addChild(merge(children1[i1], children2[i2]));
					i1++;
					i2++;
				}
			}
			
			return result;
		} else if (edit1 instanceof MultiTextEdit) {
			TextEdit[] children= edit1.getChildren();
			
			int i= 0;
			while (children[i].getExclusiveEnd() < edit2.getOffset()) {
				edit1.removeChild(0);
				result.addChild(children[i]);
				i++;
				if (i >= children.length) {
					result.addChild(edit2);
					return result;
				}
			}
			edit1.removeChild(0);
			result.addChild(merge(children[i], edit2));
			i++;
			while (i < children.length) {
				edit1.removeChild(0);
				result.addChild(children[i]);
				i++;
			}
			
			return result;
		} else if (edit2 instanceof MultiTextEdit) {
			TextEdit[] children= edit2.getChildren();
			
			int i= 0;
			while (children[i].getExclusiveEnd() < edit1.getOffset()) {
				edit2.removeChild(0);
				result.addChild(children[i]);
				i++;
				if (i >= children.length) {
					result.addChild(edit1);
					return result;
				}
			}
			edit2.removeChild(0);
			result.addChild(merge(edit1, children[i]));
			i++;
			while (i < children.length) {
				edit2.removeChild(0);
				result.addChild(children[i]);
				i++;
			}
			
			return result;
		} else {
			if (edit1.getExclusiveEnd() < edit2.getOffset()) {
				result.addChild(edit1);
				result.addChild(edit2);
			} else {
				result.addChild(edit2);
				result.addChild(edit1);
			}
			
			return result;
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ltk.core.refactoring.Refactoring#getRefactoringTickProvider()
	 */
	protected RefactoringTickProvider doGetRefactoringTickProvider() {
		return CLEAN_UP_REFACTORING_TICK_PROVIDER;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public ISchedulingRule getSchedulingRule() {
		return ResourcesPlugin.getWorkspace().getRoot();
	}
	
	public static ICleanUp[] createCleanUps() {
		return new ICleanUp[] {
//				new CodeStyleCleanUp(), 
				new ControlStatementsCleanUp(), 
//				new ConvertLoopCleanUp(), 
//				new VariableDeclarationCleanUp(), 
				new ExpressionsCleanUp(), 
				new UnusedCodeCleanUp(), 
//				new Java50CleanUp(), 
//				new UnnecessaryCodeCleanUp(), 
				new StringCleanUp(), 
				new SortMembersCleanUp(), 
//				new ImportsCleanUp(), 
				new CodeFormatCleanUp(), 
				new CommentFormatCleanUp()};
	}
	
	public static ICleanUp[] createCleanUps(Map settings) {
		return new ICleanUp[] {
//				new CodeStyleCleanUp(settings), 
				new ControlStatementsCleanUp(settings), 
//				new ConvertLoopCleanUp(settings), 
//				new VariableDeclarationCleanUp(settings), 
				new ExpressionsCleanUp(settings), 
				new UnusedCodeCleanUp(settings), 
//				new Java50CleanUp(settings), 
//				new UnnecessaryCodeCleanUp(settings), 
				new StringCleanUp(settings),
				new SortMembersCleanUp(settings), 
//				new ImportsCleanUp(settings), 
				new CodeFormatCleanUp(settings), 
				new CommentFormatCleanUp(settings)};
	}
}
