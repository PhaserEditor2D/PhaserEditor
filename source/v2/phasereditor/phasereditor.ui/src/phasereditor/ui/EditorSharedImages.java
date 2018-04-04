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
package phasereditor.ui;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class EditorSharedImages implements IEditorSharedImages {
	private static Map<String, javafx.scene.image.Image> _fxMap = new HashMap<>();

	public static Image getImage(String path) {
		Activator plugin = Activator.getDefault();
		Image image = plugin.getImageRegistry().get(path);
		if (image == null) {
			plugin.getImageRegistry().put(path, getImageDescriptor(path));
			image = plugin.getImageRegistry().get(path);
		}
		return image;
	}

	public static ImageDescriptor getImageDescriptor(String path) {
		return AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID, path);
	}

	public static javafx.scene.image.Image getFXImage(String path) {
		if (_fxMap.containsKey(path)) {
			return _fxMap.get(path);
		}
		javafx.scene.image.Image img = new javafx.scene.image.Image("platform:/plugin/phasereditor.ui/" + path);
		_fxMap.put(path, img);
		return img;
	}
}
