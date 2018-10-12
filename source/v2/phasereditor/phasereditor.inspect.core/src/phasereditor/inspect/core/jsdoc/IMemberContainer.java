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

import java.util.List;
import java.util.Map;

/**
 * @author arian
 *
 */
public interface IMemberContainer extends IPhaserMember {

	default PhaserType castType() {
		if (this instanceof PhaserType) {
			return (PhaserType) this;
		}
		return null;
	}

	default PhaserNamespace castNamespace() {
		if (this instanceof PhaserNamespace) {
			return (PhaserNamespace) this;
		}
		return null;
	}

	public int getHeight();

	Map<String, IPhaserMember> getMemberMap();

	List<PhaserNamespace> getNamespaces();

	List<PhaserType> getTypes();

	List<PhaserProperty> getProperties();

	List<PhaserMethod> getMethods();

	List<PhaserConstant> getConstants();

	List<PhaserProperty> getAllProperties();

	List<PhaserMethod> getAllMethods();

	List<PhaserConstant> getAllConstants();

	void build();
}
