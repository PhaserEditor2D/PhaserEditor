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
package phasereditor.inspect.core.jsdoc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author arian
 *
 */
public class PhaserNamespace extends PhaserMember implements IMemberContainer {

	private List<PhaserNamespace> _namespaces;
	private List<PhaserType> _types;
	private List<PhaserConstant> _consts;
	private List<PhaserProperty> _properties;
	private List<PhaserMethod> _methods;
	private Map<String, IPhaserMember> _memberMap;

	public PhaserNamespace() {
		_namespaces = new ArrayList<>();
		_types = new ArrayList<>();
		_properties = new ArrayList<>();
		_methods = new ArrayList<>();
		_consts = new ArrayList<>();
		_memberMap = new HashMap<>();
	}

	@Override
	public void build() {
		for (IPhaserMember member : _memberMap.values()) {
			if (member instanceof PhaserType) {
				_types.add((PhaserType) member);
			} else if (member instanceof PhaserMethod) {
				_methods.add((PhaserMethod) member);
			} else if (member instanceof PhaserConstant) {
				_consts.add((PhaserConstant) member);
			} else if (member instanceof PhaserProperty) {
				_properties.add((PhaserProperty) member);
			} else if (member instanceof PhaserNamespace) {
				_namespaces.add((PhaserNamespace) member);
			}
		}
	}

	@Override
	public List<PhaserNamespace> getNamespaces() {
		return _namespaces;
	}

	@Override
	public List<PhaserType> getTypes() {
		return _types;
	}

	@Override
	public List<PhaserProperty> getProperties() {
		return _properties;
	}

	@Override
	public List<PhaserMethod> getMethods() {
		return _methods;
	}

	@Override
	public List<PhaserConstant> getConstants() {
		return _consts;
	}

	@Override
	public Map<String, IPhaserMember> getMemberMap() {
		return _memberMap;
	}

}
