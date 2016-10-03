/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.corext.refactoring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.wst.jsdt.core.IField;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptModel;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.ILocalVariable;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.Signature;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ClassInstanceCreation;
import org.eclipse.wst.jsdt.core.dom.ExpressionStatement;
import org.eclipse.wst.jsdt.core.dom.PrimitiveType;
import org.eclipse.wst.jsdt.core.dom.Statement;
import org.eclipse.wst.jsdt.core.infer.IInferenceFile;
import org.eclipse.wst.jsdt.core.infer.InferrenceManager;
import org.eclipse.wst.jsdt.core.infer.InferrenceProvider;
import org.eclipse.wst.jsdt.core.infer.RefactoringSupport;
import org.eclipse.wst.jsdt.internal.corext.SourceRange;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.corext.refactoring.rename.MethodChecks;
import org.eclipse.wst.jsdt.internal.corext.refactoring.reorg.ReorgPolicyFactory;
import org.eclipse.wst.jsdt.internal.corext.refactoring.reorg.ReorgUtils;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.corext.util.JdtFlags;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.JavaTextSelection;
import org.eclipse.wst.jsdt.internal.ui.refactoring.actions.RefactoringActions;

/**
 * Helper class to detect whether a certain refactoring can be enabled on a
 * selection.
 * <p>
 * This class has been introduced to decouple actions from the refactoring code,
 * in order not to eagerly load refactoring classes during action
 * initialization.
 * </p>
 * 
 * 
 */
public final class RefactoringAvailabilityTester {

	public static IType getDeclaringType(IJavaScriptElement element) {
		if (element == null)
			return null;
		if (!(element instanceof IType))
			element= element.getAncestor(IJavaScriptElement.TYPE);
		return (IType) element;
	}

	public static IJavaScriptElement[] getJavaElements(final Object[] elements) {
		List result= new ArrayList();
		for (int index= 0; index < elements.length; index++) {
			if (elements[index] instanceof IJavaScriptElement)
				result.add(elements[index]);
		}
		return (IJavaScriptElement[]) result.toArray(new IJavaScriptElement[result.size()]);
	}

	public static IMember[] getPullUpMembers(final IType type) throws JavaScriptModelException {
		final List list= new ArrayList(3);
		if (type.exists()) {
			IMember[] members= type.getFields();
			for (int index= 0; index < members.length; index++) {
				if (isPullUpAvailable(members[index]))
					list.add(members[index]);
			}
			members= type.getFunctions();
			for (int index= 0; index < members.length; index++) {
				if (isPullUpAvailable(members[index]))
					list.add(members[index]);
			}
			members= type.getTypes();
			for (int index= 0; index < members.length; index++) {
				if (isPullUpAvailable(members[index]))
					list.add(members[index]);
			}
		}
		return (IMember[]) list.toArray(new IMember[list.size()]);
	}

	public static IMember[] getPushDownMembers(final IType type) throws JavaScriptModelException {
		final List list= new ArrayList(3);
		if (type.exists()) {
			IMember[] members= type.getFields();
			for (int index= 0; index < members.length; index++) {
				if (isPushDownAvailable(members[index]))
					list.add(members[index]);
			}
			members= type.getFunctions();
			for (int index= 0; index < members.length; index++) {
				if (isPushDownAvailable(members[index]))
					list.add(members[index]);
			}
		}
		return (IMember[]) list.toArray(new IMember[list.size()]);
	}

	public static IResource[] getResources(final Object[] elements) {
		List result= new ArrayList();
		for (int index= 0; index < elements.length; index++) {
			if (elements[index] instanceof IResource)
				result.add(elements[index]);
		}
		return (IResource[]) result.toArray(new IResource[result.size()]);
	}

	public static IType getSingleSelectedType(IStructuredSelection selection) throws JavaScriptModelException {
		Object first= selection.getFirstElement();
		if (first instanceof IType)
			return (IType) first;
		if (first instanceof IJavaScriptUnit) {
			final IJavaScriptUnit unit= (IJavaScriptUnit) first;
			if (unit.exists())
			return  JavaElementUtil.getMainType(unit);
		}
		return null;
	}

	public static IType getTopLevelType(final IMember[] members) {
		if (members != null && members.length == 1 && Checks.isTopLevelType(members[0]))
			return (IType) members[0];
		return null;
	}

	public static boolean isChangeSignatureAvailable(final IFunction method) throws JavaScriptModelException {
		return Checks.isAvailable(method);
	}

	public static boolean isChangeSignatureAvailable(final IStructuredSelection selection) throws JavaScriptModelException {
		if (selection.size() == 1) {
			if (selection.getFirstElement() instanceof IFunction) {
				final IFunction method= (IFunction) selection.getFirstElement();
				return isChangeSignatureAvailable(method);
			}
		}
		return false;
	}

	public static boolean isChangeSignatureAvailable(final JavaTextSelection selection) throws JavaScriptModelException {
		final IJavaScriptElement[] elements= selection.resolveElementAtOffset();
		if (elements.length == 1 && (elements[0] instanceof IFunction))
			return isChangeSignatureAvailable((IFunction) elements[0]);
		final IJavaScriptElement element= selection.resolveEnclosingElement();
		return (element instanceof IFunction) && isChangeSignatureAvailable((IFunction) element);
	}

