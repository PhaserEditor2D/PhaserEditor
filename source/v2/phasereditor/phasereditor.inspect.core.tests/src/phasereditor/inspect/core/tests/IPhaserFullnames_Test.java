// The MIT License (MIT)
//
// Copyright (c) 2015 Arian Fornaris
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
package phasereditor.inspect.core.tests;

import static java.lang.System.out;
import static org.junit.Assert.assertNotNull;

import java.nio.file.Paths;

import org.junit.Test;

import phasereditor.inspect.core.InspectCore;
import phasereditor.inspect.core.jsdoc.IPhaserFullnames;
import phasereditor.inspect.core.jsdoc.PhaserJsdocModel;

@SuppressWarnings("static-method")
public class IPhaserFullnames_Test {

	@Test
	public void test() throws Exception {
		var wsPath = Paths.get(".").toAbsolutePath().getParent().getParent();
		var srcFolder = wsPath.resolve(InspectCore.RESOURCES_PHASER_CODE_PLUGIN).resolve("phaser-master/src");
		var docsJsonFile = wsPath.resolve(InspectCore.RESOURCES_METADATA_PLUGIN)
				.resolve("phaser-custom/phaser3-docs/json/phaser.json").toAbsolutePath().normalize();

		var docsModel = new PhaserJsdocModel(srcFolder, docsJsonFile);

		var fields = IPhaserFullnames.class.getDeclaredFields();
		for (var field : fields) {
			var fullname = field.get(IPhaserFullnames.class);
			out.println("IPhaserFullnames_Test -> " + fullname);
			var member = docsModel.getMembersMap().get(fullname);
			assertNotNull("Get member " + fullname, member);
		}
	}

}
