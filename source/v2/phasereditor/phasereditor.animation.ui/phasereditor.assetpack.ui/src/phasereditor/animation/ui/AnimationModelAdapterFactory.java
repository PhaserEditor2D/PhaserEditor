package phasereditor.animation.ui;

import org.eclipse.core.runtime.IAdapterFactory;

import phasereditor.assetpack.core.animations.AnimationModel;
import phasereditor.ui.views.IPreviewFactory;

public class AnimationModelAdapterFactory implements IAdapterFactory {

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		if (adaptableObject instanceof AnimationModel) {
			if (adapterType == IPreviewFactory.class) {
				return (T) new AnimationModelPreviewFactory();
			}
		}
		return null;
	}

	@Override
	public Class<?>[] getAdapterList() {
		return new Class[] { IPreviewFactory.class };
	}

}
