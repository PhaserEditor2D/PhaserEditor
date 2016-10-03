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
package org.eclipse.wst.jsdt.internal.corext.refactoring.code.flow;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.wst.jsdt.core.dom.CatchClause;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.IVariableBinding;
import org.eclipse.wst.jsdt.core.dom.SingleVariableDeclaration;
import org.eclipse.wst.jsdt.core.dom.TryStatement;


public class FlowContext {

	private static class Enum {
	}
	
	public static final Enum MERGE=			new Enum();
	public static final Enum ARGUMENTS= 	new Enum();
	public static final Enum RETURN_VALUES= new Enum();
	
	private int fStart;
	private int fLength;
	private boolean fConsiderAccessMode;
	private boolean fLoopReentranceMode;
	private Enum fComputeMode;
	private IVariableBinding[] fLocals;
	private List fExceptionStack;
	
	private static final List EMPTY_CATCH_CLAUSE= new ArrayList(0);
	
	public FlowContext(int start, int length) {
		fStart= start;
		fLength= length;
		fExceptionStack= new ArrayList(3);
	}
	
	public void setConsiderAccessMode(boolean b) {
		fConsiderAccessMode= b;
	}
	
	public void setComputeMode(Enum mode) {
		fComputeMode= mode;
	}
	
	void setLoopReentranceMode(boolean b) {
		fLoopReentranceMode= b;
	}
	
	int getArrayLength() {
		return fLength;
	}
	
	int getStartingIndex() {
		return fStart;
	}
	
	boolean considerAccessMode() {
		return fConsiderAccessMode;
	}
	
	boolean isLoopReentranceMode() {
		return fLoopReentranceMode;
	}
	
	boolean computeMerge() {
		return fComputeMode == MERGE;
	}
	
	boolean computeArguments() {
		return fComputeMode == ARGUMENTS;
	}
	
	boolean computeReturnValues() {
		return fComputeMode == RETURN_VALUES;
	}
	
	public IVariableBinding getLocalFromId(int id) {
		return getLocalFromIndex(id - fStart);
	}
	
	public IVariableBinding getLocalFromIndex(int index) {
		if (fLocals == null || index > fLocals.length)
			return null;
		return fLocals[index];
	}
	
	public int getIndexFromLocal(IVariableBinding local) {
		if (fLocals == null)
			return -1;
		for (int i= 0; i < fLocals.length; i++) {
			if (fLocals[i] == local)
				return i;
		}
		return -1;
	}
	
	void manageLocal(IVariableBinding local) {
		if (fLocals == null)
			fLocals= new IVariableBinding[fLength];
		fLocals[local.getVariableId() - fStart]= local;
	}
	
	//---- Exception handling --------------------------------------------------------
	
	void pushExcptions(TryStatement node) {
		List catchClauses= node.catchClauses();
		if (catchClauses == null)
			catchClauses= EMPTY_CATCH_CLAUSE;
		fExceptionStack.add(catchClauses);
	}
	
	void popExceptions() {
		Assert.isTrue(fExceptionStack.size() > 0);
		fExceptionStack.remove(fExceptionStack.size() - 1);
	}
	
	boolean isExceptionCaught(ITypeBinding excpetionType) {
		for (Iterator exceptions= fExceptionStack.iterator(); exceptions.hasNext(); ) {
			for (Iterator catchClauses= ((List)exceptions.next()).iterator(); catchClauses.hasNext(); ) {
				SingleVariableDeclaration catchedException= ((CatchClause)catchClauses.next()).getException();
				IVariableBinding binding= catchedException.resolveBinding();
				if (binding == null)
					continue;
				ITypeBinding catchedType= binding.getType();
				while (catchedType != null) {
					if (catchedType == excpetionType)
						return true;
					catchedType= catchedType.getSuperclass();
				}
			}
		}
		return false;
	}
}
