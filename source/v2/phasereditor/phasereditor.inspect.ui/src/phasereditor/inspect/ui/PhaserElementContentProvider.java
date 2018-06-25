// The MIT License (MIT)
//
// Copyright (c) 2015, 2018 Arian Fornaris
//
// Permission is hereby granted, free of charge, to any person obtaining a
// copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to permit
// persons to whom the Software is furnished to do so, subject to the
// following conditions: The above copyright notice and this permission
// notice shall be included in all copies or substantial portions of the
// Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
// OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
// NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
// DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
// OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
// USE OR OTHER DEALINGS IN THE SOFTWARE.
package phasereditor.inspect.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;

import phasereditor.inspect.core.jsdoc.PhaserGlobalScope;
import phasereditor.inspect.core.jsdoc.IMemberContainer;
import phasereditor.inspect.core.jsdoc.IPhaserMember;
import phasereditor.inspect.core.jsdoc.PhaserJsdocModel;

/**
 * @author arian
 *
 */
public class PhaserElementContentProvider implements ITreeContentProvider {

	@Override
	public Object[] getElements(Object inputElement) {

		if (inputElement instanceof PhaserJsdocModel) {
			PhaserJsdocModel docs = (PhaserJsdocModel) inputElement;
			List<Object> list = new ArrayList<>(docs.getRootNamespaces());
			list.add(docs.getGlobalScope());

			return list.toArray();
		}

		return new Object[] {};
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof PhaserGlobalScope) {
			return ((PhaserGlobalScope) parentElement).getMembers().toArray();
		}

		if (parentElement instanceof IMemberContainer) {
			IMemberContainer container = (IMemberContainer) parentElement;
			List<Object> list = new ArrayList<>();
			list.addAll(container.getNamespaces());
			list.addAll(container.getTypes());
			list.addAll(container.getConstants());
			list.addAll(container.getProperties());
			list.addAll(container.getMethods());
			return list.toArray();
		}

		return new Object[] {};
	}

	@Override
	public Object getParent(Object element) {
		if (element instanceof IPhaserMember) {
			return ((IPhaserMember) element).getContainer();
		}
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		return getChildren(element).length > 0;
	}
}