	public static boolean isCommonDeclaringType(final IMember[] members) {
		if (members.length == 0)
			return false;
		final IType type= members[0].getDeclaringType();
		if (type == null)
			return false;
		for (int index= 0; index < members.length; index++) {
			if (!type.equals(members[index].getDeclaringType()))
				return false;
		}
		return true;
	}

	public static boolean isConvertAnonymousAvailable(final IStructuredSelection selection) throws JavaScriptModelException {
		if (selection.size() == 1) {
			if (selection.getFirstElement() instanceof IType) {
				return isConvertAnonymousAvailable((IType) selection.getFirstElement());
			}
		}
		return false;
	}

	public static boolean isConvertAnonymousAvailable(final IType type) throws JavaScriptModelException {
		if (Checks.isAvailable(type)) {
			return type.isAnonymous();
		}
		return false;
	}

	public static boolean isConvertAnonymousAvailable(final JavaTextSelection selection) throws JavaScriptModelException {
		final IType type= RefactoringActions.getEnclosingType(selection);
		if (type != null)
			return RefactoringAvailabilityTester.isConvertAnonymousAvailable(type);
		return false;
	}

	public static boolean isCopyAvailable(final IResource[] resources, final IJavaScriptElement[] elements) throws JavaScriptModelException {
		return ReorgPolicyFactory.createCopyPolicy(resources, elements).canEnable();
	}

	public static boolean isDelegateCreationAvailable(final IField field) throws JavaScriptModelException {
		return false;
	}

	public static boolean isDeleteAvailable(final IJavaScriptElement element) throws JavaScriptModelException {
		if (!element.exists())
			return false;
		if (element instanceof IJavaScriptModel || element instanceof IJavaScriptProject)
			return false;
		if (element.getParent() != null && element.getParent().isReadOnly())
			return false;
		if (element instanceof IPackageFragmentRoot) {
			IPackageFragmentRoot root= (IPackageFragmentRoot) element;
			if (root.isExternal() || Checks.isClasspathDelete(root)) // TODO
				// rename
				// isClasspathDelete
				return false;
		}
		if (element.getResource() == null && !RefactoringAvailabilityTester.isWorkingCopyElement(element))
			return false;
		if (element instanceof IMember && ((IMember) element).isBinary())
			return false;
		return true;
	}

	public static boolean isDeleteAvailable(final IResource resource) {
		if (!resource.exists() || resource.isPhantom())
			return false;
		if (resource.getType() == IResource.ROOT || resource.getType() == IResource.PROJECT)
			return false;
		return true;
	}

	public static boolean isDeleteAvailable(final IStructuredSelection selection) throws JavaScriptModelException {
		if (!selection.isEmpty())
			return isDeleteAvailable(selection.toArray());
		return false;
	}

	public static boolean isDeleteAvailable(final Object[] objects) throws JavaScriptModelException {
		if (objects.length != 0) {
			final IResource[] resources= RefactoringAvailabilityTester.getResources(objects);
			final IJavaScriptElement[] elements= RefactoringAvailabilityTester.getJavaElements(objects);
			if (objects.length != resources.length + elements.length)
				return false;
			for (int index= 0; index < resources.length; index++) {
				if (!isDeleteAvailable(resources[index]))
					return false;
			}
			for (int index= 0; index < elements.length; index++) {
				if (!isDeleteAvailable(elements[index]))
					return false;
			}
			return true;
		}
		return false;
	}

	public static boolean isExternalizeStringsAvailable(final IStructuredSelection selection) throws JavaScriptModelException {
		for (Iterator iter= selection.iterator(); iter.hasNext();) {
			Object element= iter.next();
			if (element instanceof IJavaScriptElement) {
				IJavaScriptElement javaElement= (IJavaScriptElement)element;
				if (javaElement.exists() && !javaElement.isReadOnly()) {
					int elementType= javaElement.getElementType();
					if (elementType == IJavaScriptElement.PACKAGE_FRAGMENT) {
						return true;
					} else if (elementType == IJavaScriptElement.PACKAGE_FRAGMENT_ROOT) {
						IPackageFragmentRoot root= (IPackageFragmentRoot)javaElement;
						if (!root.isExternal() && !ReorgUtils.isClassFolder(root))
							return true;
					} else if (elementType == IJavaScriptElement.JAVASCRIPT_PROJECT) {
						return true;
					} else if (elementType == IJavaScriptElement.JAVASCRIPT_UNIT) {
						IJavaScriptUnit cu= (IJavaScriptUnit)javaElement;
						if (cu.exists()) 
							return true;
					} else if (elementType == IJavaScriptElement.TYPE) {
						IType type= (IType)element;
						IJavaScriptUnit cu= type.getJavaScriptUnit();
						if (cu != null && cu.exists())
							return true;
					}
				}
			}
		}
		return false;
	}

