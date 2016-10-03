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
package org.eclipse.wst.jsdt.internal.corext.fix;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.wst.jsdt.core.dom.rewrite.ITrackedNodePosition;
import org.eclipse.wst.jsdt.internal.corext.fix.LinkedProposalPositionGroup.PositionInformation;

public class LinkedProposalModel {
	
	private Map/*<String, PositionGroup>*/ fPositionGroups;
	private LinkedProposalPositionGroup.PositionInformation fEndPosition;
	
	public void addPositionGroup(LinkedProposalPositionGroup positionGroup) {
		if (positionGroup == null) {
			throw new IllegalArgumentException("positionGroup must not be null"); //$NON-NLS-1$
		}
		
		if (fPositionGroups == null) {
			fPositionGroups= new HashMap();
		}
		fPositionGroups.put(positionGroup.getGroupId(), positionGroup);
	}
	
	public LinkedProposalPositionGroup getPositionGroup(String groupId, boolean createIfNotExisting) {
		LinkedProposalPositionGroup group= fPositionGroups != null ? (LinkedProposalPositionGroup) fPositionGroups.get(groupId) : null;
		if (createIfNotExisting && group == null) {
			group= new LinkedProposalPositionGroup(groupId);
			addPositionGroup(group);
		}
		return group;
	}
	
	public Iterator getPositionGroupIterator() {
		if (fPositionGroups == null) {
			return new Iterator() {
				public boolean hasNext() {return false;}
				public Object next() {return null;}
				public void remove() {}
			};
		}
		return fPositionGroups.values().iterator();
	}
	
	
	/**
	 * Sets the end position of the linked mode to the end of the passed range.
	 * @param position The position that describes the end position of the linked mode.
	 */
	public void setEndPosition(PositionInformation position) {
		fEndPosition= position;
	}
	
	public void setEndPosition(ITrackedNodePosition position) {
		setEndPosition(LinkedProposalPositionGroup.createPositionInformation(position, false));
	}
	
	public PositionInformation getEndPosition() {
		return fEndPosition;
	}

	public boolean hasLinkedPositions() {
		return fPositionGroups != null && !fPositionGroups.isEmpty();
	}

	public void clear() {
		fPositionGroups= null;
		fEndPosition= null;	
	}
	
}
