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
package phasereditor.inspect.core.jsdoc;

import java.io.Serializable;
import java.nio.file.Path;

import org.json.JSONObject;

import phasereditor.inspect.core.InspectCore;
import phasereditor.ui.ISourceLocation;

public interface IPhaserMember extends ISourceLocation, Serializable {
	public String getName();
	
	public String getHelp();

	public void setLine(int line);

	public int getOffset();

	public void setOffset(int offset);

	public String getFile();

	public void setFile(String filename);

	public boolean isStatic();
	
	public JSONObject getJSON();
	
	public IMemberContainer getContainer();
	
	public void setContainer(IMemberContainer container);
	
	@Override
	public int getLine();
	
	@Override
	default Path getFilePath() {
		return InspectCore.getPhaserHelp().getMemberPath(this);
	}
}