	public static boolean isExtractConstantAvailable(final JavaTextSelection selection) {
		return (selection.resolveInClassInitializer() || selection.resolveInMethodBody() || selection.resolveInVariableInitializer()) && Checks.isExtractableExpression(selection.resolveSelectedNodes(), selection.resolveCoveringNode());
	}

	public static boolean isExtractInterfaceAvailable(final IStructuredSelection selection) throws JavaScriptModelException {
		if (selection.size() == 1) {
			Object first= selection.getFirstElement();
			if (first instanceof IType) {
				return isExtractInterfaceAvailable((IType) first);
			} else if (first instanceof IJavaScriptUnit) {
				IJavaScriptUnit unit= (IJavaScriptUnit) first;
				if (!unit.exists() || unit.isReadOnly())
					return false;

				return true;
			}
		}
		return false;
	}

	public static boolean isExtractInterfaceAvailable(final IType type) throws JavaScriptModelException {
		return Checks.isAvailable(type) && !type.isBinary() && !type.isReadOnly() && !type.isAnonymous();
	}

	public static boolean isExtractInterfaceAvailable(final JavaTextSelection selection) throws JavaScriptModelException {
		return isExtractInterfaceAvailable(RefactoringActions.getEnclosingOrPrimaryType(selection));
	}

	public static boolean isExtractMethodAvailable(final ASTNode[] nodes) {
		if (nodes != null && nodes.length != 0) {
			if (nodes.length == 1)
				return nodes[0] instanceof Statement || Checks.isExtractableExpression(nodes[0]);
			else {
				for (int index= 0; index < nodes.length; index++) {
					if (!(nodes[index] instanceof Statement))
						return false;
				}
				return true;
			}
		}
		return false;
	}

	public static boolean isExtractMethodAvailable(final JavaTextSelection selection) {
		return (selection.resolveInMethodBody() || selection.resolveInClassInitializer()) && RefactoringAvailabilityTester.isExtractMethodAvailable(selection.resolveSelectedNodes());
	}

	public static boolean isExtractSupertypeAvailable(IMember member) throws JavaScriptModelException {
		if (!member.exists())
			return false;
		final int type= member.getElementType();
		if (type != IJavaScriptElement.METHOD && type != IJavaScriptElement.FIELD && type != IJavaScriptElement.TYPE)
			return false;
		if (!Checks.isAvailable(member))
			return false;
		if (member instanceof IType) {
			if (!JdtFlags.isStatic(member))
				return false;
		}
		if (member instanceof IFunction) {
			final IFunction method= (IFunction) member;
			if (method.isConstructor())
				return false;
		}
		return true;
	}

	public static boolean isExtractSupertypeAvailable(final IMember[] members) throws JavaScriptModelException {
		if (members != null && members.length != 0) {
			final IType type= getTopLevelType(members);
			if (type != null)
				return true;
			for (int index= 0; index < members.length; index++) {
				if (!isExtractSupertypeAvailable(members[index]))
					return false;
			}
			return isCommonDeclaringType(members);
		}
		return false;
	}

	public static boolean isExtractSupertypeAvailable(final IStructuredSelection selection) throws JavaScriptModelException {
		if (!selection.isEmpty()) {
			if (selection.size() == 1) {
				if (selection.getFirstElement() instanceof IJavaScriptUnit)
					return true; // Do not force opening
				final IType type= getSingleSelectedType(selection);
				if (type != null)
					return Checks.isAvailable(type) && isExtractSupertypeAvailable(new IType[] { type});
			}
			for (final Iterator iterator= selection.iterator(); iterator.hasNext();) {
				if (!(iterator.next() instanceof IMember))
					return false;
			}
			final Set members= new HashSet();
			members.addAll(Arrays.asList(selection.toArray()));
			return isExtractSupertypeAvailable((IMember[]) members.toArray(new IMember[members.size()]));
		}
		return false;
	}

	public static boolean isExtractSupertypeAvailable(final JavaTextSelection selection) throws JavaScriptModelException {
		IJavaScriptElement element= selection.resolveEnclosingElement();
		if (!(element instanceof IMember))
			return false;
		return isExtractSupertypeAvailable(new IMember[] { (IMember) element});
	}

	public static boolean isExtractTempAvailable(final JavaTextSelection selection) {
		final ASTNode[] nodes= selection.resolveSelectedNodes();
		return (selection.resolveInMethodBody() || selection.resolveInClassInitializer()) && (Checks.isExtractableExpression(nodes, selection.resolveCoveringNode()) || (nodes != null && nodes.length == 1 && nodes[0] instanceof ExpressionStatement));
	}

	public static boolean isGeneralizeTypeAvailable(final IJavaScriptElement element) throws JavaScriptModelException {
		if (element != null && element.exists()) {
			String type= null;
			if (element instanceof IFunction)
				type= ((IFunction) element).getReturnType();
			else if (element instanceof IField) {
				final IField field= (IField) element;
				type= field.getTypeSignature();
			} else if (element instanceof ILocalVariable)
				return true;
			else if (element instanceof IType) {
				return true;
			}
			if (type == null || PrimitiveType.toCode(Signature.toString(type)) != null)
				return false;
			return true;
		}
		return false;
	}

