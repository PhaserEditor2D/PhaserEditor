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

import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.jface.viewers.ITreeContentProvider;

import phasereditor.inspect.core.jsdoc.PhaserType;

/**
 * @author arian
 *
 */
public class PhaserHierarchyContentProvider implements ITreeContentProvider {

	private PhaserType _focusType;
	private Set<PhaserType> _rootTypes;

	public PhaserHierarchyContentProvider() {
	}

	public PhaserHierarchyContentProvider(PhaserType focusType) {
		super();

		_focusType = focusType;
		_rootTypes = new LinkedHashSet<>();
		_rootTypes.add(_focusType);

		buildRoots();
	}

	private void buildRoots() {
		Set<PhaserType> set = new LinkedHashSet<>();

		for (PhaserType type : _rootTypes) {
			set.addAll(type.getExtending());
		}

		if (!set.isEmpty()) {
			_rootTypes = set;
			buildRoots();
		}
	}

	@Override
	public Object[] getElements(Object inputElement) {
		if (_rootTypes == null) {
			return new Object[0];
		}
		return _rootTypes.toArray();
	}

	@Override
	public Object[] getChildren(Object parent) {

		if (parent instanceof PhaserType) {
			return ((PhaserType) parent).getExtenders().toArray();
		}

		return new Object[0];
	}

	@Override
	public Object getParent(Object element) {
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		return getChildren(element).length > 0;
	}

}
