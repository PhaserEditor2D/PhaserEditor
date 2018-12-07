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
package phasereditor.assetpack.ui.preview;

import static phasereditor.ui.PhaserEditorUI.pickFileWithoutExtension;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;

import phasereditor.assetpack.core.AssetGroupModel;
import phasereditor.assetpack.core.AssetModel;
import phasereditor.assetpack.core.AssetModelFactory;
import phasereditor.assetpack.core.AssetPackModel;
import phasereditor.assetpack.core.AssetSectionModel;
import phasereditor.assetpack.core.AssetType;
import phasereditor.assetpack.core.AtlasAssetModel;
import phasereditor.assetpack.core.AudioAssetModel;
import phasereditor.assetpack.core.AudioSpriteAssetModel;
import phasereditor.assetpack.core.BitmapFontAssetModel;
import phasereditor.assetpack.core.IAssetElementModel;
import phasereditor.assetpack.core.IAssetFrameModel;
import phasereditor.assetpack.core.ImageAssetModel;
import phasereditor.assetpack.core.MultiAtlasAssetModel;
import phasereditor.assetpack.core.PhysicsAssetModel;
import phasereditor.assetpack.core.SpritesheetAssetModel;
import phasereditor.assetpack.core.TilemapAssetModel;
import phasereditor.assetpack.core.VideoAssetModel;
import phasereditor.animation.ui.AnimationModelPreviewFactory;
import phasereditor.assetpack.core.AnimationsAssetModel.AnimationModel_in_AssetPack;
import phasereditor.assetpack.ui.AssetLabelProvider;
import phasereditor.atlas.core.AtlasFrame;
import phasereditor.audiosprite.ui.GdxMusicControl;
import phasereditor.ui.views.IPreviewFactory;

public class AssetPreviewAdapterFactory implements IAdapterFactory {

	private static final Class<?>[] ADAPTERS = { IPreviewFactory.class };

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Object getAdapter(Object adaptable, Class adapterType) {
		if (adaptable instanceof AssetModel) {
			AssetModel asset = (AssetModel) adaptable;
			AssetType type = asset.getType();
			switch (type) {
			case image:
				return createImagePreviewAdapter();
			case spritesheet:
				return createSpritesheetPreviewAdapter();
			case audio:
				return createAudioPreviewAdpter();
			case video:
				return createVideoPreviewAdpter();
			case audioSprite:
				return createAudioSpritePreviewAdpter();
			case atlas:
			case atlasXML:
			case unityAtlas:
				return createAtlasPreviewAdapter();
			case multiatlas:
				return createMultiAtlasPreviewAdapter();
			case tilemapCSV:
				return createTilemapCSVPreviewAdapter();
			case tilemapTiledJSON:
				return createTilemapJSONPreviewAdapter();
			case bitmapFont:
				return createBitmapFontPreviewAdapter();
			case physics:
				return createPhysicsPreviewAdapter();
			default:
				break;
			}
		} else if (adaptable instanceof SpritesheetAssetModel.FrameModel) {
			return createSpritesheetFramePreviewAdapter();
		} else if (adaptable instanceof IAssetFrameModel && adaptable instanceof AtlasFrame) {
			return createAtlasFramePreviewAdapter();
		} else if (adaptable instanceof AnimationModel_in_AssetPack) {
			return createAnimationPreviewAdapter();
		}
		return null;
	}

	public static abstract class AssetModelPreviewFactory implements IPreviewFactory {

		public AssetModelPreviewFactory() {
		}

		private AssetLabelProvider _labelProvider = AssetLabelProvider.GLOBAL_16;

		@Override
		public Image getIcon(Object element) {
			return _labelProvider.getImage(element);
		}

		@Override
		public void updateControl(Control preview, Object element) {
			if (element instanceof IAssetElementModel) {
				IAssetElementModel assetElem = ((IAssetElementModel) element).getSharedVersion();
				AssetModel asset = assetElem == null ? null : assetElem.getAsset();
				updateControl2(preview, asset);
				selectInControl(preview, assetElem);
			} else {
				AssetModel asset = ((AssetModel) element).getSharedVersion();
				updateControl2(preview, asset);
			}
		}

		@Override
		public void selectInControl(Control preview, Object element) {
			// nothing by default
		}

		@SuppressWarnings("unused")
		protected void updateControl2(Control preview, Object toUpdate) {
			// nothing
		}

		@Override
		public void savePreviewControl(Control previewControl, IMemento memento) {
			//
		}

		@Override
		public void initPreviewControl(Control previewControl, IMemento initialMemento) {
			//
		}

