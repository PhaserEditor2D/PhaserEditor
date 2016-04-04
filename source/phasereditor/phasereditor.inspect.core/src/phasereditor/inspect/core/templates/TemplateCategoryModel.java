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
package phasereditor.inspect.core.templates;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import phasereditor.inspect.core.IPhaserCategory;

public class TemplateCategoryModel implements IPhaserCategory {
	private String _name;
	private List<TemplateModel> _templates;
	private TemplatesModel _parent;
	private String _description;

	public TemplateCategoryModel(TemplatesModel parent, String name) {
		super();
		_name = name;
		_templates = new ArrayList<>();
		_parent = parent;
		_description = "";
	}

	public TemplatesModel getParent() {
		return _parent;
	}

	@Override
	public String getName() {
		return _name;
	}

	@Override
	public List<TemplateModel> getTemplates() {
		return _templates;
	}

	public void addTemplate(TemplateModel template) {
		_templates.add(template);
	}

	public void addTemplate(Path template) {
		addTemplate(new TemplateModel(_parent, this, template));
	}

	@Override
	public String getDescription() {
		return _description;
	}

	public void setDescription(String description) {
		_description = description;
	}

}
