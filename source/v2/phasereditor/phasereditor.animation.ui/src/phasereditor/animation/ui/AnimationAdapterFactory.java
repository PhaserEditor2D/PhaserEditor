package phasereditor.animation.ui;

import org.eclipse.core.runtime.IAdapterFactory;

import phasereditor.assetpack.core.animations.AnimationModel;
import phasereditor.ui.views.IPreviewFactory;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class AnimationAdapterFactory implements IAdapterFactory {

	private static final Class<?>[] ADAPTERS = { IPreviewFactory.class };

	@Override
	public Object getAdapter(Object adaptableObject, Class adapterType) {
		if (adaptableObject instanceof AnimationModel) {
			if (adapterType == IPreviewFactory.class) {
				return new AnimationModelPreviewFactory();
			}
		}
		return null;
	}

	@Override
	public Class<?>[] getAdapterList() {
		return ADAPTERS;
	}

}
