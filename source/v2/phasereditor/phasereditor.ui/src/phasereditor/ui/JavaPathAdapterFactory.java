package phasereditor.ui;

import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.core.runtime.IAdapterFactory;

public class JavaPathAdapterFactory implements IAdapterFactory {

	private static final Class<?>[] _adaptersList = { ISourceLocation.class };

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Object getAdapter(Object adaptableObject, Class adapterType) {
		if (adapterType == ISourceLocation.class) {
			if (adaptableObject instanceof Path) {

				Path path = (Path) adaptableObject;

				if (Files.isDirectory(path)) {
					return null;
				}

				return new ISourceLocation() {

					@Override
					public int getLine() {
						return 0;
					}

					@Override
					public Path getFilePath() {
						return (Path) adaptableObject;
					}
				};
			}
		}

		return null;
	}

	@Override
	public Class<?>[] getAdapterList() {
		return _adaptersList;
	}

}