	public static boolean isGeneralizeTypeAvailable(final IStructuredSelection selection) throws JavaScriptModelException {
		if (selection.size() == 1) {
			final Object element= selection.getFirstElement();
			if (element instanceof IFunction) {
				final IFunction method= (IFunction) element;
				if (!method.exists())
					return false;
				final String type= method.getReturnType();
				if (PrimitiveType.toCode(Signature.toString(type)) == null)
					return Checks.isAvailable(method);
			} else if (element instanceof IField) {
				final IField field= (IField) element;
				if (!field.exists())
					return false;

				return Checks.isAvailable(field);
			}
		}
		return false;
	}

	public static boolean isGeneralizeTypeAvailable(final JavaTextSelection selection) throws JavaScriptModelException {
		final IJavaScriptElement[] elements= selection.resolveElementAtOffset();
		if (elements.length != 1)
			return false;
		return isGeneralizeTypeAvailable(elements[0]);
	}

	public static boolean isInferTypeArgumentsAvailable(final IJavaScriptElement element) throws JavaScriptModelException {
		if (!Checks.isAvailable(element)) {
			return false;
		} else if (element instanceof IJavaScriptProject) {
			IJavaScriptProject project= (IJavaScriptProject) element;
			IIncludePathEntry[] classpathEntries= project.getRawIncludepath();
			for (int i= 0; i < classpathEntries.length; i++) {
				if (classpathEntries[i].getEntryKind() == IIncludePathEntry.CPE_SOURCE)
					return true;
			}
			return false;
		} else if (element instanceof IPackageFragmentRoot) {
			return ((IPackageFragmentRoot) element).getKind() == IPackageFragmentRoot.K_SOURCE;
		} else if (element instanceof IPackageFragment) {
			return ((IPackageFragment) element).getKind() == IPackageFragmentRoot.K_SOURCE;
		} else if (element instanceof IJavaScriptUnit) {
			return true;
		} else if (element.getAncestor(IJavaScriptElement.JAVASCRIPT_UNIT) != null) {
			return true;
		} else {
			return false;
		}
	}

	public static boolean isInferTypeArgumentsAvailable(final IJavaScriptElement[] elements) throws JavaScriptModelException {
		if (elements.length == 0)
			return false;

		for (int i= 0; i < elements.length; i++) {
			if (!(isInferTypeArgumentsAvailable(elements[i])))
				return false;
		}
		return true;
	}

	public static boolean isInferTypeArgumentsAvailable(final IStructuredSelection selection) throws JavaScriptModelException {
		if (selection.isEmpty())
			return false;

		for (Iterator iter= selection.iterator(); iter.hasNext();) {
			Object element= iter.next();
			if (!(element instanceof IJavaScriptElement))
				return false;
			if (element instanceof IJavaScriptUnit) {
				IJavaScriptUnit unit= (IJavaScriptUnit) element;
				if (!unit.exists() || unit.isReadOnly())
					return false;

				return true;
			}
			if (!isInferTypeArgumentsAvailable((IJavaScriptElement) element))
				return false;
		}
		return true;
	}

	public static boolean isInlineConstantAvailable(final IField field) throws JavaScriptModelException {
		return Checks.isAvailable(field) && JdtFlags.isStatic(field) && JdtFlags.isFinal(field);
	}

	public static boolean isInlineConstantAvailable(final IStructuredSelection selection) throws JavaScriptModelException {
		if (selection.isEmpty() || selection.size() != 1)
			return false;
		final Object first= selection.getFirstElement();
		return (first instanceof IField) && isInlineConstantAvailable(((IField) first));
	}

	public static boolean isInlineConstantAvailable(final JavaTextSelection selection) throws JavaScriptModelException {
		final IJavaScriptElement[] elements= selection.resolveElementAtOffset();
		if (elements.length != 1)
			return false;
		return (elements[0] instanceof IField) && isInlineConstantAvailable(((IField) elements[0]));
	}

	public static boolean isInlineMethodAvailable(IFunction method) throws JavaScriptModelException {
		if (method == null)
			return false;
		if (!method.exists())
			return false;
		if (!method.isStructureKnown())
			return false;
		if (!method.isBinary())
			return true;
		if (method.isConstructor())
			return false;
		return SourceRange.isAvailable(method.getNameRange());
	}

	public static boolean isInlineMethodAvailable(final IStructuredSelection selection) throws JavaScriptModelException {
		if (selection.isEmpty() || selection.size() != 1)
			return false;
		final Object first= selection.getFirstElement();
		return (first instanceof IFunction) && isInlineMethodAvailable(((IFunction) first));
	}

