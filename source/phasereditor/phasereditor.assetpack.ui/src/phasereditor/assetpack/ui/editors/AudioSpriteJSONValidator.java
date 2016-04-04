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
package phasereditor.assetpack.ui.editors;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import phasereditor.audiosprite.core.AudioSpriteCore;

public class AudioSpriteJSONValidator implements IValidator {
	@Override
	public IStatus validate(Object value) {
		String jsonData = (String) value;
		String error = null;

		if (jsonData != null && jsonData.length() > 0) {
			try (ByteArrayInputStream contents = new ByteArrayInputStream(
					jsonData.getBytes())) {
				error = AudioSpriteCore
						.isAudioSpriteJSONContent(contents, true);
			} catch (IOException e) {
				error = e.getMessage();
			}
		}

		if (error != null) {
			return ValidationStatus.error(error);
		}

		return Status.OK_STATUS;
	}

}
