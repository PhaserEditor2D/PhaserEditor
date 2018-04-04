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
package phasereditor.text.ui;

import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.swt.graphics.Image;

public class ProposalData {
	private String _name;
	private String _display;
	private String _info;
	private int _relevance;
	private Image _image;
	private Object _object;
	private IInformationControlCreator _controlCreator;

	public ProposalData(Object object, String name, String display, int relevance) {
		_object = object;
		_name = name;
		_display = display;
		_relevance = relevance;
	}

	public IInformationControlCreator getControlCreator() {
		return _controlCreator;
	}

	public void setControlCreator(IInformationControlCreator controlCreator) {
		_controlCreator = controlCreator;
	}

	public Object getObject() {
		return _object;
	}

	public void setObject(Object object) {
		_object = object;
	}

	public Image getImage() {
		return _image;
	}

	public void setImage(Image image) {
		_image = image;
	}

	public String getName() {
		return _name;
	}

	public void setName(String name) {
		this._name = name;
	}

	public String getDisplay() {
		return _display;
	}

	public void setDisplay(String display) {
		this._display = display;
	}

	public String getInfo() {
		return _info;
	}

	public void setInfo(String info) {
		this._info = info;
	}

	public int getRelevance() {
		return _relevance;
	}

	public void setRelevance(int relevance) {
		this._relevance = relevance;
	}
}