	public static boolean isInlineMethodAvailable(final JavaTextSelection selection) throws JavaScriptModelException {
		final IJavaScriptElement[] elements= selection.resolveElementAtOffset();
		if (elements.length != 1)
			return false;
		IJavaScriptElement element= elements[0];
		if (!(element instanceof IFunction))
			return false;
		IFunction method= (IFunction) element;
		if (!isInlineMethodAvailable((method)))
			return false;

		// in binary class, only activate for method declarations
		IJavaScriptElement enclosingElement= selection.resolveEnclosingElement();
		if (enclosingElement == null || enclosingElement.getAncestor(IJavaScriptElement.CLASS_FILE) == null)
			return true;
		if (!(enclosingElement instanceof IFunction))
			return false;
		IFunction enclosingMethod= (IFunction) enclosingElement;
		if (enclosingMethod.isConstructor())
			return false;
		int nameOffset= enclosingMethod.getNameRange().getOffset();
		int nameLength= enclosingMethod.getNameRange().getLength();
		return (nameOffset <= selection.getOffset()) && (selection.getOffset() + selection.getLength() <= nameOffset + nameLength);
	}

	public static boolean isInlineTempAvailable(final ILocalVariable variable) throws JavaScriptModelException {
		return Checks.isAvailable(variable);
	}

	public static boolean isInlineTempAvailable(final JavaTextSelection selection) throws JavaScriptModelException {
		final IJavaScriptElement[] elements= selection.resolveElementAtOffset();
		if (elements.length != 1)
			return false;
		return (elements[0] instanceof ILocalVariable) && isInlineTempAvailable((ILocalVariable) elements[0]);
	}

	public static boolean isIntroduceFactoryAvailable(final IFunction method) throws JavaScriptModelException {
		return Checks.isAvailable(method) && method.isConstructor();
	}

	public static boolean isIntroduceFactoryAvailable(final IStructuredSelection selection) throws JavaScriptModelException {
		if (selection.size() == 1 && selection.getFirstElement() instanceof IFunction)
			return isIntroduceFactoryAvailable((IFunction) selection.getFirstElement());
		return false;
	}

	public static boolean isIntroduceFactoryAvailable(final JavaTextSelection selection) throws JavaScriptModelException {
		final IJavaScriptElement[] elements= selection.resolveElementAtOffset();
		if (elements.length == 1 && elements[0] instanceof IFunction)
			return isIntroduceFactoryAvailable((IFunction) elements[0]);

		// there's no IFunction for the default constructor
		if (!Checks.isAvailable(selection.resolveEnclosingElement()))
			return false;
		ASTNode node= selection.resolveCoveringNode();
		if (node == null) {
			ASTNode[] selectedNodes= selection.resolveSelectedNodes();
			if (selectedNodes != null && selectedNodes.length == 1) {
				node= selectedNodes[0];
				if (node == null)
					return false;
			} else {
				return false;
			}
		}

		if (node.getNodeType() == ASTNode.CLASS_INSTANCE_CREATION)
			return true;

		node= ASTNodes.getNormalizedNode(node);
		if (node.getLocationInParent() == ClassInstanceCreation.TYPE_PROPERTY)
			return true;

		return false;
	}

	public static boolean isIntroduceIndirectionAvailable(IFunction method) throws JavaScriptModelException {
		if (method == null)
			return false;
		if (!method.exists())
			return false;
		if (!method.isStructureKnown())
			return false;
		if (method.isConstructor())
			return false;

		return true;
	}

	public static boolean isIntroduceIndirectionAvailable(final IStructuredSelection selection) throws JavaScriptModelException {
		if (selection.isEmpty() || selection.size() != 1)
			return false;
		final Object first= selection.getFirstElement();
		return (first instanceof IFunction) && isIntroduceIndirectionAvailable(((IFunction) first));
	}

	public static boolean isIntroduceIndirectionAvailable(final JavaTextSelection selection) throws JavaScriptModelException {
		final IJavaScriptElement[] elements= selection.resolveElementAtOffset();
		if (elements.length == 1)
			return (elements[0] instanceof IFunction) && isIntroduceIndirectionAvailable(((IFunction) elements[0]));
		ASTNode[] selectedNodes= selection.resolveSelectedNodes();
		if (selectedNodes == null || selectedNodes.length != 1)
			return false;
		switch (selectedNodes[0].getNodeType()) {
			case ASTNode.FUNCTION_DECLARATION:
			case ASTNode.FUNCTION_INVOCATION:
			case ASTNode.SUPER_METHOD_INVOCATION:
				return true;
			default:
				return false;
		}
	}

	public static boolean isIntroduceParameterAvailable(final ASTNode[] selectedNodes, ASTNode coveringNode) {
		return Checks.isExtractableExpression(selectedNodes, coveringNode);
	}

	public static boolean isIntroduceParameterAvailable(final JavaTextSelection selection) {
		return selection.resolveInMethodBody() && isIntroduceParameterAvailable(selection.resolveSelectedNodes(), selection.resolveCoveringNode());
	}

	public static boolean isMoveAvailable(final IResource[] resources, final IJavaScriptElement[] elements) throws JavaScriptModelException {
		if (elements != null) {
			for (int index= 0; index < elements.length; index++) {
				IJavaScriptElement element= elements[index];
				if (element == null || !element.exists())
					return false;
				if ((element instanceof IType) && ((IType) element).isLocal())
					return false;
			}
		}
		return ReorgPolicyFactory.createMovePolicy(resources, elements).canEnable();
	}