		@Override
		public String getTitle(Object element) {
			if (element instanceof IAssetElementModel) {
				IAssetElementModel assetElem = (IAssetElementModel) element;
				return assetElem.getKey() + " (" + assetElem.getAsset().getKey() + ")";
			}

			return ((AssetModel) element).getKey();
		}

		@Override
		public void hiddenControl(Control preview) {
			// nothing
		}

		@Override
		public IPersistableElement getPersistable(Object elem) {
			Object toPersist = elem;
			return new IPersistableElement() {

				@Override
				public void saveState(IMemento memento) {
					AssetPackModel pack = null;
					if (toPersist instanceof AssetModel) {
						pack = ((AssetModel) toPersist).getPack();
					} else if (toPersist instanceof AssetSectionModel) {
						pack = ((AssetSectionModel) toPersist).getPack();
					} else if (toPersist instanceof AssetGroupModel) {
						pack = ((AssetGroupModel) toPersist).getSection().getPack();
					} else if (toPersist instanceof IAssetElementModel) {
						pack = ((IAssetElementModel) toPersist).getAsset().getPack();
					}

					if (pack != null) {
						pack.saveState(memento, toPersist);
					}
				}

				@Override
				public String getFactoryId() {
					return AssetModelFactory.ID;
				}
			};
		}

	}

	private static IPreviewFactory createPhysicsPreviewAdapter() {
		return new AssetModelPreviewFactory() {

			@Override
			public void updateControl2(Control preview, Object element) {
				((PhysicsAssetPreviewComp) preview).setModel((PhysicsAssetModel) element);
			}

			@Override
			public void selectInControl(Control preview, Object element) {
				((PhysicsAssetPreviewComp) preview).selectElement(element);
			}

			@Override
			public Control createControl(Composite previewContainer) {
				return new PhysicsAssetPreviewComp(previewContainer, SWT.NONE);
			}

			@Override
			public boolean canReusePreviewControl(Control c, Object elem) {
				return c instanceof PhysicsAssetPreviewComp;
			}

		};
	}

	private static IPreviewFactory createBitmapFontPreviewAdapter() {
		return new AssetModelPreviewFactory() {

			@Override
			public void updateControl2(Control preview, Object element) {
				((BitmapFontAssetPreviewComp) preview).setModel((BitmapFontAssetModel) element);
			}

			@Override
			public Control createControl(Composite previewContainer) {
				return new BitmapFontAssetPreviewComp(previewContainer, SWT.NONE);
			}

			@Override
			public boolean canReusePreviewControl(Control c, Object elem) {
				return c instanceof BitmapFontAssetPreviewComp;
			}

			@Override
			public void updateToolBar(IToolBarManager toolbar, Control preview) {
				((BitmapFontAssetPreviewComp) preview).createToolBar(toolbar);
			}
		};
	}

	private static IPreviewFactory createTilemapJSONPreviewAdapter() {
		return new AssetModelPreviewFactory() {

			@Override
			public void updateControl2(Control preview, Object element) {
				((TilemapJSONAssetPreviewComp) preview).setModel((TilemapAssetModel) element);
			}

			@Override
			public void selectInControl(Control preview, Object element) {
				((TilemapJSONAssetPreviewComp) preview).selectElement(element);
			}

			@Override
			public Control createControl(Composite previewContainer) {
				return new TilemapJSONAssetPreviewComp(previewContainer, SWT.NONE);
			}

			@Override
			public boolean canReusePreviewControl(Control c, Object elem) {
				return c instanceof TilemapJSONAssetPreviewComp;
			}
		};
	}

	private static IPreviewFactory createTilemapCSVPreviewAdapter() {
		return new AssetModelPreviewFactory() {

			@Override
			public void updateControl2(Control preview, Object element) {
				((TilemapCSVAssetPreviewComp) preview).setModel((TilemapAssetModel) element);
			}

			@Override
			public void selectInControl(Control preview, Object element) {
				// nothing
			}

			@Override
			public Control createControl(Composite previewContainer) {
				return new TilemapCSVAssetPreviewComp(previewContainer, SWT.NONE);
			}

			@Override
			public boolean canReusePreviewControl(Control c, Object elem) {
				return c instanceof TilemapCSVAssetPreviewComp;
			}

			@Override
			public void updateToolBar(IToolBarManager toolbar, Control preview) {
				((TilemapCSVAssetPreviewComp) preview).createToolBar(toolbar);
			}

			@Override
			public void initPreviewControl(Control preview, IMemento memento) {
				((TilemapCSVAssetPreviewComp) preview).initState(memento);
			}

			@Override
			public void savePreviewControl(Control preview, IMemento memento) {
				((TilemapCSVAssetPreviewComp) preview).saveState(memento);
			}
		};
	}

