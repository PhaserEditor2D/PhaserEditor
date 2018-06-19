package phasereditor.bluesky.core;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.lsp4j.SymbolInformation;

import phasereditor.inspect.core.jsdoc.IJsdocProvider;

public class JsdocAdapterFactory implements IAdapterFactory {

	private static final Class<?>[] ADAPTERS = new Class<?>[] { IJsdocProvider.class };

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Object getAdapter(Object adaptableObject, Class adapterType) {
		if (adaptableObject instanceof SymbolInformation) {
			return new SymbolInformationJsdocProvider((SymbolInformation) adaptableObject);
		}
		return null;
	}

	@Override
	public Class<?>[] getAdapterList() {
		return ADAPTERS;
	}

}
