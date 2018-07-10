package phasereditor.assetpack.ui.properties;

import org.eclipse.core.runtime.IAdapterFactory;

import phasereditor.assetpack.core.AssetGroupModel;
import phasereditor.assetpack.core.AssetSectionModel;
import phasereditor.assetpack.core.ImageAssetModel;
import phasereditor.assetpack.core.SpritesheetAssetModel;
import phasereditor.ui.properties.PGridModel;

public class AssetPGrigAdapterFactory implements IAdapterFactory {

	private static final Class<?>[] LIST = { PGridModel.class };

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Object getAdapter(Object adaptableObject, Class adapterType) {

		if (adapterType == PGridModel.class) {

			if (adaptableObject instanceof AssetSectionModel) {
				return new SectionPGridModel((AssetSectionModel) adaptableObject);
			} else if (adaptableObject instanceof AssetGroupModel) {
				return new AssetGroupPGridModel((AssetGroupModel) adaptableObject);
			} else if (adaptableObject instanceof ImageAssetModel) {
				return new ImageAssetPGridModel((ImageAssetModel) adaptableObject);
			} else if (adaptableObject instanceof SpritesheetAssetModel) {
				return new SpritesheetAssetPGridModel((SpritesheetAssetModel) adaptableObject);
			}

		}

		return null;
	}

	@Override
	public Class<?>[] getAdapterList() {
		return LIST;
	}

}
