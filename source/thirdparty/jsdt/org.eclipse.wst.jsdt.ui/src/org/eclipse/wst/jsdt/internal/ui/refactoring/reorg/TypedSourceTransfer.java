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
package org.eclipse.wst.jsdt.internal.ui.refactoring.reorg;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.eclipse.core.runtime.Assert;
import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.wst.jsdt.internal.corext.refactoring.TypedSource;

public class TypedSourceTransfer extends ByteArrayTransfer {

	/**
	 * Singleton instance.
	 */
	private static final TypedSourceTransfer fgInstance = new TypedSourceTransfer();
	
	// Create a unique ID to make sure that different Eclipse
	// applications use different "types" of <code>TypedSourceTransfer</code>
	private static final String TYPE_NAME = "typed-source-transfer-format:" + System.currentTimeMillis() + ":" + fgInstance.hashCode();//$NON-NLS-2$//$NON-NLS-1$
	
	private static final int TYPEID = registerType(TYPE_NAME);

	private TypedSourceTransfer() {
	}
	
	/**
	 * Returns the singleton instance.
 	*
 	* @return the singleton instance
 	*/
	public static TypedSourceTransfer getInstance() {
		return fgInstance;
	}

	/* (non-Javadoc)
	 * Method declared on Transfer.
	 */
	protected int[] getTypeIds() {
		return new int[] {TYPEID};
	}
	
	/* (non-Javadoc)
	 * Returns the type names.
	 *
	 * @return the list of type names
	 */
	protected String[] getTypeNames() {
		return new String[] {TYPE_NAME};
	}

	/* (non-Javadoc)
	 * Method declared on Transfer.
	 */
	protected void javaToNative(Object data, TransferData transferData) {
		if (! (data instanceof TypedSource[]))
			return;
		TypedSource[] sources = (TypedSource[]) data;	

		/*
		 * The serialization format is:
		 *  (int) number of elements
		 * Then, the following for each element:
		 *  (int) type (see <code>IJavaScriptElement</code>)
		 *  (String) source of the element
		 */
		
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			DataOutputStream dataOut = new DataOutputStream(out);

			dataOut.writeInt(sources.length);

			for (int i = 0; i < sources.length; i++) {
				writeJavaElement(dataOut, sources[i]);
			}

			dataOut.close();
			out.close();

			super.javaToNative(out.toByteArray(), transferData);
		} catch (IOException e) {
			//it's best to send nothing if there were problems
		}		
	}

	/* (non-Javadoc)
	 * Method declared on Transfer.
	 */
	protected Object nativeToJava(TransferData transferData) {
	
		byte[] bytes = (byte[]) super.nativeToJava(transferData);
		if (bytes == null)
			return null;
		DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
		try {
			int count = in.readInt();
			TypedSource[] results = new TypedSource[count];
			for (int i = 0; i < count; i++) {
				results[i] = readJavaElement(in);
				Assert.isNotNull(results[i]);
			}
			in.close();
			return results;
		} catch (IOException e) {
			return null;
		}
	}

	private static TypedSource readJavaElement(DataInputStream dataIn) throws IOException {
		int type= dataIn.readInt();
		String source= dataIn.readUTF();
		return TypedSource.create(source, type);
	}

	private static void writeJavaElement(DataOutputStream dataOut, TypedSource sourceReference) throws IOException {
		dataOut.writeInt(sourceReference.getType());
		dataOut.writeUTF(sourceReference.getSource());
	}
}

