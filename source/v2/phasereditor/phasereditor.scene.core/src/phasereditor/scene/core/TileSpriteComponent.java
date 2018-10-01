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
package phasereditor.scene.core;

/**
 * @author arian
 *
 */
@SuppressWarnings("boxing")
public interface TileSpriteComponent {
	// tilePosition
	static String tilePositionX_name = "tilePositionX";
	static String tilePositionY_name = "tilePositionY";

	static float tilePositionX_default = 0;
	static float tilePositionY_default = 0;

	static float get_tilePositionX(ObjectModel obj) {
		return (float) obj.get("tilePositionX");
	}

	static void set_tilePositionX(ObjectModel obj, float tilePositionX) {
		obj.put("tilePositionX", tilePositionX);
	}

	static float get_tilePositionY(ObjectModel obj) {
		return (float) obj.get("tilePositionY");
	}

	static void set_tilePositionY(ObjectModel obj, float tilePositionY) {
		obj.put("tilePositionY", tilePositionY);
	}

	// tileScale

	static String tileScaleX_name = "tileScaleX";
	static String tileScaleY_name = "tileScaleY";

	static float tileScaleX_default = 1;
	static float tileScaleY_default = 1;

	static float get_tileScaleX(ObjectModel obj) {
		return (float) obj.get("tileScaleX");
	}

	static void set_tileScaleX(ObjectModel obj, float tileScaleX) {
		obj.put("tileScaleX", tileScaleX);
	}

	static float get_tileScaleY(ObjectModel obj) {
		return (float) obj.get("tileScaleY");
	}

	static void set_tileScaleY(ObjectModel obj, float tileScaleY) {
		obj.put("tileScaleY", tileScaleY);
	}

	// size

	// width

	static String width_name = "width";

	static float width_default = -1;

	static float get_width(ObjectModel obj) {
		return (float) obj.get("width");
	}

	static void set_width(ObjectModel obj, float width) {
		obj.put("width", width);
	}

	// height

	static String height_name = "height";

	static float height_default = -1;

	static float get_height(ObjectModel obj) {
		return (float) obj.get("height");
	}

	static void set_height(ObjectModel obj, float height) {
		obj.put("height", height);
	}

	// init

	static void init(ObjectModel obj) {
		set_tilePositionX(obj, tilePositionX_default);
		set_tilePositionY(obj, tilePositionY_default);

		set_tileScaleX(obj, tileScaleX_default);
		set_tileScaleY(obj, tileScaleY_default);

		set_width(obj, width_default);
		set_height(obj, height_default);
	}
}
