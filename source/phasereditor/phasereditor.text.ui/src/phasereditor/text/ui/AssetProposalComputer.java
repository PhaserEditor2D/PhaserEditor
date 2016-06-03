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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.swt.graphics.Image;

import phasereditor.assetpack.core.AssetModel;
import phasereditor.assetpack.core.AssetPackCore;
import phasereditor.assetpack.core.AssetPackModel;
import phasereditor.assetpack.core.AssetSectionModel;
import phasereditor.assetpack.core.AssetType;
import phasereditor.assetpack.core.AtlasAssetModel;
import phasereditor.assetpack.core.AtlasAssetModel.Frame;
import phasereditor.assetpack.core.AudioSpriteAssetModel;
import phasereditor.assetpack.core.AudioSpriteAssetModel.AssetAudioSprite;
import phasereditor.assetpack.core.PhysicsAssetModel;
import phasereditor.assetpack.core.TilemapAssetModel;
import phasereditor.assetpack.core.TilemapAssetModel.Layer;
import phasereditor.assetpack.core.TilemapAssetModel.Tilemap;
import phasereditor.assetpack.core.TilemapAssetModel.Tileset;
import phasereditor.assetpack.ui.AssetLabelProvider;
import phasereditor.assetpack.ui.preview.AtlasAssetInformationControl;
import phasereditor.assetpack.ui.preview.AtlasFrameInformationControl;
import phasereditor.assetpack.ui.preview.AudioSpriteAssetElementInformationControl;
import phasereditor.assetpack.ui.preview.AudioSpriteAssetInformationControl;
import phasereditor.assetpack.ui.preview.BitmapFontAssetInformationControl;
import phasereditor.assetpack.ui.preview.ImageAssetInformationControl;
import phasereditor.assetpack.ui.preview.OtherAssetInformationControl;
import phasereditor.assetpack.ui.preview.PhysicsAssetInformationControl;
import phasereditor.assetpack.ui.preview.SpritesheetAssetInformationControl;
import phasereditor.assetpack.ui.preview.TilemapAssetInformationControl;
import phasereditor.assetpack.ui.preview.TilemapTilesetInformationControl;
import phasereditor.assetpack.ui.preview.VideoAssetScreenshotInformationControl;
import phasereditor.ui.info.GenericInformationControlCreator;

public class AssetProposalComputer extends BaseProposalComputer {

	private static final int ASSET_KEY_ORDER = 3000;
	private static final int SPRITE_ATLAS_ORDER = 2500;
	private static final int SPRITE_AUDIO_ORDER = 2000;
	private static final int TILEMAP_ORDER = 1500;
	private static final int SECTION_KEY_ORDER = 1000;
	protected static Image _elementImage;
	private static AssetLabelProvider _labelProvider;

	static {
		_elementImage = AssetLabelProvider.getElementImage();
		_labelProvider = new AssetLabelProvider();
	}

	public AssetProposalComputer() {
	}

	@Override
	protected List<ProposalData> computeProjectProposals(IProject project) {
		List<ProposalData> list = new ArrayList<>();
		List<AssetPackModel> models = AssetPackCore.getAssetPackModels(project);

		for (AssetPackModel model : models) {
			list.addAll(computeAssetProposals(model));
		}

		return list;
	}