	private static IPreviewFactory createAtlasFramePreviewAdapter() {
		return new AssetModelPreviewFactory() {

			@Override
			public void updateControl(Control preview, Object element) {
				var comp = (AtlasAssetFramePreviewComp) preview;
				IAssetFrameModel frame = (IAssetFrameModel) element;
				comp.setModel(frame);
			}

			@Override
			public Control createControl(Composite previewContainer) {
				return new AtlasAssetFramePreviewComp(previewContainer, SWT.NONE);
			}

			@Override
			public boolean canReusePreviewControl(Control c, Object elem) {
				return c instanceof AtlasAssetFramePreviewComp && elem instanceof IAssetFrameModel
						&& elem instanceof AtlasFrame;
			}

			@Override
			public void updateToolBar(IToolBarManager toolbar, Control preview) {
				((AtlasAssetFramePreviewComp) preview).createToolBar(toolbar);
			}
		};
	}

	private static IPreviewFactory createAtlasPreviewAdapter() {
		return new AssetModelPreviewFactory() {

			@Override
			public void updateControl(Control preview, Object element) {
				AtlasAssetPreviewComp comp = (AtlasAssetPreviewComp) preview;
				comp.setModel((AtlasAssetModel) element);
			}

			@Override
			public Control createControl(Composite previewContainer) {
				return new AtlasAssetPreviewComp(previewContainer, SWT.NONE);
			}

			@Override
			public boolean canReusePreviewControl(Control c, Object elem) {
				return c instanceof AtlasAssetPreviewComp && elem instanceof AtlasAssetModel;
			}

			@Override
			public void updateToolBar(IToolBarManager toolbar, Control preview) {
				((AtlasAssetPreviewComp) preview).fillToolBar(toolbar);
			}
		};
	}

	private static IPreviewFactory createMultiAtlasPreviewAdapter() {
		return new AssetModelPreviewFactory() {

			@Override
			public void updateControl(Control preview, Object element) {
				var comp = (MultiAtlasAssetPreviewComp) preview;
				comp.setModel((MultiAtlasAssetModel) element);
			}

			@Override
			public Control createControl(Composite previewContainer) {
				return new MultiAtlasAssetPreviewComp(previewContainer, SWT.NONE);
			}

			@Override
			public boolean canReusePreviewControl(Control c, Object elem) {
				return c instanceof MultiAtlasAssetPreviewComp && elem instanceof MultiAtlasAssetModel;
			}

			@Override
			public void updateToolBar(IToolBarManager toolbar, Control preview) {
				((MultiAtlasAssetPreviewComp) preview).fillToolBar(toolbar);
			}
		};
	}

	private static IPreviewFactory createAudioPreviewAdpter() {
		return new AssetModelPreviewFactory() {

			@Override
			public void updateControl2(Control preview, Object element) {
				GdxMusicControl comp = (GdxMusicControl) preview;
				AudioAssetModel model = (AudioAssetModel) element;
				IFile file = pickFileWithoutExtension(model.getFilesFromUrls(model.getUrls()), "mp3", "ogg");
				comp.load(file);
			}

			@Override
			public void hiddenControl(Control preview) {
				GdxMusicControl control = (GdxMusicControl) preview;
				control.stop();
				control.disposeMusic();
			}

			@Override
			public Control createControl(Composite previewContainer) {
				return new GdxMusicControl(previewContainer, SWT.NONE);
			}

			@Override
			public boolean canReusePreviewControl(Control c, Object elem) {
				return c instanceof GdxMusicControl;
			}
		};
	}

	private static IPreviewFactory createVideoPreviewAdpter() {
		return new AssetModelPreviewFactory() {

			@Override
			public void updateControl2(Control preview, Object element) {
				VideoPreviewComp comp = (VideoPreviewComp) preview;
				VideoAssetModel model = (VideoAssetModel) element;
				comp.setModel(model);
			}

			@Override
			public void hiddenControl(Control preview) {
				VideoPreviewComp comp = (VideoPreviewComp) preview;
				comp.setVideoFile(null);
			}

			@Override
			public Control createControl(Composite previewContainer) {
				return new VideoPreviewComp(previewContainer, SWT.NONE);
			}

			@Override
			public boolean canReusePreviewControl(Control c, Object elem) {
				return c instanceof VideoPreviewComp;
			}
		};
	}