	public static boolean isMoveAvailable(final JavaTextSelection selection) throws JavaScriptModelException {
		final IJavaScriptElement element= selection.resolveEnclosingElement();
		if (element == null)
			return false;
		return isMoveAvailable(new IResource[0], new IJavaScriptElement[] { element});
	}

	public static boolean isMoveInnerAvailable(final IStructuredSelection selection) throws JavaScriptModelException {
		if (selection.size() == 1) {
			Object first= selection.getFirstElement();
			if (first instanceof IType) {
				return isMoveInnerAvailable((IType) first);
			}
		}
		return false;
	}

	public static boolean isMoveInnerAvailable(final IType type) throws JavaScriptModelException {
		return Checks.isAvailable(type) && !Checks.isAnonymous(type) && !Checks.isTopLevel(type) && !Checks.isInsideLocalType(type);
	}

	public static boolean isMoveInnerAvailable(final JavaTextSelection selection) throws JavaScriptModelException {
		IType type= RefactoringAvailabilityTester.getDeclaringType(selection.resolveEnclosingElement());
		if (type == null)
			return false;
		return isMoveInnerAvailable(type);
	}

	public static boolean isMoveMethodAvailable(final IFunction method) throws JavaScriptModelException {
		return method.exists() && !method.isConstructor() && !method.isBinary() && !method.isReadOnly() && !JdtFlags.isStatic(method);
	}

	public static boolean isMoveMethodAvailable(final IStructuredSelection selection) throws JavaScriptModelException {
		if (selection.size() == 1) {
			final Object first= selection.getFirstElement();
			return first instanceof IFunction && isMoveMethodAvailable((IFunction) first);
		}
		return false;
	}

	public static boolean isMoveMethodAvailable(final JavaTextSelection selection) throws JavaScriptModelException {
		final IJavaScriptElement method= selection.resolveEnclosingElement();
		if (!(method instanceof IFunction))
			return false;
		return isMoveMethodAvailable((IFunction) method);
	}

	public static boolean isMoveStaticAvailable(final IMember member) throws JavaScriptModelException {
		if (!member.exists())
			return false;
		final int type= member.getElementType();
		if (type != IJavaScriptElement.METHOD && type != IJavaScriptElement.FIELD && type != IJavaScriptElement.TYPE)
			return false;
		final IType declaring= member.getDeclaringType();
		if (declaring == null)
			return false;
		if (!Checks.isAvailable(member))
			return false;
		if (type == IJavaScriptElement.METHOD && !JdtFlags.isStatic(member))
			return false;
		if (type == IJavaScriptElement.METHOD && ((IFunction) member).isConstructor())
			return false;
		if (type == IJavaScriptElement.TYPE && !JdtFlags.isStatic(member))
			return false;
		if (!JdtFlags.isStatic(member))
			return false;
		return true;
	}

	public static boolean isMoveStaticAvailable(final IMember[] members) throws JavaScriptModelException {
		for (int index= 0; index < members.length; index++) {
			if (!isMoveStaticAvailable(members[index]))
				return false;
		}
		return true;
	}

	public static boolean isMoveStaticAvailable(final JavaTextSelection selection) throws JavaScriptModelException {
		final IJavaScriptElement element= selection.resolveEnclosingElement();
		if (!(element instanceof IMember))
			return false;
		return RefactoringAvailabilityTester.isMoveStaticMembersAvailable(new IMember[] { (IMember) element});
	}

	public static boolean isMoveStaticMembersAvailable(final IMember[] members) throws JavaScriptModelException {
		if (members == null)
			return false;
		if (members.length == 0)
			return false;
		if (!isMoveStaticAvailable(members))
			return false;
		if (!isCommonDeclaringType(members))
			return false;
		return true;
	}

	public static boolean isPromoteTempAvailable(final ILocalVariable variable) throws JavaScriptModelException {
		return Checks.isAvailable(variable);
	}

	public static boolean isPromoteTempAvailable(final JavaTextSelection selection) throws JavaScriptModelException {
		final IJavaScriptElement[] elements= selection.resolveElementAtOffset();
		if (elements.length != 1)
			return false;
		return (elements[0] instanceof ILocalVariable) && isPromoteTempAvailable((ILocalVariable) elements[0]);
	}

	public static boolean isPullUpAvailable(IMember member) throws JavaScriptModelException {
		if (!member.exists())
			return false;
		final int type= member.getElementType();
		if (type != IJavaScriptElement.METHOD && type != IJavaScriptElement.FIELD && type != IJavaScriptElement.TYPE)
			return false;
		if (!Checks.isAvailable(member))
			return false;
		if (member instanceof IType) {
			if (!JdtFlags.isStatic(member))
				return false;
		}
		if (member instanceof IFunction) {
			final IFunction method= (IFunction) member;
			if (method.isConstructor())
				return false;
		}
		return true;
	}

	public static boolean isPullUpAvailable(final IMember[] members) throws JavaScriptModelException {
		if (members != null && members.length != 0) {
			final IType type= getTopLevelType(members);
			if (type != null && getPullUpMembers(type).length != 0)
				return true;
			for (int index= 0; index < members.length; index++) {
				if (!isPullUpAvailable(members[index]))
					return false;
			}
			return isCommonDeclaringType(members);
		}
		return false;
	}

