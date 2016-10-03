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
package org.eclipse.wst.jsdt.internal.corext.refactoring.reorg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.IResourceChangeDescriptionFactory;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.IParticipantDescriptorFilter;
import org.eclipse.ltk.core.refactoring.participants.MoveArguments;
import org.eclipse.ltk.core.refactoring.participants.ParticipantManager;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;
import org.eclipse.ltk.core.refactoring.participants.ValidateEditChecker;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.internal.corext.refactoring.participants.ResourceModifications;

public class MoveModifications extends RefactoringModifications {
	
	private List fMoves;
	private List fMoveArguments;
	private List fParticipantDescriptorFilter;
	
	public MoveModifications() {
		fMoves= new ArrayList();
		fMoveArguments= new ArrayList();
		fParticipantDescriptorFilter= new ArrayList();
	}
	
	public void move(IResource resource, MoveArguments args) {
		add(resource, args, null);
	}

	public void move(IPackageFragmentRoot sourceFolder, MoveArguments arguments) {
		add(sourceFolder, arguments, null);
		IResource sourceResource= sourceFolder.getResource();
		if (sourceResource != null) {
			getResourceModifications().addMove(sourceResource, 
				new MoveArguments(getResourceDestination(arguments), arguments.getUpdateReferences()));
			IFile classpath= getClasspathFile(sourceResource);
			if (classpath != null) {
				getResourceModifications().addChanged(classpath);
			}
			classpath= getClasspathFile(getResourceDestination(arguments));
			if (classpath != null) {
				getResourceModifications().addChanged(classpath);
			}
		}
	}
	
	public void move(IPackageFragment pack, MoveArguments args) throws CoreException {
		add(pack, args, null);
		if (pack.getResource() == null)
			return;
		IPackageFragmentRoot javaDestination= (IPackageFragmentRoot) args.getDestination();
		if (javaDestination.getResource() == null)
			return;
		IPackageFragment newPack= javaDestination.getPackageFragment(pack.getElementName());
		if (!pack.hasSubpackages() && !newPack.exists()) {
			// we can do a simple move
			IContainer resourceDestination= newPack.getResource().getParent();
			createIncludingParents(resourceDestination);
			getResourceModifications().addMove(
				pack.getResource(), 
				new MoveArguments(resourceDestination, args.getUpdateReferences()));
		} else {
			IContainer resourceSource= (IContainer)pack.getResource();
			IContainer resourceDestination= (IContainer) newPack.getResource();
			createIncludingParents(resourceDestination);
			MoveArguments arguments= new MoveArguments(resourceDestination, args.getUpdateReferences());
			IResource[] resourcesToMove= collectResourcesOfInterest(pack);
			Set allMembers= new HashSet(Arrays.asList(resourceSource.members()));
			for (int i= 0; i < resourcesToMove.length; i++) {
				IResource toMove= resourcesToMove[i];
				getResourceModifications().addMove(toMove, arguments);
				allMembers.remove(toMove);
			}
			for (Iterator iter= allMembers.iterator(); iter.hasNext();) {
				IResource element= (IResource) iter.next();
				if (element instanceof IFile) {
					getResourceModifications().addDelete(element);
					iter.remove();
				}
			}
			if (allMembers.isEmpty()) {
				getResourceModifications().addDelete(resourceSource);
			}
		}
	}

	public void move(IJavaScriptUnit unit, MoveArguments args) throws CoreException {
		add(unit, args, null);
		IType[] types= unit.getTypes();
		for (int tt= 0; tt < types.length; tt++) {
			add(types[tt], args, null);
		}
		IResource resourceDestination= getResourceDestination(args);
		if (resourceDestination != null && unit.getResource() != null) {
			getResourceModifications().addMove(unit.getResource(), new MoveArguments(resourceDestination, args.getUpdateReferences()));
		}
	}

	public void buildDelta(IResourceChangeDescriptionFactory builder) {
		for (int i= 0; i < fMoves.size(); i++) {
			Object element= fMoves.get(i);
			if (element instanceof IResource) {
				ResourceModifications.buildMoveDelta(builder, (IResource) element, (MoveArguments) fMoveArguments.get(i));
			}
		}
		getResourceModifications().buildDelta(builder);
	}
	
	public void buildValidateEdits(ValidateEditChecker checker) {
		for (Iterator iter= fMoves.iterator(); iter.hasNext();) {
			Object element= iter.next();
			if (element instanceof IJavaScriptUnit) {
				IJavaScriptUnit unit= (IJavaScriptUnit)element;
				IResource resource= unit.getResource();
				if (resource != null && resource.getType() == IResource.FILE) {
					checker.addFile((IFile)resource);
				}
			}
		}
	}

	public RefactoringParticipant[] loadParticipants(RefactoringStatus status, RefactoringProcessor owner, String[] natures, SharableParticipants shared) {
		List result= new ArrayList();
		for (int i= 0; i < fMoves.size(); i++) {
			result.addAll(Arrays.asList(ParticipantManager.loadMoveParticipants(status, 
				owner, fMoves.get(i), 
				(MoveArguments) fMoveArguments.get(i), 
				(IParticipantDescriptorFilter) fParticipantDescriptorFilter.get(i), 
				natures, shared)));
		}
		result.addAll(Arrays.asList(getResourceModifications().getParticipants(status, owner, natures, shared)));
		return (RefactoringParticipant[]) result.toArray(new RefactoringParticipant[result.size()]);
	}
	
	private void add(Object element, RefactoringArguments args, IParticipantDescriptorFilter filter) {
		Assert.isNotNull(element);
		Assert.isNotNull(args);
		fMoves.add(element);
		fMoveArguments.add(args);
		fParticipantDescriptorFilter.add(filter);
	}
	
	private IResource getResourceDestination(MoveArguments args) {
		Object genericDestination= args.getDestination();
		IResource resourceDestination= null;
		if (genericDestination instanceof IJavaScriptElement) {
			resourceDestination= ((IJavaScriptElement)genericDestination).getResource();
		} else if (genericDestination instanceof IResource) {
			resourceDestination= (IResource)genericDestination;
		}
		return resourceDestination;
	}
} 
