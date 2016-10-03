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
package org.eclipse.wst.jsdt.internal.corext.codemanipulation;

import java.util.Comparator;
import java.util.List;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.BodyDeclaration;
import org.eclipse.wst.jsdt.core.dom.FieldDeclaration;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.Initializer;
import org.eclipse.wst.jsdt.core.dom.Modifier;
import org.eclipse.wst.jsdt.core.dom.SingleVariableDeclaration;
import org.eclipse.wst.jsdt.core.dom.Type;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment;
import org.eclipse.wst.jsdt.core.util.JavaScriptUnitSorter;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.corext.util.JdtFlags;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.preferences.MembersOrderPreferenceCache;

import com.ibm.icu.text.Collator;
/**
*
* Provisional API: This class/interface is part of an interim API that is still under development and expected to
* change significantly before reaching stability. It is being made available at this early stage to solicit feedback
* from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
* (repeatedly) as the API evolves.
*/
public class SortMembersOperation implements IWorkspaceRunnable {


	/**
	 * Default comparator for body declarations.
	 */
	public static class DefaultJavaElementComparator implements Comparator {

		private final Collator fCollator;
		private final MembersOrderPreferenceCache fMemberOrderCache;
		private final boolean fDoNotSortFields;

		public DefaultJavaElementComparator(boolean doNotSortFields) {
			fDoNotSortFields= doNotSortFields;
			fCollator= Collator.getInstance();
			fMemberOrderCache= JavaScriptPlugin.getDefault().getMemberOrderPreferenceCache();
		}

		private int category(BodyDeclaration bodyDeclaration) {
			switch (bodyDeclaration.getNodeType()) {
				case ASTNode.FUNCTION_DECLARATION:
					{
						FunctionDeclaration method= (FunctionDeclaration) bodyDeclaration;
						if (method.isConstructor()) {
							return getMemberCategory(MembersOrderPreferenceCache.CONSTRUCTORS_INDEX);
						}
						int flags= method.getModifiers();
						if (Modifier.isStatic(flags))
							return getMemberCategory(MembersOrderPreferenceCache.STATIC_METHODS_INDEX);
						else
							return getMemberCategory(MembersOrderPreferenceCache.METHOD_INDEX);
					}
				case ASTNode.FIELD_DECLARATION :
					{
						int flags= ((FieldDeclaration) bodyDeclaration).getModifiers();
						if (Modifier.isStatic(flags))
							return getMemberCategory(MembersOrderPreferenceCache.STATIC_FIELDS_INDEX);
						else
							return getMemberCategory(MembersOrderPreferenceCache.FIELDS_INDEX);
					}
				case ASTNode.INITIALIZER :
					{
						int flags= ((Initializer) bodyDeclaration).getModifiers();
						if (Modifier.isStatic(flags))
							return getMemberCategory(MembersOrderPreferenceCache.STATIC_INIT_INDEX);
						else
							return getMemberCategory(MembersOrderPreferenceCache.INIT_INDEX);
					}
				case ASTNode.TYPE_DECLARATION :
					return getMemberCategory(MembersOrderPreferenceCache.TYPE_INDEX);
					
			}
			return 0; // should never happen
		}

		private int getMemberCategory(int kind) {
			return fMemberOrderCache.getCategoryIndex(kind);
		}