	public static boolean isPullUpAvailable(final IStructuredSelection selection) throws JavaScriptModelException {
		if (!selection.isEmpty()) {
			if (selection.size() == 1) {
				if (selection.getFirstElement() instanceof IJavaScriptUnit)
					return true; // Do not force opening
				final IType type= getSingleSelectedType(selection);
				if (type != null)
					return Checks.isAvailable(type) && isPullUpAvailable(new IType[] { type});
			}
			for (final Iterator iterator= selection.iterator(); iterator.hasNext();) {
				if (!(iterator.next() instanceof IMember))
					return false;
			}
			final Set members= new HashSet();
			members.addAll(Arrays.asList(selection.toArray()));
			return isPullUpAvailable((IMember[]) members.toArray(new IMember[members.size()]));
		}
		return false;
	}

	public static boolean isPullUpAvailable(final JavaTextSelection selection) throws JavaScriptModelException {
		IJavaScriptElement element= selection.resolveEnclosingElement();
		if (!(element instanceof IMember))
			return false;
		return isPullUpAvailable(new IMember[] { (IMember) element});
	}

	public static boolean isPushDownAvailable(final IMember member) throws JavaScriptModelException {
		if (!member.exists())
			return false;
		final int type= member.getElementType();
		if (type != IJavaScriptElement.METHOD && type != IJavaScriptElement.FIELD)
			return false;
		if (!Checks.isAvailable(member))
			return false;
		if (JdtFlags.isStatic(member))
			return false;
		if (type == IJavaScriptElement.METHOD) {
			final IFunction method= (IFunction) member;
			if (method.isConstructor())
				return false;
		}
		return true;
	}

	public static boolean isPushDownAvailable(final IMember[] members) throws JavaScriptModelException {
		if (members != null && members.length != 0) {
			final IType type= getTopLevelType(members);
			if (type != null && RefactoringAvailabilityTester.getPushDownMembers(type).length != 0)
				return true;
			for (int index= 0; index < members.length; index++) {
				if (!isPushDownAvailable(members[index]))
					return false;
			}
			return isCommonDeclaringType(members);
		}
		return false;
	}

	public static boolean isPushDownAvailable(final IStructuredSelection selection) throws JavaScriptModelException {
		if (!selection.isEmpty()) {
			if (selection.size() == 1) {
				if (selection.getFirstElement() instanceof IJavaScriptUnit)
					return true; // Do not force opening
				final IType type= getSingleSelectedType(selection);
				if (type != null)
					return isPushDownAvailable(new IType[] { type});
			}
			for (final Iterator iterator= selection.iterator(); iterator.hasNext();) {
				if (!(iterator.next() instanceof IMember))
					return false;
			}
			final Set members= new HashSet();
			members.addAll(Arrays.asList(selection.toArray()));
			return isPushDownAvailable((IMember[]) members.toArray(new IMember[members.size()]));
		}
		return false;
	}

	public static boolean isPushDownAvailable(final JavaTextSelection selection) throws JavaScriptModelException {
		IJavaScriptElement element= selection.resolveEnclosingElement();
		if (!(element instanceof IMember))
			return false;
		return isPullUpAvailable(new IMember[] { (IMember) element});
	}

	public static boolean isRenameAvailable(final IJavaScriptUnit unit) {
		if (unit == null)
			return false;
		if (!unit.exists())
			return false;
		if (!JavaModelUtil.isPrimary(unit))
			return false;
		if (unit.isReadOnly())
			return false;
		return true;
	}

	public static boolean isRenameAvailable(final IJavaScriptProject project) throws JavaScriptModelException {
		if (project == null)
			return false;
		if (!Checks.isAvailable(project))
			return false;
		if (!project.isConsistent())
			return false;
		return true;
	}

	public static boolean isRenameAvailable(final ILocalVariable variable) throws JavaScriptModelException {
		return Checks.isAvailable(variable);
	}

	public static boolean isRenameAvailable(final IFunction method) throws CoreException {
		if (method == null)
			return false;
		if (!Checks.isAvailable(method))
			return false;
		if (method.isConstructor())
			return false;
		if (isRenameProhibited(method))
			return false;
		return true;
	}

	public static boolean isRenameAvailable(final IPackageFragment fragment) throws JavaScriptModelException {
		if (fragment == null)
			return false;
		if (!Checks.isAvailable(fragment))
			return false;
		if (fragment.isDefaultPackage())
			return false;
		return true;
	}

	public static boolean isRenameAvailable(final IPackageFragmentRoot root) throws JavaScriptModelException {
		if (root == null)
			return false;
		if (!Checks.isAvailable(root))
			return false;
		if (root.isArchive())
			return false;
		if (root.isExternal())
			return false;
		if (!root.isConsistent())
			return false;
		if (root.getResource() instanceof IProject)
			return false;
		return true;
	}

	public static boolean isRenameAvailable(final IResource resource) {
		if (resource == null)
			return false;
		if (!resource.exists())
			return false;
		if (!resource.isAccessible())
			return false;
		return true;
	}

