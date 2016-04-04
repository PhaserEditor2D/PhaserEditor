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
package phasereditor.inspect.core;

import java.io.IOException;
import java.nio.file.Path;

import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.IType;

import phasereditor.inspect.core.examples.ExamplesModel;
import phasereditor.inspect.core.resources.InspectCoreResources;
import phasereditor.inspect.core.templates.TemplatesModel;

public class InspectCore {

	public static final String RESOURCES_PLUGIN_ID = InspectCoreResources.PLUGIN_ID;

	public static final String PHASER_VERSION = "2.4.6";

	protected static ExamplesModel _examplesModel;
	private static TemplatesModel _builtInTemplates;

	public static ExamplesModel getExamplesModel() {
		if (_examplesModel == null) {
			try {
				Path bundlePath = InspectCoreResources.getBundleFolder();
				_examplesModel = new ExamplesModel(bundlePath);
				Path cache = bundlePath.resolve("phaser-examples-cache.json");
				_examplesModel.loadCache(cache);
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
		return _examplesModel;
	}

	public static TemplatesModel getGeneralTemplates() {
		if (_builtInTemplates == null) {
			try {
				Path bundlePath = InspectCoreResources.getBundleFolder();
				String rel = "templates";
				Path templatesPath = bundlePath.resolve(rel);
				_builtInTemplates = new TemplatesModel(templatesPath);
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
		return _builtInTemplates;
	}

	public static String getFullName(IMember member) {
		String name;

		if (member instanceof IType) {
			name = member.getDisplayName();
		} else {
			IType type = member.getDeclaringType();
			if (type == null) {
				return "<invalid-member>";
			}
			name = type.getDisplayName() + "." + member.getDisplayName();
		}
		return name;
	}

}
