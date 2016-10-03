/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.corext.refactoring.changes;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.NullChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.corext.refactoring.AbstractJavaElementRenameChange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;

public final class RenamePackageChange extends AbstractJavaElementRenameChange {

	private static IPath createPath(String packageName) {
		return new Path(packageName.replace('.', IPath.SEPARATOR));
	}

	private Map fCompilationUnitStamps;

	private final boolean fRenameSubpackages;

	public RenamePackageChange(IPackageFragment pack, String newName, boolean renameSubpackages) {
		this(pack.getPath(), pack.getElementName(), newName, IResource.NULL_STAMP, null, renameSubpackages);
		Assert.isTrue(!pack.isReadOnly(), "package must not be read only"); //$NON-NLS-1$
	}

	private RenamePackageChange(IPath resourcePath, String oldName, String newName, long stampToRestore, Map compilationUnitStamps, boolean renameSubpackages) {
		super(resourcePath, oldName, newName, stampToRestore);
		fCompilationUnitStamps= compilationUnitStamps;
		fRenameSubpackages= renameSubpackages;
	}

	private void addStamps(Map stamps, IJavaScriptUnit[] units) {
		for (int i= 0; i < units.length; i++) {
			IResource resource= units[i].getResource();
			long stamp= IResource.NULL_STAMP;
			if (resource != null && (stamp= resource.getModificationStamp()) != IResource.NULL_STAMP) {
				stamps.put(resource, Long.valueOf(stamp));
			}
		}
	}

	protected IPath createNewPath() {
		IPackageFragment oldPackage= getPackage();
		IPath oldPackageName= createPath(oldPackage.getElementName());
		IPath newPackageName= createPath(getNewName());
		return getResourcePath().removeLastSegments(oldPackageName.segmentCount()).append(newPackageName);
	}

	protected IPath createNewPath(IPackageFragment oldPackage) {
		IPath oldPackagePath= createPath(oldPackage.getElementName());
		IPath newPackagePath= createPath(getNewName(oldPackage));
		return oldPackage.getPath().removeLastSegments(oldPackagePath.segmentCount()).append(newPackagePath);
	}

	protected Change createUndoChange(long stampToRestore) throws CoreException {
		IPackageFragment pack= getPackage();
		if (pack == null)
			return new NullChange();
		Map stamps= new HashMap();
		if (!fRenameSubpackages) {
			addStamps(stamps, pack.getJavaScriptUnits());
		} else {
			IPackageFragment[] allPackages= JavaElementUtil.getPackageAndSubpackages(pack);
			for (int i= 0; i < allPackages.length; i++) {
				IPackageFragment currentPackage= allPackages[i];
				addStamps(stamps, currentPackage.getJavaScriptUnits());
			}
		}
		return new RenamePackageChange(createNewPath(), getNewName(), getOldName(), stampToRestore, stamps, fRenameSubpackages);
		// Note: This reverse change only works if the renamePackage change did
		// not merge the source package into an existing target.
	}

	protected void doRename(IProgressMonitor pm) throws CoreException {
		IPackageFragment pack= getPackage();
		if (pack == null)
			return;

		if (!fRenameSubpackages) {
			renamePackage(pack, pm, createNewPath(), getNewName());
		} else {
			IPackageFragment[] allPackages= JavaElementUtil.getPackageAndSubpackages(pack);

			pm.beginTask("", allPackages.length); //$NON-NLS-1$
			try {
				for (int i= 0; i < allPackages.length; i++) {
					IPackageFragment currentPackage= allPackages[i];
					renamePackage(currentPackage, new SubProgressMonitor(pm, 1), createNewPath(currentPackage), getNewName(currentPackage));
				}
			} finally {
				pm.done();
			}
		}
	}

	public String getName() {
		String msg= fRenameSubpackages ? RefactoringCoreMessages.RenamePackageChange_name_with_subpackages : RefactoringCoreMessages.RenamePackageChange_name;
		return Messages.format(msg, new String[] { getOldName(), getNewName()});
	}

	private String getNewName(IPackageFragment subpackage) {
		return getNewName() + subpackage.getElementName().substring(getOldName().length());
	}

	private IPackageFragment getPackage() {
		return (IPackageFragment) getModifiedElement();
	}

	public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException {
		pm.beginTask("", 2); //$NON-NLS-1$
		RefactoringStatus result;
		try {
			result= new RefactoringStatus();
			IJavaScriptElement element= (IJavaScriptElement) getModifiedElement();
			// don't check for read-only since we don't go through
			// validate edit.
			result.merge(isValid(new SubProgressMonitor(pm, 1), DIRTY));
			if (result.hasFatalError())
				return result;
			if (element != null && element.exists() && element instanceof IPackageFragment) {
				IPackageFragment pack= (IPackageFragment) element;
				if (fRenameSubpackages) {
					IPackageFragment[] allPackages= JavaElementUtil.getPackageAndSubpackages(pack);
					SubProgressMonitor subPm= new SubProgressMonitor(pm, 1);
					subPm.beginTask("", allPackages.length); //$NON-NLS-1$
					for (int i= 0; i < allPackages.length; i++) {
						// don't check for read-only since we don't go through
						// validate edit.
						checkIfModifiable(result, allPackages[i], DIRTY);
						if (result.hasFatalError())
							return result;
						isValid(result, allPackages[i], new SubProgressMonitor(subPm, 1));
					}
				} else {
					isValid(result, pack, new SubProgressMonitor(pm, 1));
				}
			}
		} finally {
			pm.done();
		}
		return result;
	}

	private void isValid(RefactoringStatus result, IPackageFragment pack, IProgressMonitor pm) throws JavaScriptModelException {
		IJavaScriptUnit[] units= pack.getJavaScriptUnits();
		pm.beginTask("", units.length); //$NON-NLS-1$
		for (int i= 0; i < units.length; i++) {
			pm.subTask(Messages.format(RefactoringCoreMessages.RenamePackageChange_checking_change, pack.getElementName()));
			checkIfModifiable(result, units[i], READ_ONLY | DIRTY);
			pm.worked(1);
		}
		pm.done();
	}

	private void renamePackage(IPackageFragment pack, IProgressMonitor pm, IPath newPath, String newName) throws JavaScriptModelException, CoreException {
		pack.rename(newName, false, pm);
		if (fCompilationUnitStamps != null) {
			IPackageFragment newPack= (IPackageFragment) JavaScriptCore.create(ResourcesPlugin.getWorkspace().getRoot().getFolder(newPath));
			if (newPack.exists()) {
				IJavaScriptUnit[] units= newPack.getJavaScriptUnits();
				for (int i= 0; i < units.length; i++) {
					IResource resource= units[i].getResource();
					if (resource != null) {
						Long stamp= (Long) fCompilationUnitStamps.get(resource);
						if (stamp != null) {
							resource.revertModificationStamp(stamp.longValue());
						}
					}
				}
			}
		}
	}
}