	public static boolean isRenameAvailable(final IType type) throws JavaScriptModelException {
		if (type == null)
			return false;
		if (type.isAnonymous())
			return false;
		if (!Checks.isAvailable(type))
			return false;
		if (isRenameProhibited(type))
			return false;
		InferrenceProvider[] inferenceProviders = InferrenceManager.getInstance().getInferenceProviders( (IInferenceFile)type.getJavaScriptUnit());
		if (inferenceProviders.length>0 && inferenceProviders[0].getRefactoringSupport()!=null)
		{
			RefactoringSupport refactoringSupport = inferenceProviders[0].getRefactoringSupport();
			if (refactoringSupport!=null)
				return refactoringSupport.supportsClassRename();
		}
		
		
		return false;
	}

	public static boolean isRenameFieldAvailable(final IField field) throws JavaScriptModelException {
		return Checks.isAvailable(field);
	}

	public static boolean isRenameNonVirtualMethodAvailable(final IFunction method) throws JavaScriptModelException, CoreException {
		return isRenameAvailable(method) && !MethodChecks.isVirtual(method);
	}

	public static boolean isRenameProhibited(final IFunction method) throws CoreException {
		if (method.getElementName().equals("toString") //$NON-NLS-1$
				&& (method.getNumberOfParameters() == 0) && (method.getReturnType().equals("Ljava.lang.String;") //$NON-NLS-1$
						|| method.getReturnType().equals("QString;") //$NON-NLS-1$
				|| method.getReturnType().equals("Qjava.lang.String;"))) //$NON-NLS-1$
			return true;
		else
			return false;
	}

	public static boolean isRenameProhibited(final IType type) {
		return type.getPackageFragment().getElementName().equals("java.lang"); //$NON-NLS-1$
	}

	public static boolean isRenameVirtualMethodAvailable(final IFunction method) throws CoreException {
		return isRenameAvailable(method) && MethodChecks.isVirtual(method);
	}

	public static boolean isReplaceInvocationsAvailable(IFunction method) throws JavaScriptModelException {
		if (method == null)
			return false;
		if (!method.exists())
			return false;
		if (method.isConstructor())
			return false;
		return true;
	}

	public static boolean isReplaceInvocationsAvailable(final IStructuredSelection selection) throws JavaScriptModelException {
		if (selection.isEmpty() || selection.size() != 1)
			return false;
		final Object first= selection.getFirstElement();
		return (first instanceof IFunction) && isReplaceInvocationsAvailable(((IFunction) first));
	}

	public static boolean isReplaceInvocationsAvailable(final JavaTextSelection selection) throws JavaScriptModelException {
		final IJavaScriptElement[] elements= selection.resolveElementAtOffset();
		if (elements.length != 1)
			return false;
		IJavaScriptElement element= elements[0];
		return (element instanceof IFunction) && isReplaceInvocationsAvailable(((IFunction) element));
	}

	public static boolean isSelfEncapsulateAvailable(IField field) throws JavaScriptModelException {
		return Checks.isAvailable(field);
	}

	public static boolean isSelfEncapsulateAvailable(final IStructuredSelection selection) throws JavaScriptModelException {
		if (selection.size() == 1) {
			if (selection.getFirstElement() instanceof IField) {
				final IField field= (IField) selection.getFirstElement();
				return Checks.isAvailable(field);
			}
		}
		return false;
	}

	public static boolean isSelfEncapsulateAvailable(final JavaTextSelection selection) throws JavaScriptModelException {
		final IJavaScriptElement[] elements= selection.resolveElementAtOffset();
		if (elements.length != 1)
			return false;
		return (elements[0] instanceof IField) && isSelfEncapsulateAvailable((IField) elements[0]);
	}

	public static boolean isUseSuperTypeAvailable(final IStructuredSelection selection) throws JavaScriptModelException {
		if (selection.size() == 1) {
			final Object first= selection.getFirstElement();
			if (first instanceof IType) {
				return isUseSuperTypeAvailable((IType) first);
			} else if (first instanceof IJavaScriptUnit) {
				IJavaScriptUnit unit= (IJavaScriptUnit) first;
				if (!unit.exists() || unit.isReadOnly())
					return false;

				return true;
			}
		}
		return false;
	}

	public static boolean isUseSuperTypeAvailable(final IType type) throws JavaScriptModelException {
		return type != null && type.exists();
	}

	public static boolean isUseSuperTypeAvailable(final JavaTextSelection selection) throws JavaScriptModelException {
		return isUseSuperTypeAvailable(RefactoringActions.getEnclosingOrPrimaryType(selection));
	}

	public static boolean isWorkingCopyElement(final IJavaScriptElement element) {
		if (element instanceof IJavaScriptUnit)
			return ((IJavaScriptUnit) element).isWorkingCopy();
		if (ReorgUtils.isInsideCompilationUnit(element))
			return ReorgUtils.getCompilationUnit(element).isWorkingCopy();
		return false;
	}

	private RefactoringAvailabilityTester() {
		// Not for instantiation
	}
}
