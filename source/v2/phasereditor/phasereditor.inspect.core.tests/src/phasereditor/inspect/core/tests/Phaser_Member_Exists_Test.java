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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import phasereditor.inspect.core.InspectCore;
import phasereditor.inspect.core.jsdoc.IPhaserMember;
import phasereditor.inspect.core.jsdoc.PhaserJsdocModel;
import phasereditor.inspect.core.jsdoc.PhaserMethod;
import phasereditor.inspect.core.jsdoc.PhaserVariable;

@SuppressWarnings("static-method")
public class Phaser_Member_Exists_Test {

	@Test
	public void test() throws IOException {
		Path wsPath = Paths.get(".").toAbsolutePath().getParent().getParent();
		Path sourceProjectPath = wsPath.resolve(InspectCore.RESOURCES_PHASER_CODE_PLUGIN);
		Path metadataProjectPath = wsPath.resolve(InspectCore.RESOURCES_METADATA_PLUGIN);
		PhaserJsdocModel jsDoc = new PhaserJsdocModel(sourceProjectPath.resolve("phaser-master/src"),
				metadataProjectPath.resolve("phaser-custom/jsdoc/docs.json"));

		// test the members exist

		String[] names = {

				"Phaser.StateManager.getCurrentState", //
				"Phaser.Sprite.anchor", //
				"Phaser.Sprite.scale", //
				"Phaser.Group.scale", //
				"Phaser.Sprite.rotation", //
				"Phaser.KeyCode.SPACEBAR", //
				"Phaser.Keyboard.SPACEBAR", //
				"Phaser.PENDING_ATLAS", //
				"Phaser.Physics.Arcade.Body.onCeiling", //
				"Phaser.Loader.audioSprite", //
				"Phaser.Line.intersectsRectangle",//
				"Phaser.Utils.reverseString",//

				// TODO: check for PIXI.Graphics, method should be inherited
				// from there.
				// "Phaser.Graphics.lineTo", //

				// requires PIXI.Graphics.tint update
				// "Phaser.Graphics.tint",

				// definition not found
				// "Phaser.RetroFont.sendToBack",

		};

		Map<String, IPhaserMember> map = jsDoc.getMembersMap();

		for (String name : names) {
			IPhaserMember member = map.get(name);
			Assert.assertNotNull(name, member);
		}

		// test the type of the members

		String[][] memberTypeMap = {
				{ 
					"Phaser.Sprite.scale", "Phaser.Point" 
				}

		};
		for (String[] tuple : memberTypeMap) {
			IPhaserMember member = map.get(tuple[0]);
			String result = null;
			if (member instanceof PhaserMethod) {
				result = ((PhaserMethod) member).getReturnTypes()[0];
			} else {
				result = ((PhaserVariable) member).getTypes()[0];
			}
			Assert.assertTrue(tuple[0] + " results " + result + " but expected " + tuple[1], tuple[1].equals(result));
		}
	}

}
