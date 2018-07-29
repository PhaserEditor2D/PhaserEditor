package phasereditor.assetpack.core;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.content.IContentDescriber;
import org.eclipse.core.runtime.content.IContentDescription;

public class AnimationsContentTypeDescriber implements IContentDescriber  {
	public static final String CONTENT_TYPE_ID = "phasereditor.assetpack.core.animations";
	
	public AnimationsContentTypeDescriber() {
	}

	@Override
	public int describe(InputStream contents, IContentDescription description) throws IOException {
		
		var valid = AssetPackCore.isAnimationsContentType(contents);
		
		return valid ? VALID : INVALID;
	}

	@Override
	public QualifiedName[] getSupportedOptions() {
		return new QualifiedName[0];
	}

}