	private static List<ProposalData> computeAssetProposals(AssetPackModel pack) {
		List<ProposalData> list = new ArrayList<>();
		List<AssetSectionModel> sections = pack.getSections();

		for (AssetSectionModel section : sections) {

			// section key

			String assetKey = section.getKey();
			if (assetKey != null) {
				String display = "\"" + assetKey + "\" - section";
				ProposalData propData = new ProposalData(section, assetKey, display, SECTION_KEY_ORDER);
				propData.setImage(AssetLabelProvider.getKeyImage());
				propData.setControlCreator(new GenericInformationControlCreator(OtherAssetInformationControl.class,
						OtherAssetInformationControl::new));
				list.add(propData);
			}

			// asset key

			for (AssetModel asset : section.getAssets()) {
				assetKey = asset.getKey();
				AssetType type = asset.getType();

				if (assetKey == null) {
					assetKey = "<key no set>";
				}

				ProposalData propData;
				{
					String display = "\"" + assetKey + "\" - " + type;
					propData = new ProposalData(asset, assetKey, display, ASSET_KEY_ORDER + type.ordinal());
					propData.setControlCreator(new GenericInformationControlCreator(OtherAssetInformationControl.class,
							OtherAssetInformationControl::new));
					propData.setImage(_labelProvider.getImage(asset));
					list.add(propData);
				}
				
				switch (type) {
				case image:
					propData.setControlCreator(new GenericInformationControlCreator(ImageAssetInformationControl.class,
							ImageAssetInformationControl::new));
					break;
				case spritesheet:
					propData.setControlCreator(new GenericInformationControlCreator(
							SpritesheetAssetInformationControl.class, SpritesheetAssetInformationControl::new));
					break;
				case bitmapFont:
					propData.setControlCreator(new GenericInformationControlCreator(
							BitmapFontAssetInformationControl.class, BitmapFontAssetInformationControl::new));
					break;
				case audiosprite:
					propData.setControlCreator(new GenericInformationControlCreator(
							AudioSpriteAssetInformationControl.class, AudioSpriteAssetInformationControl::new));
					List<AssetAudioSprite> spritemap = ((AudioSpriteAssetModel) asset).getSpriteMap();
					for (AssetAudioSprite sprite : spritemap) {
						if (sprite.getName() != null) {
							String display = "\"" + sprite.getName() + "\" - sprite of audiosprite";
							ProposalData proposal = new ProposalData(sprite, sprite.getName(), display,
									SPRITE_AUDIO_ORDER);
							proposal.setControlCreator(new GenericInformationControlCreator(
									AudioSpriteAssetElementInformationControl.class,
									AudioSpriteAssetElementInformationControl::new));
							proposal.setImage(_labelProvider.getImage(asset));
							list.add(proposal);
						}
					}
					break;
				case atlas:
					propData.setControlCreator(new GenericInformationControlCreator(AtlasAssetInformationControl.class,
							AtlasAssetInformationControl::new));

					List<Frame> frames = ((AtlasAssetModel) asset).getAtlasFrames();
					for (Frame frame : frames) {
						String name = frame.getName();
						if (name != null) {
							String display = "\"" + name + "\" - sprite of atlas";
							ProposalData proposal = new ProposalData(frame, name, display, SPRITE_ATLAS_ORDER);
							proposal.setControlCreator(new GenericInformationControlCreator(
									AtlasFrameInformationControl.class, AtlasFrameInformationControl::new));
							proposal.setImage(_labelProvider.getImage(frame));
							list.add(proposal);
						}
					}
					break;
				case tilemap: {
					propData.setControlCreator(new GenericInformationControlCreator(
							TilemapAssetInformationControl.class, TilemapAssetInformationControl::new));

					Tilemap tilemap = ((TilemapAssetModel) asset).getTilemap();

					for (Layer layer : tilemap.getLayers()) {
						String display = "\"" + layer.getName() + "\" - layer of tilemap";
						ProposalData proposal = new ProposalData(layer, layer.getName(), display, TILEMAP_ORDER);
						proposal.setImage(_elementImage);
						list.add(proposal);
					}
					for (Tileset tileset : tilemap.getTilesets()) {
						String display = "\"" + tileset.getName() + "\" - tileset of tilemap";
						ProposalData proposal = new ProposalData(tileset, tileset.getName(), display, TILEMAP_ORDER);
						proposal.setControlCreator(new GenericInformationControlCreator(
								TilemapTilesetInformationControl.class, TilemapTilesetInformationControl::new));
						proposal.setImage(_elementImage);
						list.add(proposal);
					}
					break;
				}
				case physics: {
					GenericInformationControlCreator controlCreator = new GenericInformationControlCreator(
							PhysicsAssetInformationControl.class, PhysicsAssetInformationControl::new);
					propData.setControlCreator(controlCreator);
					List<PhysicsAssetModel.SpriteData> sprites = ((PhysicsAssetModel) asset).getSprites();
					for (PhysicsAssetModel.SpriteData sprite : sprites) {
						String display = "\"" + sprite.getName() + "\" - sprite of physics";
						ProposalData proposal = new ProposalData(sprite, sprite.getName(), display, TILEMAP_ORDER);
						proposal.setControlCreator(controlCreator);
						proposal.setImage(_elementImage);
						list.add(proposal);
					}
					break;
				}
				case video:
					propData.setControlCreator(new GenericInformationControlCreator(VideoAssetScreenshotInformationControl.class,
							VideoAssetScreenshotInformationControl::new));
					break;
				default:
					break;
				}
			}
		}
		return list;
	}
}
