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

import static java.lang.System.out;

import java.net.URL;

import phasereditor.inspect.core.InspectCore;

/**
 * @author arian
 *
 */
public class PhaserMemberJsdocProvider implements IJsdocProvider {

	private IPhaserMember _member;

	public PhaserMemberJsdocProvider(IPhaserMember member) {
		super();
		_member = member;
	}

	@Override
	public String getJsdoc() {
		return JsdocRenderer.getInstance().render(_member);
	}

	@Override
	public IJsdocProvider processLink(String link) {

		String link2 = link;

		link2 = link2.replace("about:blank", "");

		if (link2.startsWith("#")) {
			IMemberContainer container;
			if (_member instanceof IMemberContainer) {
				container = (IMemberContainer) _member;
			} else {
				container = _member.getContainer();
			}

			if (container != null) {
				link2 = container.getName() + link2;
			}

		}

		link2 = link2.replace("#", ".");

		String name;
		try {
			URL url = new URL(link2);
			name = url.getPath().replace("/", "");
		} catch (Exception e) {
			name = link2;
		}

		out.println("PhaserMemberJsdocProvider: looking for member " + name);

		IPhaserMember member = InspectCore.getPhaserHelp().getMembersMap().get(name);

		if (member != null) {
			return new PhaserMemberJsdocProvider(member);
		}

		return null;

	}

}