		/**
		 * This comparator follows the contract defined in JavaScriptUnitSorter.sort.
		 * @see Comparator#compare(java.lang.Object, java.lang.Object)
		 * @see JavaScriptUnitSorter#sort(int, org.eclipse.wst.jsdt.core.IJavaScriptUnit, int[], java.util.Comparator, int, org.eclipse.core.runtime.IProgressMonitor)
		 */
		public int compare(Object e1, Object e2) {
			BodyDeclaration bodyDeclaration1= (BodyDeclaration) e1;
			BodyDeclaration bodyDeclaration2= (BodyDeclaration) e2;
			int cat1= category(bodyDeclaration1);
			int cat2= category(bodyDeclaration2);

			if (cat1 != cat2) {
				return cat1 - cat2;
			}
			
			if (fMemberOrderCache.isSortByVisibility()) {
				int flags1= JdtFlags.getVisibilityCode(bodyDeclaration1);
				int flags2= JdtFlags.getVisibilityCode(bodyDeclaration2);
				int vis= fMemberOrderCache.getVisibilityIndex(flags1) - fMemberOrderCache.getVisibilityIndex(flags2);
				if (vis != 0) {
					return vis;
				}
			}

			switch (bodyDeclaration1.getNodeType()) {
				case ASTNode.FUNCTION_DECLARATION :
					{
						FunctionDeclaration method1= (FunctionDeclaration) bodyDeclaration1;
						FunctionDeclaration method2= (FunctionDeclaration) bodyDeclaration2;

						if (fMemberOrderCache.isSortByVisibility()) {
							int vis= fMemberOrderCache.getVisibilityIndex(method1.getModifiers()) - fMemberOrderCache.getVisibilityIndex(method2.getModifiers());
							if (vis != 0) {
								return vis;
							}
						}
						
						String name1= method1.getName().getIdentifier();
						String name2= method2.getName().getIdentifier();

						// method declarations (constructors) are sorted by name
						int cmp= this.fCollator.compare(name1, name2);
						if (cmp != 0) {
							return cmp;
						}

						// if names equal, sort by parameter types
						List parameters1= method1.parameters();
						List parameters2= method2.parameters();
						int length1= parameters1.size();
						int length2= parameters2.size();

						int len= Math.min(length1, length2);
						for (int i= 0; i < len; i++) {
							SingleVariableDeclaration param1= (SingleVariableDeclaration) parameters1.get(i);
							SingleVariableDeclaration param2= (SingleVariableDeclaration) parameters2.get(i);
							cmp= this.fCollator.compare(buildSignature(param1.getType()), buildSignature(param2.getType()));
							if (cmp != 0) {
								return cmp;
							}
						}
						if (length1 != length2) {
							return length1 - length2;
						}
						return preserveRelativeOrder(bodyDeclaration1, bodyDeclaration2);
					}
				case ASTNode.FIELD_DECLARATION :
					{
						if (!fDoNotSortFields) {
							FieldDeclaration field1= (FieldDeclaration) bodyDeclaration1;
							FieldDeclaration field2= (FieldDeclaration) bodyDeclaration2;
	
							String name1= ((VariableDeclarationFragment) field1.fragments().get(0)).getName().getIdentifier();
							String name2= ((VariableDeclarationFragment) field2.fragments().get(0)).getName().getIdentifier();
	
							// field declarations are sorted by name
							return compareNames(bodyDeclaration1, bodyDeclaration2, name1, name2);
						} else {
							return preserveRelativeOrder(bodyDeclaration1, bodyDeclaration2);
						}
					}
				case ASTNode.INITIALIZER :
					{
						// preserve relative order
						return preserveRelativeOrder(bodyDeclaration1, bodyDeclaration2);
					}
				case ASTNode.TYPE_DECLARATION :
					{
						AbstractTypeDeclaration type1= (AbstractTypeDeclaration) bodyDeclaration1;
						AbstractTypeDeclaration type2= (AbstractTypeDeclaration) bodyDeclaration2;

						String name1= type1.getName().getIdentifier();
						String name2= type2.getName().getIdentifier();

						// typedeclarations are sorted by name
						return compareNames(bodyDeclaration1, bodyDeclaration2, name1, name2);					
					}
			}
			return 0;
		}

		private int preserveRelativeOrder(BodyDeclaration bodyDeclaration1, BodyDeclaration bodyDeclaration2) {
			int value1= ((Integer) bodyDeclaration1.getProperty(JavaScriptUnitSorter.RELATIVE_ORDER)).intValue();
			int value2= ((Integer) bodyDeclaration2.getProperty(JavaScriptUnitSorter.RELATIVE_ORDER)).intValue();
			return value1 - value2;
		}

		private int compareNames(BodyDeclaration bodyDeclaration1, BodyDeclaration bodyDeclaration2, String name1, String name2) {
			int cmp= this.fCollator.compare(name1, name2);
			if (cmp != 0) {
				return cmp;
			}
			return preserveRelativeOrder(bodyDeclaration1, bodyDeclaration2);
		}

		private String buildSignature(Type type) {
			return ASTNodes.asString(type);
		}
	}


	private IJavaScriptUnit fCompilationUnit;
	private int[] fPositions;
	private final boolean fDoNotSortFields;
	
	/**
	 * Creates the operation.
	 * @param cu The working copy of a compilation unit.
	 * @param positions Positions to track or <code>null</code> if no positions
	 * should be tracked.
	 */
	public SortMembersOperation(IJavaScriptUnit cu, int[] positions, boolean doNotSortFields) {
		fCompilationUnit= cu;
		fPositions= positions;
		fDoNotSortFields= doNotSortFields;
	}


	/**
	 * Runs the operation.
	 */
	public void run(IProgressMonitor monitor) throws CoreException {
		JavaScriptUnitSorter.sort(AST.JLS3, fCompilationUnit, fPositions, new DefaultJavaElementComparator(fDoNotSortFields), 0, monitor);
	}

	/**
	 * @return Returns the scheduling rule for this operation
	 */
	public ISchedulingRule getScheduleRule() {
		return ResourcesPlugin.getWorkspace().getRoot();
	}
	
}