	private static IPreviewFactory createAudioSpritePreviewAdpter() {
		return new AssetModelPreviewFactory() {

			@Override
			public void updateControl2(Control preview, Object element) {
				AudioSpriteAssetPreviewComp comp = (AudioSpriteAssetPreviewComp) preview;
				comp.setModel((AudioSpriteAssetModel) element);
			}

			@Override
			public void selectInControl(Control preview, Object element) {
				AudioSpriteAssetPreviewComp comp = (AudioSpriteAssetPreviewComp) preview;
				comp.selectElement(element);
			}

			@Override
			public void hiddenControl(Control preview) {
				AudioSpriteAssetPreviewComp comp = (AudioSpriteAssetPreviewComp) preview;
				comp.disposeMusicControl();
			}

			@Override
			public Control createControl(Composite previewContainer) {
				return new AudioSpriteAssetPreviewComp(previewContainer, SWT.NONE);
			}

			@Override
			public boolean canReusePreviewControl(Control c, Object elem) {
				return c instanceof AudioSpriteAssetPreviewComp;
			}
		};
	}

	private static IPreviewFactory createSpritesheetPreviewAdapter() {
		return new AssetModelPreviewFactory() {

			@Override
			public void updateControl2(Control preview, Object element) {
				SpritesheetAssetPreviewComp comp = (SpritesheetAssetPreviewComp) preview;
				SpritesheetAssetModel asset = (SpritesheetAssetModel) element;
				comp.setModel(asset);
			}

			@Override
			public void updateToolBar(IToolBarManager toolbar, Control preview) {
				SpritesheetAssetPreviewComp comp = (SpritesheetAssetPreviewComp) preview;
				comp.fillToolBar(toolbar);
			}

			@Override
			public Control createControl(Composite previewContainer) {
				return new SpritesheetAssetPreviewComp(previewContainer, SWT.NONE);
			}

			@Override
			public boolean canReusePreviewControl(Control c, Object elem) {
				return c instanceof SpritesheetAssetPreviewComp && elem instanceof SpritesheetAssetModel;
			}

			@Override
			public void hiddenControl(Control preview) {
				//
			}
		};
	}

	private static IPreviewFactory createSpritesheetFramePreviewAdapter() {
		return new AssetModelPreviewFactory() {

			@Override
			public void updateControl(Control preview, Object element) {
				var comp = (SpritesheetFramePreviewComp) preview;
				SpritesheetAssetModel.FrameModel frame = (SpritesheetAssetModel.FrameModel) element;
				comp.setModel(frame);
			}

			@Override
			public Control createControl(Composite previewContainer) {
				return new SpritesheetFramePreviewComp(previewContainer, SWT.NONE);
			}

			@Override
			public boolean canReusePreviewControl(Control c, Object elem) {
				return c instanceof SpritesheetFramePreviewComp && elem instanceof SpritesheetAssetModel.FrameModel;
			}

			@Override
			public void updateToolBar(IToolBarManager toolbar, Control preview) {
				((SpritesheetFramePreviewComp) preview).createToolBar(toolbar);
			}

		};
	}

	private static IPreviewFactory createImagePreviewAdapter() {
		return new AssetModelPreviewFactory() {
			@Override
			public boolean canReusePreviewControl(Control c, Object elem) {
				return c instanceof ImageAssetPreviewComp;
			}

			@Override
			public Control createControl(Composite previewContainer) {
				return new ImageAssetPreviewComp(previewContainer, SWT.NONE);
			}

			@Override
			public void updateControl2(Control preview, Object element) {
				((ImageAssetPreviewComp) preview).setModel((ImageAssetModel) element);
			}

			@Override
			public void updateToolBar(IToolBarManager toolbar, Control preview) {
				((ImageAssetPreviewComp) preview).createToolBar(toolbar);
			}
		};
	}

	private static IPreviewFactory createAnimationPreviewAdapter() {
		return new AssetModelPreviewFactory() {
			private AnimationModelPreviewFactory _factory = new AnimationModelPreviewFactory();

			@Override
			public void updateControl(Control preview, Object element) {
				_factory.updateControl(preview, element);
			}

			@Override
			public Control createControl(Composite previewContainer) {
				return _factory.createControl(previewContainer);
			}

			@Override
			public boolean canReusePreviewControl(Control c, Object elem) {
				return _factory.canReusePreviewControl(c, elem);
			}

			@Override
			public void updateToolBar(IToolBarManager toolbar, Control preview) {
				_factory.updateToolBar(toolbar, preview);
			}

		};

	}

	@Override
	public Class<?>[] getAdapterList() {
		return ADAPTERS;
	}
}
