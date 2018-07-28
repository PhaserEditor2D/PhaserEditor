package phasereditor.assetpack.ui.properties;

import org.eclipse.core.runtime.IAdapterFactory;

import phasereditor.assetpack.core.AnimationsAssetModel;
import phasereditor.assetpack.core.AnimationsAssetModel.AnimationFrameModel_in_AssetPack;
import phasereditor.assetpack.core.AnimationsAssetModel.AnimationModel_in_AssetPack;
import phasereditor.assetpack.core.AssetGroupModel;
import phasereditor.assetpack.core.AssetSectionModel;
import phasereditor.assetpack.core.AtlasAssetModel;
import phasereditor.assetpack.core.AudioAssetModel;
import phasereditor.assetpack.core.AudioSpriteAssetModel;
import phasereditor.assetpack.core.BitmapFontAssetModel;
import phasereditor.assetpack.core.HtmlAssetModel;
import phasereditor.assetpack.core.ImageAssetModel;
import phasereditor.assetpack.core.MultiAtlasAssetModel;
import phasereditor.assetpack.core.PluginAssetModel;
import phasereditor.assetpack.core.ScenePluginAssetModel;
import phasereditor.assetpack.core.SimpleFileAssetModel;
import phasereditor.assetpack.core.SpritesheetAssetModel;
import phasereditor.assetpack.core.SvgAssetModel;
import phasereditor.assetpack.core.TilemapAssetModel;
import phasereditor.assetpack.core.animations.AnimationFrameModel;
import phasereditor.ui.properties.PGridModel;

public class AssetPGridAdapterFactory implements IAdapterFactory {

	private static final Class<?>[] LIST = { PGridModel.class };

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Object getAdapter(Object obj, Class adapterType) {

		if (adapterType == PGridModel.class) {
			if (obj instanceof AssetSectionModel) {
				return new SectionPGridModel((AssetSectionModel) obj);
			} else if (obj instanceof AssetGroupModel) {
				return new AssetGroupPGridModel((AssetGroupModel) obj);
			} else if (obj instanceof ImageAssetModel) {
				return new ImageAssetPGridModel((ImageAssetModel) obj);
			} else if (obj instanceof SvgAssetModel) {
				return new SvgAssetPGridModel((SvgAssetModel) obj);
			} else if (obj instanceof SpritesheetAssetModel) {
				return new SpritesheetAssetPGridModel((SpritesheetAssetModel) obj);
			} else if (obj instanceof AtlasAssetModel) {
				return new AtlasAssetPGridModel((AtlasAssetModel) obj);
			} else if (obj instanceof MultiAtlasAssetModel) {
				return new MultiAtlasAssetPGridModel((MultiAtlasAssetModel) obj);
			} else if (obj instanceof AudioSpriteAssetModel) {
				return new AudioSpriteAssetPGridModel((AudioSpriteAssetModel) obj);
			} else if (obj instanceof AudioAssetModel) {
				return new AudioAssetPGridModel((AudioAssetModel) obj);
			} else if (obj instanceof TilemapAssetModel) {
				return new TilemapAssetPGridModel((TilemapAssetModel) obj);
			} else if (obj instanceof BitmapFontAssetModel) {
				return new BitmapFontAssetPGridModel((BitmapFontAssetModel) obj);
			} else if (obj instanceof SimpleFileAssetModel) {
				return new SimpleFileAssetPGridModel((SimpleFileAssetModel) obj);
			} else if (obj instanceof HtmlAssetModel) {
				return new HtmlAssetPGridModel((HtmlAssetModel) obj);
			} else if (obj instanceof PluginAssetModel) {
				return new PluginAssetPGridModel((PluginAssetModel) obj);
			} else if (obj instanceof ScenePluginAssetModel) {
				return new ScenePluginAssetPGridModel((ScenePluginAssetModel) obj);
			} else if (obj instanceof AnimationsAssetModel) {
				return new AnimationsAssetPGridModel((AnimationsAssetModel) obj);
			} else if (obj instanceof AnimationModel_in_AssetPack) {
				return new AnimationModel_in_AssetPack_PGridModel((AnimationModel_in_AssetPack) obj);
			} else if (obj instanceof AnimationFrameModel_in_AssetPack) {
				return new AnimationFramePGridModel((AnimationFrameModel) obj);
			}
		}
		return null;
	}

	@Override
	public Class<?>[] getAdapterList() {
		return LIST;
	}

}
