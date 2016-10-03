/*******************************************************************************
 * Copyright (c) 2006, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.codeassist.complete;

import org.eclipse.wst.jsdt.internal.compiler.ast.MessageSend;
import org.eclipse.wst.jsdt.internal.compiler.ast.NameReference;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;

public class CompletionOnMessageSendName extends MessageSend {
	public CompletionOnMessageSendName(char[] selector, int start, int end) {
		super();
		this.selector = selector;
		this.sourceStart = start;
		this.sourceEnd = end;
		this.nameSourcePosition = end;
	}

	public TypeBinding resolveType(BlockScope scope) {

		if (receiver==null || receiver.isImplicitThis())
			throw new CompletionNodeFound();

		this.actualReceiverType = receiver.resolveType(scope);
		if (this.actualReceiverType == null || this.actualReceiverType.isBaseType() || this.actualReceiverType.isArrayType())
			throw new CompletionNodeFound();

		if(this.receiver instanceof NameReference) {
			throw new CompletionNodeFound(this, ((NameReference)this.receiver).binding, scope);
		} else if(this.receiver instanceof MessageSend) {
			throw new CompletionNodeFound(this, ((MessageSend)this.receiver).binding, scope);
		}
		throw new CompletionNodeFound(this, this.actualReceiverType, scope);
	}

	public StringBuffer printExpression(int indent, StringBuffer output) {

		output.append("<CompleteOnMessageSendName:"); //$NON-NLS-1$
		if (receiver!=null && receiver.isImplicitThis()) receiver.printExpression(0, output).append('.');
		output.append(selector).append('(');
		return output.append(")>"); //$NON-NLS-1$
	}
}
